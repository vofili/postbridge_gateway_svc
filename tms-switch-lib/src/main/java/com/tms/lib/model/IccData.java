package com.tms.lib.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "IccData")
@Data
public class IccData {

    @XmlElement(name = "IccRequest")
    private IccRequest iccRequest;
    @XmlElement(name = "IccResponse")
    private IccResponse iccResponse;
}
