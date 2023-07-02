package com.tms.lib.exceptions;

public class TransactionProcessingException extends Exception {

    public TransactionProcessingException(String s, Throwable t) {
        super(s, t);
    }

    public TransactionProcessingException(String s) {
        super(s);
    }

    public TransactionProcessingException(Throwable t) {
        super(t);
    }
}
