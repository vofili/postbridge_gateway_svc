package com.tms.pos.source.processors.rest;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.service.MappedTerminalKeyDownloadService;
import com.tms.pos.model.TerminalConfigurationDetails;
import com.tms.pos.service.TerminalDetailsService;
import com.tms.pos.utils.POSMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class ParametersDownloadRestSourceTransactionProcessor implements PosRestSourceTransactionProcessor {

    private TerminalDetailsService terminalDetailsService;
    private MappedTerminalKeyDownloadService mappedTerminalKeyDownloadService;

    @Autowired
    public ParametersDownloadRestSourceTransactionProcessor(TerminalDetailsService terminalDetailsService, MappedTerminalKeyDownloadService mappedTerminalKeyDownloadService) {
        this.terminalDetailsService = terminalDetailsService;
        this.mappedTerminalKeyDownloadService = mappedTerminalKeyDownloadService;
    }


    @Override
    public boolean canTreat(TransactionRequest transactionRequest) {
        return POSMessageUtils.isParametersDownload(transactionRequest.getProcessingCode());
    }

    @Override
    public TransactionResponse treat(TransactionRequest transactionRequest) throws TransactionProcessingException {
        TransactionResponse parametersDownloadTransactionResponse = new TransactionResponse(transactionRequest);

        try {

            String terminalId = transactionRequest.getTerminalId();
            if (StringUtils.isEmpty(terminalId)) {
                throw new TransactionProcessingException("Terminal id is null, returning 96");
            }

            Optional<TerminalConfigurationDetails> terminal = terminalDetailsService.findByTerminalId(terminalId);

            TerminalConfigurationDetails terminalConfigurationDetails = terminal.orElseThrow(
                    () -> new TransactionProcessingException(String.format("Unknown terminal with id %s", terminalId)));

            parametersDownloadTransactionResponse.setIsoResponseCode("00");
            parametersDownloadTransactionResponse.setCardAcceptorId(terminalConfigurationDetails.getCardAcceptorId());
            parametersDownloadTransactionResponse.setCardAcceptorLocation(terminalConfigurationDetails.getMerchantNameLocation());
            parametersDownloadTransactionResponse.setMerchantType(terminalConfigurationDetails.getMcc());
            parametersDownloadTransactionResponse.setTransactionCurrencyCode(terminalConfigurationDetails.getCurrencyCode());

            log.trace("Parameters download message processed successfully");

            return parametersDownloadTransactionResponse;
        } catch (Exception e) {
            throw new TransactionProcessingException("Could not process parameters download", e);
        }
    }
}
