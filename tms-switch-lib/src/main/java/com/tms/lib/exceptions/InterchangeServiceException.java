package com.tms.lib.exceptions;

public class InterchangeServiceException extends Exception {

    public InterchangeServiceException(String s, Throwable t){
        super(s, t);
    }

    public InterchangeServiceException(String s){
        super(s);
    }

    public InterchangeServiceException(Throwable t){
        super(t);
    }
}
