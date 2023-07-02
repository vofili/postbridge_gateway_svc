package com.tms.pos;


import com.tms.lib.exceptions.InterchangeConstructionException;
import com.tms.lib.exceptions.InterchangeServiceException;
import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.interchange.Interchange;
import com.tms.lib.interchange.InterchangeConfig;
import com.tms.lib.interchange.InterchangeMode;
import com.tms.lib.interchange.SocketTypeInterchangeConfig;
import com.tms.lib.matcher.SolicitedMessageDifferentiator;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.network.AsyncSocketServer;
import com.tms.lib.network.transciever.TranscieveFunction;
import com.tms.lib.processor.SourceTransactionProcessor;
import com.tms.lib.router.Router;
import com.tms.lib.transactionrecord.service.TransactionRecordService;
import com.tms.lib.util.AsyncWorkerPool;
import com.tms.lib.util.LogHelper;
import com.tms.pos.sink.generic.NibssSinkMessageTranslator;
import com.tms.pos.sink.generic.PosTmsInterchangeClientManager;
import com.tms.pos.source.processors.PosSourceTransactionProcessor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.tms.lib.interchange.InterchangeMode.SinkMode;
import static com.tms.lib.interchange.InterchangeMode.SourceMode;

@Slf4j
@Component
public class PosInterchange implements Interchange {

    private static final String POS_TYPE_NAME = "POSTMS";
    private InterchangeConfig config;
    private SocketTypeInterchangeConfig socketTypeInterchangeConfig;
    private AsyncSocketServer<ISOMsg> source;
    private PosTmsInterchangeClientManager sink;
    private boolean isStarted;

    public PosInterchange() {

    }

    private PosInterchange(InterchangeConfig config,
                           AsyncWorkerPool workerPool,
                           TransactionRecordService transactionRecordService,
                           ApplicationContext applicationContext, Router router) throws InterchangeConstructionException {
        this.config = config;
        setConfig(config);

        TranscieveFunction<ISOMsg, byte[]> transcieveFunction;
        SolicitedMessageDifferentiator<ISOMsg> solicitedMessageDifferentiator;

        List<SourceTransactionProcessor> sourceNodeMessageProcessors = new ArrayList<>(applicationContext.getBeansOfType(PosSourceTransactionProcessor.class)
                .values());

        transcieveFunction = new PosTransciever(sourceNodeMessageProcessors, transactionRecordService, this, router, applicationContext);
        solicitedMessageDifferentiator = new PosSolicitedMessageDifferentiator(getPackager());

        source = new AsyncSocketServer<>(this.socketTypeInterchangeConfig.getSourceHost(), this.socketTypeInterchangeConfig.getSourcePort(),
                transcieveFunction, solicitedMessageDifferentiator, null, workerPool);

        NibssSinkMessageTranslator nibssSinkMessageTranslator = applicationContext.getBean(NibssSinkMessageTranslator.class);
        sink = new PosTmsInterchangeClientManager(this, nibssSinkMessageTranslator, getPackager());
    }


    @Override
    public String getTypeName() {
        return POS_TYPE_NAME;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public InterchangeConfig getConfig() {
        return config;
    }

    @Override
    public boolean setConfig(InterchangeConfig config) throws InterchangeConstructionException {
        SocketTypeInterchangeConfig newSocketTypeInterchangeConfig;
        try {
            newSocketTypeInterchangeConfig = SocketTypeInterchangeConfig.getConfig(config.getInterchangeSpecificData());
        } catch (UtilOperationException e) {
            throw new InterchangeConstructionException(e);
        }
        boolean isRestartNeeded = newSocketTypeInterchangeConfig.isRestartRequired(this.socketTypeInterchangeConfig, SourceMode)
                || config.isRestartRequired(this.config);
        this.config = config;
        this.socketTypeInterchangeConfig = newSocketTypeInterchangeConfig;
        this.config.setSocketTypeInterchangeConfig(newSocketTypeInterchangeConfig);
        return isRestartNeeded;
    }

    @Override
    public void start() throws InterchangeServiceException {
        switch (config.getInterchangeMode()) {
            case SourceMode:
                try {
                    source.start();
                } catch (IOException e) {
                    throw new InterchangeServiceException("Unable to start source interchange", e);
                }
                break;
            case SinkMode:
                break;
            default:
                throw new InterchangeServiceException("Cannot start pos tms interchange with an unknown mode");
        }

        isStarted = true;
    }

    @Override
    public void stop() throws InterchangeServiceException {
        switch (config.getInterchangeMode()) {
            case SourceMode:
                try {
                    source.stop();
                } catch (IOException e) {
                    throw new InterchangeServiceException("Unable to stop source interchange", e);
                }
                break;
            case SinkMode:
                break;
            default:
                throw new InterchangeServiceException("Cannot stop pos tms interchange with an unknown mode");
        }

        isStarted = false;
    }

    @Override
    public boolean isStarted() {
        switch (config.getInterchangeMode()) {
            case SinkMode:
                return isStarted;
            case SourceMode:
                return isStarted && source.isStarted();
            default:
                return false;
        }
    }

    @Override
    public TransactionResponse send(TransactionRequest transactionRequest) throws InterchangeServiceException {
        try {
            log.info("transactionRequest ======== {}", LogHelper.dump(transactionRequest));
            return sink.send(transactionRequest);
        } catch (TransactionProcessingException e) {
            throw new InterchangeServiceException("Could not send transaction request", e);
        }
    }

    @Override
    public InterchangeMode[] getSupportedModes() {
        return new InterchangeMode[]{SourceMode, SinkMode};
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public Interchange construct(InterchangeConfig config, AsyncWorkerPool workerPool, ApplicationContext context, TransactionRecordService transactionRecordService) throws InterchangeConstructionException {
        log.info("Constructing pos tms interchange");
        Router router = context.getBean(Router.class);
        return new PosInterchange(config, workerPool, transactionRecordService, context, router);
    }

    public ISOPackager getPackager() {
        return new PosPackager();
    }
}
