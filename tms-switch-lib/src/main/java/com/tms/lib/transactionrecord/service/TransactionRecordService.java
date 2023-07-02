package com.tms.lib.transactionrecord.service;

import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.model.OriginalDataElements;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.transactionrecord.entities.TransactionRecord;

public interface TransactionRecordService {

    TransactionRecord save(TransactionRecord transactionRecord);

    TransactionRecord save(int configId, TransactionRequest transactionRequest) throws ServiceProcessingException;

    TransactionRecord update(TransactionRequest transactionRequest, TransactionResponse transactionResponse) throws ServiceProcessingException;

    TransactionRecord fetchMatchingTransaction(OriginalDataElements originalDataElements, long interchangeId) throws ServiceProcessingException;
}
