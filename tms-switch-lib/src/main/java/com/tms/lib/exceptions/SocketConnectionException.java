package com.tms.lib.exceptions;

import java.io.IOException;

public class SocketConnectionException extends IOException {

    public SocketConnectionException(String s, Throwable t) {
        super(s, t);
    }

    public SocketConnectionException(String s) {
        super(s);
    }

    public SocketConnectionException(Throwable t) {
        super(t);
    }
}
