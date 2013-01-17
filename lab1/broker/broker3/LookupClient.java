import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class LookupClient {
    private String lookupHost;
    private int lookupPort;

    public LookupClient(String host, int port) {
        this.lookupHost = host;
        this.lookupPort = port;
    }

    public boolean register(String exchange, String host, int port) throws IOException, ClassNotFoundException {
        Socket socket = new Socket(lookupHost, lookupPort);
        ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());

        /* Send Register request to Lookup server */
        BrokerPacket packetToClient = new BrokerPacket();
        packetToClient.type = BrokerPacket.LOOKUP_REGISTER;
        packetToClient.exchange = exchange;
        packetToClient.num_locations = 1;
        packetToClient.locations = new BrokerLocation[] { new BrokerLocation(host, port) };
        toClient.writeObject(packetToClient);

        /* Read ack from lookup server */
        BrokerPacket packetFromClient = (BrokerPacket) fromClient.readObject();
        boolean ackReceived = false;
        if(packetFromClient != null
                && packetFromClient.type == BrokerPacket.LOOKUP_REPLY
                && exchange.equalsIgnoreCase(packetFromClient.exchange)) {
            for(BrokerLocation location : packetFromClient.locations) {
                if(location.broker_host.equals(host) && location.broker_port == port) {
                    ackReceived = true;
                    break;
                }
            }
        }

        toClient.close();
        fromClient.close();
        socket.close();

        return ackReceived ? true : false;
    }

    /**
     * Lookup brokers for an exchange.
     * @param exchange Name of exchange to lookup broker for.
     * @return BrokerLocation or null if none found
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public BrokerLocation lookup(String exchange) throws IOException, ClassNotFoundException {
        Socket socket = new Socket(lookupHost, lookupPort);
        ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());

        /* Send Register request to Lookup server */
        BrokerPacket packetToClient = new BrokerPacket();
        packetToClient.type = BrokerPacket.LOOKUP_REQUEST;
        packetToClient.exchange = exchange;
        toClient.writeObject(packetToClient);

        /* Read reply from server */
        BrokerPacket packetFromClient = (BrokerPacket) fromClient.readObject();
        BrokerLocation location = null;
        if(packetFromClient != null
                && packetFromClient.type == BrokerPacket.LOOKUP_REPLY
                && exchange.equals(packetFromClient.exchange)
                && packetFromClient.num_locations > 0
                && packetFromClient.locations.length > 0) {
            location = packetFromClient.locations[0];
        }

        toClient.close();
        fromClient.close();
        socket.close();

        return location;
    }
}
