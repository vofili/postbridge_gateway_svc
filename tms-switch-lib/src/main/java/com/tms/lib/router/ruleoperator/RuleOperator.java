package com.tms.lib.router.ruleoperator;

import com.tms.lib.router.entities.RuleOperatorType;

public interface RuleOperator {

    boolean apply(Object actualValue, Object expectedValue);

    RuleOperatorType getRuleOperatorType();
}
