package com.tms.postbridge;

import com.tms.lib.exceptions.InterchangeIOException;
import com.tms.lib.exceptions.InterchangeServiceException;
import com.tms.lib.matcher.IsoClientResponseMatcher;
import com.tms.lib.model.DefaultIsoResponseCodes;
import com.tms.lib.network.ClientSocketChannel;
import com.tms.lib.network.ConnectionProvider;
import com.tms.lib.network.transciever.IsoMsgTransceiveFunction;
import com.tms.lib.util.ByteUtils;
import com.tms.lib.util.IsoLogger;
import com.tms.postbridge.matchers.PostBridgeResponseMatcherFactory;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PostBridgeResponseIsoTransciever implements IsoMsgTransceiveFunction, Consumer<ISOMsg> {

    private static final Logger logger = Logger.getLogger(PostBridgeResponseIsoTransciever.class.getName());

    private PostBridgeResponseMatcherFactory responseMatcherFactory;
    private ISOPackager isoPackager;
    private int receiveTimeout;
    private ConcurrentMap<String, IsoClientResponseMatcher> responseMatcherConcurrentMap = new ConcurrentHashMap<>();
    PostBridgeInterchange postBridgeInterchange;
    private ConnectionProvider connectionProvider;


    public PostBridgeResponseIsoTransciever(PostBridgeInterchange postBridgeInterchange,
                                            int receiveTimeout) {


        this.isoPackager = postBridgeInterchange.getPackager();
        this.responseMatcherFactory = new PostBridgeResponseMatcherFactory();
        this.receiveTimeout = receiveTimeout;
        this.postBridgeInterchange = postBridgeInterchange;
    }

    @Override
    public ISOMsg transcieve(ISOMsg t) throws InterchangeIOException, InterchangeServiceException {
        ClientSocketChannel clientSocketChannel = getClientSocketChannel();
        if (clientSocketChannel == null) {
            throw new InterchangeIOException("Client is not connected");
        }
        IsoClientResponseMatcher responseMatcher = getResponseMatcher(t);
        if (responseMatcher == null) {
            throw new InterchangeServiceException("Could not get a response matcher for request");
        }
        t.setPackager(isoPackager);
        byte[] bytes;
        try {
            bytes = t.pack();
        } catch (ISOException e) {
            throw new InterchangeServiceException("Could not pack request ISO", e);
        }

        if (responseMatcherConcurrentMap.containsKey(responseMatcher.getKey())) {
            try {
                //logger.log(Level.SEVERE, String.format("Matcher key is not unique: %s, declining transaction with error code 06... %s", responseMatcher.getKey(), IsoPrinter.dump(t)));
                t.set(39, DefaultIsoResponseCodes.Error.toString());
                return t;
            } catch (Exception e) {
                throw new InterchangeServiceException("An error occurred while declining transaction with non unique matcher key", e);
            }
        }
        responseMatcherConcurrentMap.put(responseMatcher.getKey(), responseMatcher);

        try {
            byte[] fullBytes = ByteUtils.prependLenBytes(bytes);
            String log = String.format("writing %d bytes\r\n%s", fullBytes.length, new String(fullBytes));
            logger.log(Level.INFO, log);
            clientSocketChannel.writeRequest(fullBytes);
        } catch (IOException e) {
            responseMatcherConcurrentMap.remove(responseMatcher.getKey());
            throw new InterchangeIOException("There was an error while writing to stream", e);
        }

        try {
            synchronized (responseMatcher) {
                responseMatcher.wait(receiveTimeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            clientSocketChannel.notifyResponseWaitTimeOut();
            throw new InterchangeIOException("Thread was interrupted while waiting for response from upstream");
        } finally {
            responseMatcherConcurrentMap.remove(responseMatcher.getKey());
        }

        if (responseMatcher.getResponse() == null) {
            clientSocketChannel.notifyResponseWaitTimeOut();
        }
        return responseMatcher.getResponse();
    }

    private IsoClientResponseMatcher getResponseMatcher(ISOMsg request) {
        return responseMatcherFactory.getMatcher(request);
    }

    private String getResponseMatchKey(ISOMsg response) {
        return responseMatcherFactory.getMatcherKey(response);
    }

    public void notifyResponseMessage(ISOMsg data) {
        //logger.log(Level.INFO, String.format("Received Response Message %s ", IsoPrinter.dump(data)));
        String responseMatchKey = getResponseMatchKey(data);
        if (responseMatchKey != null) {
            IsoClientResponseMatcher isoClientResponseMatcher = responseMatcherConcurrentMap.get(responseMatchKey);
            if (isoClientResponseMatcher != null) {
                isoClientResponseMatcher.setResponse(data);
                synchronized (isoClientResponseMatcher) {
                    isoClientResponseMatcher.notify();
                }
                logger.log(Level.INFO, "Response matcher notified");
                return;
            }
        }
        logger.log(Level.SEVERE, String.format("Could not get response matcher for response %s ", IsoLogger.dump(data)));
    }

    private ClientSocketChannel getClientSocketChannel() {
        if (connectionProvider == null) {
            logger.log(Level.INFO, "Connection provider is not set");
            return null;
        }
        return connectionProvider.getSocketConnection();
    }

    public void setConnectionProvider(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void accept(ISOMsg isoMsg) {
        notifyResponseMessage(isoMsg);
    }
}

