package com.tms.lib.router.repository;

import com.tms.lib.router.entities.RoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutingRuleRepository extends JpaRepository<RoutingRule, Long> {
}
