package com.tms.lib.helper;

import com.tms.lib.model.*;
import com.tms.lib.transactionrecord.entities.TransactionRecord;
import com.tms.lib.hsm.HsmService;
import com.tms.lib.hsm.model.PinTranslationRequest;
import com.tms.lib.interchange.Interchange;
import com.tms.lib.network.transciever.IsoMsgTransceiveFunction;
import com.tms.lib.processor.SinkTransactionProcessor;
import com.tms.lib.security.Encrypter;
import com.tms.lib.transactionrecord.service.TransactionRecordService;
import com.tms.lib.terminals.services.TerminalKeyService;
import com.tms.lib.util.AsyncWorkerPool;
import com.tms.lib.util.IsoLogger;
import com.tms.lib.util.LogHelper;
import com.tms.lib.exceptions.*;
import com.tms.lib.util.TDesEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class ISOSinkNodeHelper {

    private List<SinkTransactionProcessor> processors;
    private IsoMsgTransceiveFunction transcieveFunction;
    private Interchange interchange;
    private HsmService hsmService;
    private AsyncWorkerPool asyncWorkerPool;
    private TransactionRecordService transactionRecordService;
    private TerminalKeyService terminalKeyService;
    private Encrypter encrypter;

    public ISOSinkNodeHelper(List<SinkTransactionProcessor> processors,
                             IsoMsgTransceiveFunction transcieveFunction,
                             Interchange interchange, ApplicationContext context,
                             AsyncWorkerPool asyncWorkerPool, TransactionRecordService transactionRecordService) {
        this.processors = processors;
        this.transcieveFunction = transcieveFunction;
        this.interchange = interchange;
        this.hsmService = context.getBean(HsmService.class);
        this.terminalKeyService = context.getBean(TerminalKeyService.class);
        this.asyncWorkerPool = asyncWorkerPool;
        this.transactionRecordService = transactionRecordService;
        this.encrypter = context.getBean(Encrypter.class);
    }


    private SinkTransactionProcessor getProcessorThatCanConvert(TransactionRequest channelRequest) {
        for (SinkTransactionProcessor processor : processors) {
            if (processor.canConvert(channelRequest.getRequestType())) {
                return processor;
            }
        }
        return null;
    }

    public TransactionResponse send(TransactionRequest transactionRequest) throws TransactionProcessingException {
        log.trace("Processing message \r\n {}", LogHelper.dump(transactionRequest));

        transactionRequest.setSinkInterchange(interchange);

        SinkTransactionProcessor processorThatCanConvert = getProcessorThatCanConvert(transactionRequest);
        if (processorThatCanConvert == null) {
            String msg = "Cannot process this message through interchange because there is no registered processor for it";
            throw new TransactionProcessingException(msg);
        }

       // if (interchange.getConfig().isPinTranslationRequired()) {
            doPinTranslation(transactionRequest);
       // }

        ISOMsg request = processorThatCanConvert.toISOMsg(transactionRequest);
        log.trace("Raw ISO request \r\n {}", IsoLogger.dump(request));
        ISOMsg rawResponse;

        try {
            rawResponse = transcieveFunction.transcieve(request);
        } catch (InterchangeServiceException e) {
            throw new TransactionProcessingException("Could not process request", e);

        } catch (InterchangeIOException e) {
            log.error("An IO error occurred while waiting for response", e);
            initiateReversal(transactionRequest, processorThatCanConvert);
            return constructIOResponse(transactionRequest);
        }

        if (rawResponse == null) {
            log.info("No response from upstream, Raw response is null");
            initiateReversal(transactionRequest, processorThatCanConvert);
            return constructIOResponse(transactionRequest);
        }
        log.trace("Raw response {}", IsoLogger.dump(rawResponse));

        TransactionResponse response;
        try {
            response = processorThatCanConvert.toTransactionResponse(rawResponse, transactionRequest);
        } catch (Exception e) {
            log.error("An error occurred while converting raw response", e);
            initiateReversal(transactionRequest, processorThatCanConvert);
            return constructErrorResponse(transactionRequest);
        }
        if (response != null) {
            response.setResponseInterchange(interchange);
            response.setResponseFromRemoteEntity(true);
            response.setRequestSent(true);
            response.setRequestRecordId(transactionRequest.getTransactionId());


        }
        //additional processing to reverse responseCodes -91,96,09
        if(Arrays.asList("91","09","96").contains(response.getIsoResponseCode())){
            log.error("An indeterminate response was received from switch, trigger reversal");
            initiateReversal(transactionRequest, processorThatCanConvert);

        }

        log.trace("Channel response {}", LogHelper.dump(response));

        return response;
    }

    public TransactionResponse sendTransactionReversal(TransactionRequest transactionRequest) throws TransactionProcessingException {
        log.trace("Processing message \r\n {}", LogHelper.dump(transactionRequest));

        transactionRequest.setSinkInterchange(interchange);

        SinkTransactionProcessor processorThatCanConvert = getProcessorThatCanConvert(transactionRequest);
        if (processorThatCanConvert == null) {
            String msg = "Cannot process this message through interchange because there is no registered processor for it";
            throw new TransactionProcessingException(msg);
        }

        // if (interchange.getConfig().isPinTranslationRequired()) {
        doPinTranslation(transactionRequest);
        // }

        ISOMsg request = processorThatCanConvert.toISOMsg(transactionRequest);
        log.trace("Raw ISO request \r\n {}", IsoLogger.dump(request));
        ISOMsg rawResponse;

        try {
            rawResponse = transcieveFunction.transcieve(request);
        } catch (InterchangeServiceException e) {
            throw new TransactionProcessingException("Could not process request", e);

        } catch (InterchangeIOException e) {
            log.error("An IO error occurred while waiting for response", e);
            initiateReversal(transactionRequest, processorThatCanConvert);
            return constructIOResponse(transactionRequest);
        }

        if (rawResponse == null) {
            log.info("No response from upstream, Raw response is null");
            initiateReversal(transactionRequest, processorThatCanConvert);
            return constructIOResponse(transactionRequest);
        }
        log.trace("Raw response {}", IsoLogger.dump(rawResponse));

        TransactionResponse response;
        try {
            response = processorThatCanConvert.toTransactionResponse(rawResponse, transactionRequest);
        } catch (Exception e) {
            log.error("An error occurred while converting raw response", e);
            initiateReversal(transactionRequest, processorThatCanConvert);
            return constructErrorResponse(transactionRequest);
        }
        if (response != null) {
            response.setResponseInterchange(interchange);
            response.setResponseFromRemoteEntity(true);
            response.setRequestSent(true);
            response.setRequestRecordId(transactionRequest.getTransactionId());
            //additional processing to reverse responseCodes -91,96,09
            if(Arrays.asList("91","09","96").contains(response.getIsoResponseCode())){
                log.error("An indeterminate response was received from switch, trigger reversal");
                initiateReversal(transactionRequest, processorThatCanConvert);
            }
        }

        log.trace("Channel response {}", LogHelper.dump(response));

        return response;
    }

    private void doPinTranslation(TransactionRequest transactionRequest) throws TransactionProcessingException {
        String pinData = transactionRequest.getPinBlock();
        if (pinData != null) {
            transactionRequest.setPinBlock(convertPinBlockFromTpkToDestinationZpk(transactionRequest));
        }
    }

    private String convertPinBlockFromTpkToDestinationZpk(TransactionRequest transactionRequest) throws TransactionProcessingException {
        String pinBlock = transactionRequest.getPinBlock();
        if (pinBlock == null) {
            return null;
        }

        String sourceEncryptionKey;
        try {
            sourceEncryptionKey = terminalKeyService.getConfiguredTerminalPinKey(transactionRequest.getTerminalId());
        } catch (ServiceProcessingException e) {
            throw new TransactionProcessingException("Could not get configured terminal pin key");
        }

        log.info("Source encryption key under lmk {}", sourceEncryptionKey);
        String destinationEncryptionKey = interchange.getConfig().getEncryptedSinkZpk();

        if (StringUtils.isEmpty(destinationEncryptionKey)) {
            log.info("No pin translation done, destination encryption key was not found");
            return pinBlock;
        }

        String decryptedDestinationEncryptionKey;
        try {
            decryptedDestinationEncryptionKey = encrypter.decrypt(destinationEncryptionKey);
        } catch (CryptoException e) {
            throw new TransactionProcessingException("Could not decrypt encrypted zpk", e);
        }

        log.info("Destination encrypted key under lmk {}", decryptedDestinationEncryptionKey);

        String encryptedPinBlock;
        try {
            encryptedPinBlock = hsmService.translatePinBlockFromTpkToDestinationZpk(
                    buildPinTranslationRequest(pinBlock, sourceEncryptionKey,
                            decryptedDestinationEncryptionKey, transactionRequest.getPan()));
        } catch (HsmException e) {
            throw new TransactionProcessingException("Could not translate pin block to destination zpk", e);
        }

        log.info("Encrypted pin block is {}", encryptedPinBlock);

        return encryptedPinBlock;
    }

    private PinTranslationRequest buildPinTranslationRequest(String pinBlock, String sourceEncryptionKey,
                                                             String destinationEncryptionKey, String pan) {
        PinTranslationRequest pinTranslationRequest = new PinTranslationRequest();
        pinTranslationRequest.setPinBlock(pinBlock);
        pinTranslationRequest.setSourceZpk(sourceEncryptionKey);
        pinTranslationRequest.setDestinationZpk(destinationEncryptionKey);
        log.info("destinationEncryptionKey is {}", destinationEncryptionKey);
        try {
            String keycheck = new String(TDesEncryptionUtil.generateKeyCheckValue(Hex.decodeHex(destinationEncryptionKey.toCharArray())));
            log.info("keycheck is {}", keycheck);
        }catch(Exception s){
            log.error("could not generate keycheck",s);
        }
        pinTranslationRequest.setPan(pan);
        return pinTranslationRequest;
    }

    private SinkTransactionProcessor getProcessorThatCanProcess(TransactionResponse transactionResponse) {
        for (SinkTransactionProcessor processor : processors) {
            if (processor.canProcess(transactionResponse.getOriginalRequest().getRequestType())) {
                return processor;
            }
        }
        return null;
    }

    public void process(TransactionResponse transactionResponse) throws TransactionProcessingException {
        SinkTransactionProcessor processorThatCanProcess = getProcessorThatCanProcess(transactionResponse);

        if (processorThatCanProcess == null) {
            throw new TransactionProcessingException("Could not find processor that can process the channel response");
        }
        processorThatCanProcess.process(transactionResponse, interchange);
    }

    private SinkTransactionProcessor getProcessorThatCanReverse(TransactionRequest channelRequest) {
        for (SinkTransactionProcessor processor : processors) {
            if (processor.canReverse(channelRequest.getRequestType())) {
                return processor;
            }
        }

        return null;
    }

    private void initiateReversal(TransactionRequest channelRequest, SinkTransactionProcessor processorThatCanConvert) {
        if (isReversibleMessage(channelRequest)) {
            log.info("Initiating reversal");
            asyncWorkerPool.queueJob(() -> {
                sendReversalAdvice(channelRequest, processorThatCanConvert);
                return null;
            });
        }
    }

    private void sendReversalAdvice(TransactionRequest channelRequest, SinkTransactionProcessor processorThatCanConvert) throws ReversalProcessingException {
        if (!isReversibleMessage(channelRequest)) {
            return;
        }
        SinkTransactionProcessor processorThatCanReverse = getProcessorThatCanReverse(channelRequest);

        if (processorThatCanReverse == null) {
            log.info("Could not find processor that can reverse request");
            return;
        }

        TransactionRequest reversalAdviceTransactionRequest = processorThatCanReverse.toReversalRequest(channelRequest, interchange);
        StringBuilder fld90=new StringBuilder();
        log.info("Channel Reversal request is {}", reversalAdviceTransactionRequest.toString());
        reversalAdviceTransactionRequest.setOriginalTransactionId(channelRequest.getTransactionId());

        ISOMsg rawRequest;
        try {
            rawRequest = processorThatCanReverse.toISOMsg(reversalAdviceTransactionRequest);

            fld90.append(rawRequest.getString(0))
                    .append(rawRequest.getString(11))
                    .append(rawRequest.getString(7))
                    .append(StringUtils.leftPad(
                    (StringUtils.isEmpty(rawRequest.getString(32)) ?
                            "" : rawRequest.getString(32)), 11, '0'))
                    .append(StringUtils.leftPad(
                            (StringUtils.isEmpty(rawRequest.getString(33)) ?
                                    "" : rawRequest.getString(33)), 11, '0'));
            rawRequest.set(90,fld90.toString());

        } catch (TransactionProcessingException e) {
            throw new ReversalProcessingException("Could not convert reversal advice request to iso msg", e);
        }

        if (rawRequest != null) {
            try {
                rawRequest.setMTI(processorThatCanReverse.getReversalAdviceMti());
                log.info("Raw ISOMsg reversal request ");
                rawRequest.dump(System.out," >>");
            } catch (ISOException e) {
                throw new ReversalProcessingException("Could not set mti for reversal request", e);
            }
        }

        try {
            saveTransactionRecord(reversalAdviceTransactionRequest);
        } catch (ServiceProcessingException e) {
            throw new ReversalProcessingException("Could not save reversal transaction for processing", e);
        }

        TransactionResponse response = processRawReversalRequest(rawRequest, reversalAdviceTransactionRequest, processorThatCanConvert);
        if (response.isResponseFromRemoteEntity()) {
            log.info("Got response from remote entity for reversal advice");
            processReversalResponse(reversalAdviceTransactionRequest, response);
        } else {
            ISOMsg rawRequestRepeat = processorThatCanReverse.toRawReversalRequestRepeat(rawRequest);
            if (rawRequestRepeat != null) {
                try {
                    rawRequest.setMTI(processorThatCanReverse.getReversalAdviceRepeatMti());

                } catch (ISOException e) {
                    throw new ReversalProcessingException("Could not set mti for reversal request", e);
                }
            }
            //prevent sending reversal repeat message
//            asyncWorkerPool.queueJob(() -> {
//                sendReversalAdviceRepeat(rawRequestRepeat, reversalAdviceTransactionRequest, processorThatCanConvert);
//                return null;
//            });
        }

    }

    private void sendReversalAdviceRepeat(ISOMsg rawRequest, TransactionRequest reversalAdviceChannelRequest, SinkTransactionProcessor processorThatCanConvert) throws ReversalProcessingException {

        TransactionResponse response = processRawReversalRequest(rawRequest, reversalAdviceChannelRequest, processorThatCanConvert);
        if (response.isResponseFromRemoteEntity()) {
            processReversalResponse(reversalAdviceChannelRequest, response);
        } else {
            asyncWorkerPool.queueJob(() -> {
                sendReversalAdviceRepeat(rawRequest, reversalAdviceChannelRequest, processorThatCanConvert);
                return null;
            });
        }

    }

    private TransactionResponse processRawReversalRequest(ISOMsg rawReversalRequest, TransactionRequest reversalAdviceChannelRequest, SinkTransactionProcessor processorThatCanConvert) throws ReversalProcessingException {
        log.info("Raw Reversal request \r\n {}", IsoLogger.dump(rawReversalRequest));
        ISOMsg rawResponse;
        try {
            rawResponse = transcieveFunction.transcieve(rawReversalRequest);
        } catch (InterchangeServiceException e) {
            log.error("There was an error processing reversal request", e);
            return constructErrorResponse(reversalAdviceChannelRequest);
        } catch (InterchangeIOException e) {
            log.error("An IO Exception occurred while processing reversal request", e);
            return constructIOResponse(reversalAdviceChannelRequest);
        }
        if (rawResponse == null) {
            log.info("No response from upstream");
            return constructIOResponse(reversalAdviceChannelRequest);
        }

        log.info("Raw response {}", IsoLogger.dump(rawResponse));
        TransactionResponse reversalAdviceChannelResponse = null;
        try {
            reversalAdviceChannelResponse = processorThatCanConvert.toTransactionResponse(rawResponse, reversalAdviceChannelRequest);
        } catch (TransactionProcessingException e) {
            throw new ReversalProcessingException(e);
        }
        if (reversalAdviceChannelResponse != null) {
            reversalAdviceChannelResponse.setResponseFromRemoteEntity(true);
            reversalAdviceChannelResponse.setResponseInterchange(interchange);
            reversalAdviceChannelResponse.setRequestSent(true);
            reversalAdviceChannelResponse.setRequestRecordId(reversalAdviceChannelRequest.getTransactionId());
        }
        log.info("Channel response {}", LogHelper.dump(reversalAdviceChannelResponse));

        //TODO: send financial transaction advice if response code 25 was received
        return reversalAdviceChannelResponse;
    }

    private void processReversalResponse(TransactionRequest reversalAdviceChannelRequest, TransactionResponse reversalAdviceChannelResponse) throws ReversalProcessingException {

        if (DefaultIsoResponseCodes.Approved.toString().equals(reversalAdviceChannelResponse.getIsoResponseCode())) {
//            TransactionRecord originalTransactionRecord = transactionRecordService.findOne(reversalAdviceChannelRequest.getOriginalTransactionId());
//            originalTransactionRecord.setReversalTransactionId(reversalAdviceChannelRequest.getTransactionId());
//            originalTransactionRecord.setReversed(true);
//            transactionRecordService.save(originalTransactionRecord);
        }

        try {
            updateTransactionRecord(reversalAdviceChannelRequest, reversalAdviceChannelResponse);
        } catch (TransactionProcessingException e) {
            throw new ReversalProcessingException("Could not update transaction record", e);
        }
    }

    private boolean isReversibleMessage(TransactionRequest channelRequest) {
        return RequestType.UNKNOWN != channelRequest.getRequestType() && !"0800".equals(channelRequest.getMti())
                && !"0420".equals(channelRequest.getMti()) && !"0421".equals(channelRequest.getMti());
    }

    private TransactionRecord saveTransactionRecord(TransactionRequest request) throws ServiceProcessingException {
        return transactionRecordService.save(interchange.getConfig().getId(), request);
    }

    private void updateTransactionRecord(TransactionRequest transactionRequest, TransactionResponse transactionResponse) throws TransactionProcessingException {
        try {
            transactionRecordService.update(transactionRequest, transactionResponse);
        } catch (Exception e) {
            throw new TransactionProcessingException("Could not update request with response", e);
        }
    }

    private TransactionResponse constructIOResponse(TransactionRequest channelRequest) {
        TransactionResponse response = channelRequest.constructResponse();
        response.setRequestSent(true);
        response.setIsoResponseCode(DefaultIsoResponseCodes.IssuerOrSwitchInOperative);
        response.setResponseInterchange(interchange);
        response.setRequestRecordId(channelRequest.getTransactionId());
        return response;
    }

    private TransactionResponse constructErrorResponse(TransactionRequest channelRequest) {
        TransactionResponse response = channelRequest.constructResponse();
        response.setRequestSent(false);
        response.setIsoResponseCode(DefaultIsoResponseCodes.Error);
        response.setResponseInterchange(interchange);
        response.setRequestRecordId(channelRequest.getTransactionId());
        return response;
    }
}
