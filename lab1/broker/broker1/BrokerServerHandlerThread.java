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
                /* create a packet to send reply back to client */
                BrokerPacket packetToClient = new BrokerPacket();
                packetToClient.type = BrokerPacket.BROKER_QUOTE;

                /* process message */
		// Using the symbol from the client, respond back the correcposing value from the hashtable
                if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
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

                if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
                    gotByePacket = true;
                    packetToClient = new BrokerPacket();
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
