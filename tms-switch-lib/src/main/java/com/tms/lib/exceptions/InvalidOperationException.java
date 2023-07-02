package com.tms.lib.exceptions;

public class InvalidOperationException extends Exception {

    public InvalidOperationException(String s, Throwable t){
        super(s, t);
    }

    public InvalidOperationException(String s){
        super(s);
    }

    public InvalidOperationException(Throwable t){
        super(t);
    }
}
