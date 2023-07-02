package com.tms.lib.exceptions;

public class RouterException extends Exception {

    public RouterException(String s, Throwable t){
        super(s, t);
    }

    public RouterException(String s){
        super(s);
    }

    public RouterException(Throwable t){
        super(t);
    }
}
