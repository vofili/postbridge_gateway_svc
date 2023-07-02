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
public class PinKeyPersister extends PosKeysPersisterImpl {

    @Override
    public void persistKeyFromResponse(ISOMsg isoResponse) throws ServiceProcessingException {
        log.info("Persisting pin key");

        String terminalID = getTerminalIDFromResponse(isoResponse);
        String encryptedKeyInResponse;
        try {
            encryptedKeyInResponse = POSMessageUtils.getEncryptedKeyFromResponse(isoResponse);
        } catch (UtilOperationException e) {
            throw new ServiceProcessingException("Could not get encrypted pin key from response", e);
        }

        String masterKey = getTerminalMasterKey(terminalID);
        String clearKey = decryptEncryptedKey(encryptedKeyInResponse, masterKey);

        saveTerminalPinKey(terminalID, clearKey);
    }

    private void saveTerminalPinKey(String terminalID, String pinKey) throws ServiceProcessingException {
        String encryptedKey = encryptKey(pinKey);

        TerminalKeys terminalKeys = createOrGetTerminalKeysIfExist(terminalID);
        terminalKeys.setEncryptedTpk(encryptedKey);
        terminalKeys.setTpkUnderLmk(pinKey);//Ideally should be under an hsm lmk key
        terminalKeyService.save(terminalKeys);
    }
}
