package com.tms.lib.service;

import com.tms.lib.util.AsyncWorkerPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappedTerminalKeyDownloadService {

    private final AsyncWorkerPool asyncWorkerPool;
    private final List<TerminalKeyDownloadService> terminalKeyDownloadServices;

    public void triggerKeyDownloadForMappedTerminals(String terminalId) {
        for (TerminalKeyDownloadService terminalKeyDownloadService : terminalKeyDownloadServices) {
            asyncWorkerPool.queueJob(() -> {
                terminalKeyDownloadService.downloadKeysForMappedTerminal(terminalId);
                return null;
            });
        }
    }

    public void triggerKeyDownloadInstant(String terminalId) {
        for (TerminalKeyDownloadService terminalKeyDownloadService : terminalKeyDownloadServices) {
            terminalKeyDownloadService.downloadKeysForMappedTerminal(terminalId);
        }
    }

}
