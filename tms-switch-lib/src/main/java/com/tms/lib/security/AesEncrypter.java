package com.tms.lib.security;

import com.tms.lib.exceptions.CryptoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Data encryption procedure
 * - Perform encryption on data using key for encryption and a random IV generated
 * - The encrypted data is encoded as base64
 * - The IV is concatenated with the base64 encoded encrypted data using a delimiter
 * - The concatenated result is then encoded as base64 and returned
 * <p>
 * Data decryption procedure
 * - The encrypted data is encoded as base64 and as such is decoded
 * - The decoded data is split with the delimiter to the IV and the base64 encoded data
 * - the base64 encoded data is the decoded to bytes
 * - The IV and the encryption key is used to perform decryption of the data (edited)
 */
@Component
public final class AesEncrypter implements Encrypter {
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    @Value("${app.encryption.key:EA2C018A3BF8C0D4}")
    private String encryptionKey;

    public String encrypt(String data) throws CryptoException {
        if (StringUtils.isEmpty(data)) {
            throw new CryptoException("Invalid data supplied");
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            String nonce = getIV();
            byte[] iv = nonce.getBytes();
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec(), new IvParameterSpec(iv));
            byte[] encryptedDataBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String encryptedData = Base64.getEncoder().encodeToString(encryptedDataBytes);
            String builder = nonce + ":" + encryptedData;
            return Base64.getEncoder().encodeToString(builder.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new CryptoException("An error occurred while encrypting data", e);
        }
    }

    private String getIV() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }

    public String decrypt(String data) throws CryptoException {
        if (StringUtils.isEmpty(data)) {
            throw new CryptoException("Invalid data supplied");
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            String combinedData = new String(Base64.getDecoder().decode((data)));
            int lastIndexOfColon = combinedData.lastIndexOf(':');
            String nonce = combinedData.substring(0, lastIndexOfColon);
            String encryptedDataString = combinedData.substring(lastIndexOfColon + 1);
            byte[] iv = nonce.getBytes();
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec(), new IvParameterSpec(iv));
            byte[] encryptedData = Base64.getDecoder().decode(encryptedDataString);
            byte[] decryptedDataBytes = cipher.doFinal(encryptedData);
            return new String(decryptedDataBytes);
        } catch (Exception e) {
            throw new CryptoException("An error occurred while decrypting data", e);
        }
    }

    private SecretKeySpec getSecretKeySpec() {
        return new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
    }
}
