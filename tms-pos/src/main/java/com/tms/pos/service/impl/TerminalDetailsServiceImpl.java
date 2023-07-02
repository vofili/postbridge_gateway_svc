package com.tms.pos.service.impl;

import com.tms.lib.terminals.entities.PosTerminal;
import com.tms.lib.terminals.repository.PosTerminalRepository;
import com.tms.pos.model.TerminalConfigurationDetails;
import com.tms.pos.service.TerminalDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TerminalDetailsServiceImpl implements TerminalDetailsService {

    private PosTerminalRepository posTerminalRepository;

    @Autowired
    public TerminalDetailsServiceImpl(PosTerminalRepository posTerminalRepository) {
        this.posTerminalRepository = posTerminalRepository;
    }

    @Override
    public Optional<TerminalConfigurationDetails> findByTerminalId(String terminalId) {
        PosTerminal posTerminal = posTerminalRepository.findByTerminalId(terminalId);

        if (posTerminal == null) {
            return Optional.empty();
        }

        TerminalConfigurationDetails terminalConfigurationDetails = new TerminalConfigurationDetails();
        terminalConfigurationDetails.setCallHomeTimeInHours(posTerminal.getCallHomeTimeInHours());
        terminalConfigurationDetails.setCardAcceptorId(posTerminal.getCardAcceptorId());
        terminalConfigurationDetails.setCountryCode(posTerminal.getCountryCode());
        terminalConfigurationDetails.setCurrencyCode(posTerminal.getCurrencyCode());
        terminalConfigurationDetails.setMcc(posTerminal.getMcc());
        terminalConfigurationDetails.setTimeOutInSeconds(posTerminal.getTimeOutInSeconds());
        terminalConfigurationDetails.setMerchantNameLocation(posTerminal.getMerchantNameLocation());
        terminalConfigurationDetails.setTerminalId(terminalId);

        return Optional.of(terminalConfigurationDetails);
    }

    @Override
    public boolean isValidTerminalId(String terminalId) {
        return posTerminalRepository.existsByTerminalId(terminalId);
    }
}
