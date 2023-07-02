package com.tms.lib.router.service;

import com.tms.lib.router.entities.RoutingRule;

public interface RoutingRuleService {

    RoutingRule save(RoutingRule routingRule);

    void reloadRules();
}
