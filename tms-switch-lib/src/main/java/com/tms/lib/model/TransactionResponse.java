package com.tms.lib.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tms.lib.interchange.Interchange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISOMsg;

import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    @JsonIgnore
    private TransactionRequest originalRequest;
    private Date serverResponseTime = new Date();
    @JsonIgnore
    private ISOMsg responseMessage;
    private String pan;
    private String terminalId;
    private String cardAcceptorId;
    private String cardAcceptorLocation;
    private String posEntryMode;
    private String posConditionCode;
    private String pinCaptureCode;
    private String posDataCode;
    private String cardCurrencyCode;
    private String transactionCurrencyCode;
    private long minorAmount;
    private String transactionFee;
    private String settlementFee;
    private String transactionProcessingFee;
    private String settlementProcessingFee;
    @JsonIgnore
    private Interchange responseInterchange;
    private boolean responseFromRemoteEntity;
    private boolean requestSent;
    private String isoResponseCode;
    private String message;
    private long requestRecordId;
    private EmvData emvData;
    private String authId;
    private Date responseTime;
    private String processingCode;
    private String transmissionDateTime;
    private String expiryDate;
    private String internationalNetworkIdentifier;
    private String acquiringInstitutionCountryCode;
    private String receivingInstitutionId;
    private String fromAccountIdentification;
    private String toAccountIdentification;
    private String authorizingAgentIdCode;
    private ReplacementAmounts replacementAmounts;
    private OriginalDataElements originalDataElements;
    private List<AdditionalAmount> additionalAmounts;
    private Pair<String, String> keyToKcvPair;
    private String keyUnderEncryptionKey;
    private String keyCheckValue;
    private String merchantType;


    public void setIsoResponseCode(DefaultIsoResponseCodes responseCode) {
        this.isoResponseCode = (responseCode != null) ? responseCode.toString() : null;
    }

    public void setIsoResponseCode(String isoResponseCode) {
        this.isoResponseCode = isoResponseCode;
    }

    public TransactionResponse(TransactionRequest originalRequest){
        this.originalRequest = originalRequest;
    }

    public static TransactionResponse fromCodeAndMessage(String responseCode, String message) {
        TransactionResponse response = new TransactionResponse();
        response.setIsoResponseCode(responseCode);
        response.setMessage(message);

        return response;
    }
}
