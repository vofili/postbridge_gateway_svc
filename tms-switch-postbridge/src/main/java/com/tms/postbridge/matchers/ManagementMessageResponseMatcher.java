package com.tms.postbridge.matchers;

import com.tms.lib.matcher.IsoClientResponseMatcher;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

public class ManagementMessageResponseMatcher implements IsoClientResponseMatcher {
    private ISOMsg response;
    private ISOMsg request;
    private String matchKey;

    public ManagementMessageResponseMatcher(ISOMsg request) {
        this.request = request;
        this.matchKey = getKey(request);
    }

    public static String getKey(ISOMsg data) {
        return data.getString(7) + data.getString(11) + data.getString(70);
    }

    @Override
    public Pair<Boolean, ISOMsg> isResponse(ISOMsg response) {
        if (request == null || response == null) {
            return new ImmutablePair<>(Boolean.FALSE, response);
        }
        try {
            if (!"0810".equals(response.getMTI())) {
                return new ImmutablePair<>(Boolean.FALSE, response);
            }
        } catch (ISOException e) {
            return new ImmutablePair<>(Boolean.FALSE, response);
        }
        String responseMatchKey = getKey(response);

        return new ImmutablePair<>(matchKey.equalsIgnoreCase(responseMatchKey), response);
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

    public static boolean isManagementMessage(ISOMsg isoMsg) {
        try {
            return "0800".equals(isoMsg.getMTI()) || "0810".equals(isoMsg.getMTI());
        } catch (ISOException e) {
            return false;
        }
    }
}
