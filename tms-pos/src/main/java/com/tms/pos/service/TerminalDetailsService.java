package com.tms.pos.service;

import com.tms.pos.model.TerminalConfigurationDetails;

import java.util.Optional;

public interface TerminalDetailsService {

    Optional<TerminalConfigurationDetails> findByTerminalId(String terminalId);

    boolean isValidTerminalId(String terminalId);
}
