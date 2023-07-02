package com.tms.lib.exceptions;

import java.io.IOException;

public class SocketWriteException extends IOException {

    public SocketWriteException(String s, Throwable t) {
        super(s, t);
    }

    public SocketWriteException(String s) {
        super(s);
    }

    public SocketWriteException(Throwable t) {
        super(t);
    }
}
