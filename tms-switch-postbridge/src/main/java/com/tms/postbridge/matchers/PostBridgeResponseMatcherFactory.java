package com.tms.postbridge.matchers;

import com.tms.lib.exceptions.ServiceRuntimeException;
import com.tms.lib.matcher.IsoClientResponseMatcher;
import com.tms.lib.matcher.ResponseMatcherFactory;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

public class PostBridgeResponseMatcherFactory implements ResponseMatcherFactory<ISOMsg> {

    @Override
    public IsoClientResponseMatcher getMatcher(ISOMsg isoMsg) {
        if (ManagementMessageResponseMatcher.isManagementMessage(isoMsg)) {
            return new ManagementMessageResponseMatcher(isoMsg);
        }
        if (TransactionMessageResponseMatcher.isTransactionMessage(isoMsg)) {
            return new TransactionMessageResponseMatcher(isoMsg);
        }
        throw new ServiceRuntimeException("Could not find response matcher for request");
    }

    @Override
    public String getMatcherKey(ISOMsg data) {
        if (ManagementMessageResponseMatcher.isManagementMessage(data)) {
            return ManagementMessageResponseMatcher.getKey(data);
        }
        if (TransactionMessageResponseMatcher.isTransactionMessage(data)) {
            try {
                return TransactionMessageResponseMatcher.getKey(data);
            } catch (ISOException e) {
                return null;
            }
        }
        return null;
    }


}
