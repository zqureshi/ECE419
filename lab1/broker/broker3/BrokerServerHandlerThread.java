import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BrokerServerHandlerThread  extends Thread  {
    private Socket socket = null;
    static ConcurrentHashMap<String, Long> hash;
    static String filename;
    static int lookport;
    static String lookhost;

    public BrokerServerHandlerThread(Socket socket) {
        super("BrokerServerHandlerThread");
        this.socket = socket;
        System.out.println("Created new Thread to handle client");
    }
    public static void setFilename(String filename) {
        BrokerServerHandlerThread.filename = filename;
    }

    public static void setHash(ConcurrentHashMap<String, Long> hash){
        BrokerServerHandlerThread.hash = hash;
    }
    public static void setLookport(int lookport){
        BrokerServerHandlerThread.lookport = lookport;
    }
    public static void setLookhost(String lookhost){
        BrokerServerHandlerThread.lookhost = lookhost;
    }


    public void run() {

        boolean gotByePacket = false;

        try {
            /* stream to read from client */
            ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
            BrokerPacket packetFromClient;

            /* stream to write back to client */
            ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

            // open lookup socket
            Socket lookupSocket = null;
            ObjectOutputStream lookout = null;
            ObjectInputStream lookin = null;

            /* variables for hostname/port */
            try{
                lookupSocket = new Socket(lookhost, lookport);
                lookout = new ObjectOutputStream(lookupSocket.getOutputStream());
                lookin = new ObjectInputStream(lookupSocket.getInputStream());

            } catch (UnknownHostException e) {
                System.err.println("ERROR: Don't know where to connect!!");
                System.exit(1);
            } catch (IOException e) {
                System.err.println("ERROR: Couldn't get I/O for the connection.");
                System.exit(1);
            }


            while ((gotByePacket == false) && (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null)){

                /* process message */
                // Using the symbol from the client, respond back the corresponding value from the hashtable
                // handles clients requests.
                if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
                    /* create a packet to send reply back to client */
                    BrokerPacket packetToClient = new BrokerPacket();
                    packetToClient.symbol = packetFromClient.symbol;
                    Long response = hash.get(packetFromClient.symbol);
                    if (response != null){
                        packetToClient.type = BrokerPacket.BROKER_QUOTE;
                        packetToClient.quote = hash.get(packetFromClient.symbol);
                        System.out.println("From Client: " + packetFromClient.symbol);
                    }
                    else {
                        // return ERROR if the symbol requested from the client does not exists in the nasdaq file or tse file.
                        if (filename.equals("tse")){
                            Socket brokerSocket = null;
                            ObjectOutputStream outb = null;
                            ObjectInputStream inb = null;

                            BrokerPacket packetToServer = new BrokerPacket();
                            packetToServer.type = BrokerPacket.LOOKUP_REQUEST;
                            packetToServer.exchange = "nasdaq";
                            //System.out.println("TO server : " + packetToServer.exchange);
                            lookout.writeObject(packetToServer);

                            /* server reply */
                            String hostname = "localhost";
                            int port = 4444;
                            BrokerPacket packetFromServer;
                            packetFromServer = (BrokerPacket) lookin.readObject();
                            if (packetFromServer.type == BrokerPacket.LOOKUP_REPLY){
                                port = packetFromServer.locations[0].broker_port;
                                hostname = packetFromServer.locations[0].broker_host;
                                System.out.println(" From Lookup server" + packetFromServer.locations[0].toString());
                                //System.out.println(packetFromServer.exchange +" as local.");
                            }
                            if (packetFromServer.type == BrokerPacket.ERROR_INVALID_EXCHANGE){
                                System.out.println(packetFromServer.symbol + " invalid.");
                                continue;
                            }

                            brokerSocket = new Socket(hostname, port);
                            outb = new ObjectOutputStream(brokerSocket.getOutputStream());
                            inb = new ObjectInputStream(brokerSocket.getInputStream());
                            // write to server
                            BrokerPacket packetToServer1 = new BrokerPacket();
                            packetToServer1.type = BrokerPacket.BROKER_FORWARD;
                            packetToServer1.symbol = packetFromClient.symbol;
                            outb.writeObject(packetToServer1);

                              /* print server reply     */
                            BrokerPacket packetFromServer1;
                            packetFromServer1 = (BrokerPacket) inb.readObject();
                            toClient.writeObject(packetFromServer1);


                            outb.close();
                            inb.close();
                            brokerSocket.close();
                        }
                        else if (filename.equals("nasdaq")){
                            Socket brokerSocket = null;
                            ObjectOutputStream outb = null;
                            ObjectInputStream inb = null;

                            BrokerPacket packetToServer = new BrokerPacket();
                            packetToServer.type = BrokerPacket.LOOKUP_REQUEST;
                            packetToServer.exchange = "tse";
                            //System.out.println("TO server : " + packetToServer.exchange);
                            lookout.writeObject(packetToServer);

                            /* server reply */
                            String hostname = "localhost";
                            int port = 4444;
                            BrokerPacket packetFromServer;
                            packetFromServer = (BrokerPacket) lookin.readObject();
                            if (packetFromServer.type == BrokerPacket.LOOKUP_REPLY){
                                port = packetFromServer.locations[0].broker_port;
                                hostname = packetFromServer.locations[0].broker_host;
                                System.out.println(" From Lookup server" + packetFromServer.locations[0].toString());
                                //System.out.println(packetFromServer.exchange +" as local.");
                            }
                            if (packetFromServer.type == BrokerPacket.ERROR_INVALID_EXCHANGE){
                                System.out.println(packetFromServer.symbol + " invalid.");
                                continue;
                            }

                            brokerSocket = new Socket(hostname, port);
                            outb = new ObjectOutputStream(brokerSocket.getOutputStream());
                            inb = new ObjectInputStream(brokerSocket.getInputStream());
                            // write to server
                            BrokerPacket packetToServer1 = new BrokerPacket();
                            packetToServer1.type = BrokerPacket.BROKER_FORWARD;
                            packetToServer1.symbol = packetFromClient.symbol;
                            outb.writeObject(packetToServer1);

                              /* print server reply     */
                            BrokerPacket packetFromServer1;
                            packetFromServer1 = (BrokerPacket) inb.readObject();
                            toClient.writeObject(packetFromServer1);

                            outb.close();
                            inb.close();
                            brokerSocket.close();
                        }
                    }
                     /* send reply back to client */
                    toClient.writeObject(packetToClient);
                    /* wait for next packet */
                    continue;
                }
                // handles forwarded request
                else if(packetFromClient.type == BrokerPacket.BROKER_FORWARD){
                    /* create a packet to send reply back to client */
                    BrokerPacket packetToClient = new BrokerPacket();
                    packetToClient.symbol = packetFromClient.symbol;
                    Long response = hash.get(packetFromClient.symbol);
                    if (response != null){
                        packetToClient.type = BrokerPacket.BROKER_QUOTE;
                        packetToClient.quote = hash.get(packetFromClient.symbol);
                        System.out.println("From Client: " + packetFromClient.symbol);
                    }
                    else{
                        packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
                        System.out.println(packetFromClient.symbol + " invalid.");
                    }
                    /* send reply back to client */
                    toClient.writeObject(packetToClient);

                    /* wait for next packet */
                    continue;

                }
                // handles EXCHANGE add query
                else if(packetFromClient.type == BrokerPacket.EXCHANGE_ADD) {
                    /* create a packet to send reply back to client */
                    BrokerPacket packetToClient = new BrokerPacket();
                    packetToClient.symbol = packetFromClient.symbol;
                    // symbol already exists
                    if (hash.get(packetFromClient.symbol) != null){
                        packetToClient.type = BrokerPacket.ERROR_SYMBOL_EXISTS;
                        System.out.println("From Client: Error " + packetFromClient.symbol + " already exists!");
                    }
                    else {
                        packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
                        hash.put(packetFromClient.symbol,(long) 0);
                        System.out.println("From Client: " + packetFromClient.symbol);
                    }

                    /* send reply back to client */
                    toClient.writeObject(packetToClient);

                    /* wait for next packet */
                    continue;
                }
                // handles EXCHANGE update query
                else if(packetFromClient.type == BrokerPacket.EXCHANGE_UPDATE) {
                    /* create a packet to send reply back to client */
                    BrokerPacket packetToClient = new BrokerPacket();
                    packetToClient.symbol = packetFromClient.symbol;
                    // symbol does not exist
                    if (hash.get(packetFromClient.symbol) == null){
                        packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
                        System.out.println("From Client: Error " + packetFromClient.symbol + " does not exists, hence connot be modified!");
                    }
                    else if (packetFromClient.quote >300 || packetFromClient.quote <1){
                        packetToClient.type = BrokerPacket.ERROR_OUT_OF_RANGE;
                        System.out.println("From Client: Error " + packetFromClient.symbol + " out of range [1,300].");
                    }
                    else {
                        packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
                        packetToClient.quote = packetFromClient.quote;
                        hash.put(packetFromClient.symbol, packetFromClient.quote);
                        System.out.println("From Client: " + packetFromClient.symbol + ", " + packetFromClient.quote);
                    }

                    /* send reply back to client */
                    toClient.writeObject(packetToClient);

                    /* wait for next packet */
                    continue;
                }
                // handles EXCHANGE remove query
                else if(packetFromClient.type == BrokerPacket.EXCHANGE_REMOVE) {
                    /* create a packet to send reply back to client */
                    BrokerPacket packetToClient = new BrokerPacket();
                    packetToClient.symbol = packetFromClient.symbol;
                    // symbol does not exist
                    if (hash.get(packetFromClient.symbol) == null){
                        packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
                        System.out.println("From Client: Error " + packetFromClient.symbol + " does not exists, hence connot be modified!");
                    }
                    else {
                        packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
                        hash.remove(packetFromClient.symbol);
                        System.out.println("From Client: " + packetFromClient.symbol);
                    }

                    /* send reply back to client */
                    toClient.writeObject(packetToClient);

                    /* wait for next packet */
                    continue;
                }

                if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
                    gotByePacket = true;
                    System.out.println("From Client: " + packetFromClient.symbol);
                    continue;
                }

                /* if code comes here, there is an error in the packet */
                System.err.println("ERROR: !!");
                System.exit(-1);
            }
            lookout.close();
            lookin.close();
            /* cleanup when client exits */
            //flush hasttable onto the file once server closes
            FileWriter file1 = new FileWriter(filename);
            BufferedWriter outfile = new BufferedWriter(file1);
            Enumeration keys = hash.keys();
            while(keys.hasMoreElements()){
                try {
                    Object key = keys.nextElement();
                    Object value = hash.get(key);
                    outfile.write(key + " " + value + "\n");
                } catch (IOException e) {
                    System.err.println("ERROR: Could not open file!");
                    System.exit(-1);
                }
            }

            outfile.close();

            fromClient.close();
            toClient.close();
            socket.close();

	    
        } catch (IOException e) {
            if(!gotByePacket)
                e.printStackTrace();
        } catch (ClassNotFoundException e) {
            if(!gotByePacket)
                e.printStackTrace();
        }
    }
}
