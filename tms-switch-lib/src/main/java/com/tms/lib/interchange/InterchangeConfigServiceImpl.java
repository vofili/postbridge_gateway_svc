package com.tms.lib.interchange;

import com.tms.lib.apimodel.KeyDTO;
import com.tms.lib.exceptions.CryptoException;
import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.security.Encrypter;
import com.tms.lib.util.KeyUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InterchangeConfigServiceImpl implements InterchangeConfigService {

    private final InterchangeConfigRepository interchangeConfigRepository;
    private final Encrypter encrypter;

    @Override
    public InterchangeConfig saveInterchange(InterchangeConfig config) {
        return interchangeConfigRepository.save(config);
    }

    @Override
    public KeyDTO updateInterchangeKey(String interchangeCode, KeyDTO keyDTO) throws ServiceProcessingException {
        InterchangeConfig interchangeConfig = interchangeConfigRepository.findByCode(interchangeCode)
                .orElseThrow(() -> new ServiceProcessingException(String.format("Cannot find interchange config with code %s", interchangeCode)));

        Pair<String, String> keyToKcvPair;
        try {
            keyToKcvPair = KeyUtil.combineKey(keyDTO.getComponent1(), keyDTO.getComponent2());
        } catch (UtilOperationException e) {
            throw new ServiceProcessingException("Could not combine key components", e);
        }

        String combinedComponent = keyToKcvPair.getLeft();
        String generatedKcv = keyToKcvPair.getRight();

        if (!StringUtils.equals(generatedKcv, keyDTO.getKcv())) {
            throw new ServiceProcessingException("Generated kcv and supplied kcv don't match");
        }

        String encryptedInterchangeKey;
        try {
            encryptedInterchangeKey = encrypter.encrypt(combinedComponent);
        } catch (CryptoException e) {
            throw new ServiceProcessingException("There was an errory encrypting combined key", e);
        }

        interchangeConfig.setEncryptedInterchangeKey(encryptedInterchangeKey);
        saveInterchange(interchangeConfig);

        keyDTO.setCombinedComponent(combinedComponent);
        keyDTO.setGeneratedKcv(generatedKcv);
        return keyDTO;
    }

    @Override
    public List<InterchangeConfig> getAllInterchangeConfigs() {
        return interchangeConfigRepository.findAll();
    }

    @Override
    public void deleteInterchange(int id) {
        interchangeConfigRepository.deleteById(id);
    }

    @Override
    public InterchangeConfig updateInterchangeSinkZpk(String encryptedZpk, int configId) throws ServiceProcessingException {
        InterchangeConfig interchangeConfig = interchangeConfigRepository.findById(configId)
                .orElseThrow(() -> new ServiceProcessingException("Cannot find interchange with specified config"));

        interchangeConfig.setEncryptedSinkZpk(encryptedZpk);

        interchangeConfig = interchangeConfigRepository.save(interchangeConfig);
        return interchangeConfig;
    }

    @Override
    public Optional<InterchangeConfig> findByCode(String code) {
        return interchangeConfigRepository.findByCode(code);
    }

    @Override
    public Optional<InterchangeConfig> findById(int id) {
        return interchangeConfigRepository.findById(id);
    }
}

