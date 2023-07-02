package com.tms.pos.poskeyspersisters.impl;

import com.tms.lib.exceptions.CryptoException;
import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.security.Encrypter;
import com.tms.lib.terminals.entities.TerminalKeys;
import com.tms.lib.terminals.services.TerminalKeyService;
import com.tms.lib.util.TDesEncryptionUtil;
import com.tms.pos.poskeyspersisters.PosKeysPersister;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
abstract class PosKeysPersisterImpl implements PosKeysPersister {

    @Autowired
    private Encrypter encrypter;

    @Autowired
    TerminalKeyService terminalKeyService;

    String getTerminalIDFromResponse(ISOMsg isoResponse) throws ServiceProcessingException {
        String terminalID = isoResponse.getString(41);
        if (StringUtils.isBlank(terminalID)) {
            throw new ServiceProcessingException("Empty terminal ID found when trying to persist master key");
        }

        return terminalID;
    }

    String getTerminalMasterKey(String terminalID) throws ServiceProcessingException {
        TerminalKeys terminalKeys = terminalKeyService.findByTerminalIdOptional(terminalID)
                .orElseThrow(() -> new ServiceProcessingException("No terminal keys found for terminal ID " + terminalID));

        return decryptKey(terminalKeys.getEncryptedTmk());
    }

    String decryptEncryptedKey(String encryptedKey, String key) throws ServiceProcessingException {
        try {
            return tdesDecryptECB(encryptedKey, key);
        } catch (CryptoException | DecoderException e) {
            throw new ServiceProcessingException("Failed to decrypt the encrypted key", e);
        }
    }

    private String tdesDecryptECB(String encryptedData, String key) throws DecoderException, CryptoException {
        return Hex.encodeHexString(
                TDesEncryptionUtil.tdesDecryptECB(
                        Hex.decodeHex(encryptedData.toUpperCase().toCharArray()),
                        Hex.decodeHex(key.toUpperCase().toCharArray()))).toUpperCase();
    }

    String encryptKey(String clearKey) throws ServiceProcessingException {
        try {
            return encrypter.encrypt(clearKey);
        } catch (CryptoException ex) {
            throw new ServiceProcessingException("Failed to encrypt the clear key", ex);
        }
    }

    String decryptKey(String encryptedKey) throws ServiceProcessingException {
        try {
            return encrypter.decrypt(encryptedKey);
        } catch (CryptoException ex) {
            throw new ServiceProcessingException("Failed to decrypt the encrypted key", ex);
        }
    }

    TerminalKeys                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  createOrGetTerminalKeysIfExist(String terminalID) {
        return terminalKeyService.findByTerminalIdOptional(terminalID).orElseGet(() -> {
            TerminalKeys terminalKeys = new TerminalKeys();
            terminalKeys.setTerminalId(terminalID);
            return terminalKeys;
        });
    }
}
