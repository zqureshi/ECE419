import java.net.Socket;
import java.util.HashMap;

public class OnlineBrokerHandlerThread extends Thread {
    private Socket socket = null;
    private static HashMap<String, Integer> quotes;

    public OnlineBrokerHandlerThread(Socket socket) {
        super("OnlineBrokerHandlerThread");
        assert(quotes != null);
        this.socket = socket;
        System.out.println("Created new Thread to handle client.");
    }

    public void run() {
        /*
         * TODO: Implement server logic
         */
    }

    public static void setQuotes(HashMap<String, Integer> quotes) {
        OnlineBrokerHandlerThread.quotes = quotes;
    }
}
