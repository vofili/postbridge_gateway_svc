package com.tms.lib.router.transactionparams;

import com.tms.lib.model.TransactionRequest;
import com.tms.lib.router.entities.TransactionParameterType;

public interface TransactionParameter {

    Object getParameter(TransactionRequest transactionRequest);

    TransactionParameterType getTransactionParameterType();
}
