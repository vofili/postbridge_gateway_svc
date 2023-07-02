package com.tms.lib.router.ruleoperator;

import com.tms.lib.router.entities.RuleOperatorType;
import org.springframework.stereotype.Component;

@Component
public class GreaterThanRuleOperator implements RuleOperator {
    @Override
    public boolean apply(Object actualValue, Object expectedValue) {
        return Long.valueOf("" + actualValue).compareTo(Long.valueOf("" + expectedValue)) > 0;
    }

    @Override
    public RuleOperatorType getRuleOperatorType() {
        return RuleOperatorType.GREATER_THAN;
    }
}
