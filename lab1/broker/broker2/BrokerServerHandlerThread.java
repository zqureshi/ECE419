import java.net.*;
import java.io.*;
import java.util.*;

public class BrokerServerHandlerThread extends Thread {
    private Socket socket = null;
    Hashtable<String, Long> hash;

    public BrokerServerHandlerThread(Socket socket, Hashtable<String, Long> hash) {
        super("BrokerServerHandlerThread");
        this.socket = socket;
	this.hash = hash;
        System.out.println("Created new Thread to handle client");
    }

    public void run() {

        boolean gotByePacket = false;

        try {
            /* stream to read from client */
            ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
            BrokerPacket packetFromClient;

            /* stream to write back to client */
            ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());


            while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {

                /* process message */
		// Using the symbol from the client, respond back the correcposing value from the hashtable
		// handles clients requests.
                if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
                    /* create a packet to send reply back to client */
                    BrokerPacket packetToClient = new BrokerPacket();
                    packetToClient.type = BrokerPacket.BROKER_QUOTE;
		    Long response = hash.get(packetFromClient.symbol);
		    if (response != null){
			    packetToClient.quote = hash.get(packetFromClient.symbol);
			}
		    else {
			// return zero if the symbol requested from the client does not exsist in the nasdaq file.
			packetToClient.quote = (long) 0;
			}
                    System.out.println("From Client: " + packetFromClient.symbol);

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
                    BrokerPacket packetToClient = new BrokerPacket();
                    packetToClient.type = BrokerPacket.BROKER_NULL;
                    packetToClient.quote = ( long ) 0;
                    toClient.writeObject(packetToClient);
                    break;
                }

                /* if code comes here, there is an error in the packet */
                System.err.println("ERROR: !!");
                System.exit(-1);
            }

            /* cleanup when client exits */
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
