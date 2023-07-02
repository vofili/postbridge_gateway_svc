package com.tms.pos.source.processors.rest;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.model.RequestType;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.util.IsoUtil;
import com.tms.pos.utils.POSMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class PurchaseRestSourceTransactionProcessor implements PosRestSourceTransactionProcessor {

    @Value("${tms.posConditionCode:00}")
    private String posConditionCode;
    @Value("${tms.posEntryMode:051}")
    private String posEntryMode;
    @Value("${tms.pinCaptureCode:06}")
    private String pinCaptureCode;
    @Value("${tms.posDataCode:510101513344101}")
    private String posDataCode;

    @Override
    public boolean canConvert(TransactionRequest transactionRequest) {
        return POSMessageUtils.isPurchaseMessage(transactionRequest.getProcessingCode());
    }

    @Override
    public TransactionRequest convert(TransactionRequest transactionRequest) throws TransactionProcessingException {
        log.trace("Matching a purchase request");

        try {
            transactionRequest.setPosConditionCode(posConditionCode);
            transactionRequest.setPosEntryMode(posEntryMode);
            transactionRequest.setPosDataCode(posDataCode);
            transactionRequest.setPan(extractPan(transactionRequest.getTrack2Data()));
            transactionRequest.setServiceRestrictionCode(extractServiceRestrictionCode(transactionRequest.getTrack2Data()));
            transactionRequest.setEmvData(IsoUtil.extractEmvData(transactionRequest.getEmvDataString()));

            Date now = new Date();
            transactionRequest.setTransmissionDateTime(IsoUtil.transmissionDateAndTime(now));
            transactionRequest.setTransactionTime(IsoUtil.timeLocalTransaction(now));
            transactionRequest.setTransactionDate(IsoUtil.dateLocalTransaction(now));

            transactionRequest.setRequestType(RequestType.PURCHASE);

            return transactionRequest;
        } catch (Exception e) {
            throw new TransactionProcessingException("Could not convert transaction request " + e.getMessage(), e);
        }
    }

    private String extractPan(String track2) {
        char separator = ((track2.indexOf('=') != -1) ? '=' : 'D');
        int separatorIndex = track2.indexOf(separator);
        return track2.substring(0, separatorIndex);
    }

    private String extractServiceRestrictionCode(String track2) {
        char separator = ((track2.indexOf('=') != -1) ? '=' : 'D');
        int separatorIndex = track2.indexOf(separator);
        if (separatorIndex + 8 <= track2.length()) {
            return track2.substring(separatorIndex + 5, separatorIndex + 8);
        }
        return "221";
    }
}
