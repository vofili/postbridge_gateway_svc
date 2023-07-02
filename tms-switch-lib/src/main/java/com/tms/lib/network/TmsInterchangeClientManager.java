package com.tms.lib.network;

import com.tms.lib.exceptions.*;
import com.tms.lib.interchange.InterchangeConfig;
import com.tms.lib.interchange.SocketTypeInterchangeConfig;
import com.tms.lib.network.io.TwoByteLenBlockingSingleSocketClient;
import com.tms.lib.util.ByteUtils;
import com.tms.lib.util.IsoLogger;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

import java.io.IOException;

@Slf4j
public class TmsInterchangeClientManager {

    private static final String INVALID_DATA_MSG = "Invalid data supplied for sending";

    private TmsInterchangeClientManager(){

    }

    public static ISOMsg send(ISOMsg isoMsg, InterchangeConfig interchangeConfig, ISOPackager packager) throws TransactionProcessingException, IOException {
        if (interchangeConfig == null || isoMsg == null) {
            throw new TransactionProcessingException(INVALID_DATA_MSG);
        }

        byte[] isoRequestBytes;
        isoMsg.setPackager(packager);

        try {
            isoRequestBytes = isoMsg.pack();
        } catch (ISOException e) {
            throw new TransactionProcessingException("Cannot pack iso msg to bytes", e);
        }

        log.info("Raw Request {}", IsoLogger.dump(isoMsg));

        ISOMsg responseIso = new ISOMsg();


        TwoByteLenBlockingSingleSocketClient client = null;
        try {
            SocketTypeInterchangeConfig socketTypeInterchangeConfig = interchangeConfig.getSocketTypeInterchangeConfig();
            if (socketTypeInterchangeConfig == null) {
                try {
                    socketTypeInterchangeConfig = SocketTypeInterchangeConfig.getConfig(interchangeConfig.getInterchangeSpecificData());
                } catch (UtilOperationException e) {
                    throw new TransactionProcessingException("Could not extract socket configuration", e);
                }
            }

            String portsList = socketTypeInterchangeConfig.getSinkPorts();
            if (portsList == null) {
                throw new TransactionProcessingException("Cannot send transaction, no sink port configured");
            }

            String host = socketTypeInterchangeConfig.getSinkHost();
            int sinkPort;
            String[] sinkPortArray = portsList.split(",");
            try {
                sinkPort = Integer.parseInt(sinkPortArray[0]);
            } catch (NumberFormatException e) {
                throw new ServiceRuntimeException(String.format("Cannot convert port %s to int", sinkPortArray[0]), e);
            }
            client = new TwoByteLenBlockingSingleSocketClient(socketTypeInterchangeConfig.isUseSSL(),
                    host, sinkPort,
                    socketTypeInterchangeConfig.getSocketTimeOut(), socketTypeInterchangeConfig.getSocketTimeOut());

            try {
                client.connect();
            } catch (IOException e) {
                throw new SocketConnectionException(String.format("Could not connect to server on %s:%d", host, sinkPort), e);
            }

            try {
                client.write(ByteUtils.prependLenBytes(isoRequestBytes));
            } catch (IOException e) {
                throw new SocketWriteException(String.format("Could not write to server on %s:%d", host, sinkPort), e);
            }

            try {
                byte[] response = client.read();
                log.info("read message {}", new String(response));

                packager.unpack(responseIso, response);
                log.info("Raw Response {}", IsoLogger.dump(responseIso));
                return responseIso;
            } catch (IOException e) {
                throw new SocketReadException(String.format("Could not read from server on %s:%d", host, sinkPort), e);
            } catch (ISOException e) {
                throw new TransactionProcessingException("Could not unpack response message", e);
            }


        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    log.error("Could not close socket cleanly", e);
                }
            }
        }
    }

}
