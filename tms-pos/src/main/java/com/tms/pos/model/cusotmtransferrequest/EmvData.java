package com.tms.pos.model.cusotmtransferrequest;

import lombok.Data;

@Data
public class EmvData {
    private Object amountAuthorized;
    private Object amountOther;
    private Object applicationInterchangeProfile;
    private String atc;
    private Object cryptogram;
    private Object cryptogramInformationData;
    private Object cvmResults;
    private String iad;
    private Object transactionCurrencyCode;
    private Object terminalVerificationResult;
    private Object terminalCountryCode;
    private Object terminalType;
    private Object terminalCapabilities;
    private Object transactionDate;
    private Object transactionType;
    private Object unpredictableNumber;
    private Object dedicatedFileName;
}
