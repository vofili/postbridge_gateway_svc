package com.tms.lib.exceptions;

public class CryptoException extends Exception {

    public CryptoException(String s, Throwable t){
        super(s, t);
    }

    public CryptoException(String s){
        super(s);
    }

    public CryptoException(Throwable t){
        super(t);
    }
}
