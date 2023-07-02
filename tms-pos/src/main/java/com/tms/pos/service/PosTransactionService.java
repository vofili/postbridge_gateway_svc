package com.tms.pos.service;


import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.interchange.Interchange;
import com.tms.lib.interchange.InterchangeConfig;
import com.tms.lib.interchange.InterchangeConfigService;
import com.tms.lib.interchange.InterchangeFactory;
import com.tms.lib.model.DefaultIsoResponseCodes;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.router.Router;
import com.tms.lib.transactionrecord.entities.TransactionRecord;
import com.tms.lib.transactionrecord.service.TransactionRecordService;
import com.tms.lib.util.LogHelper;
import com.tms.pos.PosRestInterchange;
import com.tms.pos.model.cusotmtransferrequest.CustomTransactionRequest;
import com.tms.pos.source.processors.rest.PosRestSourceTransactionProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PosTransactionService {

    @Autowired(required = false)
    private List<PosRestSourceTransactionProcessor> processors = new ArrayList<>();
    @Autowired
    private TransactionRecordService transactionRecordService;
    @Autowired
    private InterchangeFactory interchangeFactory;
    @Autowired
    private InterchangeConfigService interchangeConfigService;
    @Autowired
    private Router router;


    public TransactionResponse processCustomTransactionRequest(CustomTransactionRequest customTransactionRequest) throws TransactionProcessingException {
        log.info("Received new custom transaction request {}", LogHelper.dump(customTransactionRequest));

        return processTransactionRequest(fromCustomTransactionRequest(customTransactionRequest));
    }

    private TransactionRequest fromCustomTransactionRequest(CustomTransactionRequest customTransactionRequest) {

        return new TransactionRequest();
//        TransferRequest transferRequest = customTransactionRequest.getTransferRequest();
//        CardData cardData = transferRequest.getCardData();
//        TransactionRequest transactionRequest = new TransactionRequest();
//
//        transactionRequest.setSourceMessage(isoMsg);
//        if (isoMsg.hasField(55)) {
//            EmvData emvData = IsoUtil.extractEmvData(new String(isoMsg.getBytes(55)));
//            transactionRequest.setEmvData(emvData);
//        }
//        transactionRequest.setMti("0200");
//        transactionRequest.setPan(cardData.getTrack2());
//
//            transactionRequest.setProcessingCode(cardData.get);
//            if (field3.length() != 6) {
//                String msg = String.format("Invalid field 3, should be six characters, %s", field3);
//                throw new UtilOperationException(msg);
//            }
//        }
//
//        transactionRequest.setMinorAmount(IsoUtil.extractAmount(isoMsg.getString(4)));
//        transactionRequest.setSettlementAmount(IsoUtil.extractAmount(isoMsg.getString(5)));
//        transactionRequest.setAmountCardHolderBilling(IsoUtil.extractAmount(isoMsg.getString(6)));
//        transactionRequest.setTransmissionDateTime(isoMsg.getString(7));
//        transactionRequest.setStan(isoMsg.getString(11));
//        transactionRequest.setTransactionTime(isoMsg.getString(12));
//        transactionRequest.setTransactionDate(isoMsg.getString(13));
//        transactionRequest.setExpiryDate(isoMsg.getString(14));
//        transactionRequest.setSettlementDate(isoMsg.getString(15));
//        transactionRequest.setConversionDate(isoMsg.getString(16));
//        transactionRequest.setMerchantType(isoMsg.getString(18));
//        transactionRequest.setAcquiringInstitutionCountryCode(isoMsg.getString(19));//not available for pbridge
//        transactionRequest.setCardSequenceNumber(isoMsg.getString(23));
//        transactionRequest.setInternationalNetworkIdentifier(isoMsg.getString(24));//not available for pbridge
//        transactionRequest.setTransactionFee(isoMsg.getString(28));
//        transactionRequest.setSettlementFee(isoMsg.getString(29));
//        transactionRequest.setTransactionProcessingFee(isoMsg.getString(30));
//        transactionRequest.setSettlementProcessingFee(isoMsg.getString(31));
//        transactionRequest.setAcquiringInstitutionId(isoMsg.getString(32));
//        transactionRequest.setForwardingInstitutionId(isoMsg.getString(33));
//        transactionRequest.setTrack2Data(isoMsg.getString(35));
//        transactionRequest.setRrn(isoMsg.getString(37));
//        transactionRequest.setServiceRestrictionCode(isoMsg.getString(40));
//        transactionRequest.setAuthorizationIdResponse(isoMsg.getString(38));
//        transactionRequest.setPinBlock(isoMsg.getString(52));
//        transactionRequest.setTerminalId(isoMsg.getString(41));
//        transactionRequest.setCardAcceptorId(isoMsg.getString(42));
//        transactionRequest.setCardAcceptorLocation(isoMsg.getString(43));
//        transactionRequest.setPosEntryMode(isoMsg.getString(22));
//        transactionRequest.setPosConditionCode(isoMsg.getString(25));
//        transactionRequest.setPinCaptureCode(isoMsg.getString(26));
//        transactionRequest.setPosDataCode(isoMsg.getString(123));
//        transactionRequest.setTransactionCurrencyCode(isoMsg.getString(49));
//        transactionRequest.setSettlementCurrencyCode(isoMsg.getString(50));
//        transactionRequest.setCardCurrencyCode(isoMsg.getString(51));
//        transactionRequest.setEchoData(isoMsg.getString(59));
//        transactionRequest.setExtendedPaymentCode(isoMsg.getString(67));
//        transactionRequest.setOriginalDataElements(IsoUtil.extractOriginalDataElements(isoMsg));
//        transactionRequest.setReplacementAmounts(IsoUtil.extractReplacementAmounts(isoMsg));
//        transactionRequest.setPayee(isoMsg.getString(98));
//        transactionRequest.setReceivingInstitutionId(isoMsg.getString(100));
//        transactionRequest.setToAccountIdentification(isoMsg.getString(103));
//        transactionRequest.setFromAccountIdentification(isoMsg.getString(102));

    }

    public TransactionResponse processTransactionRequest(TransactionRequest transactionRequest) throws TransactionProcessingException {
        log.info("Received new transaction request {}", LogHelper.dump(transactionRequest));

        Interchange interchange = interchangeFactory.getFirstMatchingInterchangeOfType(PosRestInterchange.POS_TYPE_NAME);

        if (interchange == null) {
            throw new TransactionProcessingException("Cannot find interchange for pos rest transaction");
        }

        PosRestSourceTransactionProcessor processorThatCanTreat = getProcessorThatCanTreat(transactionRequest);

        if (processorThatCanTreat != null) {
            TransactionResponse response = processorThatCanTreat.treat(transactionRequest);
            log.trace("Transaction response is {} ", LogHelper.dump(response));
            return response;
        }

        PosRestSourceTransactionProcessor processorThatCanConvert = getProcessorThatCanConvert(transactionRequest);

        if (processorThatCanConvert == null) {
            throw new TransactionProcessingException("Cannot find processor to process the request");
        }

        transactionRequest = processorThatCanConvert.convert(transactionRequest);

        transactionRequest.setSourceInterchange(interchange);

        saveTransactionRecord(transactionRequest, interchange.getConfig());

        TransactionResponse transactionResponse;

        try {
            transactionResponse = router.route(transactionRequest);
        } catch (Exception e) {
            log.error("Could not route request", e);
            transactionResponse = transactionRequest.constructResponse();
            transactionResponse.setIsoResponseCode(DefaultIsoResponseCodes.SystemMalFunction);
            transactionResponse.setResponseInterchange(interchange);
        }

        if (transactionResponse == null) {
            transactionResponse = transactionRequest.constructResponse();
            transactionResponse.setIsoResponseCode(DefaultIsoResponseCodes.InvalidResponse);
            transactionResponse.setResponseInterchange(interchange);
        } else {
            if (transactionResponse.getResponseInterchange() == null) {
                transactionResponse.setResponseInterchange(interchange);
            }
        }
        log.trace("Channel response is {}", LogHelper.dump(transactionResponse));

        updateTransactionRecord(transactionRequest, transactionResponse);
        return transactionResponse;
    }

    private PosRestSourceTransactionProcessor getProcessorThatCanTreat(TransactionRequest transactionRequest) {
        for (PosRestSourceTransactionProcessor processor : processors) {
            if (processor.canTreat(transactionRequest)) {
                return processor;
            }
        }
        return null;
    }

    private PosRestSourceTransactionProcessor getProcessorThatCanConvert(TransactionRequest transactionRequest) {
        for (PosRestSourceTransactionProcessor processor : processors) {
            if (processor.canConvert(transactionRequest)) {
                return processor;
            }
        }
        return null;
    }

    private TransactionRecord saveTransactionRecord(TransactionRequest request, InterchangeConfig interchange) throws TransactionProcessingException {
        try {
            return transactionRecordService.save(interchange.getId(), request);
        } catch (ServiceProcessingException e) {
            throw new TransactionProcessingException("Could not create transaction request record", e);
        }
    }

    private void updateTransactionRecord(TransactionRequest request, TransactionResponse response) throws TransactionProcessingException {
        try {
            transactionRecordService.update(request, response);
        } catch (Exception e) {
            log.error("Could not update request with response", e);
            throw new TransactionProcessingException("Could not update request with response", e);
        }
    }

    private InterchangeConfig getDestinationConfig(String processorKey) throws TransactionProcessingException {
        return interchangeConfigService.findByCode(processorKey)
                .orElseThrow(() -> new TransactionProcessingException(String.format("Cannot find config for processor key %s", processorKey)));
    }

}
