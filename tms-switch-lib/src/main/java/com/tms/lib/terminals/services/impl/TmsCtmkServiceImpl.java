package com.tms.lib.terminals.services.impl;

import com.tms.lib.apimodel.KeyDTO;
import com.tms.lib.exceptions.CryptoException;
import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.security.Encrypter;
import com.tms.lib.terminals.entities.TmsCtmk;
import com.tms.lib.terminals.repository.TmsCtmkRepository;
import com.tms.lib.terminals.services.TmsCtmkService;
import com.tms.lib.util.KeyUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TmsCtmkServiceImpl implements TmsCtmkService {

    private final TmsCtmkRepository tmsCtmkRepository;
    private final Encrypter encrypter;


    @Override
    public KeyDTO createCtmk(KeyDTO keyDTO) throws ServiceProcessingException {
        TmsCtmk tmsCtmk = new TmsCtmk();

        try {
            Pair<String, String> keyToKcvPair = KeyUtil.combineKey(keyDTO.getComponent1(), keyDTO.getComponent2());

            String generatedKcv = keyToKcvPair.getRight();
            String combinedComponents = keyToKcvPair.getLeft();

            if (!generatedKcv.equalsIgnoreCase(keyDTO.getKcv())) {
                throw new ServiceProcessingException("Generated key check value doesn't match supplied kcv");
            }

            tmsCtmk.setEncryptedComponent1(encrypter.encrypt(keyDTO.getComponent1()));
            tmsCtmk.setEncryptedComponent2(encrypter.encrypt(keyDTO.getComponent2()));
            tmsCtmk.setEncryptedCtmk(encrypter.encrypt(combinedComponents));
            tmsCtmk.setKeyCheckValue(generatedKcv);
            tmsCtmkRepository.save(tmsCtmk);

            keyDTO.setCombinedComponent(combinedComponents);
            keyDTO.setGeneratedKcv(generatedKcv);

            return keyDTO;
        } catch (CryptoException | UtilOperationException e) {
            throw new ServiceProcessingException("Could not save ctmk", e);
        }
    }

    public String getCtmk() throws ServiceProcessingException {
        TmsCtmk tmsCtmk = tmsCtmkRepository.findByActive(true);
        if (tmsCtmk == null) {
            throw new ServiceProcessingException("No configured CTMK was found for TMS");
        }
        try {
            return encrypter.decrypt(tmsCtmk.getEncryptedCtmk());
        } catch (CryptoException e) {
            throw new ServiceProcessingException("Could not get ctmk for tms", e);
        }
    }
}
