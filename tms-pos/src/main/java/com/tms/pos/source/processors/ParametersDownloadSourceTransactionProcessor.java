package com.tms.pos.source.processors;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.util.IsoUtil;
import com.tms.pos.model.TerminalConfigurationDetails;
import com.tms.pos.service.TerminalDetailsService;
import com.tms.pos.source.PosSourceIsoChannelAdapter;
import com.tms.pos.utils.POSMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class ParametersDownloadSourceTransactionProcessor implements PosSourceTransactionProcessor {

    private TerminalDetailsService terminalDetailsService;

    @Autowired
    public ParametersDownloadSourceTransactionProcessor(TerminalDetailsService terminalDetailsService) {
        this.terminalDetailsService = terminalDetailsService;
    }

    @Override
    public boolean canConvert(ISOMsg isoMsg) {
        return canTreat(isoMsg);
    }

    @Override
    public TransactionRequest toTransactionRequest(ISOMsg isoMsg) throws TransactionProcessingException {
        log.trace("Creating a parameters download Request");
        TransactionRequest parametersDownloadTransactionRequest = new TransactionRequest();
        try {
            PosSourceIsoChannelAdapter.commonIsoMsgToTransactionRequest(isoMsg, parametersDownloadTransactionRequest);
            return parametersDownloadTransactionRequest;
        } catch (ISOException | UtilOperationException e) {
            String msg = "Could not convert parameters download request";
            log.error(msg, e);
            throw new TransactionProcessingException(msg, e);
        }
    }

    @Override
    public boolean canTreat(ISOMsg isoMsg) {
        return POSMessageUtils.isParametersDownload(isoMsg);
    }

    @Override
    public Pair<TransactionResponse, ISOMsg> treat(ISOMsg isoMsg, TransactionRequest transactionRequest) throws TransactionProcessingException {
        TransactionResponse masterKeyChannelResponse = new TransactionResponse(transactionRequest);

        ISOMsg responseIso = new ISOMsg();
        try {

            IsoUtil.copyFields(isoMsg, responseIso);
            IsoUtil.setMti(responseIso, getResponseMti());

            responseIso.unset(3);
            responseIso.unset(62);
            responseIso.unset(64);

            String terminalId = isoMsg.getString(41);
            if (StringUtils.isEmpty(terminalId)) {
                throw new TransactionProcessingException("Terminal id is null, returning 96");
            }

            Optional<TerminalConfigurationDetails> terminal = terminalDetailsService.findByTerminalId(terminalId);

            IsoUtil.setIsoField(responseIso, 39, "00");
            masterKeyChannelResponse.setIsoResponseCode("00");
            IsoUtil.setIsoField(responseIso, 62, buildF62(terminal.orElseThrow(
                    () -> new TransactionProcessingException(String.format("Unknown terminal with id %s", terminalId)))));

            log.trace("Parameters download message processed successfully");

            return new ImmutablePair<>(masterKeyChannelResponse, responseIso);
        } catch (Exception e) {
            throw new TransactionProcessingException("Could not process parameters download", e);
        }
    }

    private String buildF62(TerminalConfigurationDetails terminal) throws TransactionProcessingException {
        String ctmsDateTime = ISODate.getDateTime(new Date());

        StringBuilder builder = new StringBuilder();


        if (StringUtils.length(terminal.getCardAcceptorId()) != 15) {
            throw new TransactionProcessingException("Invalid card acceptor id");
        }

        if (terminal.getTimeOutInSeconds() < 0 || terminal.getTimeOutInSeconds() > 99) {
            throw new TransactionProcessingException("Invalid time out configuration");
        }

        if (StringUtils.length(terminal.getCurrencyCode()) != 3) {
            throw new TransactionProcessingException("Invalid currency code supplied");
        }

        if (StringUtils.length(terminal.getCountryCode()) != 3) {
            throw new TransactionProcessingException("Invalid country code supplied");
        }

        if (terminal.getCallHomeTimeInHours() < 0 || terminal.getCallHomeTimeInHours() > 99) {
            throw new TransactionProcessingException("Invalid call home time configuration");
        }

        if (StringUtils.length(terminal.getMcc()) != 4) {
            throw new TransactionProcessingException("Invalid mcc supplied");
        }

        builder.append(createTlvString("02", ctmsDateTime));
        builder.append(createTlvString("03", terminal.getCardAcceptorId()));
        builder.append(createTlvString("04", String.format("%02d", terminal.getTimeOutInSeconds())));
        builder.append(createTlvString("05", terminal.getCurrencyCode()));
        builder.append(createTlvString("06", terminal.getCountryCode()));
        builder.append(createTlvString("07", String.format("%02d", terminal.getCallHomeTimeInHours())));
        builder.append(createTlvString("52", StringUtils.rightPad(terminal.getMerchantNameLocation(), 40)));
        builder.append(createTlvString("08", terminal.getMcc()));

        return builder.toString();
    }

    private String createTlvString(String tag, String value) {
        return String.format("%s%03d%s", tag, value.length(), value);
    }

    @Override
    public String getResponseMti() {
        return "0810";
    }
}
