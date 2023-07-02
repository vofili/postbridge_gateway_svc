package com.tms.lib.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "IccRequest")
@Data
public class IccRequest {

    @XmlElement(name = "Bitmap", required = true)
    private String bitmap;
    @XmlElement(name = "AmountAuthorized", required = true)
    private String amountAuthorized;
    @XmlElement(name = "AmountOther")
    private String amountOther;
    @XmlElement(name = "ApplicationIdentifier")
    private String applicationIdentifier;
    @XmlElement(name = "ApplicationInterchangeProfile", required = true)
    private String applicationInterchangeProfile;
    @XmlElement(name = "ApplicationTransactionCounter", required = true)
    private String applicationTransactionCounter;
    @XmlElement(name = "ApplicationUsageControl")
    private String applicationUsageControl;
    @XmlElement(name = "AuthorizationResponseCode")
    private String authorizationResponseCode;
    @XmlElement(name = "CardAuthenticationReliabilityIndicator")
    private String cardAuthenticationReliabilityIndicator;
    @XmlElement(name = "CardAuthenticationResultsCode")
    private String cardAuthenticationResultsCode;
    @XmlElement(name = "ChipConditionCode")
    private String chipConditionCode;
    @XmlElement(name = "Cryptogram", required = true)
    private String cryptogram;
    @XmlElement(name = "CryptogramInformationData", required = true)
    private String cryptogramInformationData;
    @XmlElement(name = "CmvList")
    private String cvmList;
    @XmlElement(name = "CvmResults")
    private String cvmResults;
    @XmlElement(name = "InterfaceDeviceSerialNumber")
    private String interfaceDeviceSerialNumber;
    @XmlElement(name = "IssuerActionCode")
    private String issuerActionCode;
    @XmlElement(name = "IssuerApplicationData", required = true)
    private String issuerApplicationData;
    @XmlElement(name = "IssuerScriptResults")
    private String issuerScriptResults;
    @XmlElement(name = "TerminalApplicationVersionNumber")
    private String terminalApplicationVersionNumber;
    @XmlElement(name = "TerminalCapabilities")
    private String terminalCapabilities;
    @XmlElement(name = "TerminalCountryCode", required = true)
    private String terminalCountryCode;
    @XmlElement(name = "TerminalType")
    private String terminalType;
    @XmlElement(name = "TerminalVerificationResult", required = true)
    private String terminalVerificationResult;
    @XmlElement(name = "TransactionCategoryCode")
    private String transactionCategoryCode;
    @XmlElement(name = "TransactionCurrencyCode", required = true)
    private String transactionCurrencyCode;
    @XmlElement(name = "TransactionDate", required = true)
    private String transactionDate;
    @XmlElement(name = "TransactionSequenceCounter")
    private String transactionSequenceCounter;
    @XmlElement(name = "TransactionType", required = true)
    private String transactionType;
    @XmlElement(name = "UnpredictableNumber", required = true)
    private String unpredictableNumber;
}
