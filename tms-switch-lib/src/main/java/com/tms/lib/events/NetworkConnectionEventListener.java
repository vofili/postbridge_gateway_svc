package com.tms.lib.events;

import com.tms.lib.network.NetworkConnection;

import java.io.IOException;
import java.util.List;

public interface NetworkConnectionEventListener {

    public void notifyConnect(NetworkConnection networkConnection);

    public void notifyDisconnect(NetworkConnection networkConnection);

    default void disconnectAll() throws IOException {
        return;
    }

    List<NetworkConnection> getNetworkConnections();

}
