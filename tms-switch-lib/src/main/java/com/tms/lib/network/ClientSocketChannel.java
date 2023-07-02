package com.tms.lib.network;

import com.tms.lib.exceptions.InterchangeIOException;

import java.io.IOException;


public interface ClientSocketChannel {

    public abstract void writeRequest(byte[] data) throws IOException, InterchangeIOException;

    public abstract void writeResponse(byte[] data) throws IOException, InterchangeIOException;

    public abstract void write(byte[] data) throws IOException, InterchangeIOException;

    public abstract void notifyResponseWaitTimeOut();
}
