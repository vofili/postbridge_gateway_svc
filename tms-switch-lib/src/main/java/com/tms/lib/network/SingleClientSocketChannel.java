package com.tms.lib.network;

import com.tms.lib.util.AsyncWorkerPool;
import com.tms.lib.events.NetworkConnectionEventListener;
import com.tms.lib.exceptions.InterchangeIOException;
import com.tms.lib.exceptions.ServiceRuntimeException;
import com.tms.lib.matcher.SolicitedMessageDifferentiator;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public abstract class SingleClientSocketChannel<T> implements ClientSocketChannel {

    protected String host;
    protected int port;
    protected AsyncWorkerPool asyncWorkerPool;
    ExecutorService executorService = Executors.newFixedThreadPool(3);
    protected static final int MIN_POOL_SIZE = 3;
    protected final AtomicBoolean enabled = new AtomicBoolean(false);
    protected NetworkConnectionEventListener networkConnectionEventListener;
    protected SolicitedMessageDifferentiator solicitedMessageDifferentiator;
    protected NetworkConnection currentNetworkConnection;
    protected Consumer<T> reader;

    protected final Object connectLock = new Object();
    protected final Object disconnectLock = new Object();
    protected final Object retryConnectLock = new Object();

    protected int reconnectMultiple = 1;
    protected static int RECONNECT_TIME = 100;
    protected static int MAX_RECONNECT_TIME = 1000;

    public SingleClientSocketChannel(String host,
                                     int port,
                                     AsyncWorkerPool asyncWorkerPool,
                                     NetworkConnectionEventListener networkConnectionEventListener,
                                     Consumer<T> reader,
                                     SolicitedMessageDifferentiator solicitedMessageDifferentiator) {
        this.host = host;
        this.port = port;
        this.asyncWorkerPool = asyncWorkerPool;
        this.networkConnectionEventListener = networkConnectionEventListener;
        this.reader = reader;
        this.solicitedMessageDifferentiator = solicitedMessageDifferentiator;

        if (!isRequiredMinPoolSize()) {
            throw new ServiceRuntimeException(String.format("Could not initiate connection. Minimum pool size for async worker should be %d", MIN_POOL_SIZE));
        }

        asyncWorkerPool.queueJob(() -> {
            read();
            return null;
        });

        asyncWorkerPool.queueJob(() -> {
            connectLoop();
            return null;
        });

    }

    private boolean isRequiredMinPoolSize() {
        return this.asyncWorkerPool.getPoolSize() >= MIN_POOL_SIZE;
    }

    public abstract void start();

    public abstract void stop();

    public abstract void reset();

    protected abstract void connectLoop();

    protected abstract void read();

    public abstract void write(byte[] data) throws IOException, InterchangeIOException;

    @Override
    public void writeRequest(byte[] data) throws IOException, InterchangeIOException {
        write(data);
    }

    @Override
    public void writeResponse(byte[] data) throws IOException, InterchangeIOException {
        write(data);
    }

    @Override
    public void notifyResponseWaitTimeOut() {

    }

    protected abstract void closeSocket();

    public abstract NetworkConnection getCurrentNetworkConnection();

}
