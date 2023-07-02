package com.tms.pos.service;

import com.tms.lib.exceptions.CryptoException;
import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.security.Encrypter;
import com.tms.lib.terminals.entities.TerminalKeys;
import com.tms.lib.terminals.services.TerminalKeyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
public class TerminalMacValidator {

    private Encrypter encrypter;
    private TerminalKeyService terminalKeyService;

    @Autowired
    public TerminalMacValidator(Encrypter encrypter, TerminalKeyService terminalKeyService) {
        this.encrypter = encrypter;
        this.terminalKeyService = terminalKeyService;
    }

    public boolean isValidMessage(ISOMsg isoMsg) throws ServiceProcessingException {
        if (isExcludedMessageType(isoMsg)) {
            return true;
        }

        if (isoMsg.hasField(128)) {
            return isValidHashForMessage(isoMsg, 128);
        } else if (isoMsg.hasField(64)) {
            return isValidHashForMessage(isoMsg, 64);
        } else {
            return false;
        }
    }

    private boolean isExcludedMessageType(ISOMsg isoMsg) throws ServiceProcessingException {
        try {
            String processingCode = isoMsg.getString(3);
            return "0800".equals(isoMsg.getMTI()) && (processingCode.startsWith("9A")
                    || processingCode.startsWith("9G") || processingCode.startsWith("9B"));
        } catch (ISOException e) {
            throw new ServiceProcessingException("Could not get message type indicator", e);
        }
    }

    private boolean isValidHashForMessage(ISOMsg isoMsg, int fieldNo) throws ServiceProcessingException {
        String hashValueFromTerminal = isoMsg.getString(fieldNo);
        String generatedHashValue = generateHashForIsoMsg(isoMsg);
        return StringUtils.equalsIgnoreCase(hashValueFromTerminal, generatedHashValue);
    }

    public String generateHashForIsoMsg(ISOMsg isoMsg) throws ServiceProcessingException {
        TerminalKeys terminalKeys = terminalKeyService.findByTerminalId(isoMsg.getString(41));
        String key = terminalKeys.getEncryptedTsk();

        if (StringUtils.isEmpty(key)) {
            throw new ServiceProcessingException("No session key found for terminal");
        }

        String clearKey;
        try {
            clearKey = encrypter.decrypt(key);
        } catch (CryptoException e) {
            throw new ServiceProcessingException("Could not retrieve terminal session key");
        }

        try {
            byte[] data = isoMsg.pack();
            int length = data.length;
            byte[] dataToHash = new byte[length - 64];
            if (length >= 64) {
                System.arraycopy(data, 0, dataToHash, 0, dataToHash.length);
            }

            String generatedHashValue = hash(dataToHash, Hex.decodeHex(clearKey.toCharArray()));

            return StringUtils.leftPad(generatedHashValue, 64, '0').toUpperCase();
        } catch (ISOException e) {
            throw new ServiceProcessingException("Could not pack iso msg as bytes", e);
        } catch (DecoderException e) {
            throw new ServiceProcessingException("Could not decode session key", e);
        }
    }

    private String hash(byte[] data, byte[] key) {
        MessageDigest md = getDigest();
        md.update(key);
        md.update(data);
        return Hex.encodeHexString(md.digest());
    }

    private MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e);
        }
    }
}
