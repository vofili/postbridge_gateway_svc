package com.tms.pos.source.processors;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.util.IsoUtil;
import com.tms.pos.source.PosSourceIsoChannelAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CallHomeSourceTransactionProcessor implements PosSourceTransactionProcessor {

    @Override
    public boolean canTreat(ISOMsg isoMsg) {
        try {
            String processingCode = isoMsg.getString(3);
            String mti = isoMsg.getMTI();
            return processingCode != null && "0800".equals(mti) && processingCode.startsWith("9D");
        } catch (ISOException e) {
            log.error("Could not get mti", e);
            return false;
        }
    }

    @Override
    public String getResponseMti() {
        return "0810";
    }

    @Override
    public boolean canConvert(ISOMsg isoMsg) {
        return canTreat(isoMsg);
    }

    @Override
    public TransactionRequest toTransactionRequest(ISOMsg isoMsg) throws TransactionProcessingException {
        log.trace("Creating a call home Request");
        TransactionRequest callHomeTransactionRequest = new TransactionRequest();
        try {
            PosSourceIsoChannelAdapter.commonIsoMsgToTransactionRequest(isoMsg, callHomeTransactionRequest);
            return callHomeTransactionRequest;
        } catch (ISOException | UtilOperationException e) {
            String msg = "Could not convert call home request";
            log.error(msg, e);
            throw new TransactionProcessingException(msg, e);
        }
    }

    @Override
    public Pair<TransactionResponse, ISOMsg> treat(ISOMsg isoMsg, TransactionRequest transactionRequest) throws TransactionProcessingException {
        TransactionResponse callHomeChannelResponse = new TransactionResponse(transactionRequest);
        ISOMsg responseIso = new ISOMsg();
        try {
            IsoUtil.copyFields(isoMsg, responseIso);
            IsoUtil.setMti(responseIso, getResponseMti());

            String field62 = isoMsg.getString(62);

            responseIso.unset(3);
            responseIso.unset(62);
            responseIso.unset(64);

            IsoUtil.setIsoField(responseIso, 39, "00");
            callHomeChannelResponse.setIsoResponseCode("00");

            //TODO: process and persist call home information as deemed fit by the team

            log.trace("Call home message processed successfully");
            return new ImmutablePair<>(callHomeChannelResponse, responseIso);
        } catch (Exception e) {
            throw new TransactionProcessingException("Could not process call home", e);
        }
    }


}
