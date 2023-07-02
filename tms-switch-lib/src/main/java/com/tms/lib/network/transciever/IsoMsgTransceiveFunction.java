package com.tms.lib.network.transciever;

import com.tms.lib.exceptions.InterchangeIOException;
import com.tms.lib.exceptions.InterchangeServiceException;
import org.jpos.iso.ISOMsg;

public interface IsoMsgTransceiveFunction extends TranscieveFunction<ISOMsg, ISOMsg> {

    @Override
    ISOMsg transcieve(ISOMsg t) throws InterchangeIOException, InterchangeServiceException;
}
