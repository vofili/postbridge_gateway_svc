package com.tms.pos.service.impl;

import com.tms.pos.entities.MappedNibssTerminal;
import com.tms.pos.repository.MappedNibssTerminalRepository;
import com.tms.pos.service.MappedNibssTerminalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MappedNibssTerminalServiceImpl implements MappedNibssTerminalService {

    private final MappedNibssTerminalRepository mappedNibssTerminalRepository;

    @Override
    public List<MappedNibssTerminal> findByMappedTerminalId(String mappedTerminalId) {
        return mappedNibssTerminalRepository.findByMappedTerminalId(mappedTerminalId);
    }

    @Override
    public Optional<MappedNibssTerminal> findByMappedTerminalIdAndInterchangeConfigId(String mappedTerminalId, int interchangeConfigId) {
        return mappedNibssTerminalRepository.findByMappedTerminalIdAndInterchangeConfigId(mappedTerminalId, interchangeConfigId);
    }
}
