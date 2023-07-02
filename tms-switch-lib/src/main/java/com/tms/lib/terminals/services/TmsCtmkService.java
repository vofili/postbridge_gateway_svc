package com.tms.lib.terminals.services;

import com.tms.lib.apimodel.KeyDTO;
import com.tms.lib.exceptions.ServiceProcessingException;

public interface TmsCtmkService {

    public KeyDTO createCtmk(KeyDTO keyDTO) throws ServiceProcessingException;

    public String getCtmk() throws ServiceProcessingException;
}
