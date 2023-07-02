package com.tms.lib.exceptions;

public class HsmException extends Exception {

    public HsmException(String s, Throwable t){
        super(s, t);
    }

    public HsmException(String s){
        super(s);
    }

    public HsmException(Throwable t){
        super(t);
    }
}
