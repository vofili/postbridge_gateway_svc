package com.tms.lib.matcher;


public interface ResponseMatcherFactory<T> {

    ResponseMatcher<T> getMatcher(T data);

    String getMatcherKey(T data);

}
