package pl.debuguj.tcp.server;


import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public final class ServerTcp {
    

    private ServerSocket serverSocket;

    private Map<Integer, ClientConnection> clients;

    private LinkedBlockingQueue<String> messages;

    private int port;

    private int messageHandlingThreadCount;

    private volatile boolean running;

    private volatile boolean alive;

    private int clientLimit;

    private List<ServerTcpListener> listeners;

    public static final int TIMEOUT = 10000;

    public ServerTcp(int port, int messageHandlingThreadCount) {
        
        this.port = port;
        clients = Collections.synchronizedMap(new HashMap<>());
        messages = new LinkedBlockingQueue<>();
        listeners = Collections.synchronizedList(new ArrayList<>());
        this.messageHandlingThreadCount = Math.max(1, messageHandlingThreadCount);
        clientLimit = -1;
        alive = true;

        addServerListener(new ServerTcpAdapter() {
            @Override
            public void onMessageReceived(String msg){

            }
        });
    }

    public ServerTcp(int port) {
        this(port, 1);
    }

    protected Object messageReceivedInit(int id, Object msg) {
        return msg;
    }
   
    protected ClientConnection connectionInit(int id, Socket socket, ObjectInputStream in, ObjectOutputStream out)
            throws IOException, ClassNotFoundException {
        return new ClientConnection(id, socket, in, out);
    }

    public ClientConnection getClient(int id) {
        return clients.get(id);
    }

    public Collection<ClientConnection> getClients() {
        return clients.values();
    }

    public boolean containsId(int id) {
        return clients.containsKey(id);
    }

    public int getMessageHandlingThreadCount() {
        return messageHandlingThreadCount;
    }

    public boolean send(String msg, int id) {
        if (!running() || msg == null) {
            return false;
        }
        ClientConnection con = getClient(id);
        if (con == null) {
            return false;
        }
        return con.send(msg);
    }

    public boolean sendStringToAll(String msg) {
        if (!running()) {
            return false;
        }
        boolean passed = true;
        for (ClientConnection c : getClients()) {
            passed &= c.send(msg);
        }
        return passed; // all went through
    }

    public int getPort() {
        return port;
    }

    private volatile boolean started = false;

    public boolean start() {
        if (!isAlive() || running()) {
            return false;
        }

        if (started) {
            return false;
        }
        synchronized (this) {
            if (started) {
                return false;
            }
            started = true;
        }

        try {
            serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        running = true;

        Thread acception = new Thread(new Acception(), "Client acception thread");

        for (int i = 0; i < messageHandlingThreadCount; i++) {
            Thread t = new Thread(new MessageHandler(), "Received messages handler thread #" + i);
            t.setDaemon(true);
            t.start();
        }

        acception.setDaemon(true);
        acception.start();

        return true;
    }

    public void sync() throws InterruptedException {
        if (!running) {
            return;
        }
        synchronized (this) {
            if (!running) {
                return;
            }
            wait();
        }
    }

    public void stopAccepting() {
        try {
            serverSocket.close();
        } catch (IOException e) {
        }
        serverSocket = null;
    }

    public void setClientLimit(int limit) {
        clientLimit = limit;
    }

    public int getClientLimit() {
        return clientLimit;
    }

    public void shutDown() {
        if (!isAlive()) {
            return;
        }

        synchronized (this) {
            if (!isAlive()) {
                return;
            }
            alive = false;
            running = false;
        }
        for (ClientConnection c : getClients()) {
            c.localShutDown();
        }
    }

    public boolean running() {
        return running;
    }

    public boolean isAlive() {
        return alive;
    }

    public void addServerListener(ServerTcpListener sl) {
        listeners.add(sl);
    }

    public void removeServerListener(ServerTcpListener sl) {
        listeners.remove(sl);
    }

    private class Acception implements Runnable {

        public void run() {
            while (running() && !serverSocket.isClosed()) {
                Socket socket = null;
                ObjectInputStream in = null;
                ObjectOutputStream out = null;
                try {
                    socket = serverSocket.accept();
                    out = new ObjectOutputStream(socket.getOutputStream());
                    in = new ObjectInputStream(socket.getInputStream());
                    
                    int id = 1;
                    ClientConnection con = connectionInit(id, socket, in, out);
                    clients.put(id, con);
                    
                    con.localStart();
                    for (ServerTcpListener sl : listeners) {
                        sl.onClientConnected(ServerTcp.this, con);
                    }
                    
                } catch (IOException e) {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e1) {
                    }
                } catch (RuntimeException rte) {
                    rte.printStackTrace();
                    shutDown(); // Programmers error, why continue ??
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
    private class MessageHandler implements Runnable {

        public void run() {
            while (running()) {
                try {
                    String msg =  messages.take();
                   
                    if (!msg.isEmpty()) {
                        listeners.forEach((cl) -> {
                            cl.onMessageReceived(msg);
                        });
                    }
                    //if(dm == null){
                    //    continue;
                    //}
 
                } catch (InterruptedException e) {
                } catch (RuntimeException rte) {
                    rte.printStackTrace();
                    shutDown();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
}

    public class ClientConnection {

        private ObjectInputStream in;
        private ObjectOutputStream out;
        private Socket socket;
        private int clientId;
        private volatile boolean localRunning;
        private volatile boolean localAlive;

        public ClientConnection(int id, Socket socket, ObjectInputStream in, ObjectOutputStream out) {
            if (socket.isClosed()) {
                throw new IllegalStateException("Socket for client " + id + " is closed.");
            }
            clientId = id;
            this.socket = socket;
            this.in = in;
            this.out = out;
            localAlive = true;
        }

        private volatile boolean localStarted = false;

        private void localStart() {
            if (!localAlive || localRunning) {
                return;
            }

            if (localStarted) {
                return;
            }
            synchronized (this) {
                if (localStarted) {
                    return;
                }
                started = true;
                localRunning = true;
            }

            Thread read = new Thread(new Reading(), "Client: " + clientId + " reading thread");
            read.setDaemon(true);
            read.start();

        }

        public int getClientId() {
            return clientId;
        }

        protected ObjectInputStream getInputStream() {
            return in;
        }

        protected ObjectOutputStream getOutputStream() {
            return out;
        }

        protected Socket getSocket() {
            return socket;
        }

        public boolean localRunning() {
            return localRunning;
        }

        protected boolean send(String msg) {
            if (!localRunning || socket.isClosed()) {
                return false;
            }

            msg = sendInit(msg);
            if (msg == null) {
                return false;
            }

            try {
                out.writeObject(msg);
                out.flush();
                out.reset();

                if (msg instanceof String) {
                    for (ServerTcpListener sl : listeners) {
                        sl.onMessageSent(ServerTcp.this, this, msg);
                    }
                } else {
                    for (ServerTcpListener sl : listeners) {
                        sl.onInternalError(ServerTcp.this, this, "SERVER INTERNAL ERROR");
                    }
                    return false;
                }

                return true;
            } catch (IOException e) {
                localShutDown();
                return false;
            }
        }

        protected String sendInit(String msg) {
            return msg;
        }

        protected void disconnectionInit() {
        }

        public void localShutDown() {
            if (!localAlive) {
                return;
            }
            synchronized (this) {
                if (!localAlive) {
                    return;
                }
                localAlive = false;
            }

            try {
                if (!socket.isClosed()) {
                    out.writeObject("DISCONNECTED");
                }
            } catch (IOException e) {
            }
            try {
                in.close();
                out.close();
            } catch (IOException e) {
            }

            boolean init = localRunning;

            localRunning = false;
            if (init) {
                disconnectionInit();
                for (ServerTcpListener sl : listeners) {
                    sl.onClientDisconnected(ServerTcp.this, this);
                }
            }

            clients.remove(clientId);

        }

        public int hashCode() {
            return clientId * 31;
        }

        private class Reading implements Runnable {

            public void run() {
                try {
                    while (localRunning()) {
                        String obj = null;

                        try {

                            obj = (String) in.readObject();

                        } catch (IOException | ClassNotFoundException e) {
                            try {
                                if (!socket.isClosed()) {
                                    out.writeObject("CONNECTION_REJECTED");
                                    out.flush();
                                    out.reset();
                                }
                            } catch (IOException e1) {
                            }
                            localShutDown();
                            break;
                        }

                        try {
                            messages.put(obj);                            
                            
                        } catch (InterruptedException e) {
                        }
                    }
                } catch (RuntimeException rte) {
                    rte.printStackTrace();
                    localShutDown();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        @Override
        protected void finalize() {
            localShutDown();
        }
    }

    private static class Message {

        Object msg;
        int id;

        Message(Object msg, int id) {
            this.msg = msg;
            this.id = id;
        }
    }

    @Override
    protected void finalize() {
        shutDown();
    }
    
    public class DataCreator{
        
    }
}
