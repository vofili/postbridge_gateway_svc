package com.tms.pos.repository;

import com.tms.pos.entities.MappedNibssTerminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MappedNibssTerminalRepository extends JpaRepository<MappedNibssTerminal, Long> {

    List<MappedNibssTerminal> findByMappedTerminalId(String mappedTerminalId);

    Optional<MappedNibssTerminal> findByMappedTerminalIdAndInterchangeConfigId(String mappedTerminalId, int interchangeConfigId);
}
