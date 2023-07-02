package com.tms.lib.exceptions;

import java.io.IOException;

public class SocketReadException extends IOException {

    public SocketReadException(String s, Throwable t) {
        super(s, t);
    }

    public SocketReadException(String s) {
        super(s);
    }

    public SocketReadException(Throwable t) {
        super(t);
    }
}
