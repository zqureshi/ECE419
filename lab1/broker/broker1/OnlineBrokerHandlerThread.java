import java.net.Socket;

public class OnlineBrokerHandlerThread extends Thread {
    private Socket socket = null;

    public OnlineBrokerHandlerThread(Socket socket) {
        super("OnlineBrokerHandlerThread");
        this.socket = socket;
        System.out.println("Created new Thread to handle client.");
    }

    public void run() {
        /*
         * TODO: Implement server logic
         */
    }
}
