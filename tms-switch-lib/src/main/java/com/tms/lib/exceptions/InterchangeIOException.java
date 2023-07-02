package com.tms.lib.exceptions;

public class InterchangeIOException extends Exception {

    public InterchangeIOException(String s, Throwable t){
        super(s, t);
    }

    public InterchangeIOException(String s){
        super(s);
    }

    public InterchangeIOException(Throwable t){
        super(t);
    }
}
