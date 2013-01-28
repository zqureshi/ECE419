import java.io.*;
import java.net.*;
import java.util.*;

public class BrokerExchange {
    public static void main(String[] args) throws IOException,
            ClassNotFoundException {

        // Register with the Lookup server
        Socket LookupSocket = null;
        Socket brokerSocket = null;

        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            /* variables for hostname/port */
            String hostname = "localhost";
            int port = 4444;

            if(args.length == 3 ) {
                hostname = args[0];
                port = Integer.parseInt(args[1]);
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
            LookupSocket = new Socket(hostname, port);

            out = new ObjectOutputStream(LookupSocket.getOutputStream());
            in = new ObjectInputStream(LookupSocket.getInputStream());

        } catch (UnknownHostException e) {
            System.err.println("ERROR: Don't know where to connect!!");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ERROR: Couldn't get I/O for the connection.");
            System.exit(1);
        }

        String exchange = args[2];
        BrokerPacket packetToServer1 = new BrokerPacket();
        packetToServer1.type = BrokerPacket.LOOKUP_REQUEST;
        packetToServer1.exchange = exchange;
        out.writeObject(packetToServer1);

         /* server reply */
        BrokerPacket packetFromServer1;
        packetFromServer1 = (BrokerPacket) in.readObject();
        if (packetFromServer1.type == BrokerPacket.LOOKUP_REPLY){

            int port = packetFromServer1.locations[0].broker_port;
            String hostname = packetFromServer1.locations[0].broker_host;
            System.out.println(packetFromServer1.exchange +" connected.");
            try{
                brokerSocket = new Socket(hostname, port);
            } catch (UnknownHostException e) {
                System.err.println("ERROR: Don't know where to connect!!");
                System.exit(1);
            } catch (IOException e) {
                System.err.println("ERROR: Couldn't get I/O for the connection.");
                System.exit(1);
            }
        }
        if (packetFromServer1.type == BrokerPacket.ERROR_INVALID_EXCHANGE){
            System.out.println("INVALID EXCHANGE <" + packetFromServer1.exchange + ">");
            System.exit(-1);
        }
        out.close();
        in.close();
        LookupSocket.close();

        // Connection with exchange tse/nasdaq

        out = new ObjectOutputStream(brokerSocket.getOutputStream());
        in = new ObjectInputStream(brokerSocket.getInputStream());

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;

        System.out.print("CONSOLE>");
        Scanner scan;
        // close connection when "x" is entered
        while ((userInput = stdIn.readLine()) != null  && userInput.toLowerCase().indexOf("x") == -1) {
            BrokerPacket packetToServer = new BrokerPacket();
            /* make a new request packet */
            // parse client request, to add, update or remove
            scan = new Scanner(userInput);
            String cmd = scan.next();
            if ( (cmd.equals("add")) || (cmd.equals("update")) || (cmd.equals("remove"))){
                String sym = scan.next();
                if (cmd.equals("add")){
                    packetToServer.type = BrokerPacket.EXCHANGE_ADD;
                    packetToServer.symbol = sym;
                    out.writeObject(packetToServer);
                }
                else if(cmd.equals("update")){
                    packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
                    packetToServer.symbol = sym;
                    Long quote = scan.nextLong();
                    packetToServer.quote= quote;
                    out.writeObject(packetToServer);
                }
                else if(cmd.equals("remove")){
                    packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
                    packetToServer.symbol = sym;
                    out.writeObject(packetToServer);
                }
			

		    /*packetToServer.type = BrokerPacket.BROKER_REQUEST;
		    packetToServer.symbol = userInput;
		    out.writeObject(packetToServer);*/

		    /* print server reply */
                BrokerPacket packetFromServer;
                packetFromServer = (BrokerPacket) in.readObject();

                if (cmd.equals("add") && packetFromServer.type == BrokerPacket.EXCHANGE_REPLY)
                    System.out.println(packetFromServer.symbol + " added.");
                else if(cmd.equals("update") && packetFromServer.type == BrokerPacket.EXCHANGE_REPLY)
                    System.out.println(packetFromServer.symbol + " updated to " + packetFromServer.quote);
                else if(cmd.equals("remove") && packetFromServer.type == BrokerPacket.EXCHANGE_REPLY)
                    System.out.println(packetFromServer.symbol + " removed.");

                // handling error msges
                if (packetFromServer.type == BrokerPacket.ERROR_INVALID_SYMBOL)
                    System.out.println(packetFromServer.symbol + " invalid.");
                if (packetFromServer.type == BrokerPacket.ERROR_OUT_OF_RANGE)
                    System.out.println(packetFromServer.symbol + " out of range.");
                if (packetFromServer.type == BrokerPacket.ERROR_SYMBOL_EXISTS)
                    System.out.println(packetFromServer.symbol + " exists.");
                if (packetFromServer.type == BrokerPacket.ERROR_INVALID_EXCHANGE)
                    System.out.println("Exchange error.");
			

		    /* re-print console prompt */
                System.out.print("CONSOLE>");
            }
            else {
                System.out.print("Invalid command, should be (add, update or remove), you entered <"+ cmd +">\n");
                System.out.print("CONSOLE>");
            }
        }

	 /* tell server that i'm quitting */
        BrokerPacket packetToServer = new BrokerPacket();
        packetToServer.type = BrokerPacket.BROKER_BYE;
        packetToServer.symbol= "Bye!";
        out.writeObject(packetToServer);

        out.close();
        in.close();
        stdIn.close();
        brokerSocket.close();
    }
}
