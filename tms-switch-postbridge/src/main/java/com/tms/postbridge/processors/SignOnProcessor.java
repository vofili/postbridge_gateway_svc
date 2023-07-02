package com.tms.postbridge.processors;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.RequestType;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.provider.StanProvider;
import com.tms.postbridge.util.PostBridgeSinkIsoChannelAdapter;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class SignOnProcessor implements PostBridgeSinkTransactionProcessor {


    public static TransactionRequest createSignOnMessage(StanProvider stanProvider) {
        Date now = new Date();

        return TransactionRequest.builder()
                .requestType(RequestType.SIGN_ON)
                .stan(stanProvider.getNextStan())
                .transactionTime(ISODate.getTime(now))
                .transactionDate(ISODate.getDate(now))
                .transmissionDateTime(ISODate.getDateTime(now))
                .mti("0800")
                .build();
    }

    @Override
    public boolean canConvert(RequestType requestType) {
        return RequestType.SIGN_ON.equals(requestType);
    }

    @Override
    public ISOMsg toISOMsg(TransactionRequest transactionRequest) throws TransactionProcessingException {
        ISOMsg isoMsg = new ISOMsg();

        try {
            isoMsg.setMTI("0800");
            isoMsg.set(70, "001");
            PostBridgeSinkIsoChannelAdapter.transactionRequestToCommonIsoMsg(transactionRequest, isoMsg);
        } catch (UtilOperationException | ISOException e) {
            String msg = String.format("There was a channel error converting postbridge sign on message ex: %s", e.toString());
            throw new TransactionProcessingException(msg, e);
        }

        return isoMsg;
    }

    @Override
    public TransactionResponse toTransactionResponse(ISOMsg isoMsg, TransactionRequest transactionRequest) throws TransactionProcessingException {
        if (isoMsg == null) {
            throw new TransactionProcessingException("Raw Response is null");
        }

        TransactionResponse transactionResponse = transactionRequest.constructResponse();
        try {
            PostBridgeSinkIsoChannelAdapter.commonIsoMsgToTransactionResponse(isoMsg, transactionResponse);
        } catch (UtilOperationException e) {
            throw new TransactionProcessingException("There was an ISO error while converting the ISO message ex: %s", e);
        }

        return transactionResponse;
    }
}
