package com.tms.pos.source.processors;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.RequestType;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.pos.source.PosSourceIsoChannelAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PurchaseSourceTransactionProcessor implements PosSourceTransactionProcessor {

    @Override
    public String getResponseMti() {
        return "0210";
    }

    @Override
    public boolean canConvert(ISOMsg isoMsg) {
        try {
            String mti = isoMsg.getMTI();
            String field3 = isoMsg.getString(3);
            if (StringUtils.isEmpty(field3) || field3.length() != 6) {
                return false;
            }
            return ("0200".equals(mti) && field3.startsWith("00"));
        } catch (ISOException e) {
            log.error("Could not get mti", e);
            return false;
        }
    }

    @Override
    public TransactionRequest toTransactionRequest(ISOMsg isoMsg) throws TransactionProcessingException {
        log.trace("Matching a purchase request");

        TransactionRequest purchaseChannelRequest = new TransactionRequest();
        purchaseChannelRequest.setRequestType(RequestType.PURCHASE);
        try {
            PosSourceIsoChannelAdapter.commonIsoMsgToTransactionRequest(isoMsg, purchaseChannelRequest);
        } catch (ISOException | UtilOperationException e) {
            String msg = "There was an ISO error converting pos purchase request message ";
            throw new TransactionProcessingException(msg, e);
        }
        return purchaseChannelRequest;
    }

    @Override
    public ISOMsg toISOMsg(TransactionResponse response) throws TransactionProcessingException {
        if (response == null) {
            String msg = "Channel Response is null";
            log.trace(msg);
            throw new TransactionProcessingException(msg);
        }


        ISOMsg isoResponse = new ISOMsg();
        try {
            isoResponse.setMTI(getResponseMti());
            isoResponse.set(39, response.getIsoResponseCode());
            String processingCode = response.getOriginalRequest().getProcessingCode();
            isoResponse.set(3, processingCode);
            PosSourceIsoChannelAdapter.transactionResponseToCommonIsoMsg(response, isoResponse);
        } catch (ISOException | UtilOperationException e) {
            String msg = "Error processing response iso";
            throw new TransactionProcessingException(msg, e);
        }
        return isoResponse;

    }
}
