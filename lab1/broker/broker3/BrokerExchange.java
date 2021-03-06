import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class BrokerExchange {
    private enum Commands {
        ADD,
        UPDATE,
        REMOVE,
        BYE,
        EXIT,
        QUIT;
    }

    public static void main(String args[]) throws IOException, ClassNotFoundException {
        Socket brokerSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            BrokerLocation brokerLocation = null;

            if(args.length == 3 ) {
                String lookup_host = args[0];
                int lookup_port = Integer.parseInt(args[1]);
                String lookup_exchange = args[2];

                System.out.println("Looking up broker for exchange " + lookup_exchange);
                brokerLocation = new LookupClient(lookup_host, lookup_port).lookup(lookup_exchange);
                if(brokerLocation == null) {
                    System.err.println("No broker found!");
                    System.exit(-1);
                }
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
            System.out.println("Connecting to broker..." + brokerLocation);
            brokerSocket = new Socket(brokerLocation.broker_host, brokerLocation.broker_port);

            out = new ObjectOutputStream(brokerSocket.getOutputStream());
            in = new ObjectInputStream(brokerSocket.getInputStream());

        } catch (UnknownHostException e) {
            System.err.println("ERROR: Don't know where to connect!!");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ERROR: Couldn't get I/O for the connection.");
            System.exit(1);
        }

        Scanner stdIn = new Scanner(System.in);
        Commands command = null;

        System.out.print("EXCHANGE>");
        outside_loop:
        while (stdIn.hasNext()) {
            /* make a new request packet */
            BrokerPacket packetToServer = new BrokerPacket();

            try {
                command = Commands.valueOf(stdIn.next().toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Unrecognized Command, exiting.");
                break outside_loop;
            }

            /* Get additional parameters based on command */
            switch(command) {
            case ADD:
                packetToServer.type = BrokerPacket.EXCHANGE_ADD;
                packetToServer.symbol = stdIn.next();
                break;

            case UPDATE:
                packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
                packetToServer.symbol = stdIn.next();
                packetToServer.quote = stdIn.nextLong();
                break;

            case REMOVE:
                packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
                packetToServer.symbol = stdIn.next();
                break;

            case BYE:
            case QUIT:
            case EXIT:
                break outside_loop;
            }

            out.writeObject(packetToServer);

            /* print server reply */
            BrokerPacket packetFromServer;
            packetFromServer = (BrokerPacket) in.readObject();

            if (packetFromServer.type == BrokerPacket.EXCHANGE_REPLY) {
                System.out.println(BrokerConfig.messages.get(packetToServer.type));
            } else if(packetFromServer.type == BrokerPacket.BROKER_ERROR) {
                System.out.println(BrokerConfig.errorMessages.get(packetFromServer.error_code));
            }

            /* re-print console prompt */
            System.out.print("EXCHANGE>");
        }

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
