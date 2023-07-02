package com.tms.lib.terminals.services;

import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.terminals.entities.TerminalKeys;

import java.util.Optional;

public interface TerminalKeyService {

    TerminalKeys save(TerminalKeys terminalKeys);

    TerminalKeys findByTerminalId(String terminalId) throws ServiceProcessingException;

    Optional<TerminalKeys> findByTerminalIdOptional(String terminalId);

    String getConfiguredTerminalPinKey(String terminalId) throws ServiceProcessingException;

    String getClearPinKey(String terminalId) throws ServiceProcessingException;
}
