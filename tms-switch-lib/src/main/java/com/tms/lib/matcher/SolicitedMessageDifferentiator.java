package com.tms.lib.matcher;

import org.apache.commons.lang3.tuple.Pair;


public interface SolicitedMessageDifferentiator<T> {
    Pair<Boolean, T> isSolicitedMessage(byte[] message);
}
