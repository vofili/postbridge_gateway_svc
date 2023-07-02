package com.tms.lib.events;

public interface ClientConnectionEventListener {

    void notifyClientConnected(String host, int port);

    void notifyClientDisconnected(String host, int port);
}
