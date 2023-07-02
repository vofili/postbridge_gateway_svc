package com.tms.service.apimodel;

import com.tms.lib.router.entities.RoutingRule;
import com.tms.lib.router.entities.RuleOperatorType;
import com.tms.lib.router.entities.TransactionParameterType;
import lombok.Data;

@Data
public class RoutingRuleDTO {

    private RuleOperatorType ruleOperatorType;
    private TransactionParameterType transactionParameterType;
    private Long sinkInterchangeId;
    private String expectedValue;

    public RoutingRule toRoutingRule() {
        return RoutingRule.builder()
                .expectedValue(expectedValue)
                .sinkInterchangeId(sinkInterchangeId)
                .ruleOperatorType(ruleOperatorType)
                .transactionParameterType(transactionParameterType)
                .build();

    }
}
