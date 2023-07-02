package com.tms.postbridge.processors;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.RequestType;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.postbridge.util.PostBridgeSinkIsoChannelAdapter;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

@Slf4j
public class PurchaseSinkProcessor implements PostBridgeSinkTransactionProcessor {

    @Override
    public boolean canConvert(RequestType requestType) {
        return RequestType.PURCHASE == requestType;
    }

    @Override
    public ISOMsg toISOMsg(TransactionRequest transactionRequest) throws TransactionProcessingException {
        log.trace("Sending a purchase request");
        ISOMsg isoMsg = new ISOMsg();

        try {
            isoMsg.setMTI("0200");
            isoMsg.set(3, transactionRequest.getProcessingCode());
            PostBridgeSinkIsoChannelAdapter.transactionRequestToCommonIsoMsg(transactionRequest, isoMsg);
        } catch (ISOException | UtilOperationException e) {
            String msg = String.format("There was a channel error converting postbridge purchase message ex: %s", e.toString());
            throw new TransactionProcessingException(msg, e);
        }

        return isoMsg;
    }

    @Override
    public TransactionResponse toTransactionResponse(ISOMsg isoMsg, TransactionRequest transactionRequest) throws TransactionProcessingException {
        if (isoMsg == null) {
            String msg = "Raw Response is null";
            throw new TransactionProcessingException(msg);
        }

        TransactionResponse transactionResponse = new TransactionResponse(transactionRequest);
        try {
            PostBridgeSinkIsoChannelAdapter.commonIsoMsgToTransactionResponse(isoMsg, transactionResponse);
        } catch (UtilOperationException e) {
            throw new TransactionProcessingException("There was an ISO error while converting the ISO message ex: %s", e);
        }

        return transactionResponse;
    }
}
