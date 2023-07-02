package com.tms.lib.interchange;

import com.tms.lib.exceptions.InterchangeConstructionException;
import com.tms.lib.exceptions.InterchangeServiceException;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.transactionrecord.service.TransactionRecordService;
import com.tms.lib.util.AsyncWorkerPool;
import org.springframework.context.ApplicationContext;

public interface Interchange {

    String getTypeName();

    String getName();

    InterchangeConfig getConfig();

    boolean setConfig(InterchangeConfig config) throws InterchangeConstructionException;

    void start() throws InterchangeServiceException;

    void stop() throws InterchangeServiceException;

    public boolean isStarted();

    TransactionResponse send(TransactionRequest channelRequest) throws InterchangeServiceException;

    InterchangeMode[] getSupportedModes();

    boolean isSingleton();

    Interchange construct(InterchangeConfig config,
                          AsyncWorkerPool workerPool,
                          ApplicationContext context,
                          TransactionRecordService transactionRecordService) throws InterchangeConstructionException;
}
