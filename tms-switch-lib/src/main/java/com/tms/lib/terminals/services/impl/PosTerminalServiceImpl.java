package com.tms.lib.terminals.services.impl;

import com.tms.lib.terminals.entities.PosTerminal;
import com.tms.lib.terminals.repository.PosTerminalRepository;
import com.tms.lib.terminals.services.PosTerminalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PosTerminalServiceImpl implements PosTerminalService {

    private final PosTerminalRepository posTerminalRepository;

    @Override
    public PosTerminal create(PosTerminal posTerminal) {
        return posTerminalRepository.save(posTerminal);
    }
}
