import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineBroker {
    public static void main(String[] args) throws IOException, NumberFormatException, ClassNotFoundException {
        String exchange = null;
        ServerSocket serverSocket = null;
        boolean listening = true;
        LookupClient client = null;

        /* Parse command line arguments and start server */
        try {
            if(args.length == 4) {
                /* Start up server */
                serverSocket = new ServerSocket(Integer.parseInt(args[2]));

                /* Register with naming service */
                System.out.println("Registering with naming service.");
                client = new LookupClient(args[0], Integer.parseInt(args[1]));
                exchange = args[3];
                try {
                    if(!client.register(exchange, InetAddress.getLocalHost().getHostAddress(), Integer.parseInt(args[2]))) {
                        System.err.println("Could not register with naming service!");
                        System.exit(-1);
                    }
                } catch (IOException e) {
                    System.err.println("Could not connect to lookup server!");
                    System.exit(-1);
                }
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        /* Scan quotes file and put in dictionary */
        Scanner scanner = new Scanner(new File(exchange));
        final ConcurrentHashMap<String, Long> quotes = new ConcurrentHashMap<String, Long>();

        try {
            while(scanner.hasNext()) {
                quotes.put(scanner.next().toLowerCase(), scanner.nextLong());
            }
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

        /* Add shutdown hook to save cache to disk */
        final String quotesFile = exchange;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    System.out.println("\n=== Writing quotes to disk ===");
                    PrintWriter writer = new PrintWriter(new File(quotesFile));
                    for(String symbol : quotes.keySet()) {
                        writer.format("%s %d\n", symbol, quotes.get(symbol));
                        System.out.println(symbol + ": " + quotes.get(symbol));
                    }
                    writer.close();
                    System.out.println("==============================\n");
                } catch (Exception e) {
                    System.err.println("Error while saving quotes to disk!");
                    e.printStackTrace();
                }
            }
        });

        /* Inject dependencies for handler threads */
        OnlineBrokerHandlerThread.setExchange(exchange);
        OnlineBrokerHandlerThread.setLookupClient(client);
        OnlineBrokerHandlerThread.setQuotes(quotes);

        /* Bind to socket on specified Port and IP */
        while (listening) {
            new OnlineBrokerHandlerThread(serverSocket.accept()).start();
        }

        serverSocket.close();
    }
}
