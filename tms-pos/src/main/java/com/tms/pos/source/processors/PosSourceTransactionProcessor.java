package com.tms.pos.source.processors;

import com.tms.lib.exceptions.TransactionProcessingException;
import com.tms.lib.exceptions.UtilOperationException;
import com.tms.lib.model.TransactionRequest;
import com.tms.lib.model.TransactionResponse;
import com.tms.lib.processor.SourceTransactionProcessor;
import com.tms.pos.source.PosSourceIsoChannelAdapter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

public interface PosSourceTransactionProcessor extends SourceTransactionProcessor {

    default boolean canTreat(ISOMsg isoMsg) {
        return false;
    }

    default Pair<TransactionResponse, ISOMsg> treat(ISOMsg isoMsg, TransactionRequest channelRequest) throws TransactionProcessingException {
        return new ImmutablePair<>(null, isoMsg);
    }

    default ISOMsg toISOMsg(TransactionResponse response) throws TransactionProcessingException {
        if (response == null) {
            String msg = "ChannelResponse is null";
            throw new TransactionProcessingException(msg);
        }
        ISOMsg isoResponse = new ISOMsg();

        try {
            isoResponse.setMTI(getResponseMti());
            isoResponse.set(39, response.getIsoResponseCode());
            PosSourceIsoChannelAdapter.transactionResponseToCommonIsoMsg(response, isoResponse);
        } catch (ISOException | UtilOperationException e) {
            String msg = "Error processing response iso";
            throw new TransactionProcessingException(msg, e);
        }
        return isoResponse;
    }

    String getResponseMti();
}
