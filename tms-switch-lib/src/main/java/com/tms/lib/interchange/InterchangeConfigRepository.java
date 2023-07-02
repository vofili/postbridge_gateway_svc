package com.tms.lib.interchange;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterchangeConfigRepository extends JpaRepository<InterchangeConfig, Integer> {

    InterchangeConfig findByName(String name);

    List<InterchangeConfig> findByTypeName(String typeName);

    Optional<InterchangeConfig> findByCode(String code);
}
