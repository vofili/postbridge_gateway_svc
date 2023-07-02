package com.tms.lib.network;


public interface ConnectionProvider {

    ClientSocketChannel getSocketConnection();
}
