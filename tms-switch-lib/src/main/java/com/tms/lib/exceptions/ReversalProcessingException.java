package com.tms.lib.exceptions;

public class ReversalProcessingException extends Exception {

    public ReversalProcessingException(String s, Throwable t){
        super(s, t);
    }

    public ReversalProcessingException(String s){
        super(s);
    }

    public ReversalProcessingException(Throwable t){
        super(t);
    }
}
