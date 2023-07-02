package com.tms.lib.router.service.impl;

import com.tms.lib.router.Router;
import com.tms.lib.router.entities.RoutingRule;
import com.tms.lib.router.repository.RoutingRuleRepository;
import com.tms.lib.router.service.RoutingRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoutingRuleServiceImpl implements RoutingRuleService {

    private final RoutingRuleRepository routingRuleRepository;
    private final Router router;

    public RoutingRule save(RoutingRule routingRule) {
        routingRule = routingRuleRepository.save(routingRule);
        router.reloadRules();
        return routingRule;
    }

    @Override
    public void reloadRules() {
        router.reloadRules();
    }
}
