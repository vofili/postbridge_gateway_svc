package com.tms.lib.transactionrecord.service.impl;

import com.tms.lib.exceptions.ServiceProcessingException;
import com.tms.lib.interchange.InterchangeConfig;
import com.tms.lib.model.OriginalDataElements;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.transactionrecord.repository.TransactionRecordRepository;
import com.tms.lib.transactionrecord.entities.TransactionRecord;
import com.tms.lib.transactionrecord.service.TransactionRecordService;
import com.tms.lib.util.IsoUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TransactionRecordServiceImpl implements TransactionRecordService {


    private final TransactionRecordRepository transactionRecordRepository;

    public TransactionRecord save(TransactionRecord transactionRecord) {
        return transactionRecordRepository.save(transactionRecord);
    }

    public TransactionRecord save(int configId, TransactionRequest transactionRequest) throws ServiceProcessingException {
        try {
            if (isNetworkMessage(transactionRequest)) {
                return null;
            }

            TransactionRecord transactionRecord = TransactionRecord.fromTransactionRequest(configId, transactionRequest);
            transactionRecord.setRequestInterchangeName(transactionRequest.getSourceInterchange().getName());
            transactionRecord = transactionRecordRepository.save(transactionRecord);
            transactionRequest.setTransactionId(transactionRecord.getId());

            return transactionRecord;
        } catch (Exception e) {
            throw new ServiceProcessingException("Could not save record", e);
        }
    }

    public TransactionRecord update(TransactionRequest transactionRequest, TransactionResponse transactionResponse) throws ServiceProcessingException {
        if (isNetworkMessage(transactionRequest)) {
            return null;
        }
        TransactionRecord transactionRecord = transactionRecordRepository.findById(transactionRequest.getTransactionId()).orElse(null);
        if (transactionRecord == null) {
            return null;
        }

        InterchangeConfig sourceInterchange = transactionRequest.getSourceInterchange().getConfig();
        InterchangeConfig sinkInterchange = transactionResponse.getResponseInterchange() == null ? sourceInterchange : transactionResponse.getResponseInterchange().getConfig();
        if (sinkInterchange == null) {
            throw new ServiceProcessingException("Request is not associated with any interchange");
        }
        transactionResponse.setOriginalRequest(transactionRequest);
        transactionRecord = TransactionRecord.addChannelResponse(sinkInterchange.getId(), transactionResponse, transactionRecord);
        if (transactionRecord == null) {
            return null;
        }
        transactionRecord.setResponseInterchangeName(sinkInterchange.getName());
        return transactionRecordRepository.save(transactionRecord);

    }

    private boolean isNetworkMessage(TransactionRequest channelRequest) {
        return StringUtils.startsWith(channelRequest.getMti(), "08");
    }

    public TransactionRecord fetchMatchingTransaction(OriginalDataElements originalDataElements, long interchangeId) throws ServiceProcessingException {
        if (originalDataElements == null) {
            return null;
        }

        Date transmissionDateTime;
        try {
            transmissionDateTime = IsoUtil.extractDateTime(originalDataElements.getTransmissionDateTime());
        } catch (ParseException e) {
            throw new ServiceProcessingException("Could not extract transmission date time from original data elements", e);
        }
        if (originalDataElements.getAcquiringInstitutionIdCode() != null && originalDataElements.getForwardingInstitutionIdCode() != null) {

            return transactionRecordRepository.findByRequestInterchangeIdAndMtiAndStanAndTransmissionDateTimeAndAcquiringInstitutionIdentifierAndForwardingInstitutionCode(
                    interchangeId,
                    originalDataElements.getMti(), originalDataElements.getStan(), transmissionDateTime,
                    originalDataElements.getAcquiringInstitutionIdCode(), originalDataElements.getForwardingInstitutionIdCode()
            );
        } else if (originalDataElements.getAcquiringInstitutionIdCode() == null && originalDataElements.getForwardingInstitutionIdCode() != null) {
            return transactionRecordRepository.findByRequestInterchangeIdAndMtiAndStanAndTransmissionDateTimeAndForwardingInstitutionCode(interchangeId,
                    originalDataElements.getMti(), originalDataElements.getStan(),
                    transmissionDateTime, originalDataElements.getForwardingInstitutionIdCode());
        } else if (originalDataElements.getAcquiringInstitutionIdCode() != null && originalDataElements.getForwardingInstitutionIdCode() == null) {
            return transactionRecordRepository.findByRequestInterchangeIdAndMtiAndStanAndTransmissionDateTimeAndAcquiringInstitutionIdentifier(interchangeId,
                    originalDataElements.getMti(), originalDataElements.getStan(),
                    transmissionDateTime, originalDataElements.getAcquiringInstitutionIdCode());
        } else {
            return transactionRecordRepository.findByRequestInterchangeIdAndMtiAndStanAndTransmissionDateTime(interchangeId, originalDataElements.getMti(),
                    originalDataElements.getStan(), transmissionDateTime);
        }
    }
}
