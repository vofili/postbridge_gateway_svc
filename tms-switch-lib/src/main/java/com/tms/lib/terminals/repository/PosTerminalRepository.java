package com.tms.lib.terminals.repository;

import com.tms.lib.terminals.entities.PosTerminal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosTerminalRepository extends JpaRepository<PosTerminal, Long> {

    PosTerminal findByTerminalId(String terminalId);

    boolean existsByTerminalId(String terminalId);

}
