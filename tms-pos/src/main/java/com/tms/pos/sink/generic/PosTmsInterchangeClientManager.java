package com.tms.pos.sink.generic;

import com.tms.lib.exceptions.*;
import com.tms.lib.interchange.Interchange;
import com.tms.lib.model.DefaultIsoResponseCodes;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.network.TmsInterchangeClientManager;
import com.tms.pos.sink.PosSinkIsoChannelAdapter;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

@Slf4j
public class PosTmsInterchangeClientManager {

    private Interchange interchange;
    private NibssSinkMessageTranslator nibssSinkMessageTranslator;
    private ISOPackager isoPackager;

    private static final String INVALID_DATA_MSG = "Invalid data supplied for sending";

    public PosTmsInterchangeClientManager(Interchange interchange, NibssSinkMessageTranslator nibssSinkMessageTranslator, ISOPackager isoPackager) {
        this.interchange = interchange;
        this.nibssSinkMessageTranslator = nibssSinkMessageTranslator;
        this.isoPackager = isoPackager;
    }

    public TransactionResponse send(TransactionRequest request) throws TransactionProcessingException {

        try {
            ISOMsg isoMsg = request.getSourceMessage();

            if (isoMsg == null) {
                isoMsg = PosSinkIsoChannelAdapter.transactionRequestToISOMsg(request);
            }

            if (interchange == null) {
                throw new TransactionProcessingException(INVALID_DATA_MSG);
            }
            ISOMsg messageToSend = nibssSinkMessageTranslator.translateISOMsgToNibssEquivalent(isoMsg, interchange.getConfig().getId());

            ISOMsg responseIso = TmsInterchangeClientManager.send(messageToSend, interchange.getConfig(), isoPackager);
            return getTransactionResponse(responseIso, request);
        } catch (SocketConnectionException | SocketWriteException e) {
            log.info("Could not send transaction request", e);
            return createErrorResponse(DefaultIsoResponseCodes.IssuerOrSwitchInOperative, false, request);
        } catch (SocketReadException e) {
            log.info("There was an error reading transaction response", e);
            return createErrorResponse(DefaultIsoResponseCodes.IssuerOrSwitchInOperative, true, request);
        } catch (Exception e) {
            throw new TransactionProcessingException("An unexpected error occurred while processing transaction", e);
        }
    }

    private TransactionResponse createErrorResponse(DefaultIsoResponseCodes responseCode, boolean requestSent, TransactionRequest transactionRequest) {
        TransactionResponse genericPosMessageChannelResponse =
                new TransactionResponse(transactionRequest);
        genericPosMessageChannelResponse.setRequestSent(requestSent);
        genericPosMessageChannelResponse.setIsoResponseCode(responseCode);
        genericPosMessageChannelResponse.setResponseInterchange(interchange);

        return genericPosMessageChannelResponse;
    }

    private TransactionResponse getTransactionResponse(ISOMsg responseIso, TransactionRequest transactionRequest) throws TransactionProcessingException {
        TransactionResponse transactionResponse =
                new TransactionResponse(transactionRequest);

        try {
            PosSinkIsoChannelAdapter.commonIsoMsgToTransactionResponse(responseIso, transactionResponse);
        } catch (UtilOperationException e) {
            throw new TransactionProcessingException("Could not convert iso response to transaction response", e);
        }

        transactionResponse.setResponseMessage(responseIso);
        transactionResponse.setResponseFromRemoteEntity(true);
        transactionResponse.setResponseInterchange(interchange);
        transactionResponse.setRequestSent(true);

        return transactionResponse;
    }
}
