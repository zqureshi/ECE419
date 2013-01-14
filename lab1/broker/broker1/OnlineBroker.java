import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class OnlineBroker {
    private static final String QUOTES_FILE = "nasdaq";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        /* Parse command line arguments */
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

        /* Scan quotes file and put in dictionary */
        Scanner scanner = new Scanner(new File(QUOTES_FILE));
        HashMap<String, Long> quotes = new HashMap<String, Long>();

        try {
            while(scanner.hasNext()) {
                quotes.put(scanner.next().toLowerCase(), scanner.nextLong());
            }

            /* Inject quotes into the Handler */
            OnlineBrokerHandlerThread.setQuotes(quotes);
        } catch (NoSuchElementException e) {
            System.err.println("Error while parsing quotes file!");
            throw e;
        } finally {
            scanner.close();
        }

        /* Print out cached quotes */
        System.out.println("=== Current Quotes ===");
        for(String symbol : quotes.keySet()) {
            System.out.println(symbol + ": " + quotes.get(symbol));
        }
        System.out.println("======================\n");

        /* Bind to socket on specified Port and IP */
        while (listening) {
            new OnlineBrokerHandlerThread(serverSocket.accept()).run();
        }

        serverSocket.close();
    }
}
