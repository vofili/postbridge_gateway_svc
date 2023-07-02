package com.tms.pos.source;

import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.EmvData;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.util.IsoUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

public class PosSourceIsoChannelAdapter {

    private PosSourceIsoChannelAdapter() {

    }

    public static TransactionRequest commonIsoMsgToTransactionRequest(ISOMsg isoMsg, TransactionRequest transactionRequest) throws UtilOperationException, ISOException {
        transactionRequest.setSourceMessage(isoMsg);
        if (isoMsg.hasField(55)) {
            EmvData emvData = IsoUtil.extractEmvData(new String(isoMsg.getBytes(55)));
            transactionRequest.setEmvData(emvData);
        }
        transactionRequest.setMti(isoMsg.getMTI());
        transactionRequest.setPan(isoMsg.getString(2));
        String field3 = isoMsg.getString(3);
        if (StringUtils.isNotEmpty(field3)) {
            transactionRequest.setProcessingCode(field3);
            if (field3.length() != 6) {
                String msg = String.format("Invalid field 3, should be six characters, %s", field3);
                throw new UtilOperationException(msg);
            }
        }

        transactionRequest.setMinorAmount(IsoUtil.extractAmount(isoMsg.getString(4)));
        transactionRequest.setSettlementAmount(IsoUtil.extractAmount(isoMsg.getString(5)));
        transactionRequest.setAmountCardHolderBilling(IsoUtil.extractAmount(isoMsg.getString(6)));
        transactionRequest.setTransmissionDateTime(isoMsg.getString(7));
        transactionRequest.setStan(isoMsg.getString(11));
        transactionRequest.setTransactionTime(isoMsg.getString(12));
        transactionRequest.setTransactionDate(isoMsg.getString(13));
        transactionRequest.setExpiryDate(isoMsg.getString(14));
        transactionRequest.setSettlementDate(isoMsg.getString(15));
        transactionRequest.setConversionDate(isoMsg.getString(16));
        transactionRequest.setMerchantType(isoMsg.getString(18));
        transactionRequest.setAcquiringInstitutionCountryCode(isoMsg.getString(19));//not available for pbridge
        transactionRequest.setCardSequenceNumber(isoMsg.getString(23));
        transactionRequest.setInternationalNetworkIdentifier(isoMsg.getString(24));//not available for pbridge
        transactionRequest.setTransactionFee(isoMsg.getString(28));
        transactionRequest.setSettlementFee(isoMsg.getString(29));
        transactionRequest.setTransactionProcessingFee(isoMsg.getString(30));
        transactionRequest.setSettlementProcessingFee(isoMsg.getString(31));
        transactionRequest.setAcquiringInstitutionId(isoMsg.getString(32));
        transactionRequest.setForwardingInstitutionId(isoMsg.getString(33));
        transactionRequest.setTrack2Data(isoMsg.getString(35));
        transactionRequest.setRrn(isoMsg.getString(37));
        transactionRequest.setServiceRestrictionCode(isoMsg.getString(40));
        transactionRequest.setAuthorizationIdResponse(isoMsg.getString(38));
        transactionRequest.setPinBlock(isoMsg.getString(52));
        transactionRequest.setTerminalId(isoMsg.getString(41));
        transactionRequest.setCardAcceptorId(isoMsg.getString(42));
        transactionRequest.setCardAcceptorLocation(isoMsg.getString(43));
        transactionRequest.setPosEntryMode(isoMsg.getString(22));
        transactionRequest.setPosConditionCode(isoMsg.getString(25));
        transactionRequest.setPinCaptureCode(isoMsg.getString(26));
        transactionRequest.setPosDataCode(isoMsg.getString(123));
        transactionRequest.setTransactionCurrencyCode(isoMsg.getString(49));
        transactionRequest.setSettlementCurrencyCode(isoMsg.getString(50));
        transactionRequest.setCardCurrencyCode(isoMsg.getString(51));
        transactionRequest.setEchoData(isoMsg.getString(59));
        transactionRequest.setExtendedPaymentCode(isoMsg.getString(67));
        transactionRequest.setOriginalDataElements(IsoUtil.extractOriginalDataElements(isoMsg));
        transactionRequest.setReplacementAmounts(IsoUtil.extractReplacementAmounts(isoMsg));
        transactionRequest.setPayee(isoMsg.getString(98));
        transactionRequest.setReceivingInstitutionId(isoMsg.getString(100));
        transactionRequest.setToAccountIdentification(isoMsg.getString(103));
        transactionRequest.setFromAccountIdentification(isoMsg.getString(102));

        return transactionRequest;
    }

    public static ISOMsg transactionResponseToCommonIsoMsg(TransactionResponse transactionResponse, ISOMsg isoMsg) throws UtilOperationException {
        TransactionRequest originalRequest = transactionResponse.getOriginalRequest();
        if (originalRequest == null) {
            throw new UtilOperationException("original request is null");
        }
        isoMsg.set(2, originalRequest.getPan());
        isoMsg.set(3, originalRequest.getProcessingCode());
        isoMsg.set(4, String.format("%012d", originalRequest.getMinorAmount()));

        isoMsg.set(7, originalRequest.getTransmissionDateTime());

        isoMsg.set(11, originalRequest.getStan());
        isoMsg.set(12, originalRequest.getTransactionTime());
        isoMsg.set(13, originalRequest.getTransactionDate());
        isoMsg.set(14, originalRequest.getExpiryDate());
        isoMsg.set(15, originalRequest.getSettlementDate());
        isoMsg.set(16, originalRequest.getConversionDate());
        isoMsg.set(18, originalRequest.getMerchantType());
        isoMsg.set(19, originalRequest.getAcquiringInstitutionCountryCode());
        isoMsg.set(24, originalRequest.getInternationalNetworkIdentifier());
        isoMsg.set(28, originalRequest.getTransactionFee());
        isoMsg.set(29, originalRequest.getSettlementFee());
        isoMsg.set(30, originalRequest.getTransactionProcessingFee());
        isoMsg.set(31, originalRequest.getSettlementProcessingFee());
        isoMsg.set(32, originalRequest.getAcquiringInstitutionId());
        isoMsg.set(33, originalRequest.getForwardingInstitutionId());
        isoMsg.set(37, originalRequest.getRrn());
        isoMsg.set(38, transactionResponse.getAuthId());
        isoMsg.set(39, transactionResponse.getIsoResponseCode());
        isoMsg.set(41, originalRequest.getTerminalId());
        isoMsg.set(42, originalRequest.getCardAcceptorId());
        isoMsg.set(43, originalRequest.getCardAcceptorLocation());
        isoMsg.set(22, originalRequest.getPosEntryMode());
        isoMsg.set(25, originalRequest.getPosConditionCode());
        isoMsg.set(123, originalRequest.getPosDataCode());
        isoMsg.set(55, Hex.encodeHexString(IsoUtil.emvDataToIsoBytes(transactionResponse.getEmvData())));


        isoMsg.set(49, originalRequest.getTransactionCurrencyCode());
        isoMsg.set(54, IsoUtil.convertAdditionalAmountsToString(transactionResponse.getAdditionalAmounts()));
        isoMsg.set(59, originalRequest.getEchoData());
        isoMsg.set(67, originalRequest.getExtendedPaymentCode());
        isoMsg.set(90, IsoUtil.originalDataElementsToString(transactionResponse.getOriginalDataElements()));
        isoMsg.set(95, IsoUtil.replacementAmountsToString(transactionResponse.getReplacementAmounts()));
        isoMsg.set(98, originalRequest.getPayee());
        isoMsg.set(100, transactionResponse.getReceivingInstitutionId());
        isoMsg.set(102, originalRequest.getFromAccountIdentification());
        isoMsg.set(103, originalRequest.getToAccountIdentification());

        return isoMsg;

    }
}
