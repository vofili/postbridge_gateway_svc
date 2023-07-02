package com.tms.lib.router.ruleoperator;

import com.tms.lib.router.entities.RuleOperatorType;
import org.springframework.stereotype.Component;

@Component
public class EqualsOperator implements RuleOperator {

    @Override
    public boolean apply(Object actualValue, Object expectedValue) {
        if (actualValue == null) {
            return expectedValue == null;
        }
        return String.valueOf(actualValue).equals(String.valueOf(expectedValue));
    }

    @Override
    public RuleOperatorType getRuleOperatorType() {
        return RuleOperatorType.EQUALS;
    }
}
