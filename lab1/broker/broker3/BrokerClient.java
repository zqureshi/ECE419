import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class BrokerClient {
    private enum Commands {
        LOCAL,
        BYE,
        EXIT,
        QUIT;
    }

    public static void main(String args[]) throws IOException, ClassNotFoundException {
        LookupClient client = null;
        Socket brokerSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        if(args.length == 2 ) {
            client = new LookupClient(args[0], Integer.parseInt(args[1]));
        } else {
            System.err.println("ERROR: Invalid arguments!");
            System.exit(-1);
        }

        Scanner stdIn = new Scanner(System.in);
        String userInput;

        System.out.print("CONSOLE>");
        outside_loop:
        while (stdIn.hasNext()) {
            /* Read input from standard input */
            userInput = stdIn.next();

            try {
                switch(Commands.valueOf(userInput.toUpperCase())) {
                case LOCAL:
                    String exchange = stdIn.next();
                    BrokerLocation brokerLocation = null;

                    /* Look up brokers for exchange */
                    System.out.println("Looking up broker for exchange " + exchange);
                    try {
                        if((brokerLocation = client.lookup(exchange)) == null) {
                           System.out.println("No broker found!");
                           break;
                        }
                    } catch (IOException e) {
                        System.out.println("Could not connect to lookup server!");
                        System.exit(-1);
                    }

                    if(brokerLocation == null) {
                        System.err.println("No broker found!");
                    }

                    /* Connect to broker */
                    System.out.println("Connecting to broker..." + brokerLocation);
                    brokerSocket = new Socket(brokerLocation.broker_host, brokerLocation.broker_port);
                    out = new ObjectOutputStream(brokerSocket.getOutputStream());
                    in = new ObjectInputStream(brokerSocket.getInputStream());
                    break;

                case BYE:
                case EXIT:
                case QUIT:
                    break outside_loop;
                }
            } catch (IllegalArgumentException e) {
                /* If no command matched, then must be a Symbol, request quote for it */
                if(brokerSocket != null) {
                    /* make a new request packet */
                    BrokerPacket packetToServer = new BrokerPacket();
                    packetToServer.type = BrokerPacket.BROKER_REQUEST;
                    packetToServer.symbol = userInput;
                    out.writeObject(packetToServer);

                    /* print server reply */
                    BrokerPacket packetFromServer;
                    packetFromServer = (BrokerPacket) in.readObject();

                    if (packetFromServer.type == BrokerPacket.BROKER_QUOTE) {
                        if(packetFromServer.error_code == BrokerPacket.ERROR_INVALID_SYMBOL) {
                            System.out.println("Invalid Symbol.");
                        } else {
                            System.out.println("Quote: " + packetFromServer.quote);
                        }
                    }
                } else {
                    System.out.println("Must be connected to a broker first! Use 'local' command to connect.");
                }
            }

            /* re-print console prompt */
            System.out.print("CONSOLE>");
        }

        if(brokerSocket != null) {
            /* tell server that i'm quitting */
            BrokerPacket packetToServer = new BrokerPacket();
            packetToServer.type = BrokerPacket.BROKER_BYE;
            out.writeObject(packetToServer);

            out.close();
            in.close();
            stdIn.close();
            brokerSocket.close();
        }
    }
}
