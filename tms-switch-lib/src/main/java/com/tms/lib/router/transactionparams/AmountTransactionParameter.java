package com.tms.lib.router.transactionparams;

import com.tms.lib.model.TransactionRequest;
import com.tms.lib.router.entities.TransactionParameterType;
import org.springframework.stereotype.Component;

@Component
public class AmountTransactionParameter implements TransactionParameter {

    @Override
    public Long getParameter(TransactionRequest transactionRequest) {
        return transactionRequest == null ? 0L : transactionRequest.getMinorAmount();
    }

    @Override
    public TransactionParameterType getTransactionParameterType() {
        return TransactionParameterType.AMOUNT;
    }
}
