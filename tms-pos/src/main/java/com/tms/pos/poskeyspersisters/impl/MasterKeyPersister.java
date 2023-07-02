package com.tms.pos.poskeyspersisters.impl;

import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.terminals.entities.TerminalKeys;
import com.tms.pos.utils.POSMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MasterKeyPersister extends PosKeysPersisterImpl {

    @Override
    public void persistKeyFromResponse(ISOMsg isoResponse, String ptspCtmk) throws ServiceProcessingException {
        if (StringUtils.isEmpty(ptspCtmk)) {
            throw new ServiceProcessingException("Invalid ptsp details supplied");
        }

        if (!POSMessageUtils.isApprovedResponse(isoResponse)) {
            throw new ServiceProcessingException("Response received is not a successful response");
        }

        log.info("Persisting master key");

        String terminalID = getTerminalIDFromResponse(isoResponse);
        String encryptedKeyInResponse;
        try {
            encryptedKeyInResponse = POSMessageUtils.getEncryptedKeyFromResponse(isoResponse);
        } catch (UtilOperationException e) {
            throw new ServiceProcessingException("Could not get encrypted key from response", e);
        }

        String clearKey = decryptEncryptedKey(encryptedKeyInResponse, ptspCtmk);

        saveTerminalMasterKey(terminalID, clearKey);
    }

    @Override
    public void persistKeyFromResponse(ISOMsg isoResponse) throws ServiceProcessingException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    private void saveTerminalMasterKey(String terminalID, String masterKey) throws ServiceProcessingException {
        String encryptedMasterKey = encryptKey(masterKey);
        TerminalKeys terminalKeys = createOrGetTerminalKeysIfExist(terminalID);
        terminalKeys.setEncryptedTmk(encryptedMasterKey);
        terminalKeyService.save(terminalKeys);
    }
}
