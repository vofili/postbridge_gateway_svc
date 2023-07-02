package com.tms.lib.exceptions;

public class ServiceRuntimeException extends RuntimeException {

    public ServiceRuntimeException(String s, Throwable t){
        super(s, t);
    }

    public ServiceRuntimeException(String s){
        super(s);
    }

    public ServiceRuntimeException(Throwable t){
        super(t);
    }
}
