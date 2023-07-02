package com.tms.pos.service;

import com.tms.lib.exceptions.ServiceProcessingException;
import org.apache.commons.lang3.tuple.Pair;

public interface TerminalKeyGenerationService {

    Pair<byte[], byte[]> generateTerminalMasterKey(String terminalId) throws ServiceProcessingException;

    Pair<byte[], byte[]> generateTerminalSessionKey(String terminalId) throws ServiceProcessingException;

    Pair<byte[], byte[]> generateTerminalPinKey(String terminalId) throws ServiceProcessingException;
}
