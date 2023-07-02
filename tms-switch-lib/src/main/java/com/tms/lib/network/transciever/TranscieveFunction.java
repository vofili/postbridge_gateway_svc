package com.tms.lib.network.transciever;

import com.tms.lib.exceptions.InterchangeIOException;
import com.tms.lib.exceptions.InterchangeServiceException;

public interface TranscieveFunction<Request, Response> {

    Response transcieve(Request t) throws InterchangeIOException, InterchangeServiceException;
}
