package com.tms.lib.matcher;

import org.apache.commons.lang3.tuple.Pair;


public interface ResponseMatcher<T> {

    Pair<Boolean, T> isResponse(T message);

    void setResponse(T response);

    T getResponse();

    String getKey();
}
