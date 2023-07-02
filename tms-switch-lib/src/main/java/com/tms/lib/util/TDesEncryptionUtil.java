package com.tms.lib.util;

import com.tms.lib.exceptions.CryptoException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Slf4j
public final class TDesEncryptionUtil {

    private TDesEncryptionUtil() {

    }

    public static byte[] generateRandomTdesDoubleLenKey() throws CryptoException {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("DESede");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            generator.init(random);
            SecretKey myKey = generator.generateKey();
            byte[] key = myKey.getEncoded();
            byte[] doubleLenKey = new byte[16];
            System.arraycopy(key, 1, doubleLenKey, 0, 16);
            return doubleLenKey;
        } catch (NoSuchAlgorithmException var5) {
            String msg = "Could not generate random key";
            log.error(msg, var5);
            throw new CryptoException(msg, var5);
        }
    }

    public static byte[] tdesEncryptECB(byte[] data, byte[] keyBytes) throws CryptoException {
        try {
            byte[] key;
            if (keyBytes.length == 16) {
                key = new byte[24];
                System.arraycopy(keyBytes, 0, key, 0, 16);
                System.arraycopy(keyBytes, 0, key, 16, 8);
            } else {
                key = keyBytes;
            }

            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(1, new SecretKeySpec(key, "DESede"));
            return cipher.doFinal(data);
        } catch (InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException var6) {
            String msg = "Could not TDES encrypt ";
            log.error(msg, var6);
            throw new CryptoException(msg, var6);
        }
    }

    public static byte[] tdesDecryptECB(byte[] data, byte[] keyBytes) throws CryptoException {
        try {
            byte[] key;
            if (keyBytes.length == 16) {
                key = new byte[24];
                System.arraycopy(keyBytes, 0, key, 0, 16);
                System.arraycopy(keyBytes, 0, key, 16, 8);
            } else {
                key = keyBytes;
            }

            Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
            cipher.init(2, new SecretKeySpec(key, "DESede"));
            return cipher.doFinal(data);
        } catch (InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException var6) {
            String msg = "Could not TDES decrypt ";
            log.error(msg, var6);
            throw new CryptoException(msg, var6);
        }
    }

    public static byte[] generateKeyCheckValue(byte[] key) throws CryptoException {
        byte[] zeroBytes = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] fullKeyCheck = tdesEncryptECB(zeroBytes, key);
        byte[] keyCheckValue = new byte[3];
        System.arraycopy(fullKeyCheck, 0, keyCheckValue, 0, keyCheckValue.length);

        return keyCheckValue;
    }

    public static byte[] exclusiveOr(byte[] data1, byte[] data2) {
        byte[] result = new byte[Math.min(data1.length, data2.length)];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (((int) data1[i]) ^ ((int) data2[i]));
        }
        return result;
    }
}
