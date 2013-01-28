import java.io.*;
import java.net.*;
import java.util.Scanner;

public class BrokerClient {
    public static void main(String[] args) throws IOException,
            ClassNotFoundException, NullPointerException {
        // Connect to lookup server and get the location of tse/nasdaq
        Socket lookupSocket = null;
        Socket brokerSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        ObjectOutputStream outb = null;
        ObjectInputStream inb = null;

            /* variables for hostname/port */
        String lookuphostname = "localhost";
        int lookupport = 4444;
        String hostname = null;
        int port = 0;
        try{
            if(args.length == 2 ) {
                lookuphostname = args[0];
                lookupport = Integer.parseInt(args[1]);
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
            lookupSocket = new Socket(lookuphostname, lookupport);
            out = new ObjectOutputStream(lookupSocket.getOutputStream());
            in = new ObjectInputStream(lookupSocket.getInputStream());

        } catch (UnknownHostException e) {
            System.err.println("ERROR: Don't know where to connect!!");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("ERROR: Couldn't get I/O for the connection.");
            System.exit(1);
        }


        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;

        System.out.print("CONSOLE>");
        Scanner scan;
        // close connection when "x" is entered
        while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("x") == -1) {
            scan = new Scanner(userInput);
            String sym = scan.next();
            if (sym.equals("local")){

                String exchange = scan.next();
                BrokerPacket packetToServer = new BrokerPacket();
                packetToServer.type = BrokerPacket.LOOKUP_REQUEST;
                packetToServer.exchange = exchange;
                //System.out.println("TO server : " + packetToServer.exchange);
                out.writeObject(packetToServer);

                /* server reply */
                BrokerPacket packetFromServer;
                packetFromServer = (BrokerPacket) in.readObject();
                if (packetFromServer.type == BrokerPacket.LOOKUP_REPLY){
                    port = packetFromServer.locations[0].broker_port;
                    hostname = packetFromServer.locations[0].broker_host;
                    //System.out.println(" From Lookup server" + packetFromServer.locations[0].toString());
                    System.out.println(packetFromServer.exchange +" as local.");
                }
                if (packetFromServer.type == BrokerPacket.ERROR_INVALID_EXCHANGE){
                    System.out.println("INVALID EXCHANGE <" + packetFromServer.exchange + ">");
                    System.out.print("CONSOLE>");
                    continue;
                }
                brokerSocket = new Socket(hostname, port);
                outb = new ObjectOutputStream(brokerSocket.getOutputStream());
                inb = new ObjectInputStream(brokerSocket.getInputStream());


            }
            else{
                if (port == 0 || hostname == null){
                    System.out.println("Please register "+ sym +" first by executing local command\n");

                }
                else{

                    // write to server
                    BrokerPacket packetToServer = new BrokerPacket();
                    packetToServer.type = BrokerPacket.BROKER_REQUEST;
                    packetToServer.symbol = sym;
                    outb.writeObject(packetToServer);

                    /* print server reply */
                    BrokerPacket packetFromServer;
                    packetFromServer = (BrokerPacket) inb.readObject();

                    if (packetFromServer.type == BrokerPacket.BROKER_QUOTE)
                        System.out.println("Quote from broker: " + packetFromServer.quote);
                    // handle error
                    if (packetFromServer.type == BrokerPacket.ERROR_INVALID_SYMBOL)
                        System.out.println(packetFromServer.symbol + " invalid.");

                    if (packetFromServer.type == BrokerPacket.BROKER_ERROR){
                        System.out.println(BrokerPacket.BROKER_NULL);
                    }

                }
            }
            System.out.print("CONSOLE>");
        }
        out.close();
        in.close();
        outb.close();
        inb.close();
        brokerSocket.close();
        lookupSocket.close();
        stdIn.close();
    }
}


