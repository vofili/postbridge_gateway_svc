package com.tms.lib.router;

import com.tms.lib.exceptions.RouterException;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;

public interface Router {

    TransactionResponse route(TransactionRequest request) throws RouterException;

    TransactionResponse send(TransactionRequest request, Long sinkInterchangeId) throws RouterException;

    void reloadRules();
}
