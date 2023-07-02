package com.tms.lib.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tms.lib.interchange.Interchange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jpos.iso.ISOMsg;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TransactionRequest {

    private RequestType requestType = RequestType.UNKNOWN;
    @JsonIgnore
    private ISOMsg sourceMessage;
    private Date serverRequestTime = new Date();
    private String pan;
    private String sourceAccountType;
    private String destinationAccountType;
    private String processingCode;
    private long minorAmount;
    private long settlementAmount;
    private long amountCardHolderBilling;
    private String terminalId;
    private String cardAcceptorId;
    private String cardAcceptorLocation;
    private String posEntryMode;
    private String posConditionCode;
    private String pinCaptureCode;
    private String posDataCode;
    private String expiryDate;
    private String transactionDate;
    private String transactionTime;
    private String transmissionDateTime;
    private String settlementDate;
    private String conversionDate;
    private String stan;
    private String rrn;
    private String merchantType;
    private String acquiringInstitutionCountryCode;
    private String transactionFee;
    private String settlementFee;
    private String transactionProcessingFee;
    private String settlementProcessingFee;
    private String acquiringInstitutionId;
    private String forwardingInstitutionId;
    private String transactionCurrencyCode;
    private String settlementCurrencyCode;
    private String cardCurrencyCode;
    private String receivingInstitutionId;
    private String fromAccountIdentification;
    private String toAccountIdentification;
    private String pinBlock;
    private String track2Data;
    private String serviceRestrictionCode;
    private String cardSequenceNumber;
    private EmvData emvData;
    private String emvDataString;
    private String mti;
    private long transactionId;
    private long originalTransactionId;
    private String echoData;
    private String internationalNetworkIdentifier;
    private String extendedPaymentCode;
    private String authorizationIdResponse;
    private List<AdditionalAmount> additionalAmounts;
    private String payee;
    private OriginalDataElements originalDataElements;
    private ReplacementAmounts replacementAmounts;
    private String processorKey;
    @JsonIgnore
private Interchange sourceInterchange;
    @JsonIgnore
    private Interchange sinkInterchange;

    public TransactionResponse constructResponse() {
        return new TransactionResponse(this);
    }

    public TransactionResponse constructResponse(DefaultIsoResponseCodes defaultIsoResponseCodes) {
        TransactionResponse transactionResponse = constructResponse();
        transactionResponse.setIsoResponseCode(defaultIsoResponseCodes);
        return transactionResponse;
    }

    public String getBin() {
        if (pan == null) {
            return null;
        }
        if (pan.length() < 6) {
            return pan;
        }
        return pan.substring(0, 6);
    }
}
