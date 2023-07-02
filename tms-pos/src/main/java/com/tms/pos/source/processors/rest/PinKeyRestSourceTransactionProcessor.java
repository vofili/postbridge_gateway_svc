package com.tms.pos.source.processors.rest;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.model.DefaultIsoResponseCodes;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.pos.service.TerminalKeyGenerationService;
import com.tms.pos.utils.POSMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PinKeyRestSourceTransactionProcessor implements PosRestSourceTransactionProcessor {

    private TerminalKeyGenerationService terminalKeyGenerationService;

    @Autowired
    public PinKeyRestSourceTransactionProcessor(TerminalKeyGenerationService terminalKeyGenerationService) {
        this.terminalKeyGenerationService = terminalKeyGenerationService;
    }

    @Override
    public boolean canTreat(TransactionRequest transactionRequest) {
        return POSMessageUtils.isPinKeyDownload(transactionRequest.getProcessingCode());
    }

    @Override
    public TransactionResponse treat(TransactionRequest transactionRequest) throws TransactionProcessingException {
        TransactionResponse pinKeyChannelResponse = new TransactionResponse(transactionRequest);

        try {

            String terminalId = transactionRequest.getTerminalId();
            if (StringUtils.isEmpty(terminalId)) {
                throw new TransactionProcessingException("Terminal id is null, returning 96");
            }

            Pair<byte[], byte[]> keyToCheckValue = terminalKeyGenerationService.generateTerminalPinKey(terminalId);


            String key = Hex.encodeHexString(keyToCheckValue.getKey()).toUpperCase();
            String keyCheckValue = Hex.encodeHexString(keyToCheckValue.getValue()).toUpperCase();


            pinKeyChannelResponse.setIsoResponseCode(DefaultIsoResponseCodes.Approved.toString());
            pinKeyChannelResponse.setKeyUnderEncryptionKey(key);
            pinKeyChannelResponse.setKeyCheckValue(keyCheckValue);

            log.trace("Pin key download message processed successfully");

            return pinKeyChannelResponse;
        } catch (Exception e) {
            throw new TransactionProcessingException("Could not process pin key download", e);
        }
    }

}
