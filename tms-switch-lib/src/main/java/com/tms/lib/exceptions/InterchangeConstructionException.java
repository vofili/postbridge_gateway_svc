package com.tms.lib.exceptions;

public class InterchangeConstructionException extends Exception {

    public InterchangeConstructionException(String s, Throwable t){
        super(s, t);
    }

    public InterchangeConstructionException(String s){
        super(s);
    }

    public InterchangeConstructionException(Throwable t){
        super(t);
    }
}
