package com.tms.lib.exceptions;

public class ServiceProcessingException extends Exception {

    public ServiceProcessingException(String s, Throwable t){
        super(s, t);
    }

    public ServiceProcessingException(String s){
        super(s);
    }

    public ServiceProcessingException(Throwable t){
        super(t);
    }
}
