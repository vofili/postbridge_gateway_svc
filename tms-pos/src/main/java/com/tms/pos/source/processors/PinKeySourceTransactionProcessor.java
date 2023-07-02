package com.tms.pos.source.processors;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.util.IsoUtil;
import com.tms.pos.service.TerminalKeyGenerationService;
import com.tms.pos.source.PosSourceIsoChannelAdapter;
import com.tms.pos.utils.POSMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PinKeySourceTransactionProcessor implements PosSourceTransactionProcessor {

    private TerminalKeyGenerationService terminalKeyGenerationService;

    @Autowired
    public PinKeySourceTransactionProcessor(TerminalKeyGenerationService terminalKeyGenerationService) {
        this.terminalKeyGenerationService = terminalKeyGenerationService;
    }

    @Override
    public boolean canConvert(ISOMsg isoMsg) {
        return canTreat(isoMsg);
    }

    @Override
    public TransactionRequest toTransactionRequest(ISOMsg isoMsg) throws TransactionProcessingException {
        log.trace("Creating a pin key download Request");
        TransactionRequest pinKeyTransactionRequest = new TransactionRequest();
        try {
            PosSourceIsoChannelAdapter.commonIsoMsgToTransactionRequest(isoMsg, pinKeyTransactionRequest);
            return pinKeyTransactionRequest;
        } catch (ISOException | UtilOperationException e) {
            String msg = "Could not convert pin key request";
            log.error(msg, e);
            throw new TransactionProcessingException(msg, e);
        }
    }

    @Override
    public boolean canTreat(ISOMsg isoMsg) {
        return POSMessageUtils.isPinKeyDownload(isoMsg);
    }

    @Override
    public Pair<TransactionResponse, ISOMsg> treat(ISOMsg isoMsg, TransactionRequest transactionRequest) throws TransactionProcessingException {
        TransactionResponse pinKeyTransactionResponse = new TransactionResponse(transactionRequest);
        ISOMsg responseIso = new ISOMsg();
        try {


            IsoUtil.copyFields(isoMsg, responseIso);
            IsoUtil.setMti(responseIso, getResponseMti());

            responseIso.unset(3);
            responseIso.unset(62);

            String terminalId = isoMsg.getString(41);
            if (StringUtils.isEmpty(terminalId)) {
                throw new TransactionProcessingException("Terminal id is null, returning 96");
            }

            Pair<byte[], byte[]> keyToCheckValue = terminalKeyGenerationService.generateTerminalPinKey(terminalId);


            responseIso.set(39, "00");
            pinKeyTransactionResponse.setIsoResponseCode("00");
            responseIso.set(53, Hex.encodeHexString(IsoUtil.buildKeyExchangeField53(keyToCheckValue.getKey(), keyToCheckValue.getValue())).toUpperCase());

            log.trace("Pin key download message processed successfully");

            return new ImmutablePair<>(pinKeyTransactionResponse, responseIso);
        } catch (Exception e) {
            throw new TransactionProcessingException("Could not process pin key download", e);
        }
    }

    @Override
    public String getResponseMti() {
        return "0810";
    }
}
