package com.tms.pos.service.impl;

import com.tms.pos.entities.MappedNibssTerminal;
import com.tms.pos.service.MappedNibssTerminalService;
import com.tms.pos.service.NibssTerminalIdMappingService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nibss.terminal.id.source", havingValue = "tms")
public class NibssTerminalIdMappingServiceImpl implements NibssTerminalIdMappingService {

    private final MappedNibssTerminalService mappedNibssTerminalService;

    @Value("${nibss.default.terminal.id:201119LC}")
    private String nibssTerminalId;
    @Value("${nibss.default.merchant.id:2011LA023050091}")
    private String nibssMerchantId;

    @Override
    public Pair<String, String> getNibssMappedTerminalIdAndMerchantIdPair(String terminalId, int interchangeConfigId) {
        Optional<MappedNibssTerminal> optionalMappedNibssTerminal = mappedNibssTerminalService.findByMappedTerminalIdAndInterchangeConfigId(terminalId, interchangeConfigId);

        if (optionalMappedNibssTerminal.isPresent()) {
            MappedNibssTerminal mappedNibssTerminal = optionalMappedNibssTerminal.get();
            return new ImmutablePair<>(mappedNibssTerminal.getNibssTerminalId(), mappedNibssTerminal.getNibssCardAcceptorId());
        }

        return new ImmutablePair<>(nibssTerminalId, nibssMerchantId);
    }

    @Override
    public List<MappedNibssTerminal> getAllPairOfMappedTerminalId(String terminalId) {
        return mappedNibssTerminalService.findByMappedTerminalId(terminalId);
    }
}
