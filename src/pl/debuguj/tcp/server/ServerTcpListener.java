package pl.debuguj.tcp.server;


import pl.debuguj.tcp.server.ServerTcp.ClientConnection;

public interface ServerTcpListener {
	//status
	public void onClientConnected(ServerTcp server, ClientConnection client);
	
        public void onClientDisconnected(ServerTcp server, ClientConnection client);
        
        //receiving message
	public void onMessageReceived(String msg);
        
	
        //sending message 
        public void onMessageSent(ServerTcp server, ClientConnection toClient, String msg); 
        
       
        
        public void onInternalError(ServerTcp server, ClientConnection toClient, String error);
}
