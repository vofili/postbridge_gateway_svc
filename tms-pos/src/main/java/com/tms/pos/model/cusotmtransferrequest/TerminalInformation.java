package com.tms.pos.model.cusotmtransferrequest;

import lombok.Data;

@Data
public class TerminalInformation{
    private String batteryInformation;
    private String currencyCode;
    private String languageInfo;
    private String merchantId;
    private String merhcantLocation;
    private String posConditionCode;
    private String posDataCode;
    private String posEntryMode;
    private String posGeoCode;
    private String printerStatus;
    private String terminalId;
    private String terminalType;
    private String transmissionDate;
    private String uniqueId;
}
