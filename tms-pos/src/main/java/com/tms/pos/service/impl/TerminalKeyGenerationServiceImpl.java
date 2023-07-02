package com.tms.pos.service.impl;

import com.tms.lib.exceptions.CryptoException;
import com.tms.lib.exceptions.HsmException;
import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.hsm.HsmService;
import com.tms.lib.hsm.model.GeneratedKeyMessage;
import com.tms.lib.security.Encrypter;
import com.tms.lib.terminals.entities.TerminalKeys;
import com.tms.lib.terminals.services.TerminalKeyService;
import com.tms.lib.terminals.services.TmsCtmkService;
import com.tms.lib.util.TDesEncryptionUtil;
import com.tms.pos.service.TerminalKeyGenerationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TerminalKeyGenerationServiceImpl implements TerminalKeyGenerationService {

    private final HsmService hsmSDKService;
    private final TerminalKeyService terminalKeyService;
    private final Encrypter encrypter;
    private final TmsCtmkService tmsCtmkService;

    @Override
    public Pair<byte[], byte[]> generateTerminalMasterKey(String terminalId) throws ServiceProcessingException {
        Optional<TerminalKeys> terminalKeysOptional = terminalKeyService.findByTerminalIdOptional(terminalId);

        TerminalKeys terminalKeys = terminalKeysOptional.orElseGet(() -> {
            TerminalKeys terminalKeys1 = new TerminalKeys();
            terminalKeys1.setTerminalId(terminalId);
            return terminalKeys1;
        });

        GeneratedKeyMessage generatedKeyMessage;
        try {
            generatedKeyMessage = hsmSDKService.generateTMKAndEncryptUnderZmk(tmsCtmkService.getCtmk());
        } catch (HsmException e) {
            throw new ServiceProcessingException("Could not generate tmk", e);
        }

        terminalKeys.setTmkUnderLmk(generatedKeyMessage.getKeyUnderLmk().toUpperCase());

        String keyUnderEncryptionKey = generatedKeyMessage.getKeyUnderEncryptionKey();

        String clearCtmk = tmsCtmkService.getCtmk();

        String clearTmk;
        try {
            clearTmk = Hex.encodeHexString(
                    TDesEncryptionUtil.tdesDecryptECB(
                            Hex.decodeHex(keyUnderEncryptionKey.toUpperCase().toCharArray()),
                            Hex.decodeHex(clearCtmk.toUpperCase().toCharArray())));
            terminalKeys.setEncryptedTmk(encrypter.encrypt(clearTmk.toUpperCase()));
        } catch (CryptoException | DecoderException e) {
            throw new ServiceProcessingException("Could not encrypt generated tmk", e);
        }

        terminalKeyService.save(terminalKeys);

        return extractByteKeysFromGeneratedKeyMessage(generatedKeyMessage);
    }


    @Override
    public Pair<byte[], byte[]> generateTerminalSessionKey(String terminalId) throws ServiceProcessingException {
        TerminalKeys terminalKeys = terminalKeyService.findByTerminalId(terminalId);

        String tmkUnderLmk = terminalKeys.getTmkUnderLmk();

        GeneratedKeyMessage generatedKeyMessage;
        try {
            generatedKeyMessage = hsmSDKService.generateTSKAndEncryptUnderTmk(tmkUnderLmk);
        } catch (HsmException e) {
            throw new ServiceProcessingException("Could not generate tsk", e);
        }

        terminalKeys.setTskUnderLmk(generatedKeyMessage.getKeyUnderLmk().toUpperCase());

        String keyUnderEncryptionKey = generatedKeyMessage.getKeyUnderEncryptionKey();

        String clearTmk;

        String clearTsk;
        try {

            clearTmk = encrypter.decrypt(terminalKeys.getEncryptedTmk());

            clearTsk = Hex.encodeHexString(TDesEncryptionUtil.tdesDecryptECB(Hex.decodeHex(keyUnderEncryptionKey.toUpperCase().toCharArray()),
                    Hex.decodeHex(clearTmk.toCharArray())));

            terminalKeys.setEncryptedTsk(encrypter.encrypt(clearTsk.toUpperCase()));
        } catch (CryptoException | DecoderException e) {
            throw new ServiceProcessingException("Could not encrypt generated tsk", e);
        }


        terminalKeyService.save(terminalKeys);

        return extractByteKeysFromGeneratedKeyMessage(generatedKeyMessage);
    }

    @Override
    public Pair<byte[], byte[]> generateTerminalPinKey(String terminalId) throws ServiceProcessingException {
        TerminalKeys terminalKeys = terminalKeyService.findByTerminalId(terminalId);

        String tmkUnderLmk = terminalKeys.getTmkUnderLmk();

        GeneratedKeyMessage generatedKeyMessage;
        try {
            generatedKeyMessage = hsmSDKService.generateTPKAndEncryptUnderTmk(tmkUnderLmk);
        } catch (HsmException e) {
            throw new ServiceProcessingException("Could not generate tpk", e);
        }

        terminalKeys.setTpkUnderLmk(generatedKeyMessage.getKeyUnderLmk().toUpperCase());

        String keyUnderEncryptionKey = generatedKeyMessage.getKeyUnderEncryptionKey();

        String clearTmk;
        String clearTpk;

        try {

            clearTmk = encrypter.decrypt(terminalKeys.getEncryptedTmk());

            clearTpk = Hex.encodeHexString(TDesEncryptionUtil.tdesDecryptECB(Hex.decodeHex(keyUnderEncryptionKey.toUpperCase().toCharArray()),
                    Hex.decodeHex(clearTmk.toCharArray())));

            terminalKeys.setEncryptedTpk(encrypter.encrypt(clearTpk.toUpperCase()));
        } catch (CryptoException | DecoderException e) {
            throw new ServiceProcessingException("Could not encrypt generated tpk", e);
        }

        terminalKeyService.save(terminalKeys);

        return extractByteKeysFromGeneratedKeyMessage(generatedKeyMessage);
    }

    private Pair<byte[], byte[]> extractByteKeysFromGeneratedKeyMessage(GeneratedKeyMessage generatedKeyMessage) throws ServiceProcessingException {
        byte[] key;
        byte[] keyCheckValue;
        try {
            key = Hex.decodeHex(generatedKeyMessage.getKeyUnderEncryptionKey().toUpperCase().toCharArray());
            keyCheckValue = Hex.decodeHex(generatedKeyMessage.getKeyCheckValue().toUpperCase().toCharArray());
        } catch (DecoderException e) {
            throw new ServiceProcessingException("Could not hex decode key", e);
        }

        return new ImmutablePair<>(key, keyCheckValue);
    }
}
