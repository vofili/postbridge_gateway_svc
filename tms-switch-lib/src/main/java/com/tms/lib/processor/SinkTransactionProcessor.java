package com.tms.lib.processor;

import com.tms.lib.exceptions.ReversalProcessingException;
import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.interchange.Interchange;
import com.tms.lib.model.RequestType;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import org.jpos.iso.ISOMsg;

public interface SinkTransactionProcessor {

    boolean canConvert(RequestType requestType);

    ISOMsg toISOMsg(TransactionRequest transactionRequest) throws TransactionProcessingException;

    TransactionResponse toTransactionResponse(ISOMsg isoMsg, TransactionRequest transactionRequest) throws TransactionProcessingException;

    default boolean canProcess(RequestType requestType) {
        return false;
    }

    default void process(TransactionResponse transactionResponse, Interchange interchange) throws TransactionProcessingException {
    }

    default boolean canReverse(RequestType requestType) {
        return false;
    }

    default TransactionRequest toReversalRequest(TransactionRequest transactionRequest, Interchange interchange) throws ReversalProcessingException {
        return transactionRequest;
    }

    default ISOMsg toRawReversalRequestRepeat(ISOMsg isoMsg) {
        return isoMsg;
    }

    default String getReversalAdviceMti() {
        return "0420";
    }

    default String getReversalAdviceRepeatMti() {
        return "0421";
    }

}
