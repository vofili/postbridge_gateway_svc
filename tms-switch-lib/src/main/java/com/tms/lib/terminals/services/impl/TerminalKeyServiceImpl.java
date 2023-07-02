package com.tms.lib.terminals.services.impl;

import com.tms.lib.exceptions.CryptoException;
import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.security.Encrypter;
import com.tms.lib.terminals.entities.TerminalKeys;
import com.tms.lib.terminals.repository.TerminalKeysRepository;
import com.tms.lib.terminals.services.TerminalKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TerminalKeyServiceImpl implements TerminalKeyService {

    private final TerminalKeysRepository terminalKeysRepository;
    private final Encrypter encrypter;


    @Override
    public TerminalKeys save(TerminalKeys terminalKeys) {
        return terminalKeysRepository.save(terminalKeys);
    }

    @Override
    public TerminalKeys findByTerminalId(String terminalId) throws ServiceProcessingException {

        if (StringUtils.isEmpty(terminalId)) {
            throw new ServiceProcessingException("Invalid terminal id supplied");
        }
        TerminalKeys terminal = terminalKeysRepository.findByTerminalId(terminalId);

        if (terminal == null) {
            throw new ServiceProcessingException(String.format("Cannot find terminal with id %s", terminalId));
        }

        return terminal;
    }

    @Override
    public Optional<TerminalKeys> findByTerminalIdOptional(String terminalId) {
        if (StringUtils.isEmpty(terminalId)) {
            return Optional.empty();
        }

        TerminalKeys terminalKeys = terminalKeysRepository.findByTerminalId(terminalId);
        if (terminalKeys == null) {
            return Optional.empty();
        }

        return Optional.of(terminalKeys);
    }

    @Override
    public String getConfiguredTerminalPinKey(String terminalId) throws ServiceProcessingException {
        TerminalKeys posTerminal = findByTerminalId(terminalId);

        String tpkUnderLmk = posTerminal.getTpkUnderLmk();

        if (StringUtils.isEmpty(tpkUnderLmk)) {
            throw new ServiceProcessingException("Terminal Pin key has not been generated yet");
        }

        return tpkUnderLmk;
    }

    @Override
    public String getClearPinKey(String terminalId) throws ServiceProcessingException {
        TerminalKeys posTerminalKey = findByTerminalId(terminalId);
        String encryptedPinKey = posTerminalKey.getEncryptedTsk();

        try {
            return encrypter.decrypt(encryptedPinKey);
        } catch (CryptoException ex) {
            log.error("An error occurred on attempt to decrypt terminal Key");
        }
        return null;
    }
}
