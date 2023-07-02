package com.tms.service.controller;

import com.tms.lib.service.MappedTerminalKeyDownloadService;
import com.tms.lib.terminals.services.PosTerminalService;
import com.tms.service.apimodel.PosTerminalDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/terminals")
@RequiredArgsConstructor
public class TerminalController {

    private final PosTerminalService posTerminalService;
    private final MappedTerminalKeyDownloadService mappedTerminalKeyDownloadService;

    @PostMapping("/create")
    public PosTerminalDTO createTerminal(@RequestBody @Validated PosTerminalDTO posTerminalDTO) {
        posTerminalService.create(posTerminalDTO.toPosTerminal());
        return posTerminalDTO;
    }

    @PostMapping("/trigger-key-download/{terminalId}")
    public void triggerKeyDownload(@PathVariable("terminalId") String terminalId) {
        mappedTerminalKeyDownloadService.triggerKeyDownloadInstant(terminalId);
    }
}
