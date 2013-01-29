import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BrokerLookupServer{
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            if(args.length == 1) {
                serverSocket = new ServerSocket(Integer.parseInt(args[0]));
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        // Initialize hash map "lookup" to store the location of clients
        ConcurrentHashMap<String, BrokerLocation[]> lookup = new ConcurrentHashMap<String, BrokerLocation[]>();

        while (listening) {
            new BrokerLookupServerHandlerThread(serverSocket.accept(), lookup).start();
        }
        serverSocket.close();
    }
}

class BrokerLookupServerHandlerThread extends Thread {
    private Socket socket = null;
    ConcurrentHashMap<String, BrokerLocation[]> lookup;

    public BrokerLookupServerHandlerThread(Socket socket, ConcurrentHashMap<String, BrokerLocation[]> lookup) {
        super("BrokerServerHandlerThread");
        this.socket = socket;
        this.lookup = lookup;
        System.out.println("Created new Thread to handle client");
    }

    public void run() {

        try {
            /* stream to read from client */
            ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
            BrokerPacket packetFromClient;

            /* stream to write back to client */
            ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
            packetFromClient = (BrokerPacket) fromClient.readObject();
                /* process message */
            // Using the symbol from the client, respond back the correcposing value from the hashtable
            // handles clients requests.
            if(packetFromClient.type == BrokerPacket.LOOKUP_REGISTER) {
                    /* create a packet to send reply back to client */
                BrokerPacket packetToClient = new BrokerPacket();
                packetToClient.exchange = packetFromClient.exchange;
                packetToClient.type = BrokerPacket.LOOKUP_REPLY;
                lookup.put(packetFromClient.exchange, packetFromClient.locations);
                System.out.println("From Client: " + packetFromClient.exchange + ", " + packetFromClient.locations[0].toString());
                    /* send reply back to client */
                toClient.writeObject(packetToClient);

            }
            if(packetFromClient.type == BrokerPacket.LOOKUP_REQUEST) {
                    /* create a packet to send reply back to client */
                BrokerPacket packetToClient = new BrokerPacket();
                packetToClient.exchange = packetFromClient.exchange;

                packetToClient.type = BrokerPacket.LOOKUP_REPLY;
                packetToClient.locations =  lookup.get(packetFromClient.exchange);
                if (packetToClient.locations == null)
                    packetToClient.type = BrokerPacket.ERROR_INVALID_EXCHANGE;
                System.out.println("From Client: " + packetFromClient.exchange);
                    /* send reply back to client */
                toClient.writeObject(packetToClient);

            }

            /* cleanup when client exits */
            fromClient.close();
            toClient.close();

            socket.close();


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}