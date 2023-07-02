package com.tms.pos.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TerminalConfigurationDetails {

    private String terminalId;
    private String cardAcceptorId;
    private int timeOutInSeconds;
    private String currencyCode;
    private String countryCode;
    private int callHomeTimeInHours;
    private String merchantNameLocation;
    private String mcc;
    private String locationName;
    private String city;
    private String state;
    private String country;
}
