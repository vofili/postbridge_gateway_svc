package com.tms.lib.interchange;

import com.tms.lib.apimodel.KeyDTO;
import com.tms.lib.exceptions.ServiceProcessingException;

import java.util.List;
import java.util.Optional;

public interface InterchangeConfigService {

    InterchangeConfig saveInterchange(InterchangeConfig config);

    KeyDTO updateInterchangeKey(String interchangeCode, KeyDTO keyDTO) throws ServiceProcessingException;

    List<InterchangeConfig> getAllInterchangeConfigs();

    void deleteInterchange(int id);

    InterchangeConfig updateInterchangeSinkZpk(String zpk, int configId) throws ServiceProcessingException;

    Optional<InterchangeConfig> findByCode(String code);

    Optional<InterchangeConfig> findById(int id);
}
