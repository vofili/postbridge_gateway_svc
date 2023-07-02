package com.tms.lib.router.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingRule {

    @Id
    @GeneratedValue
    private Long id;
    @Enumerated(EnumType.STRING)
    private TransactionParameterType transactionParameterType;
    @Enumerated(EnumType.STRING)
    private RuleOperatorType ruleOperatorType;
    @Column(nullable = false)
    private String expectedValue;
    @Column(nullable = false)
    private Long sinkInterchangeId;
    private int priority;
}
