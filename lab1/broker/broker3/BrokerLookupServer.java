import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;


public class BrokerLookupServer {
    private static final ConcurrentHashMap<String, ArrayList<BrokerLocation>> brokers = new ConcurrentHashMap<String, ArrayList<BrokerLocation>>(5);

    /* Actual class to handle lookup requests */
    private static class LookupHandler implements Runnable {
        private Socket socket;

        public LookupHandler(Socket socket) {
            this.socket = socket;
            System.out.println("Created new Thread to handle client");
        }

        @Override
        public void run() {
            try {
                /* stream to read from client */
                ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
                BrokerPacket packetFromClient;

                /* stream to write back to client */
                ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
                BrokerPacket packetToClient;

                if((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
                    packetToClient = new BrokerPacket();

                    switch(packetFromClient.type) {
                    case BrokerPacket.LOOKUP_REGISTER:
                        System.out.println("Adding exchange " + packetFromClient.exchange);
                        /* Normalize exchange name to lowercase */
                        packetFromClient.exchange = packetFromClient.exchange.toLowerCase();
                        ArrayList<BrokerLocation> exchangeBrokers = brokers.get(packetFromClient.exchange);
                        if(exchangeBrokers != null) {
                            exchangeBrokers.addAll(Arrays.asList(packetFromClient.locations));
                        } else {
                            brokers.put(packetFromClient.exchange, new ArrayList<BrokerLocation>(Arrays.asList(packetFromClient.locations)));
                        }

                    case BrokerPacket.LOOKUP_REQUEST:
                        System.out.print("Looking up exchange " + packetFromClient.exchange + "... ");
                        /* Normalize exchange name to lowercase */
                        packetFromClient.exchange = packetFromClient.exchange.toLowerCase();
                        packetToClient.type = BrokerPacket.LOOKUP_REPLY;
                        packetToClient.exchange = packetFromClient.exchange;
                        if((exchangeBrokers = brokers.get(packetFromClient.exchange)) != null) {
                            System.out.println("Found");
                            packetToClient.error_code = BrokerPacket.BROKER_NULL;
                            packetToClient.num_locations = exchangeBrokers.size();
                            packetToClient.locations = exchangeBrokers.toArray(new BrokerLocation[0]);
                        } else {
                            System.out.println("Not found!");
                            packetToClient.error_code = BrokerPacket.ERROR_INVALID_EXCHANGE;
                            packetToClient.num_locations = 0;
                        }

                        toClient.writeObject(packetToClient);
                        break;

                    default:
                        /* if code comes here, there is an error in the packet */
                        System.err.println("ERROR: Unknown LOOKUP_* packet!!");
                        System.exit(-1);
                    }
                }

                fromClient.close();
                toClient.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        /* Parse command line arguments and start server*/
        try {
            if(args.length == 1) {
                serverSocket = new ServerSocket(Integer.parseInt(args[0]));
                System.out.println("Lookup server started...");
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        while(listening) {
            new Thread(new LookupHandler(serverSocket.accept())).start();
        }

        serverSocket.close();
    }

}
