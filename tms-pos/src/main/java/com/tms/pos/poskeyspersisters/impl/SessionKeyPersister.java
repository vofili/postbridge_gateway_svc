package com.tms.pos.poskeyspersisters.impl;

import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.terminals.entities.TerminalKeys;
import com.tms.pos.utils.POSMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SessionKeyPersister extends PosKeysPersisterImpl {

    @Override
    public void persistKeyFromResponse(ISOMsg isoResponse) throws ServiceProcessingException {
        log.info("Persisting session key");

        String terminalID = getTerminalIDFromResponse(isoResponse);
        String encryptedKeyInResponse;
        try {
            encryptedKeyInResponse = POSMessageUtils.getEncryptedKeyFromResponse(isoResponse);
        } catch (UtilOperationException e) {
            throw new ServiceProcessingException("Could not get encrypted session key from response", e);
        }

        String masterKey = getTerminalMasterKey(terminalID);
        String clearKey = decryptEncryptedKey(encryptedKeyInResponse, masterKey);

        saveTerminalSessionKey(terminalID, clearKey);
    }

    private void saveTerminalSessionKey(String terminalID, String sessionKey) throws ServiceProcessingException {
        String encryptedSessionKey = encryptKey(sessionKey);

        TerminalKeys terminalKeys = createOrGetTerminalKeysIfExist(terminalID);
        terminalKeys.setEncryptedTsk(encryptedSessionKey);
        terminalKeyService.save(terminalKeys);
    }
}
