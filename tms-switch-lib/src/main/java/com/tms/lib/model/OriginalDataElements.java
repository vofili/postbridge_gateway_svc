package com.tms.lib.model;

import lombok.Data;

@Data
public class OriginalDataElements {

    private String mti;
    private String stan;
    private String transmissionDateTime;
    private String acquiringInstitutionIdCode;
    private String forwardingInstitutionIdCode;

    public String getAcquiringInstitutionIdCode() {
        return ("00000000000".equals(acquiringInstitutionIdCode)) ? null : acquiringInstitutionIdCode;
    }

    public String getForwardingInstitutionIdCode() {
        return ("00000000000".equals(forwardingInstitutionIdCode)) ? null : forwardingInstitutionIdCode;
    }
}
