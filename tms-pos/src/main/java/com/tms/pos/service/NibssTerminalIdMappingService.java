package com.tms.pos.service;

import com.tms.pos.entities.MappedNibssTerminal;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface NibssTerminalIdMappingService {

    /**
     * Pair of Terminal Id on the left and Merchant Id on the right
     *
     * @param terminalId
     * @return
     */
    Pair<String, String> getNibssMappedTerminalIdAndMerchantIdPair(String terminalId, int interchangeConfigId);

    List<MappedNibssTerminal> getAllPairOfMappedTerminalId(String terminalId);
}
