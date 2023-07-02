package com.tms.lib.network.io;

import com.tms.lib.util.ByteUtils;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Slf4j
public class TwoByteLenBlockingSingleSocketClient {

    private boolean useSSL;
    private static final String SSL_PROTOCOL = "TLSv1.2";
    private Socket socket;
    private String host;
    private int port;
    private int socketConnectionTimeout;
    private int socketReadTimeout;

    public TwoByteLenBlockingSingleSocketClient(boolean useSSL, String host, int port, int socketConnectionTimeout, int socketReadTimeout) {
        this.useSSL = useSSL;
        this.host = host;
        this.port = port;
        this.socketConnectionTimeout = socketConnectionTimeout;
        this.socketReadTimeout = socketReadTimeout;
    }

    public void connect() throws IOException {
        socket = getSocket();
        if (socket != null) {
            socket.connect(new InetSocketAddress(host, port), socketConnectionTimeout);
        }
    }

    public void write(byte[] message) throws IOException {
        if (message == null) {
            throw new IOException("message to write is null");
        }
        if (socket == null) {
            throw new IOException("Socket is null");
        }
        if (!socket.isConnected()) {
            throw new IOException(String.format("There is no connection to client %s:%d", host, port));
        }
        log.trace(String.format("writing to %s:%d %d bytes%nMessage: %s%nMessage", host, port, message.length, new String(message)));
        socket.getOutputStream().write(message);
    }

    public byte[] read() throws IOException {
        byte[] lenBytes = readToByteArray(socket.getInputStream(), 2);
        int len = ByteUtils.decodeSignedLenBytes(lenBytes[0], lenBytes[1]);
        log.info(String.format("Length bytes (2) read [%d,%d]. expecting %d bytes from remote %s:%d", lenBytes[0], lenBytes[1], len, host, port));
        byte[] readData = readToByteArray(socket.getInputStream(), len);
        log.trace(String.format("Read data from %s:%d of len %d is %s", host, port, len, new String(readData)));
        return readData;
    }

    private byte[] readToByteArray(InputStream is, int len) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(is);
        byte[] data = new byte[len];
        dataInputStream.readFully(data);

        return data;
    }

    private Socket getSocket() throws IOException {
        Socket socket;
        if (useSSL) {
            socket = getConnectedSSLSocket();
        } else {
            socket = getConnectedPlainSocket();
        }
        return setSocketOptions(socket);
    }

    public void close() throws IOException {
        if (socket == null) {
            return;
        }
        socket.close();
        socket = null;
    }

    private Socket setSocketOptions(Socket socket) throws SocketException {
        socket.setSoLinger(true, 0);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(socketReadTimeout);
        return socket;
    }

    private Socket getConnectedPlainSocket() {
        Socket socket = new Socket();
        return socket;
    }

    private Socket getConnectedSSLSocket() throws IOException {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(
                            X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            X509Certificate[] certs, String authType) {
                    }
                }
        };
        try {
            SSLContext sc = SSLContext.getInstance(SSL_PROTOCOL);
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLSocketFactory factory = sc.getSocketFactory();
            return factory.createSocket();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException("Could not create SSL socket", e);
        }
    }

}
