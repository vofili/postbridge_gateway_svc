package com.tms.pos.model.cusotmtransferrequest;

import lombok.Data;

@Data
public class CardData{
    private String cardSequenceNumber;
    private EmvData emvData;
    private Track2 track2;
}
