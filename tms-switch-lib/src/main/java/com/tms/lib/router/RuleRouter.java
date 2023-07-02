package com.tms.lib.router;

import com.tms.lib.exceptions.InterchangeServiceException;
import com.tms.lib.exceptions.RouterException;
import com.tms.lib.interchange.Interchange;
import com.tms.lib.interchange.InterchangeFactory;
import com.tms.lib.model.DefaultIsoResponseCodes;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.router.entities.RoutingRule;
import com.tms.lib.router.repository.RoutingRuleRepository;
import com.tms.lib.router.ruleoperator.RuleOperator;
import com.tms.lib.router.transactionparams.TransactionParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class RuleRouter implements Router {

    @Autowired
    private RoutingRuleRepository routingRuleRepository;
    @Autowired
    private RoutingDataResolver routingDataResolver;
    @Autowired
    private InterchangeFactory interchangeFactory;

    private List<RoutingRule> routingRuleList = new ArrayList<>();

    @PostConstruct
    public void init() {
        routingRuleList = routingRuleRepository.findAll();
        routingRuleList.sort((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));
    }

    @Async
    public void reloadRules() {
        routingRuleList = routingRuleRepository.findAll();
        routingRuleList.sort((o1, o2) -> Integer.compare(o2.getPriority(), o1.getPriority()));
    }

    @Override
    public TransactionResponse route(TransactionRequest request) throws RouterException {
        Long sinkInterchangeId = resolveRequestDestination(request);

        return send(request, sinkInterchangeId);
    }

    @Override
    public TransactionResponse send(TransactionRequest request, Long sinkInterchangeId) throws RouterException {
        if (sinkInterchangeId == null) {
            return request.constructResponse(DefaultIsoResponseCodes.RoutingError);
        }

        Interchange sinkInterchange = interchangeFactory.getInterchange(sinkInterchangeId.intValue());

        if (sinkInterchange == null) {
            return request.constructResponse(DefaultIsoResponseCodes.RoutingError);
        }

        try {
            return sinkInterchange.send(request);
        } catch (InterchangeServiceException e) {
            throw new RouterException("There was an error routing the request", e);
        }
    }

    private Long resolveRequestDestination(TransactionRequest transactionRequest) throws RouterException {
        for (RoutingRule routingRule : routingRuleList) {
            TransactionParameter transactionParameter = routingDataResolver.getTransactionParameter(routingRule.getTransactionParameterType());
            RuleOperator ruleOperator = routingDataResolver.getRuleOperator(routingRule.getRuleOperatorType());

            if (transactionParameter == null || ruleOperator == null) {
                throw new RouterException("Cannot resolve routing rule, no transaction parameter or rule operator impl. was found!");
            }

            boolean valid = ruleOperator.apply(transactionParameter.getParameter(transactionRequest), routingRule.getExpectedValue());

            if (valid) {
                return routingRule.getSinkInterchangeId();
            }
        }

        return null;
    }
}
