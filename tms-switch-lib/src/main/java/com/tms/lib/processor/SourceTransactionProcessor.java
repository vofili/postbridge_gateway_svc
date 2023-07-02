package com.tms.lib.processor;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISOMsg;


public interface SourceTransactionProcessor {

    boolean canConvert(ISOMsg isoMsg);

    TransactionRequest toTransactionRequest(ISOMsg isoMsg) throws TransactionProcessingException;

    ISOMsg toISOMsg(TransactionResponse response) throws TransactionProcessingException;

    boolean canTreat(ISOMsg isoMsg);

    Pair<TransactionResponse, ISOMsg> treat(ISOMsg rawRequest, TransactionRequest transactionRequest) throws TransactionProcessingException;


}
