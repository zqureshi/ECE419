import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineBrokerHandlerThread extends Thread {
    private Socket socket = null;
    private ObjectInputStream fromClient;
    private ObjectOutputStream toClient;
    private static final long RANGE_START = 1;
    private static final long RANGE_END = 300;
    private static ConcurrentHashMap<String, Long> quotes;
    private static LookupClient client;
    private static String exchange;

    public OnlineBrokerHandlerThread(Socket socket) {
        super("OnlineBrokerHandlerThread");
        assert(client != null);
        assert(quotes != null);
        this.socket = socket;
        System.out.println("Created new Thread to handle client.");
    }

    public void run() {
        boolean gotByePacket = false;

        try {
            /* stream to read from client */
            fromClient = new ObjectInputStream(socket.getInputStream());
            BrokerPacket packetFromClient;

            /* stream to write back to client */
            toClient = new ObjectOutputStream(socket.getOutputStream());

            outside_loop:
            while((packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
                /* Create a packet to send reply back to the client */
                BrokerPacket packetToClient = new BrokerPacket();

                /* If Broker Request, then return Quote */
                if(packetFromClient.type == BrokerPacket.BROKER_REQUEST
                        || packetFromClient.type == BrokerPacket.BROKER_FORWARD) {
                    System.out.println("From Client: " + packetFromClient.symbol);

                    packetToClient.type = BrokerPacket.BROKER_ERROR;
                    packetToClient.error_code = BrokerPacket.ERROR_INVALID_SYMBOL;
                    Long quote =  quotes.get(packetFromClient.symbol.toLowerCase());

                    if(quote != null) {
                        packetToClient.type = BrokerPacket.BROKER_QUOTE;
                        packetToClient.quote = quote;
                    } else if(packetFromClient.type != BrokerPacket.BROKER_FORWARD) {
                        /* If not a forwarded request, then query other exchanges */
                        for(BrokerConfig.Exchanges e : BrokerConfig.Exchanges.values()) {
                            if(!exchange.equalsIgnoreCase(e.toString())) {
                                BrokerLocation location;
                                if((location = client.lookup(e.toString())) != null) {
                                    /* Connect to the other exchange */
                                    Socket s = new Socket(location.broker_host, location.broker_port);
                                    ObjectOutputStream exOut = new ObjectOutputStream(s.getOutputStream());
                                    ObjectInputStream exIn = new ObjectInputStream(s.getInputStream());

                                    /* Forward lookup request */
                                    BrokerPacket packetToExchange = new BrokerPacket();
                                    packetToExchange.type = BrokerPacket.BROKER_FORWARD;
                                    packetToExchange.symbol = packetFromClient.symbol;
                                    exOut.writeObject(packetToExchange);

                                    /* Read reply, if received quote, then return to client */
                                    BrokerPacket packetFromExchange = (BrokerPacket) exIn.readObject();

                                    /* Send goodbye and Close socket */
                                    packetToExchange = new BrokerPacket();
                                    packetToExchange.type = BrokerPacket.BROKER_BYE;
                                    exOut.writeObject(packetToExchange);

                                    exIn.close();
                                    exOut.close();
                                    s.close();

                                    /* Check if we received a quote */
                                    if (packetFromExchange.type == BrokerPacket.BROKER_QUOTE) {
                                        packetToClient = packetFromExchange;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    /* Send reply back to client */
                    toClient.writeObject(packetToClient);

                    /* wait for next packet */
                    continue;
                }

                /* If Exchange request, then handle accordingly */
                switch(packetFromClient.type) {
                case BrokerPacket.EXCHANGE_ADD:
                    if(quotes.putIfAbsent(packetFromClient.symbol.toLowerCase(), 0L) == null) {
                        sendExchangeReply(packetToClient, packetFromClient.symbol, 0L);
                    } else {
                        sendExchangeError(packetToClient, packetFromClient.symbol, 0L, BrokerPacket.ERROR_SYMBOL_EXISTS);
                    }

                    continue;

                case BrokerPacket.EXCHANGE_REMOVE:
                    if(quotes.remove(packetFromClient.symbol) != null) {
                        sendExchangeReply(packetToClient, packetFromClient.symbol, 0L);
                    } else {
                        sendExchangeError(packetToClient, packetFromClient.symbol, 0L, BrokerPacket.ERROR_INVALID_SYMBOL);
                    }

                    continue;

                case BrokerPacket.EXCHANGE_UPDATE:
                    if(RANGE_START <= packetFromClient.quote && packetFromClient.quote <= RANGE_END) {
                        if(quotes.replace(packetFromClient.symbol, packetFromClient.quote) != null) {
                            sendExchangeReply(packetToClient, packetFromClient.symbol, 0L);
                        } else {
                            sendExchangeError(packetToClient, packetFromClient.symbol, 0L, BrokerPacket.ERROR_INVALID_SYMBOL);
                        }
                    } else {
                        sendExchangeError(packetToClient, packetFromClient.symbol, 0L, BrokerPacket.ERROR_OUT_OF_RANGE);
                    }

                    continue;
                }

                if(packetFromClient.type == BrokerPacket.BROKER_BYE || packetFromClient.type == BrokerPacket.BROKER_NULL) {
                    gotByePacket = true;
                    packetToClient.type = BrokerPacket.BROKER_BYE;
                    toClient.writeObject(packetToClient);

                    break outside_loop;
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

    public static void setQuotes(ConcurrentHashMap<String, Long> quotes) {
        OnlineBrokerHandlerThread.quotes = quotes;
    }

    public static void setLookupClient(LookupClient client) {
        OnlineBrokerHandlerThread.client = client;
    }

    public static void setExchange(String exchange) {
        OnlineBrokerHandlerThread.exchange = exchange;
    }

    private void sendExchangeReply(BrokerPacket packetToClient, String symbol, Long quote) throws IOException {
        sendExchangePacket(packetToClient, BrokerPacket.EXCHANGE_REPLY, symbol, quote, BrokerPacket.BROKER_NULL);
    }

    private void sendExchangeError(BrokerPacket packetToClient, String symbol, Long quote, int error_code) throws IOException {
        sendExchangePacket(packetToClient, BrokerPacket.BROKER_ERROR, symbol, quote, error_code);
    }

    private void sendExchangePacket(BrokerPacket packetToClient, int type, String symbol, Long quote, int error_code) throws IOException {
        packetToClient.type = type;
        packetToClient.symbol = symbol;
        packetToClient.quote = quote;
        packetToClient.error_code = error_code;
        toClient.writeObject(packetToClient);
    }
}
