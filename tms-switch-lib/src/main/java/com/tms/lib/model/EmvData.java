package com.tms.lib.model;

import lombok.Data;

@Data
public class EmvData {

    private long authorizedAmount;
    private long amountOther;
    private String applicationInterchangeProfile;
    private String applicationTransactionCounter;
    private String cryptogram;
    private String cryptogramInformationData;
    private String cvmResult;
    private String issuerAuthenticationData;
    private String transactionCurrencyCode;
    private String terminalVerificationResult;
    private String terminalCountryCode;
    private String terminalType;
    private String terminalCapabilities;
    private String transactionDate;
    private String transactionType;
    private String unpredictableNumber;
    private String dedicatedFileName;
    private String hostResponseCode;
    private String cardAuthenticationResultCode;
    private String merchantCategoryCode;
    private String issuerApplicationData;
    private String acquirerIdentifier;
    private String transactionSequenceCounter;
    private String applicationExpiryDate;
    private String applicationPAN;
    private String applicationPANSequenceNumber;
    private String track2EquivalentData;
    private String applicationUsageControl;
    private String cardAuthReliabilityIndicator;
    private String chipConditionCode;
    private String cvmList;
    private String interfaceDeviceSerialNumber;
    private String issuerActionCode;
    private String issuerScriptResults;
    private String terminalApplicationVersionNumber;
    private String transactionCategoryCode;
    private String applicationIdentifierCard;
    private String applicationIdentifierTerminal;
    private String scriptTemplate1;
    private String scriptTemplate2;
}
