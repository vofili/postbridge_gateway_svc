package com.tms.lib.util;

import org.apache.commons.lang3.StringUtils;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class IsoLogger {

    private IsoLogger() {

    }

    public static String dump(ISOMsg msg) {
        ISOMsg loggeableMessage = new ISOMsg();
        IsoUtil.copyFields(msg, loggeableMessage);
        if (msg == null) {
            return "<nothing>";
        }
        try {
            loggeableMessage.setMTI(msg.getMTI());
        } catch (ISOException e) {
            //Purposely not logging any exception here
        }
        loggeableMessage.set(2, CardUtil.maskPan(msg.getString(2)));
        loggeableMessage.set(35, CardUtil.maskTrack2(msg.getString(35)));
        loggeableMessage.set(52, maskField(loggeableMessage.getString(52)));
        loggeableMessage.set(53, maskField(loggeableMessage.getString(53)));
        try {
            loggeableMessage.set("127", msg.getComponent(127));
        } catch (ISOException e) {
            //Purposely not logging any exception, system should log without the field should there be any issues
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        loggeableMessage.dump(ps, " ");

        return baos.toString();
    }

    private static String maskField(String data) {
        if (StringUtils.isEmpty(data)) {
            return data;
        }
        return (new String(new char[data.length()])).replace("\u0000", "*");
    }
}
