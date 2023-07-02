package com.tms.lib.router.transactionparams;

import com.tms.lib.model.TransactionRequest;
import com.tms.lib.router.entities.TransactionParameterType;
import org.springframework.stereotype.Component;

@Component
public class BinTransactionParameter implements TransactionParameter {

    @Override
    public String getParameter(TransactionRequest transactionRequest) {
        return transactionRequest == null ? "" : transactionRequest.getBin();
    }

    @Override
    public TransactionParameterType getTransactionParameterType() {
        return TransactionParameterType.BIN;
    }
}
