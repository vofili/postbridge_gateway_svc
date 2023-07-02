package com.tms.lib.transactionrecord.entities;

import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.DefaultIsoResponseCodes;
import com.tms.lib.model.RequestType;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.util.IsoUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
public class TransactionRecord {

    @GeneratedValue
    @Id
    private long id;

    private Long requestInterchangeId;

    private String hashedPan;
    private String maskedPan;

    private String stan;
    private Long originalTransactionId;
    private boolean reversed;
    @Enumerated(EnumType.STRING)
    private RequestType requestType;
    private String responseCode;
    private String fromAccountType;
    private String toAccountType;
    private String retrievalReferenceNumber;
    private String transactionCurrencyCode;
    private String cardCurrencyCode;
    private String serviceRestrictionCode;
    private String acquiringInstitutionCountryCode;
    private String acquiringInstitutionIdentifier;
    private String forwardingInstitutionCode;
    private String receivingInstitutionId;
    private String settlementCurrencyCode;


    @Temporal(TemporalType.TIMESTAMP)
    private Date requestTime;
    @Temporal(TemporalType.TIMESTAMP)
    private Date responseTime;
    @Temporal(TemporalType.TIMESTAMP)
    private Date transmissionDateTime;
    @Temporal(TemporalType.TIMESTAMP)
    private Date transactionDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date transactionTime;
    @Temporal(TemporalType.TIMESTAMP)
    private Date settlementDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date conversionDate;

    private Long responseInterchangeId;

    private String cardAcceptorId;
    protected String terminalId;
    private String cardAcceptorLocation;
    private String posGeoCode;
    private String posDataCode;
    private String posEntryMode;
    private String posConditionCode;
    private String posCurrencyCode;
    private String pinCaptureCode;

    private Long requestAmount;
    private Long responseAmount;
    private String settlementFee;
    private String transactionFee;
    private String settlementProcessingFee;
    private String transactionProcessingFee;
    private Long amountCardHolderBilling;


    private boolean responseFromRemoteEntity;

    private String mti;
    private String processingCode;
    private boolean requestSent;
    private Long reversalTransactionId;
    private String authorizationIdResponse;
    private String requestAdditionalAmounts;
    private String responseAdditionalAmounts;
    private String merchantType;
    private String fromAccountIdentification;
    private String toAccountIdentification;

    private String requestInterchangeName;
    private String responseInterchangeName;

    @Lob
    private String emvDataRequest;
    @Lob
    private String emvDataResponse;
    private boolean completed;

    public static TransactionRecord fromTransactionRequest(int sourceId, TransactionRequest transactionRequest) throws UtilOperationException {
        try {
            TransactionRecord transactionRecord = new TransactionRecord();
            transactionRecord.setStan(transactionRequest.getStan());
            transactionRecord.setMaskedPan(maskPan(transactionRequest.getPan()));
            transactionRecord.setRequestAmount(transactionRequest.getMinorAmount());
            transactionRecord.setRequestTime(transactionRequest.getServerRequestTime());
            transactionRecord.setRequestType(transactionRequest.getRequestType());
            transactionRecord.setFromAccountIdentification(transactionRequest.getFromAccountIdentification());
            transactionRecord.setToAccountIdentification(transactionRequest.getToAccountIdentification());
            transactionRecord.setRequestInterchangeId((long) sourceId);
            transactionRecord.setAcquiringInstitutionCountryCode(transactionRequest.getAcquiringInstitutionCountryCode());
            transactionRecord.setAcquiringInstitutionIdentifier(StringUtils.leftPad(transactionRequest.getAcquiringInstitutionId(), 11, '0'));
            transactionRecord.setCardCurrencyCode(transactionRequest.getCardCurrencyCode());
            transactionRecord.setForwardingInstitutionCode(StringUtils.leftPad(transactionRequest.getForwardingInstitutionId(), 11, '0'));
            transactionRecord.setRetrievalReferenceNumber(transactionRequest.getRrn());
            transactionRecord.setTransactionCurrencyCode(transactionRequest.getTransactionCurrencyCode());
            transactionRecord.setReceivingInstitutionId(StringUtils.leftPad(transactionRequest.getReceivingInstitutionId(), 11, '0'));
            transactionRecord.setServiceRestrictionCode(transactionRequest.getServiceRestrictionCode());
            transactionRecord.setTransactionTime(IsoUtil.extractTime(transactionRequest.getTransactionTime()));
            transactionRecord.setTransmissionDateTime(IsoUtil.extractDateTime(transactionRequest.getTransmissionDateTime()));
            transactionRecord.setTransactionDate(IsoUtil.extractDate(transactionRequest.getTransactionDate()));
            transactionRecord.setTransactionProcessingFee(transactionRequest.getTransactionProcessingFee());
            transactionRecord.setSettlementProcessingFee(transactionRequest.getSettlementProcessingFee());
            transactionRecord.setTransactionFee(transactionRequest.getTransactionFee());
            transactionRecord.setSettlementFee(transactionRequest.getSettlementFee());

            transactionRecord.setCardAcceptorId(transactionRequest.getCardAcceptorId());
            transactionRecord.setTerminalId(transactionRequest.getTerminalId());
            transactionRecord.setCardAcceptorLocation(transactionRequest.getCardAcceptorLocation());
            transactionRecord.setPosConditionCode(transactionRequest.getPosConditionCode());
            transactionRecord.setPosDataCode(transactionRequest.getPosDataCode());
            transactionRecord.setPosEntryMode(transactionRequest.getPosEntryMode());
            transactionRecord.setPosCurrencyCode(transactionRequest.getTransactionCurrencyCode());
            transactionRecord.setPinCaptureCode(transactionRequest.getPinCaptureCode());

            transactionRecord.setSettlementDate(IsoUtil.extractDate(transactionRequest.getSettlementDate()));
            transactionRecord.setConversionDate(IsoUtil.extractDate(transactionRequest.getConversionDate()));
            transactionRecord.setOriginalTransactionId(transactionRequest.getOriginalTransactionId());
            transactionRecord.setMti(transactionRequest.getMti());
            transactionRecord.setProcessingCode(transactionRequest.getProcessingCode());
            transactionRecord.setAuthorizationIdResponse(transactionRequest.getAuthorizationIdResponse());
            if (transactionRequest.getAdditionalAmounts() != null) {
                JSONArray jsonArray = new JSONArray(transactionRequest.getAdditionalAmounts().toArray());
                transactionRecord.setRequestAdditionalAmounts(jsonArray.toString());
            }
            transactionRecord.setSettlementCurrencyCode(transactionRequest.getSettlementCurrencyCode());
            transactionRecord.setMerchantType(transactionRequest.getMerchantType());
            transactionRecord.setFromAccountIdentification(transactionRequest.getFromAccountIdentification());
            transactionRecord.setToAccountIdentification(transactionRequest.getToAccountIdentification());
            if (transactionRequest.getSourceInterchange() != null) {
                transactionRecord.setRequestInterchangeName(transactionRequest.getSourceInterchange().getName());
            }
            if (transactionRequest.getEmvData() != null) {
                JSONObject jsonObject = new JSONObject(transactionRequest.getEmvData());
                transactionRecord.setEmvDataRequest(jsonObject.toString());
            }
            transactionRecord.setResponseCode(DefaultIsoResponseCodes.RequestInProgress.toString());
            transactionRecord.setCompleted(false);

            return transactionRecord;
        } catch (Exception e) {
            throw new UtilOperationException("Could not persis transaction record", e);
        }
    }


    public static TransactionRecord addChannelResponse(int sinkId, TransactionResponse transactionResponse, TransactionRecord prototype) {
        TransactionRequest originalRequest = transactionResponse.getOriginalRequest();
        if (!StringUtils.equals(prototype.getStan(), originalRequest.getStan())) {
            return null;
        }
        prototype.setResponseAmount(transactionResponse.getMinorAmount());
        prototype.setResponseTime(transactionResponse.getServerResponseTime());
        prototype.setResponseCode(transactionResponse.getIsoResponseCode());
        prototype.setResponseInterchangeId((long) sinkId);
        prototype.setAuthorizationIdResponse(transactionResponse.getAuthId());
        prototype.setResponseFromRemoteEntity(transactionResponse.isResponseFromRemoteEntity());

        if (originalRequest.getFromAccountIdentification() == null) {
            prototype.setFromAccountIdentification(transactionResponse.getFromAccountIdentification());
        }
        if (originalRequest.getToAccountIdentification() == null) {
            prototype.setToAccountIdentification(transactionResponse.getToAccountIdentification());
        }
        prototype.setReceivingInstitutionId(transactionResponse.getReceivingInstitutionId());
        if (transactionResponse.getAdditionalAmounts() != null) {
            JSONArray jsonArray = new JSONArray(transactionResponse.getAdditionalAmounts().toArray());
            prototype.setResponseAdditionalAmounts(jsonArray.toString());
        }
        prototype.setRequestSent(transactionResponse.isRequestSent());
        if (transactionResponse.getResponseInterchange() != null) {
            prototype.setResponseInterchangeName(transactionResponse.getResponseInterchange().getName());
        }
        if (transactionResponse.getEmvData() != null) {
            JSONObject jsonObject = new JSONObject(transactionResponse.getEmvData());
            prototype.setEmvDataResponse(jsonObject.toString());
        }
        prototype.setCompleted(true);
        return prototype;
    }


    private static String maskPan(String pan) {
        if (pan == null) {
            return null;
        }
        String maskedPan = pan.substring(0, 6);
        int len = pan.length() - 10;
        maskedPan += new String(new char[len]).replace("\0", "*");
        maskedPan += pan.substring(maskedPan.length(), pan.length());
        return maskedPan;
    }

}
