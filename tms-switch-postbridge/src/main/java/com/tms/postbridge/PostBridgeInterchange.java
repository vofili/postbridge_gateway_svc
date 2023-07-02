package com.tms.postbridge;

import com.tms.lib.exceptions.InterchangeConstructionException;
import com.tms.lib.exceptions.InterchangeServiceException;
import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.interchange.Interchange;
import com.tms.lib.interchange.InterchangeConfig;
import com.tms.lib.interchange.InterchangeMode;
import com.tms.lib.interchange.SocketTypeInterchangeConfig;
import com.tms.lib.model.IccData;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.provider.StanProvider;
import com.tms.lib.transactionrecord.service.TransactionRecordService;
import com.tms.lib.util.AsyncWorkerPool;
import com.tms.postbridge.model.PostBridgeUserParameters;
import com.tms.postbridge.processors.PostBridgeSinkTransactionProcessor;
import com.tms.postbridge.util.PostBridgeIsoUtil;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOPackager;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.List;
import java.util.stream.Collectors;

import static com.tms.lib.interchange.InterchangeMode.SinkMode;

@Slf4j
@Component
public class    PostBridgeInterchange implements Interchange {

    public static final String POSTBRIDGE_TYPE_NAME = "Postbridge";
    private SocketTypeInterchangeConfig socketTypeInterchangeConfig;
    private ISOPackager POSTBRIDGE_PACKAGER;
    private InterchangeConfig config;
    private PostBridgeUserParameters postBridgeUserParameters;
    private PostBridgeSink sink;

    public PostBridgeInterchange() {

    }


    private PostBridgeInterchange(InterchangeConfig config,
                                  AsyncWorkerPool workerPool,
                                  StanProvider stanProvider,
                                  TransactionRecordService transactionRecordService,
                                  ApplicationContext applicationContext) throws InterchangeConstructionException {
        this.config = config;
        setConfig(config);

        POSTBRIDGE_PACKAGER = new  PostBridgePackagerBBBitmap();
        List<PostBridgeSinkTransactionProcessor> sinkNodeMessageProcessors = applicationContext.getBeansOfType(PostBridgeSinkTransactionProcessor.class)
                .values()
                .stream()
                .collect(Collectors.toList());

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(IccData.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            PostBridgeIsoUtil.setMarshaller(marshaller);
            PostBridgeIsoUtil.setUnmarshaller(unmarshaller);
        } catch (JAXBException e) {
            throw new InterchangeConstructionException(String.format("Could not create jaxb context while creating interchange %s", config.getName()), e);
        }

        sink = new PostBridgeSink(this, transactionRecordService, workerPool, applicationContext, stanProvider, sinkNodeMessageProcessors);
    }

    @Override
    public String getTypeName() {
        return POSTBRIDGE_TYPE_NAME;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public PostBridgeInterchange construct(InterchangeConfig config,
                                           AsyncWorkerPool workerPool,
                                           ApplicationContext context,
                                           TransactionRecordService transactionRecordService) throws InterchangeConstructionException {

        StanProvider stanProvider = context.getBean(StanProvider.class);
        log.info("Constructing postbridge interchange");
        return new PostBridgeInterchange(config, workerPool, stanProvider, transactionRecordService, context);
    }

    @Override
    public InterchangeConfig getConfig() {
        return config;
    }

    @Override
    public boolean setConfig(InterchangeConfig config) throws InterchangeConstructionException {
        try {
            SocketTypeInterchangeConfig newSocketTypeInterchangeConfig = SocketTypeInterchangeConfig.getConfig(config.getInterchangeSpecificData());
            PostBridgeUserParameters postBridgeUserParameters = PostBridgeUserParameters.getUserParameters(config.getInterchangeSpecificData());
            boolean isRestartNeeded = newSocketTypeInterchangeConfig.isRestartRequired(this.socketTypeInterchangeConfig, SinkMode)
                    || config.isRestartRequired(this.config) || postBridgeUserParameters.isRestartRequired(this.postBridgeUserParameters);
            this.config = config;
            this.socketTypeInterchangeConfig = newSocketTypeInterchangeConfig;
            this.postBridgeUserParameters = postBridgeUserParameters;
            this.config.setSocketTypeInterchangeConfig(newSocketTypeInterchangeConfig);
            return isRestartNeeded;
        } catch (Exception e) {
            throw new InterchangeConstructionException(String.format("Could not set config for interchange %s", config.getName()), e);
        }
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public void start() throws InterchangeServiceException {
        if (sink != null) {
            sink.start();
            return;
        }

        throw new InterchangeServiceException("Cannot start postbridge interchange with a null sink");
    }

    @Override
    public void stop() throws InterchangeServiceException {
        if (sink != null) {
            sink.stop();
            return;
        }
        throw new InterchangeServiceException("Cannot stop postbridge interchange with a null sink");
    }

    @Override
    public boolean isStarted() {
        return sink.isStarted();
    }

    @Override
    public InterchangeMode[] getSupportedModes() {
        return new InterchangeMode[]{SinkMode};
    }

    @Override
    public TransactionResponse send(TransactionRequest transactionRequest) throws InterchangeServiceException {
        if (sink != null) {
            try {
                return sink.send(transactionRequest);
            } catch (TransactionProcessingException e) {
                throw new InterchangeServiceException("There was an error sending transaction", e);
            }
        }
        throw new InterchangeServiceException("Postbridge sink is null");
    }

    public ISOPackager getPackager() {
        return POSTBRIDGE_PACKAGER;
    }

    public PostBridgeUserParameters getPostBridgeUserParameters(){
        return this.postBridgeUserParameters;
    }

}
