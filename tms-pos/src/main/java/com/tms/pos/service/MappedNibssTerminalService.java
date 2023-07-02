package com.tms.pos.service;

import com.tms.pos.entities.MappedNibssTerminal;

import java.util.List;
import java.util.Optional;

public interface MappedNibssTerminalService {

    List<MappedNibssTerminal> findByMappedTerminalId(String mappedTerminalId);

    Optional<MappedNibssTerminal> findByMappedTerminalIdAndInterchangeConfigId(String mappedTerminalId, int interchangeConfigId);
}
