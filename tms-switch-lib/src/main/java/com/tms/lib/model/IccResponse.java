package com.tms.lib.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Sohlowmawn on 8/4/16.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "IccResponse")
@Data
public class IccResponse {

    @XmlElement(name = "Bitmap")
    private String bitmap;
    @XmlElement(name = "ApplicationTransactionCounter")
    private String applicationTransactionCounter;
    @XmlElement(name = "CardAuthenticationResultsCode")
    private String cardAuthResultsCode;
    @XmlElement(name = "IssuerAuthenticationData")
    private String issuerAuthData;
    @XmlElement(name = "IssuerScriptTemplate1")
    private String issuerScriptTemplate1;
    @XmlElement(name = "IssuerScriptTemplate2")
    private String issuerScriptTemplate2;
}
