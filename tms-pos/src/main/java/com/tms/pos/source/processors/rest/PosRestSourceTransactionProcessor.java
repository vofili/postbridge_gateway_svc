package com.tms.pos.source.processors.rest;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;

public interface PosRestSourceTransactionProcessor {

    default boolean canTreat(TransactionRequest transactionRequest) {
        return false;
    }

    default TransactionResponse treat(TransactionRequest channelRequest) throws TransactionProcessingException {
        return null;
    }

    default boolean canConvert(TransactionRequest transactionRequest) {
        return false;
    }

    default TransactionRequest convert(TransactionRequest transactionRequest) throws TransactionProcessingException {
        return transactionRequest;
    }
}
