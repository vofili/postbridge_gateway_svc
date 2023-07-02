package com.tms.postbridge.matchers;

import com.tms.lib.exceptions.ServiceRuntimeException;
import com.tms.lib.matcher.IsoClientResponseMatcher;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

public class TransactionMessageResponseMatcher implements IsoClientResponseMatcher {
    private ISOMsg response;
    private String matchKey;

    public TransactionMessageResponseMatcher(ISOMsg request) {
        try {
            matchKey = getKey(request);
        } catch (ISOException e) {
            throw new ServiceRuntimeException("Could not generate match key for request");
        }
    }

    public static String getKey(ISOMsg data) throws ISOException {
        if (data != null) {
            String mti = data.getMTI();
            String mtiSubString = mti.substring(0, 2);
            return mtiSubString + data.getString(7) + data.getString(11) + data.getString(41);
        }
        return null;
    }

    @Override
    public Pair<Boolean, ISOMsg> isResponse(ISOMsg responseIso) {
        if (responseIso == null || matchKey == null) {
            return new ImmutablePair<>(Boolean.FALSE, responseIso);
        }
        String responseMatchKey;
        try {
            responseMatchKey = getKey(responseIso);
        } catch (ISOException e) {
            return new ImmutablePair<>(Boolean.FALSE, responseIso);
        }

        return new ImmutablePair<>(matchKey.equalsIgnoreCase(responseMatchKey), responseIso);
    }

    @Override
    public void setResponse(ISOMsg response) {
        this.response = response;
    }

    @Override
    public ISOMsg getResponse() {
        return response;
    }

    @Override
    public String getKey() {
        return matchKey;
    }

    public static boolean isTransactionMessage(ISOMsg isoMsg) {
        try {
            String mti = isoMsg.getMTI();
            String mtiSubString = mti.substring(0, 2);
            return "02".equals(mtiSubString) || ("01".equals(mtiSubString)) || ("06".equals(mtiSubString)) || ("04".equals(mtiSubString));
        } catch (ISOException e) {
            return false;
        }
    }
}
