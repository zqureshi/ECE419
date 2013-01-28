import java.net.*;
import java.io.*;
import java.util.*;

public class OnlineBroker{
    public static void main(String[] args) throws IOException,
    ClassNotFoundException {
        // Register with the Lookup server
        Socket LookupSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            /* variables for hostname/port */
            String hostname = "localhost";
            int port = 4444;

            if(args.length == 4 ) {
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

        String exchange = args[3];
        BrokerPacket packetToServer = new BrokerPacket();
        packetToServer.type = BrokerPacket.LOOKUP_REGISTER;
        packetToServer.locations = new BrokerLocation[] { new BrokerLocation( InetAddress.getLocalHost().getHostName(),Integer.parseInt(args[2]) )};
        packetToServer.exchange = exchange;
        out.writeObject(packetToServer);

        // print server reply
        BrokerPacket packetFromServer;
        packetFromServer = (BrokerPacket) in.readObject();

        if (packetFromServer.type == BrokerPacket.LOOKUP_REPLY)
            System.out.println("Registered with Lookup server");

        out.close();
        in.close();
        LookupSocket.close();


        // open socket for client and exchange to connect
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            if(args.length == 4) {
                serverSocket = new ServerSocket(Integer.parseInt(args[2]));
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }
 	
	// Initiale hash map, and parse the nasdaq file.	
	Hashtable<String, Long> hash = new Hashtable<String, Long>();
	try {
	    File file = new File(exchange);
	    Scanner scan = new Scanner(file);
	    while(scan.hasNext()){
		hash.put(scan.next(), scan.nextLong());
	    }
	} catch (FileNotFoundException e){
	    System.err.println("ERROR: Could not open file!");
	    System.exit(-1);
	}
        BrokerServerHandlerThread.setFilename(exchange);
        BrokerServerHandlerThread.setHash(hash);

        while (listening) {
            new BrokerServerHandlerThread(serverSocket.accept()).start();
        }
        serverSocket.close();
//        try{
//            Runtime.getRuntime().addShutdownHook(Thread.currentThread());
//            // flush hasttable onto the file once server closes
//            FileWriter file1 = new FileWriter(exchange);
//            BufferedWriter outfile = new BufferedWriter(file1);
//            Enumeration keys = hash.keys();
//            while(keys.hasMoreElements()){
//                try {
//                    Object key = keys.nextElement();
//                    Object value = hash.get(key);
//                    outfile.write(key + " " + value + "\n");
//                } catch (IOException e) {
//                    System.err.println("ERROR: Could not open file!");
//                    System.exit(-1);
//                }
//            }
//            outfile.close();
//        }catch (Throwable t) {
//            // we get here when the program is run with java
//            // version 1.2.2 or older
//            System.out.println("Shutdown hook did not work");
//        }
    }
}
