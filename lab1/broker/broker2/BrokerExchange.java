import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;

public class BrokerExchange {
    private static final HashMap<Integer, String> errorMessages = new HashMap<Integer, String>();
    private static final HashMap<Integer, String> messages = new HashMap<Integer, String>();

    static {
        errorMessages.put(BrokerPacket.ERROR_INVALID_SYMBOL, "Invalid symbol");
        errorMessages.put(BrokerPacket.ERROR_OUT_OF_RANGE, "Quote out of range");
        errorMessages.put(BrokerPacket.ERROR_SYMBOL_EXISTS, "Symbol exists");

        messages.put(BrokerPacket.EXCHANGE_ADD, "Added");
        messages.put(BrokerPacket.EXCHANGE_UPDATE, "Updated");
        messages.put(BrokerPacket.EXCHANGE_REMOVE, "Removed");
    }

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
            /* variables for hostname/port */
            String hostname = "localhost";
            int port = 4444;

            if(args.length == 2 ) {
                hostname = args[0];
                port = Integer.parseInt(args[1]);
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
            brokerSocket = new Socket(hostname, port);

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
                if(packetFromServer.error_code != BrokerPacket.BROKER_NULL) {
                    System.out.println(errorMessages.get(packetFromServer.error_code));
                } else {
                    System.out.println(messages.get(packetToServer.type));
                }
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
