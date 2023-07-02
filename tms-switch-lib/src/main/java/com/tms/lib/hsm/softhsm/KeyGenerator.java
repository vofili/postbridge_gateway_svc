package com.tms.lib.hsm.softhsm;

import com.tms.lib.exceptions.CryptoException;
import com.tms.lib.exceptions.HsmException;
import com.tms.lib.hsm.model.GeneratedKeyMessage;
import com.tms.lib.util.TDesEncryptionUtil;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

public class KeyGenerator {

    private KeyGenerator() {

    }

    /**
     * generates a key to be returned to the remote entity
     *
     * @param encryptionKey the key shared between the two parties stored under the lmk
     * @return a generated key message containing the key under Lmk, key check value and key under the encryption key
     * @throws HsmException
     */
    public static GeneratedKeyMessage generateKey(String encryptionKey) throws HsmException {
        if (StringUtils.isEmpty(encryptionKey)) {
            return null;
        }
        byte[] clearZmk;
        try {
            clearZmk = Hex.decodeHex(encryptionKey.toCharArray());
        } catch (DecoderException e) {
            throw new HsmException("Could not decode hex encoded key under lmk to bytes", e);
        }
        try {
            byte[] tDesDoubleLenKey = TDesEncryptionUtil.generateRandomTdesDoubleLenKey();

            byte[] zpkUnderZmk = TDesEncryptionUtil.tdesEncryptECB(tDesDoubleLenKey, clearZmk);

            byte[] keyCheckValue = TDesEncryptionUtil.generateKeyCheckValue(tDesDoubleLenKey);

            GeneratedKeyMessage generatedZpkMessage = new GeneratedKeyMessage();
            generatedZpkMessage.setKeyCheckValue(new String(Hex.encodeHex(keyCheckValue)));
            generatedZpkMessage.setKeyUnderLmk(new String(Hex.encodeHex(tDesDoubleLenKey)));
            generatedZpkMessage.setKeyUnderEncryptionKey(new String(Hex.encodeHex(zpkUnderZmk)));
            return generatedZpkMessage;
        } catch (CryptoException e) {
            throw new HsmException("Error generating key ", e);
        }
    }
}
