import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.event.KeyEvent;

/*
Implement a queue shared by all the clients.
insert any request from client onto the queue
pop from the queue and dispatch to all the clients
*/
public class MazewarServer{
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

        // Initialize Linked list on the server side
        // Central queue to maintain order amongst clients
        LinkedBlockingQueue<ClientAction> ServerQueue = new LinkedBlockingQueue<ClientAction>();

        // List of registered clients

        List ClientList = Collections.synchronizedList(new LinkedList<String>());

        // list of sockets

        List SocketList = Collections.synchronizedList(new LinkedList<Socket>());

        // Set values in MazewarServerHandler
        MazewarServerHandlerThread.setServerQueue(ServerQueue);
        MazewarServerHandlerThread.setClientList(ClientList);
        MazewarServerHandlerThread.setSocketList(SocketList);

        while (listening) {
            new MazewarServerHandlerThread(serverSocket.accept()).start();
        }
        serverSocket.close();
    }
}
class ClientAction{
    String MyClient;
    String MyEvent;
    public ClientAction(String client, String keyEvent){
        this.MyClient = client;
        this.MyEvent = keyEvent;
    }

}

class MazewarServerHandlerThread extends Thread {
    private Socket socket = null;
    static LinkedBlockingQueue<ClientAction> ServerQueue;
    static List ClientList;
    static List SocketList;

    public MazewarServerHandlerThread(Socket socket) {
        super("MazewarServerHandlerThread");
        this.socket = socket;
        System.out.println("Created new Thread to handle client");
    }
    public static void setServerQueue(LinkedBlockingQueue<ClientAction> ServerQueue){
        MazewarServerHandlerThread.ServerQueue = ServerQueue;
    }
    public static void setClientList(List ClientList){
        MazewarServerHandlerThread.ClientList = ClientList;
    }

    public static void setSocketList(List SocketList){
        MazewarServerHandlerThread.SocketList = SocketList;
    }

    public void run() {

        try {
            /* stream to read from client */
            ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
            MazewarPacket packetFromClient;

            /* stream to write back to client */
            ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
            while (( packetFromClient = (MazewarPacket) fromClient.readObject()) != null) {
                    /* process message */

                // Maze req, when client moves sends a req to the server.
                // The server adds to the shared queue and replies to the client.

                if(packetFromClient.type == MazewarPacket.MAZE_REQUEST) {
                    /* create a packet to send reply back to client */
                    MazewarPacket packetToClient = new MazewarPacket();
                    ServerQueue.offer(new ClientAction(packetFromClient.ClientName, packetFromClient.Event));
                    packetToClient.type = MazewarPacket.MAZE_EXECUTE;
                    System.out.println("From Client: " + packetFromClient.ClientName + ", " + packetFromClient.Event);
                        /* send reply back to client */
                    try{
                        ClientAction temp  = ServerQueue.take();
                        packetToClient.ClientName= temp.MyClient;
                        packetToClient.Event = temp.MyEvent;

                    }catch (InterruptedException e){
                        System.out.println("Error in Queue!");
                    }
                    toClient.writeObject(packetToClient);

                }

                if(packetFromClient.type == MazewarPacket.MAZE_REGISTER) {
                    /* create a packet to send reply back to client */
                    MazewarPacket packetToClient = new MazewarPacket();
                    ClientList.add(packetFromClient.ClientName);

                    // remote client names
                    if(ClientList.size() >= 1 && !ClientList.get(0).equals(packetFromClient.ClientName)){
                        packetFromClient.RemoteName1 = (String)ClientList.get(0);
                    }
                    else{
                        packetFromClient.RemoteName1 = null;
                    }
                    if(ClientList.size() >= 2 && !ClientList.get(1).equals(packetFromClient.ClientName)){
                        packetFromClient.RemoteName2 = (String)ClientList.get(1);
                    }
                    else{
                        packetFromClient.RemoteName2 = null;
                    }

                    if(ClientList.size() >= 3 && !ClientList.get(2).equals(packetFromClient.ClientName)){
                        packetFromClient.RemoteName3 = (String)ClientList.get(2);
                    }
                    else{
                        packetFromClient.RemoteName3 = null;
                    }

                    if(ClientList.size() >= 4 && !ClientList.get(3).equals(packetFromClient.ClientName)){
                        packetFromClient.RemoteName4 = (String)ClientList.get(3);
                    }
                    else{
                        packetFromClient.RemoteName4 = null;
                    }
                    packetToClient.type = MazewarPacket.MAZE_REPLY;
                    System.out.println("From Client: " + packetFromClient.ClientName);
                        /* send reply back to client */
                    packetToClient.ClientName = packetFromClient.ClientName;

                    toClient.writeObject(packetToClient);

                }
            }
            /* cleanup when client exits */
            fromClient.close();
            toClient.close();

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}