package com.tms.pos.sink;

import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.util.IsoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISOMsg;

import java.util.Date;

@Slf4j
public class PosSinkIsoChannelAdapter {

    private PosSinkIsoChannelAdapter(){

    }

    public static ISOMsg transactionRequestToISOMsg(TransactionRequest transactionRequest) throws UtilOperationException {

        ISOMsg isoMsg = new ISOMsg();

        try {
            isoMsg.setMTI("0200");
            isoMsg.set(2, transactionRequest.getPan());
            isoMsg.set(3, transactionRequest.getProcessingCode());
            isoMsg.set(4, String.format("%012d", transactionRequest.getMinorAmount()));

            isoMsg.set(7, transactionRequest.getTransmissionDateTime());

            isoMsg.set(11, transactionRequest.getStan());
            isoMsg.set(12, transactionRequest.getTransactionTime());
            isoMsg.set(13, transactionRequest.getTransactionDate());
            isoMsg.set(14, transactionRequest.getExpiryDate());
            isoMsg.set(15, transactionRequest.getSettlementDate());
            isoMsg.set(16, transactionRequest.getConversionDate());
            isoMsg.set(18, transactionRequest.getMerchantType());
            isoMsg.set(19, transactionRequest.getAcquiringInstitutionCountryCode());
            isoMsg.set(23, transactionRequest.getCardSequenceNumber());
            isoMsg.set(26, transactionRequest.getPinCaptureCode());
            isoMsg.set(24, transactionRequest.getInternationalNetworkIdentifier());
            isoMsg.set(28, transactionRequest.getTransactionFee());
            isoMsg.set(29, transactionRequest.getSettlementFee());
            isoMsg.set(30, transactionRequest.getTransactionProcessingFee());
            isoMsg.set(31, transactionRequest.getSettlementProcessingFee());
            isoMsg.set(32, transactionRequest.getAcquiringInstitutionId());
            isoMsg.set(33, transactionRequest.getForwardingInstitutionId());
            isoMsg.set(35, transactionRequest.getTrack2Data());
            isoMsg.set(37, String.format("%012d", Integer.parseInt(transactionRequest.getRrn())));
            isoMsg.set(40, transactionRequest.getServiceRestrictionCode());
            isoMsg.set(41, transactionRequest.getTerminalId());
            isoMsg.set(42, transactionRequest.getCardAcceptorId());
            isoMsg.set(43, transactionRequest.getCardAcceptorLocation());
            isoMsg.set(22, transactionRequest.getPosEntryMode());
            isoMsg.set(25, transactionRequest.getPosConditionCode());
            isoMsg.set(52, transactionRequest.getPinBlock());
            isoMsg.set(55, Hex.encodeHexString(IsoUtil.emvDataToIsoBytes(transactionRequest.getEmvData())).toUpperCase());
            isoMsg.set(49, transactionRequest.getTransactionCurrencyCode());
            isoMsg.set(54, IsoUtil.convertAdditionalAmountsToString(transactionRequest.getAdditionalAmounts()));
            isoMsg.set(59, transactionRequest.getEchoData());
            isoMsg.set(67, transactionRequest.getExtendedPaymentCode());
            isoMsg.set(90, IsoUtil.originalDataElementsToString(transactionRequest.getOriginalDataElements()));
            isoMsg.set(95, IsoUtil.replacementAmountsToString(transactionRequest.getReplacementAmounts()));
            isoMsg.set(98, transactionRequest.getPayee());
            isoMsg.set(100, transactionRequest.getReceivingInstitutionId());
            isoMsg.set(102, transactionRequest.getFromAccountIdentification());
            isoMsg.set(103, transactionRequest.getToAccountIdentification());
            isoMsg.set(123, transactionRequest.getPosDataCode());

            return isoMsg;
        } catch (Exception e) {
            throw new UtilOperationException("Cannot ", e);
        }
    }

    public static TransactionResponse commonIsoMsgToTransactionResponse(ISOMsg isoMsg, TransactionResponse transactionResponse) throws UtilOperationException {
        log.trace("Converting Iso Msg to transaction response");

        transactionResponse.setPan(isoMsg.getString(2));

        transactionResponse.setIsoResponseCode(isoMsg.getString(39));
        transactionResponse.setAuthId(isoMsg.getString(38));
        transactionResponse.setServerResponseTime(new Date());


        String field3 = isoMsg.getString(3);
        if (StringUtils.isNotEmpty(field3)) {
            if (field3.length() != 6) {
                String msg = String.format("Invalid field 3, should be six characters, %s", field3);
                log.trace(msg);
                throw new UtilOperationException(msg);
            }
            transactionResponse.setProcessingCode(field3);
        }
        transactionResponse.setTransmissionDateTime(isoMsg.getString(7));
        transactionResponse.setExpiryDate(isoMsg.getString(14));

        transactionResponse.setMinorAmount(IsoUtil.extractAmount(isoMsg.getString(4)));
        transactionResponse.setInternationalNetworkIdentifier(isoMsg.getString(24));
        transactionResponse.setAcquiringInstitutionCountryCode(isoMsg.getString(19));
        transactionResponse.setTerminalId(isoMsg.getString(41));
        transactionResponse.setCardAcceptorId(isoMsg.getString(42));
        transactionResponse.setCardAcceptorLocation(isoMsg.getString(43));
        transactionResponse.setPosEntryMode(isoMsg.getString(22));
        transactionResponse.setPosConditionCode(isoMsg.getString(25));
        transactionResponse.setPinCaptureCode(isoMsg.getString(26));
        transactionResponse.setPosDataCode(isoMsg.getString(123));
        transactionResponse.setTransactionCurrencyCode(isoMsg.getString(49));
        transactionResponse.setCardCurrencyCode(isoMsg.getString(51));
        transactionResponse.setAdditionalAmounts(IsoUtil.extractAdditionalAmounts(isoMsg));
        transactionResponse.setOriginalDataElements(IsoUtil.extractOriginalDataElements(isoMsg));
        transactionResponse.setReplacementAmounts(IsoUtil.extractReplacementAmounts(isoMsg));
        transactionResponse.setAuthorizingAgentIdCode(isoMsg.getString(58));
        transactionResponse.setReceivingInstitutionId(isoMsg.getString(100));
        transactionResponse.setFromAccountIdentification(isoMsg.getString(102));
        transactionResponse.setToAccountIdentification(isoMsg.getString(103));
        return transactionResponse;
    }
}
