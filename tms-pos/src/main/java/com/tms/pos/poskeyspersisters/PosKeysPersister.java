package com.tms.pos.poskeyspersisters;

import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.model.PtspDetails;
import org.jpos.iso.ISOMsg;

public interface PosKeysPersister {

    void persistKeyFromResponse(ISOMsg isoResponse) throws ServiceProcessingException;

    default void persistKeyFromResponse(ISOMsg isoResponse, String ctmk) throws ServiceProcessingException {

    }
}
