package com.tms.pos.controller;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.pos.model.cusotmtransferrequest.CustomTransactionRequest;
import com.tms.pos.service.PosTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
public class PosTransactionController {

    private final PosTransactionService posTransactionService;

    @PostMapping("/process-request")
    public TransactionResponse processTransaction(@RequestBody @Validated TransactionRequest transactionRequest) throws TransactionProcessingException {
        return posTransactionService.processTransactionRequest(transactionRequest);
    }

    @PostMapping("/process-custom-request")
    public TransactionResponse processCustomTransactionRequest(@RequestBody @Validated CustomTransactionRequest transactionRequest) throws TransactionProcessingException {
        return posTransactionService.processCustomTransactionRequest(transactionRequest);
    }
}
