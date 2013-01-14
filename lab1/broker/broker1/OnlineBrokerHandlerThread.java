import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class OnlineBrokerHandlerThread extends Thread {
    private Socket socket = null;
    private static HashMap<String, Long> quotes;

    public OnlineBrokerHandlerThread(Socket socket) {
        super("OnlineBrokerHandlerThread");
        assert(quotes != null);
        this.socket = socket;
        System.out.println("Created new Thread to handle client.");
    }

    public void run() {
        boolean gotByePacket = false;

        try {
            /* stream to read from client */
            ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
            BrokerPacket packetFromClient;

            /* stream to write back to client */
            ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

            while((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
                /* Create a packet to send reply back to the client */
                BrokerPacket packetToClient = new BrokerPacket();

                /* If Broker Request, then return Quote */
                if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
                    System.out.println("From Client: " + packetFromClient.symbol);

                    packetToClient.type = BrokerPacket.BROKER_QUOTE;
                    Long quote =  quotes.get(packetFromClient.symbol.toLowerCase());
                    packetToClient.quote = (quote != null) ? quote : 0;

                    /* Send reply back to client */
                    toClient.writeObject(packetToClient);

                    /* wait for next packet */
                    continue;
                }

                if(packetFromClient.type == BrokerPacket.BROKER_BYE) {
                    gotByePacket = true;
                    packetToClient.type = BrokerPacket.BROKER_BYE;
                    toClient.writeObject(packetToClient);

                    break;
                }

                /* if code comes here, there is an error in the packet */
                System.err.println("ERROR: Unknown BROKER_* packet!!");
                System.exit(-1);
            }

            fromClient.close();
            toClient.close();
            socket.close();
        } catch (Exception e) {
            if(!gotByePacket)
                e.printStackTrace();
        }
    }

    public static void setQuotes(HashMap<String, Long> quotes) {
        OnlineBrokerHandlerThread.quotes = quotes;
    }
}
