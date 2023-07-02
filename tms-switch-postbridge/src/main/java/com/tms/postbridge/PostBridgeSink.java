package com.tms.postbridge;

import com.tms.lib.events.NetworkConnectionEventListener;
import com.tms.lib.exceptions.InterchangeConstructionException;
import com.tms.lib.exceptions.ServiceRuntimeException;
import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.helper.ISOSinkNodeHelper;
import com.tms.lib.interchange.InterchangeConfig;
import com.tms.lib.interchange.SocketTypeInterchangeConfig;
import com.tms.lib.model.DefaultIsoResponseCodes;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.network.ConnectionProvider;
import com.tms.lib.network.NetworkConnection;
import com.tms.lib.network.SingleClientSocketChannel;
import com.tms.lib.network.io.TwoBytesLenSingleClientIOSocket;
import com.tms.lib.provider.StanProvider;
import com.tms.lib.transactionrecord.service.TransactionRecordService;
import com.tms.lib.util.AsyncWorkerPool;
import com.tms.postbridge.matchers.PostBridgeSolicitedMessageDifferentiator;
import com.tms.postbridge.processors.KeyExchangeProcessor;
import com.tms.postbridge.processors.PollingProcessor;
import com.tms.postbridge.processors.PostBridgeSinkTransactionProcessor;
import com.tms.postbridge.processors.SignOnProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISOMsg;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdScheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;


@Slf4j
public class PostBridgeSink implements NetworkConnectionEventListener, ConnectionProvider, Consumer<ISOMsg> {

    private Timer signOnTimer;
    private Timer keyExchangeTimer;
    private boolean networkReady = false;
    private boolean finishedSetup = false;
    private boolean isStarted = false;

    private InterchangeConfig interchangeConfig;
    private SocketTypeInterchangeConfig socketTypeInterchangeConfig;
    private AsyncWorkerPool asyncWorkerPool;
    private final Object KEY_EXCHANGE_LOCK = new Object();
    private AtomicBoolean isSignedUp = new AtomicBoolean(false);
    private ISOSinkNodeHelper sinkNodeHelper;
    private StanProvider stanProvider;
    private PostBridgeInterchange postBridgeInterchange;
    private AtomicLong messageCount = new AtomicLong(0);
    private AtomicBoolean isKeyExchanged = new AtomicBoolean(false);
    private Scheduler postbridgeScheduler;
    private JobDetail pollingJobDetail;
    private AtomicInteger pollingFailedAttemptCount = new AtomicInteger();
    private TwoBytesLenSingleClientIOSocket<ISOMsg> singleSocketClient;
    private PostBridgeResponseIsoTransciever postBridgeResponseIsoTransciever;

    private final Object reConnectionLock = new Object();
    private final int RECONNECTION_WAIT_PERIOD = 10 * 1000;
    private boolean waitingForReconnection = false;


    public PostBridgeSink(PostBridgeInterchange postBridgeInterchange,
                          TransactionRecordService transactionRecordService,
                          AsyncWorkerPool asyncWorkerPool,
                          ApplicationContext context,
                          StanProvider stanProvider,
                          List<PostBridgeSinkTransactionProcessor> sinkNodeMessageProcessorList) throws InterchangeConstructionException {
        this.interchangeConfig = postBridgeInterchange.getConfig();
        try {
            this.socketTypeInterchangeConfig = SocketTypeInterchangeConfig.getConfig(this.interchangeConfig.getInterchangeSpecificData());
        } catch (UtilOperationException e) {
            throw new InterchangeConstructionException("Could not get socket type interchange config for interchange", e);
        }
        this.asyncWorkerPool = asyncWorkerPool;
        this.stanProvider = stanProvider;
        this.postBridgeInterchange = postBridgeInterchange;

        PostBridgeSolicitedMessageDifferentiator solicitedResponseDifferentiator = new PostBridgeSolicitedMessageDifferentiator(postBridgeInterchange.getPackager());

        postbridgeScheduler = context.getBean("postbridgePollingScheduler", StdScheduler.class);
        pollingJobDetail = pollingJob().getObject();
        this.postBridgeResponseIsoTransciever =
                new PostBridgeResponseIsoTransciever(postBridgeInterchange,
                        this.socketTypeInterchangeConfig.getSocketTimeOut());
        this.postBridgeResponseIsoTransciever.setConnectionProvider(this);

        String portsList = socketTypeInterchangeConfig.getSinkPorts();
        if (portsList == null) {
            throw new ServiceRuntimeException("Cannot start postbridge sink on a null port");
        }
        int sinkPort;
        String[] sinkPortArray = portsList.split(",");
        try {
            sinkPort = Integer.parseInt(sinkPortArray[0]);
        } catch (NumberFormatException e) {
            throw new ServiceRuntimeException(String.format("Cannot convert port %s to int", sinkPortArray[0]), e);
        }
        this.singleSocketClient = new TwoBytesLenSingleClientIOSocket<>(
                socketTypeInterchangeConfig.getSinkHost(),
                sinkPort,
                asyncWorkerPool,
                this, this, solicitedResponseDifferentiator);

        this.sinkNodeHelper = new ISOSinkNodeHelper(new ArrayList<>(sinkNodeMessageProcessorList), postBridgeResponseIsoTransciever, postBridgeInterchange,
                context, this.asyncWorkerPool, transactionRecordService);
    }

    public void start() {
        log.info("Starting postbridge sink");
        isStarted = true;
        singleSocketClient.start();
    }

    public void stop() {
        isStarted = false;
        singleSocketClient.stop();
        stopTimerTasks();
        clearStatusFlags();
    }

    public TransactionResponse send(TransactionRequest request) throws TransactionProcessingException {
        waitForReConnectionIfNeeded();

        if (request == null) {
            throw new TransactionProcessingException("Message to send by channel is null");
        }
        TransactionResponse response = new TransactionResponse();
        if (!finishedSetup) {
            response.setIsoResponseCode(DefaultIsoResponseCodes.IssuerOrSwitchInOperative);
            response.setResponseInterchange(postBridgeInterchange);
            return response;
        }
        synchronized (KEY_EXCHANGE_LOCK) {
            if ((interchangeConfig.isPinTranslationRequired() && !isKeyExchanged()) || !isSignedUp()) {
                response.setIsoResponseCode(DefaultIsoResponseCodes.IssuerOrSwitchInOperative);
                response.setResponseInterchange(postBridgeInterchange);
                return response;
            }
        }
        return sendPostbridgeMessage(request);
    }

    private void waitForReConnectionIfNeeded() {
        if (waitingForReconnection) {
            synchronized (reConnectionLock) {
                if (waitingForReconnection) {
                    try {
                        reConnectionLock.wait(RECONNECTION_WAIT_PERIOD);
                    } catch (InterruptedException e) {
                        log.warn("There was an error waiting on glitch lock", e);
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private TransactionResponse sendPostbridgeMessage(TransactionRequest request) throws TransactionProcessingException {
        return sinkNodeHelper.send(request);
    }

    private void processPostBridgeResponse(TransactionResponse response) throws TransactionProcessingException {
        sinkNodeHelper.process(response);
    }

    public void adviceSignup() {
        asyncWorkerPool.queueJob(() -> {
            doSignOn();
            return null;
        });
    }

    private void doSignOn() {
        log.info("Postbridge sign-on initiated");
        signOnTimer = new Timer("sign-on-timer");
        final AtomicInteger signOnFailedAttemptCount = new AtomicInteger();
        signOnTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    synchronized (KEY_EXCHANGE_LOCK) {
                        if (isSignedUp()) {
                            signOnTimer.cancel();
                            return;
                        }
                        if (signOnFailedAttemptCount.intValue() >= 5) {
                            resetSink();
                            return;
                        }
                        TransactionRequest channelRequest = SignOnProcessor.createSignOnMessage(stanProvider);
                        TransactionResponse channelResponse;
                        try {
                            channelResponse = sendPostbridgeMessage(channelRequest);
                        } catch (TransactionProcessingException e) {
                            log.error("Could not send sign on request", e);
                            return;
                        }
                        if (StringUtils.isEmpty(channelResponse.getIsoResponseCode()) || !channelResponse.getIsoResponseCode().equals("00")) {
                            signOnFailedAttemptCount.incrementAndGet();
                            return;
                        }
                        signOnTimer.cancel();
                        isSignedUp.set(true);
                        if (interchangeConfig.isPinTranslationRequired()) {
                            doKeyExchange();
                        } else {
                            finishedSetup = true;
                            postbridgeScheduler.scheduleJob(pollingJobDetail, pollingTrigger());
                        }
                    }
                } catch (Exception e) {
                    log.error("Postbridge sign-on error", e);
                }
            }
        }, 0, 20 * 1000);

    }

    private void doKeyExchange() {
        keyExchangeTimer = new Timer("key-exchange-timer");
        final AtomicInteger keyExchangeFailedAttemptCount = new AtomicInteger();
        keyExchangeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    synchronized (KEY_EXCHANGE_LOCK) {
                        if (isKeyExchanged()) {
                            keyExchangeTimer.cancel();
                            finishedSetup = true;
                            postbridgeScheduler.scheduleJob(pollingJobDetail, pollingTrigger());
                            return;
                        }
                        if (keyExchangeFailedAttemptCount.intValue() >= 5) {
                            resetSink();
                            return;
                        }

                        TransactionRequest channelRequest = KeyExchangeProcessor.createKeyExchangeMessage(stanProvider);
                        TransactionResponse channelResponse;
                        try {
                            channelResponse = sendPostbridgeMessage(channelRequest);
                        } catch (TransactionProcessingException e) {
                            log.error("Could not send key exchange message", e);
                            return;
                        }
                        if (DefaultIsoResponseCodes.IssuerOrSwitchInOperative.toString().equals(channelResponse.getIsoResponseCode())) {
                            keyExchangeFailedAttemptCount.incrementAndGet();
                            return;
                        }
                        if (!"00".equals(channelResponse.getIsoResponseCode())) {
                            return;
                        }
                        try {
                            processPostBridgeResponse(channelResponse);
                        } catch (Exception e) {
                            keyExchangeFailedAttemptCount.incrementAndGet();
                            log.error("Could not process key exchange response", e);
                            return;
                        }
                        keyExchangeTimer.cancel();
                        setIsKeyExchanged(true);
                        finishedSetup = true;
                        postbridgeScheduler.scheduleJob(pollingJobDetail, pollingTrigger());

                    }
                } catch (Exception e) {
                    log.error("Postbridge key exchange error", e);
                }

            }
        }, 0, 20 * 1000);
    }

    private MethodInvokingJobDetailFactoryBean pollingJob() {
        MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();
        bean.setTargetObject(this);
        bean.setTargetMethod("doPolling");
        bean.setName(interchangeConfig.getName() + "-Polling-Job");
        try {
            bean.afterPropertiesSet();
        } catch (Exception e) {
            log.error("Could not create polling job", e);
        }
        return bean;
    }

    public SimpleTrigger pollingTrigger() {
        return newTrigger().forJob(pollingJobDetail)
                .withIdentity(interchangeConfig.getName(), interchangeConfig.getName() + "Group")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInMilliseconds(this.socketTypeInterchangeConfig.getPollingInterval()).repeatForever()).build();
    }

    private void doPolling() {
        try {
            if (pollingFailedAttemptCount.intValue() >= 5) {
                resetSink();
                return;
            }

            synchronized (KEY_EXCHANGE_LOCK) {
                TransactionRequest channelRequest = PollingProcessor.createPollingMessage(stanProvider);
                TransactionResponse transactionResponse;
                try {
                    transactionResponse = sendPostbridgeMessage(channelRequest);
                } catch (TransactionProcessingException e) {
                    log.error("Could not process polling message", e);
                    return;
                }
                if (StringUtils.isEmpty(transactionResponse.getIsoResponseCode()) || !transactionResponse.getIsoResponseCode().equals("00")) {
                    pollingFailedAttemptCount.incrementAndGet();
                    return;
                }

                if (waitingForReconnection) {
                    notifyNetworkReconnectionWaitEnd();
                }

            }
        } catch (Exception e) {
            log.error("Postbridge polling error", e);
        }
    }

    public boolean isKeyExchanged() {
        return isKeyExchanged.get();
    }

    public void setIsKeyExchanged(boolean isKeyExchanged) {
        this.isKeyExchanged.set(isKeyExchanged);
    }

    public boolean isSignedUp() {
        return isSignedUp.get();
    }

    public boolean isStarted() {
        return (networkReady && finishedSetup) || (waitingForReconnection && !networkReady);
    }

    @Override
    public void notifyConnect(NetworkConnection networkConnection) {
        log.info(String.format("Postbridge sink %s Notified of connection event", interchangeConfig.getName()));
        clearStatusFlags();
        stopTimerTasks();
        //isSignedUp.set(true);
        adviceSignup();
//        finishedSetup = true;
//        try {
//            postbridgeScheduler.scheduleJob(pollingJobDetail, pollingTrigger());
//        }catch(Exception s){
//            log.error("Could not schedule polling job - ",s);
//        }
//        networkReady = true;
    }

    private void notifyNetworkReconnectionWaitEnd() {
        waitingForReconnection = false;
        synchronized (reConnectionLock) {
            reConnectionLock.notifyAll();
        }
    }

    @Override
    public void notifyDisconnect(NetworkConnection networkConnection) {
        log.info(String.format("Postbridge sink %s Notified of disconnection event", interchangeConfig.getName()));
        stopTimerTasks();
        clearStatusFlags();
        networkReady = false;
        if (isStarted) {
            asyncWorkerPool.queueJob(() -> {
                waitForNetworkRestore();
                return null;
            });
        }
    }

    private void waitForNetworkRestore() {
        synchronized (reConnectionLock) {
            try {
                waitingForReconnection = true;
                reConnectionLock.wait(RECONNECTION_WAIT_PERIOD);
            } catch (InterruptedException e) {
                log.error( "There was an error waiting on glitch lock", e);
                Thread.currentThread().interrupt();
            }
        }
        if (waitingForReconnection) {
            notifyNetworkReconnectionWaitEnd();
        } else {
            log.info("There was a network glitch, but it was recovered from");
        }
    }


    @Override
    public List<NetworkConnection> getNetworkConnections() {
        return Collections.singletonList(singleSocketClient.getCurrentNetworkConnection());
    }

    private void stopTimerTasks() {
        log.info("Stopping timer tasks");
        if (signOnTimer != null) {
            signOnTimer.cancel();
            signOnTimer = null;
        }
        if (keyExchangeTimer != null) {
            keyExchangeTimer.cancel();
            keyExchangeTimer = null;
        }
        try {
            postbridgeScheduler.deleteJob(pollingJobDetail.getKey());
        } catch (Exception e) {
            log.error("Could not delete polling job", e);
        }
    }

    private void clearStatusFlags() {
        log.info("Clearing status flags");
        isSignedUp.set(false);
        finishedSetup = false;
        messageCount = new AtomicLong(0);
        pollingFailedAttemptCount = new AtomicInteger(0);
    }

    private void resetSink() {
        log.info("Resetting postbridge sink");
        clearStatusFlags();
        stopTimerTasks();
        singleSocketClient.reset();
    }

    @Override
    public SingleClientSocketChannel getSocketConnection() {
        return singleSocketClient;
    }

    @Override
    public void accept(ISOMsg isoMsg) {
        postBridgeResponseIsoTransciever.notifyResponseMessage(isoMsg);
    }
}
