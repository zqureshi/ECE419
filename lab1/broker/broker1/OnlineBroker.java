import java.net.*;
import java.io.*;
import java.util.*;

public class OnlineBroker{
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
            if(args.length == 1) {
                serverSocket = new ServerSocket(Integer.parseInt(args[0]));
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
	    File file = new File("nasdaq");
	    Scanner scan = new Scanner(file);
	    while(scan.hasNext()){
		hash.put(scan.next(), scan.nextLong());
	    }
	} catch (FileNotFoundException e){
	    System.err.println("ERROR: Could not open file!");
	    System.exit(-1);
	}

        while (listening) {
            new BrokerServerHandlerThread(serverSocket.accept(), hash).start();
        }

        serverSocket.close();
    }
}
