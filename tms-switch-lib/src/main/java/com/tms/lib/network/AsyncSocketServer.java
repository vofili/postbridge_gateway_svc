package com.tms.lib.network;

import com.tms.lib.matcher.SolicitedMessageDifferentiator;
import com.tms.lib.network.transciever.TranscieveFunction;
import com.tms.lib.util.AsyncWorkerPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.function.Consumer;

@Slf4j
public class AsyncSocketServer<T> {

    private AsynchronousServerSocketChannel server;
    private String host;
    private int port;
    private TranscieveFunction<T, byte[]> transciever;
    private SolicitedMessageDifferentiator<T> responseDifferentiator;
    private Consumer<T> reader;
    private AsyncWorkerPool workerPool;
    private boolean isStarted;

    public AsyncSocketServer(String host,
                             int port,
                             TranscieveFunction<T, byte[]> transciever,
                             SolicitedMessageDifferentiator<T> responseDifferentiator,
                             Consumer<T> reader,
                             AsyncWorkerPool workerPool) {
        this.host = host;
        this.port = port;
        this.workerPool = workerPool;
        this.transciever = transciever;
        this.responseDifferentiator = responseDifferentiator;
        this.reader = reader;
    }

    public void start() throws IOException {
        server = AsynchronousServerSocketChannel.open();
        InetSocketAddress sAddr = new InetSocketAddress(host, port);
        server.bind(sAddr);
        log.info("Listening for connections on port: " + port);
        log.info("Waiting for messages from client");
        ConnectionData attach = new ConnectionData(server);
        isStarted = true;
        server.accept(attach, new ConnectionHandler());
    }

    public void stop() throws IOException {
        if (server == null) {
            return;
        }
        server.close();
        isStarted = false;
        server = null;
    }


    class ConnectionData {
        AsynchronousServerSocketChannel server;


        public ConnectionData(AsynchronousServerSocketChannel server) {
            this.server = server;
        }
    }

    class ReadData {
        AsynchronousSocketChannel client;
        ByteBuffer buffer;
    }


    class ConnectionHandler implements CompletionHandler<AsynchronousSocketChannel, ConnectionData> {
        @Override
        public void completed(AsynchronousSocketChannel client, ConnectionData connectionData) {
            try {
                connectionData.server.accept(new ConnectionData(server), this);
                SocketAddress clientAddr = client.getRemoteAddress();
                log.info(String.format("Accepted a connection from %s", clientAddr));
                setupFirstTwoBytesReading(client);
            } catch (IOException e) {
                log.error("Could not process client connection", e);
            }
        }

        @Override
        public void failed(Throwable exc, ConnectionData attachment) {
            log.error("Could not connect successfully to client ", exc);
        }
    }

    class FirstTwoBytesReader implements CompletionHandler<Integer, ReadData> {

        @Override
        public void failed(Throwable exc, ReadData firstTwoBytesData) {
            log.warn("could not read first two bytes", exc);
        }

        @Override
        public void completed(Integer result, ReadData readLenData) {
            if (result == -1) {
                return;
            }
            readLenData.buffer.flip();
            ReadData actualData = new ReadData();
            actualData.client = readLenData.client;
            short len = readLenData.buffer.getShort();
            actualData.buffer = ByteBuffer.allocate(len);
            log.info(String.format("Length bytes (2) read. expecting %d bytes", len));
            actualData.client.read(actualData.buffer, actualData, new RemainingBytesHandler());
        }
    }


    class RemainingBytesHandler implements CompletionHandler<Integer, ReadData> {

        @Override
        public void failed(Throwable exc, ReadData remainingBytesData) {
            log.error("Failed read", exc);
        }

        @Override
        public void completed(Integer result, ReadData remainingBytesData) {
            if (result == -1) {
                return;
            }
            setupFirstTwoBytesReading(remainingBytesData.client);
            remainingBytesData.buffer.flip();

            log.info(String.format("Remaining bytes of len %d read as: %s", remainingBytesData.buffer.limit(), new String(remainingBytesData.buffer.array())));


            if (transciever == null) {
                log.info("No listener found for received message");
                return;
            }
            Pair<Boolean, T> differentiatorResponsePair = responseDifferentiator.isSolicitedMessage(remainingBytesData.buffer.array());
            if (differentiatorResponsePair.getLeft()) {
                workerPool.queueJob(() -> {
                    handleSolicitedMessage(differentiatorResponsePair.getRight());
                    return null;
                });
            } else {
                workerPool.queueJob(() -> {
                    handleUnSolicitedMessage(differentiatorResponsePair.getRight(), remainingBytesData);
                    return null;
                });
            }
        }
    }

    private void handleSolicitedMessage(T response) {
        if (reader != null) {
            reader.accept(response);
            return;
        }
        log.info("Solicited response reader is null");
    }

    private void handleUnSolicitedMessage(T request, ReadData readData) {
        try {
            log.info("Trying to send message for processing");
            byte[] response = transciever.transcieve(request);
            if (response != null) {
                log.info("Received response");
                ByteBuffer responseBuffer = ByteBuffer.wrap(response);
                synchronized (readData.client) {
                    readData.client.write(responseBuffer);
                }
            }
        } catch (Exception e) {
            log.info("An error occurred while processing request", e);
        }
    }

    private void setupFirstTwoBytesReading(AsynchronousSocketChannel client) {
        ReadData lenData = new ReadData();
        lenData.client = client;
        lenData.buffer = ByteBuffer.allocate(2);
        client.read(lenData.buffer, lenData, new FirstTwoBytesReader());
    }

    public boolean isStarted() {
        return isStarted;
    }
}
