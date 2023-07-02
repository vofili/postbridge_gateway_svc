package com.tms.lib.router;

import com.tms.lib.router.entities.RuleOperatorType;
import com.tms.lib.router.entities.TransactionParameterType;
import com.tms.lib.router.ruleoperator.RuleOperator;
import com.tms.lib.router.transactionparams.TransactionParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoutingDataResolver {

    private final List<RuleOperator> ruleOperatorList;
    private final List<TransactionParameter> transactionParameterList;

    public RuleOperator getRuleOperator(RuleOperatorType ruleOperatorType) {
        for (RuleOperator ruleOperator : ruleOperatorList) {
            if (ruleOperatorType == ruleOperator.getRuleOperatorType()) {
                return ruleOperator;
            }
        }

        return null;
    }

    public TransactionParameter getTransactionParameter(TransactionParameterType transactionParameterType){
        for (TransactionParameter transactionParameter: transactionParameterList){
            if (transactionParameterType == transactionParameter.getTransactionParameterType()){
                return transactionParameter;
            }
        }

        return null;
    }
}
