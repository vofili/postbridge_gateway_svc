package com.tms.lib.router.transactionparams;

import com.tms.lib.model.TransactionRequest;
import com.tms.lib.router.entities.TransactionParameterType;
import org.springframework.stereotype.Component;

@Component
public class PanTransactionParameter implements TransactionParameter {

    @Override
    public String getParameter(TransactionRequest transactionRequest) {
        return transactionRequest == null ? "" : transactionRequest.getPan();
    }

    @Override
    public TransactionParameterType getTransactionParameterType() {
        return TransactionParameterType.PAN;
    }
}
