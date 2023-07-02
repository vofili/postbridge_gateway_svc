package com.tms.lib.helper;

import com.tms.lib.transactionrecord.entities.TransactionRecord;
import com.tms.lib.exceptions.RouterException;
import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.interchange.Interchange;
import com.tms.lib.model.DefaultIsoResponseCodes;
import com.tms.lib.model.OriginalDataElements;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.processor.SourceTransactionProcessor;
import com.tms.lib.router.Router;
import com.tms.lib.transactionrecord.service.TransactionRecordService;
import com.tms.lib.util.IsoLogger;
import com.tms.lib.util.LogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.util.List;

@Slf4j
public class ISOSourceNodeHelper {

    private List<SourceTransactionProcessor> processors;
    private TransactionRecordService transactionRecordService;
    private Interchange interchange;
    private Router router;

    public ISOSourceNodeHelper(List<SourceTransactionProcessor> processors, TransactionRecordService transactionRecordService,
                               Interchange interchange, Router router) {
        this.processors = processors;
        this.transactionRecordService = transactionRecordService;
        this.interchange = interchange;
        this.router = router;
    }

    private SourceTransactionProcessor getProcessorThatCanTreat(ISOMsg isoMsg) {
        for (SourceTransactionProcessor processor : processors) {
            if (processor.canTreat(isoMsg)) {
                return processor;
            }
        }
        return null;
    }

    private SourceTransactionProcessor getProcessorThatCanConvert(ISOMsg isoMsg) {
        for (SourceTransactionProcessor processor : processors) {
            if (processor.canConvert(isoMsg)) {
                return processor;
            }
        }
        return null;
    }

    private boolean isReversalRequest(ISOMsg isoMsg) {
        try {
            String mti = isoMsg.getMTI();
            return "0420".equals(mti) || "0421".equals(mti) || "0400".equals(mti) || "0401".equals(mti);
        } catch (ISOException e) {
            return false;
        }
    }

    public ISOMsg processMessage(ISOMsg isoMsg) throws TransactionProcessingException {

        log.trace("Raw request \r\n {}", IsoLogger.dump(isoMsg));

        if (isReversalRequest(isoMsg)) {
            return processReversalMessage(isoMsg);
        }

        SourceTransactionProcessor processorThatCanTreat = getProcessorThatCanTreat(isoMsg);
        if (processorThatCanTreat != null) {
            TransactionRequest transactionRequest = processorThatCanTreat.toTransactionRequest(isoMsg);

            saveTransactionRecord(transactionRequest);

            Pair<TransactionResponse, ISOMsg> responseISOMsgPair = processorThatCanTreat.treat(isoMsg, transactionRequest);
            TransactionResponse response = responseISOMsgPair.getKey();
            response.setResponseInterchange(interchange);
            ISOMsg rawResponse = responseISOMsgPair.getRight();
            log.trace("Channel response {} from raw response {}", LogHelper.dump(response), IsoLogger.dump(rawResponse));
            try {
                transactionRecordService.update(transactionRequest, response);
            } catch (ServiceProcessingException e) {
                throw new TransactionProcessingException("Could not update transaction record with response", e);
            }

            return responseISOMsgPair.getRight();
        }

        SourceTransactionProcessor processorThatCanConvert = getProcessorThatCanConvert(isoMsg);
        if (processorThatCanConvert == null) {
            String msg = "Cannot process this message through interchange because there is no registered processor for it";
            throw new TransactionProcessingException(msg);
        }

        TransactionRequest request = processorThatCanConvert.toTransactionRequest(isoMsg);
        if (request == null) {
            throw new TransactionProcessingException("Processor returned a null request");
        }

        request.setSourceInterchange(interchange);

        saveTransactionRecord(request);

        TransactionResponse response;
        try {
            response = router.route(request);
        } catch (Exception e) {
            log.error("Could not route request", e);
            response = request.constructResponse();
            response.setIsoResponseCode(DefaultIsoResponseCodes.SystemMalFunction);
            response.setResponseInterchange(interchange);
        }

        if (response == null) {
            response = request.constructResponse();
            response.setIsoResponseCode(DefaultIsoResponseCodes.InvalidResponse);
            response.setResponseInterchange(interchange);
        } else {
            if (response.getResponseInterchange() == null) {
                response.setResponseInterchange(interchange);
            }
        }
        log.trace("Channel response is {}", LogHelper.dump(response));
        updateTransactionRecord(request, response);

        ISOMsg rawResponse = processorThatCanConvert.toISOMsg(response);
        log.trace("Raw response is \r\n {}", IsoLogger.dump(rawResponse));

        return rawResponse;

    }

    private ISOMsg processReversalMessage(ISOMsg rawRequest) throws TransactionProcessingException {
        log.trace("Processing reversal message");
        SourceTransactionProcessor processorThatCanReverse = getProcessorThatCanConvert(rawRequest);
        if (processorThatCanReverse == null) {
            String msg = "Cannot process this message through interchange because there is no registered processor for it";
            throw new TransactionProcessingException(msg);
        }

        TransactionRequest transactionRequest = processorThatCanReverse.toTransactionRequest(rawRequest);

        if (transactionRequest == null) {
            throw new TransactionProcessingException("Processor returned a null request");
        }

        log.trace("Channel request is {}", LogHelper.dump(transactionRequest));

        transactionRequest.setSourceInterchange(interchange);

        OriginalDataElements originalDataElements = transactionRequest.getOriginalDataElements();

        TransactionRecord transactionRecord;
        try {
            transactionRecord = transactionRecordService.fetchMatchingTransaction(originalDataElements, interchange.getConfig().getId());
        } catch (ServiceProcessingException e) {
            throw new TransactionProcessingException("Could not fetch matching transaction for reversal", e);
        }

        TransactionResponse response;

        if (transactionRecord == null) {
            log.info("Could not find matching transaction");
            saveTransactionRecord(transactionRequest);
            response = transactionRequest.constructResponse();
            response.setResponseInterchange(interchange);
            response.setIsoResponseCode(DefaultIsoResponseCodes.UnableToLocateRecord);
        } else {
            log.info("Found matching transaction sending it for processing");
            response = processOriginalTransactionReversal(transactionRequest, transactionRecord);
        }

        log.trace("Channel response is {}", LogHelper.dump(response));

        updateTransactionRecord(transactionRequest, response);

        ISOMsg rawResponse = processorThatCanReverse.toISOMsg(response);
        log.trace("Raw response is \r\n {}", IsoLogger.dump(rawResponse));
        return rawResponse;
    }

    private TransactionResponse processOriginalTransactionReversal(TransactionRequest transactionRequest, TransactionRecord originalTransactionRecord) throws TransactionProcessingException {
        log.info("Original transaction has been found with id {}", originalTransactionRecord.getId());
        transactionRequest.setOriginalTransactionId(originalTransactionRecord.getId());
        TransactionRecord reversalRecord = saveTransactionRecord(transactionRequest);
        TransactionResponse response;
        if (originalTransactionRecord.isReversed()) {
            log.info("Original transaction has already been reversed");
            response = transactionRequest.constructResponse();
            response.setResponseInterchange(interchange);
            response.setIsoResponseCode(DefaultIsoResponseCodes.Approved);
        } else {
            if (originalTransactionRecord.isRequestSent()) {
                log.info("Original Transaction was sent upstream, sending to remote entity for reversal");
                Long sinkInterchangeId = originalTransactionRecord.getResponseInterchangeId();
                try {
                    response = router.send(transactionRequest, sinkInterchangeId);
                } catch (RouterException e) {
                    response = transactionRequest.constructResponse(DefaultIsoResponseCodes.SystemMalFunction);
                    response.setResponseInterchange(interchange);
                }
            } else {
                log.info("Original Transaction was not sent upstream, reversing transaction locally");
                response = transactionRequest.constructResponse();
                response.setResponseInterchange(interchange);
                response.setIsoResponseCode(DefaultIsoResponseCodes.Approved);
            }
            if (response.getIsoResponseCode().equals("00")) {
                originalTransactionRecord.setReversalTransactionId(reversalRecord.getId());
                originalTransactionRecord.setReversed(true);
                originalTransactionRecord.setCompleted(true);
                originalTransactionRecord.setResponseInterchangeId(reversalRecord.getResponseInterchangeId());
                originalTransactionRecord.setResponseInterchangeName(reversalRecord.getResponseInterchangeName());
                transactionRecordService.save(originalTransactionRecord);
            }
        }
        return response;
    }

    private TransactionRecord saveTransactionRecord(TransactionRequest request) throws TransactionProcessingException {
        try {
            return transactionRecordService.save(interchange.getConfig().getId(), request);
        } catch (ServiceProcessingException e) {
            throw new TransactionProcessingException("Could not create transaction request record", e);
        }
    }

    private void updateTransactionRecord(TransactionRequest channelRequest, TransactionResponse response) throws TransactionProcessingException {
        try {
            transactionRecordService.update(channelRequest, response);
        } catch (Exception e) {
            log.error("Could not update request with response", e);
            throw new TransactionProcessingException("Could not update request with response", e);
        }
    }
}
