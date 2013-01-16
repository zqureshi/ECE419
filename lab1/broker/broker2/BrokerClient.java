import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class BrokerClient {
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
        String userInput;

        System.out.print("CONSOLE>");
        outside_loop:
        while (stdIn.hasNext()) {
            /* Read input from standard input */
            userInput = stdIn.next().toLowerCase();

            if(userInput.equals("bye")
                || userInput.equals("exit")
                || userInput.equals("quit")) {
                break outside_loop;
            }

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

            /* re-print console prompt */
            System.out.print("CONSOLE>");
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
