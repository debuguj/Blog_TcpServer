package pl.debuguj.tcp.server;

import pl.debuguj.tcp.server.ServerTcp.ClientConnection;

public class ServerTcpAdapter implements ServerTcpListener {

    @Override
    public void onClientConnected(ServerTcp server, ClientConnection client) {

    }

    @Override
    public void onClientDisconnected(ServerTcp server, ClientConnection client) {

    }

    @Override
    public void onMessageReceived(String msg) {
    
    }

    @Override
    public void onMessageSent(ServerTcp server, ClientConnection toClient, String msg) {
    
    }


    @Override
    public void onInternalError(ServerTcp server, ClientConnection toClient, String error) {

    }

}
