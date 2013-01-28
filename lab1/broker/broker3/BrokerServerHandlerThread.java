import java.net.*;
import java.io.*;
import java.util.*;

public class BrokerServerHandlerThread extends Thread {
    private Socket socket = null;
    static Hashtable<String, Long> hash;
    static String filename;

    public BrokerServerHandlerThread(Socket socket) {
        super("BrokerServerHandlerThread");
        this.socket = socket;
        System.out.println("Created new Thread to handle client");
    }
    public static void setFilename(String filename) {
        BrokerServerHandlerThread.filename = filename;
    }

    public static void setHash(Hashtable<String, Long> hash){
        BrokerServerHandlerThread.hash = hash;
    }


    public void run() {

        boolean gotByePacket = false;

        try {
            /* stream to read from client */
            ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
            BrokerPacket packetFromClient;

            /* stream to write back to client */
            ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());


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
                        // return ERROR if the symbol requested from the client does not exsist in the nasdaq file.
                        packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
                        System.out.println("From Client: Error " + packetFromClient.symbol + " does not exists!");
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
