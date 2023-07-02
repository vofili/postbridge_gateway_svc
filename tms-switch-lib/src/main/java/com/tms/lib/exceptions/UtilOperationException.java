package com.tms.lib.exceptions;

public class UtilOperationException extends Exception {

    public UtilOperationException(String s, Throwable t){
        super(s, t);
    }

    public UtilOperationException(String s){
        super(s);
    }

    public UtilOperationException(Throwable t){
        super(t);
    }
}
