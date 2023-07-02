package com.tms.pos;


import com.tms.lib.exceptions.InterchangeConstructionException;
import com.tms.lib.exceptions.InterchangeServiceException;
import com.tms.lib.interchange.Interchange;
import com.tms.lib.interchange.InterchangeConfig;
import com.tms.lib.interchange.InterchangeMode;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.transactionrecord.service.TransactionRecordService;
import com.tms.lib.util.AsyncWorkerPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import static com.tms.lib.interchange.InterchangeMode.SinkMode;
import static com.tms.lib.interchange.InterchangeMode.SourceMode;

@Slf4j
@Component
public class PosRestInterchange implements Interchange {

    public static final String POS_TYPE_NAME = "POS_REST";
    private InterchangeConfig config;
    private boolean isStarted;

    public PosRestInterchange() {

    }

    private PosRestInterchange(InterchangeConfig config) throws InterchangeConstructionException {
        this.config = config;
        setConfig(config);
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
        this.config = config;
        return false;
    }

    @Override
    public void start() throws InterchangeServiceException {
        if (config.getInterchangeMode() == SourceMode) {
            isStarted = true;
        } else {
            throw new InterchangeServiceException("Cannot start pos tms interchange with an unknown mode");
        }
    }

    @Override
    public void stop() throws InterchangeServiceException {
        if (config.getInterchangeMode() == SourceMode) {
            isStarted = false;
        } else {
            throw new InterchangeServiceException("Cannot stop pos tms interchange with an unknown mode");
        }
    }

    @Override
    public boolean isStarted() {
        if (config.getInterchangeMode() == SourceMode) {
            return isStarted;
        }
        return false;
    }

    @Override
    public TransactionResponse send(TransactionRequest transactionRequest) throws InterchangeServiceException {
        throw new InterchangeServiceException("Unsupported Operation!");
    }

    @Override
    public InterchangeMode[] getSupportedModes() {
        return new InterchangeMode[]{SourceMode, SinkMode};
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public Interchange construct(InterchangeConfig config, AsyncWorkerPool workerPool, ApplicationContext context, TransactionRecordService transactionRecordService) throws InterchangeConstructionException {
        log.info("Constructing pos rest tms interchange");
        return new PosRestInterchange(config);
    }
}
