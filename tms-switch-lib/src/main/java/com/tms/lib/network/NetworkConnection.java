package com.tms.lib.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;


public class NetworkConnection {

    private static final Logger logger = Logger.getLogger(NetworkConnection.class.getName());
    private String remoteHost;
    private int remotePort;
    private String localHost;
    private int localPort;


    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public String getLocalHost() {
        return localHost;
    }

    public void setLocalHost(String localHost) {
        this.localHost = localHost;
    }

    public static NetworkConnection fromSocket(Socket socket) {
        NetworkConnection networkConnection = new NetworkConnection();
        networkConnection.setRemoteHost(socket.getInetAddress().getHostName());
        networkConnection.setRemotePort(socket.getPort());
        networkConnection.setLocalHost(socket.getLocalAddress().getHostName());
        networkConnection.setLocalPort(socket.getLocalPort());
        return networkConnection;
    }


    public static NetworkConnection fromAsycnhronousSocketChannel(AsynchronousSocketChannel networkChannel) {
        NetworkConnection networkConnection = new NetworkConnection();
        try {
            SocketAddress socketAddress = networkChannel.getRemoteAddress();
            if (socketAddress instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                networkConnection.setRemoteHost(inetSocketAddress.getHostName());
                networkConnection.setRemotePort(inetSocketAddress.getPort());
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Could not get remote address of network channel", e);
        }
        return networkConnection;
    }

    public static NetworkConnection fromSocketChannel(SocketChannel networkChannel) {
        NetworkConnection networkConnection = new NetworkConnection();
        try {
            SocketAddress socketAddress = networkChannel.getRemoteAddress();
            if (socketAddress instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                networkConnection.setRemoteHost(inetSocketAddress.getHostName());
                networkConnection.setRemotePort(inetSocketAddress.getPort());
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Could not get remote address of network channel", e);
        }
        return networkConnection;
    }

}
