package com.tms.lib.hsm.softhsm;

import com.tms.lib.exceptions.CryptoException;
import com.tms.lib.exceptions.HsmException;
import com.tms.lib.hsm.HsmService;
import com.tms.lib.hsm.model.GeneratedKeyMessage;
import com.tms.lib.hsm.model.PinTranslationRequest;
import com.tms.lib.util.TDesEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SoftHsmServiceImpl implements HsmService {

    @Override
    public GeneratedKeyMessage generateTMKAndEncryptUnderZmk(String encryptionKey) throws HsmException {
        return KeyGenerator.generateKey(encryptionKey);
    }

    @Override
    public GeneratedKeyMessage generateTSKAndEncryptUnderTmk(String encryptionKey) throws HsmException {
        return KeyGenerator.generateKey(encryptionKey);
    }

    @Override
    public GeneratedKeyMessage generateTPKAndEncryptUnderTmk(String encryptionKey) throws HsmException {
        return KeyGenerator.generateKey(encryptionKey);
    }

    @Override
    public GeneratedKeyMessage convertZpkUnderZmkToZpkUnderLmk(String zpkUnderZmk, String zmkUnderLmk) throws HsmException {
        if (StringUtils.isEmpty(zpkUnderZmk) || StringUtils.isEmpty(zmkUnderLmk)) {
            return null;
        }
        try {
            byte[] zpkUnderZmkBytes = Hex.decodeHex(zpkUnderZmk.toCharArray());
            byte[] zmkUnderLmkBytes = Hex.decodeHex(zmkUnderLmk.toCharArray());

            byte[] decodedZpk = TDesEncryptionUtil.tdesDecryptECB(zpkUnderZmkBytes, zmkUnderLmkBytes);
            byte[] keyCheck = TDesEncryptionUtil.generateKeyCheckValue(decodedZpk);

            String zpkUnderLmkHex = new String(Hex.encodeHex(decodedZpk));
            String keyCheckValue = new String(Hex.encodeHex(keyCheck));
            GeneratedKeyMessage generatedZpkMessage = new GeneratedKeyMessage();
            generatedZpkMessage.setKeyCheckValue(keyCheckValue);
            generatedZpkMessage.setKeyUnderLmk(zpkUnderLmkHex);
            generatedZpkMessage.setKeyUnderEncryptionKey(zpkUnderZmk);

            return generatedZpkMessage;

        } catch (CryptoException e) {
            throw new HsmException("Could not convert zpk under zmk to zpk under lmk", e);
        } catch (DecoderException e) {
            throw new HsmException("Could not decode hex string to bytes", e);
        }
    }

    @Override
    public String translatePinBlockFromTpkToDestinationZpk(PinTranslationRequest pinTranslationRequest) throws HsmException {
        String pinBlockUnderTpk = pinTranslationRequest.getPinBlock();
        String destinationZpk = pinTranslationRequest.getDestinationZpk();
        String tpk = pinTranslationRequest.getSourceZpk();

//        log.info("Pin block under tpk {}", pinBlockUnderTpk);
//        log.info("Destination zpk {}", destinationZpk);
//        log.info("Source tpk {}", tpk);
        if (StringUtils.isEmpty(pinBlockUnderTpk) || StringUtils.isEmpty(destinationZpk) || StringUtils.isEmpty(tpk)) {
            throw new HsmException("Invalid pin translation params supplied. Pin block, destination zpk and source zpk must be supplied");
        }

        try {
            byte[] clearPinBlock = TDesEncryptionUtil.tdesDecryptECB(Hex.decodeHex(pinBlockUnderTpk.toCharArray()),
                    Hex.decodeHex(tpk.toCharArray()));

//            log.info("Clear pin block {}", new String(Hex.encodeHex(clearPinBlock)));
            return new String(Hex.encodeHex(TDesEncryptionUtil.tdesEncryptECB(clearPinBlock,
                    Hex.decodeHex(destinationZpk.toCharArray()))));
        } catch (DecoderException | CryptoException e) {
            throw new HsmException("Could not convert pin block from source to destination encryption", e);
        }
    }
}
