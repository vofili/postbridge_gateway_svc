package com.tms.lib.terminals.repository;

import com.tms.lib.terminals.entities.TerminalKeys;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalKeysRepository extends JpaRepository<TerminalKeys, Long> {

    TerminalKeys findByTerminalId(String terminalId);
}
