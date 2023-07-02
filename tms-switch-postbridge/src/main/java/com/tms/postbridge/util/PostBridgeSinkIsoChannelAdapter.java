package com.tms.postbridge.util;

import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.EmvData;
import com.tms.lib.model.IccData;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.util.IsoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISOMsg;

import java.util.Date;

@Slf4j
public class PostBridgeSinkIsoChannelAdapter {

    private PostBridgeSinkIsoChannelAdapter() {
    }

    public static ISOMsg transactionRequestToCommonIsoMsg(TransactionRequest transactionRequest, ISOMsg isoMsg) throws UtilOperationException {

        isoMsg.set(2, transactionRequest.getPan());
        if (transactionRequest.getMinorAmount() != 0) {
            isoMsg.set(4, String.format("%012d", transactionRequest.getMinorAmount()));
        }
        isoMsg.set(7, transactionRequest.getTransmissionDateTime());
        isoMsg.set(11, transactionRequest.getStan());
        isoMsg.set(12, transactionRequest.getTransactionTime());
        isoMsg.set(13, transactionRequest.getTransactionDate());
        isoMsg.set(14, transactionRequest.getExpiryDate());
        isoMsg.set(15, transactionRequest.getSettlementDate());
        isoMsg.set(16, transactionRequest.getConversionDate());
        isoMsg.set(18, transactionRequest.getMerchantType());
        isoMsg.set(19, transactionRequest.getAcquiringInstitutionCountryCode());
        isoMsg.set(22, transactionRequest.getPosEntryMode());
        isoMsg.set(23, transactionRequest.getCardSequenceNumber());
        isoMsg.set(25, transactionRequest.getPosConditionCode());
        isoMsg.set(24, transactionRequest.getInternationalNetworkIdentifier());
        isoMsg.set(26, transactionRequest.getPinCaptureCode());
        isoMsg.set(28, transactionRequest.getTransactionFee());
        isoMsg.set(29, transactionRequest.getSettlementFee());
        isoMsg.set(30, transactionRequest.getTransactionProcessingFee());
        isoMsg.set(31, transactionRequest.getSettlementProcessingFee());
        isoMsg.set(32, transactionRequest.getAcquiringInstitutionId());
        isoMsg.set(33, transactionRequest.getForwardingInstitutionId());
        isoMsg.set(35, transactionRequest.getTrack2Data());
        isoMsg.set(37, transactionRequest.getRrn());
        isoMsg.set(38, transactionRequest.getAuthorizationIdResponse());
        isoMsg.set(40, transactionRequest.getServiceRestrictionCode());
        isoMsg.set(41, transactionRequest.getTerminalId());
        isoMsg.set(42, transactionRequest.getCardAcceptorId());
        isoMsg.set(43, transactionRequest.getCardAcceptorLocation());
        isoMsg.set(49, transactionRequest.getTransactionCurrencyCode());
        isoMsg.set(50, transactionRequest.getSettlementCurrencyCode());
        isoMsg.set(51, transactionRequest.getCardCurrencyCode());
        if (StringUtils.isNotEmpty(transactionRequest.getPinBlock())) {
            String pinBlock = transactionRequest.getPinBlock();
            byte[] pinBlockBytes;
            try {
                pinBlockBytes = Hex.decodeHex(pinBlock.toCharArray());
            } catch (DecoderException e) {
                throw new UtilOperationException("Error decoding pin block", e);
            }
            isoMsg.set(52, pinBlockBytes);
        }
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
        EmvData emvData = transactionRequest.getEmvData();
        if (emvData != null) {
            String iccString = PostBridgeIsoUtil.emvDataToRequestIsoString(emvData);
            if (StringUtils.isEmpty(iccString)) {
                log.warn("Icc data is null");
            }
            isoMsg.set("127.025", iccString);
        }

        return isoMsg;
    }

    public static TransactionResponse commonIsoMsgToTransactionResponse(ISOMsg isoMsg, TransactionResponse transactionResponse) throws UtilOperationException {


        IccData iccData = PostBridgeIsoUtil.extractXMLICCData(isoMsg);

        EmvData emvData = PostBridgeIsoUtil.iccResponseToEmvData(iccData.getIccResponse());


        transactionResponse.setEmvData(emvData);
        transactionResponse.setPan(isoMsg.getString(2));

        transactionResponse.setIsoResponseCode(isoMsg.getString(39));
        transactionResponse.setAuthId(isoMsg.getString(38));
        transactionResponse.setResponseTime(new Date());


        String field3 = isoMsg.getString(3);
        if (StringUtils.isNotEmpty(field3)) {
            if (field3.length() != 6) {
                String msg = String.format("Invalid field 3, should be six characters, %s", field3);
                throw new UtilOperationException(msg);
            }
            transactionResponse.setProcessingCode(field3);
        }

        transactionResponse.setTransmissionDateTime(isoMsg.getString(7));

        transactionResponse.setExpiryDate(isoMsg.getString(14));

        transactionResponse.setMinorAmount(PostBridgeIsoUtil.extractAmount(isoMsg.getString(4)));
        transactionResponse.setInternationalNetworkIdentifier(isoMsg.getString(24));
        transactionResponse.setTransactionFee(isoMsg.getString(28));
        transactionResponse.setTransactionProcessingFee(isoMsg.getString(30));
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
        transactionResponse.setAdditionalAmounts(PostBridgeIsoUtil.extractAdditionalAmounts(isoMsg));
        transactionResponse.setOriginalDataElements(IsoUtil.extractOriginalDataElements(isoMsg));
        transactionResponse.setReplacementAmounts(IsoUtil.extractReplacementAmounts(isoMsg));
        transactionResponse.setAuthorizingAgentIdCode(isoMsg.getString(58));
        transactionResponse.setReceivingInstitutionId(isoMsg.getString(100));
        transactionResponse.setFromAccountIdentification(isoMsg.getString(102));
        transactionResponse.setToAccountIdentification(isoMsg.getString(103));
        return transactionResponse;
    }

}

