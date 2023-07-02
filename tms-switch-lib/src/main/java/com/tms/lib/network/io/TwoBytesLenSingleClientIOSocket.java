package com.tms.lib.network.io;

import com.tms.lib.util.AsyncWorkerPool;
import com.tms.lib.events.NetworkConnectionEventListener;
import com.tms.lib.exceptions.InterchangeIOException;
import com.tms.lib.matcher.SolicitedMessageDifferentiator;
import com.tms.lib.network.NetworkConnection;
import com.tms.lib.network.SingleClientSocketChannel;
import com.tms.lib.util.ByteUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

@Slf4j
public class TwoBytesLenSingleClientIOSocket<T> extends SingleClientSocketChannel<T> {

    private Socket client;

    public TwoBytesLenSingleClientIOSocket(String host,
                                           int port,
                                           AsyncWorkerPool asyncWorkerPool,
                                           NetworkConnectionEventListener networkConnectionEventListener,
                                           Consumer<T> reader,
                                           SolicitedMessageDifferentiator solicitedMessageDifferentiator) {
        super(host, port, asyncWorkerPool, networkConnectionEventListener,
                reader, solicitedMessageDifferentiator);
    }

    public void reset() {
        stop();
        start();
    }

    public synchronized void start() {
        enabled.set(true);
        notifyEnable();
    }

    public synchronized void stop() {
        enabled.set(false);
        closeSocket();
    }

    protected void connectLoop() {
        while (true) {
            while (isConnected()) {
                awaitDisconnect();
            }
            while (!enabled.get()) {
                awaitEnable();
            }

            try {
                if (client != null) {
                    client.close();
                }
                client = new Socket();
                client.setSoLinger(true, 0);
                client.setKeepAlive(true);
                client.setTcpNoDelay(true);
                try {
                    log.info(String.format("Connecting to server %s:%d", host, port));
                    client.connect(new InetSocketAddress(host, port));
                    log.info(String.format("Connected to server %s:%d", host, port));
                } catch (IOException e) {
                    if (client != null) {
                        closeSocket();
                    }
                    log.error(String.format("Failure to connect to remote %s:%d", host, port), e);
                    awaitRetryConnect();
                }
                if (isConnected()) {
                    reconnectMultiple = 1;
                    notifyConnect();
                }
            } catch (Exception e) {
                log.error(String.format("There was an error during connection loop %s:%d", host, port), e);
            }
        }
    }

    protected void read() {
        while (true) {
            try {
                while (!enabled.get()) {
                    awaitEnable();
                }
                while (!isConnected()) {
                    awaitConnect();
                }

                try {
                    byte[] lenBytes = readToByteArray(client.getInputStream(), 2);
                    int len = ByteUtils.decodeSignedLenBytes(lenBytes[0], lenBytes[1]);

                    log.info(String.format("Length bytes (2) read [%d,%d]. expecting %d bytes from remote %s:%d", lenBytes[0], lenBytes[1], len, host, port));

                    byte[] readData = readToByteArray(client.getInputStream(), len);

                    log.info(String.format("Read data from %s:%d of len %d is %s", host, port, len, new String(readData)));


                    Pair<Boolean, T> response = solicitedMessageDifferentiator.isSolicitedMessage(readData);
                    if (response.getValue() == null) {
                        log.error("The read and converted response is null");
                        continue;
                    }
                    if (response.getLeft()) {
                        //solicited response to a sent message
                        asyncWorkerPool.queueJob(() -> {
                            long readerAcceptStartTime = System.currentTimeMillis();
                            reader.accept(response.getRight());
                            return null;
                        });

                    } else{
                        log.error("Cannot process received message as it is not a response to a request");
                    }

                } catch (IOException e) {
                    closeSocket();
                    log.error(String.format("There was an error reading data from socket %s:%d", host, port), e);
                }
            } catch (Exception e) {
                log.error(String.format("There was an error reading data in read loop for client %s:%d", host, port), e);
            }
        }
    }

    private byte[] readToByteArray(InputStream is, int len) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(is);
        byte[] data = new byte[len];
        dataInputStream.readFully(data);

        return data;
    }

    private void notifyConnect() {
        synchronized (connectLock) {
            connectLock.notifyAll();
        }
        if (client != null) {
            currentNetworkConnection = NetworkConnection.fromSocket(client);
            networkConnectionEventListener.notifyConnect(currentNetworkConnection);
        }

    }

    private void awaitConnect() {
        synchronized (connectLock) {
            try {
                connectLock.wait();
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isConnected() {
        if (client == null) {
            return false;
        }
        return client.isConnected();
    }

    private void notifyDisconnect() {
        synchronized (disconnectLock) {
            disconnectLock.notifyAll();
        }
        networkConnectionEventListener.notifyDisconnect(currentNetworkConnection);
        }

    private void awaitDisconnect() {
        synchronized (disconnectLock) {
            try {
                disconnectLock.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void awaitEnable() {
        synchronized (enabled) {
            try {
                enabled.wait();
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
        }
    }

    private void notifyEnable() {
        synchronized (enabled) {
            enabled.notifyAll();
        }
    }

    private void awaitRetryConnect() {
        synchronized (retryConnectLock) {
            try {
                int time = reconnectMultiple * RECONNECT_TIME;
                if (time <= MAX_RECONNECT_TIME) {
                    reconnectMultiple++;
                }
                retryConnectLock.wait(time);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected synchronized void closeSocket() {
        if (client == null || !client.isConnected()) {
            return;
        }
        try {
            closeInputStream();
            closeOutputStream();
            client.close();
        } catch (IOException e) {
            log.error(String.format("There was an exception closing the client socket %s:%d", host, port), e);
        } finally {
            client = null;
            notifyDisconnect();
            currentNetworkConnection = null;

        }
    }

    private void closeInputStream() {
        InputStream is;
        try {
            is = client.getInputStream();
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            log.error(String.format("Could not close input stream for client %s:%d", host, port), e);
        }

    }

    private void closeOutputStream() {
        OutputStream os;
        try {
            os = client.getOutputStream();
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            log.error(String.format("Could not close output stream for client %s:%d", host, port), e);
        }

    }

    @Override
    public synchronized void write(byte[] data) throws IOException, InterchangeIOException {
        if (client == null) {
            throw new InterchangeIOException(String.format("There is no connection to client %s:%d", host, port));
        }
        if (!client.isConnected()) {
            throw new InterchangeIOException(String.format("There is no connection to client %s:%d", host, port));
        }
        client.getOutputStream().write(data);
    }


    public NetworkConnection getCurrentNetworkConnection() {
        return currentNetworkConnection;
    }

}
