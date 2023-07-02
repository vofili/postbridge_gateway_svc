package com.tms.pos.model.cusotmtransferrequest;

import lombok.Data;

import java.util.Date;

@Data
public class TransferRequest{
    private TerminalInformation terminalInformation;
    private CardData cardData;
    private Date originalTransmissionDateTime;
    private String stan;
    private String fromAccount;
    private String toAccount;
    private String minorAmount;
    private String receivingInstitutionId;
    private String surcharge;
    private PinData pinData;
    private String keyLabel;
    private String destinationAccountNumber;
    private String extendedTransactionType;
}
