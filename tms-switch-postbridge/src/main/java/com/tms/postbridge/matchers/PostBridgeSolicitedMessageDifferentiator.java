package com.tms.postbridge.matchers;

import com.tms.lib.matcher.SolicitedMessageDifferentiator;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PostBridgeSolicitedMessageDifferentiator implements SolicitedMessageDifferentiator<ISOMsg> {

    private static final Logger logger = Logger.getLogger(PostBridgeSolicitedMessageDifferentiator.class.getName());
    private ISOPackager isoPackager;

    public PostBridgeSolicitedMessageDifferentiator(ISOPackager isoPackager) {
        this.isoPackager = isoPackager;
    }

    @Override
    public Pair<Boolean, ISOMsg> isSolicitedMessage(byte[] message) {
        ISOMsg responseIso = new ISOMsg();
        try {
            isoPackager.unpack(responseIso, message);
            return new ImmutablePair<>(isResponse(responseIso), responseIso);
        } catch (ISOException e) {
            logger.log(Level.INFO, "Could not unpack message", e);
            return new ImmutablePair<>(Boolean.FALSE, null);
        }
    }

    private boolean isResponse(ISOMsg isoMsg) {
        try {
            String mti = isoMsg.getMTI();
            return mti.equals("0110") || mti.equals("0130") || mti.equals("0210") || mti.equals("0230") || mti.equals("0430") || mti.equals("0610") || mti.equals("0810");
        } catch (ISOException e) {
            return false;
        }
    }
}
