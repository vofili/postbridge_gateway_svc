package com.tms.service.controller;

import com.tms.lib.router.entities.RuleOperatorType;
import com.tms.lib.router.entities.TransactionParameterType;
import com.tms.lib.router.service.RoutingRuleService;
import com.tms.service.apimodel.RoutingRuleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/routing")
@RequiredArgsConstructor
public class RoutingController {

    private final RoutingRuleService routingRuleService;

    @PostMapping("/create-rule")
    public RoutingRuleDTO createRoutingRule(@RequestBody @Validated RoutingRuleDTO routingRuleDTO) {
        routingRuleService.save(routingRuleDTO.toRoutingRule());
        return routingRuleDTO;
    }

    @GetMapping("/rule-op-types")
    public List<RuleOperatorType> getAllRuleOperatorTypes() {
        return Arrays.asList(RuleOperatorType.values());
    }

    @GetMapping("/transaction-parameter-types")
    public List<TransactionParameterType> getAllTransactionParameterType() {
        return Arrays.asList(TransactionParameterType.values());
    }

    @PostMapping("/reload")
    public void reloadRules() {
        routingRuleService.reloadRules();
    }
}
