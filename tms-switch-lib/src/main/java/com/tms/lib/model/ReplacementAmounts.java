package com.tms.lib.model;


import lombok.Data;

@Data
public class ReplacementAmounts {

    private Long actualAmountTransaction;
    private Long actualAmountSettlement;
    private Long actualAmountTransactionFee;
    private Long actualAmountSettlementFee;
}
