package com.tms.pos.sink.generic;

import com.tms.lib.matcher.SolicitedMessageDifferentiator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

@Slf4j
public class GenericPosSolicitedMessageDifferentiator implements SolicitedMessageDifferentiator<ISOMsg> {

    private ISOPackager isoPackager;

    public GenericPosSolicitedMessageDifferentiator(ISOPackager packager) {
        this.isoPackager = packager;
    }

    @Override
    public Pair<Boolean, ISOMsg> isSolicitedMessage(byte[] message) {
        ISOMsg responseIso = new ISOMsg();
        try {
            isoPackager.unpack(responseIso, message);
            responseIso.setPackager(isoPackager);
            return new ImmutablePair<>(isResponse(responseIso), responseIso);
        } catch (ISOException e) {
            log.error("Could not unpack message", e);
            return new ImmutablePair<>(Boolean.FALSE, new ISOMsg());
        }
    }

    private boolean isResponse(ISOMsg isoMsg) {
        try {
            String mti = isoMsg.getMTI();
            return mti.equals("0110") || mti.equals("0130") || mti.equals("0230") || mti.equals("0210")
                    || mti.equals("0430") || mti.equals("0610") || mti.equals("0810");
        } catch (ISOException e) {
            log.error("Could not get mti", e);
            return false;
        }
    }
}
