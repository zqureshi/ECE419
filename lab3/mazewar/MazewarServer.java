/*
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

*/
/*
Implement a queue shared by all the clients.
insert any request from client onto the queue
pop from the queue and dispatch to all the clients
*//*

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

        // list of outputstream
        List<ObjectOutputStream> OutputStreamList = Collections.synchronizedList(new LinkedList<ObjectOutputStream>());

        // List containing currently connected clients
        List<String> ClientList  = Collections.synchronizedList(new LinkedList<String>());

        Random randgen = new Random();
        int rand = randgen.nextInt();

        // Set values in MazewarServerHandler
        MazewarServerHandlerThread.setServerQueue(ServerQueue);
        MazewarServerHandlerThread.setOutputList(OutputStreamList);
        MazewarServerHandlerThread.setClientList(ClientList);
        MazewarServerHandlerThread.setRandgen(rand);


        // Setting QueueHandler thread variables
        QueueHandler.setServerQueue(ServerQueue);
        QueueHandler.setOutputStreamList(OutputStreamList);

        // Start queueHandler
        new Thread(new QueueHandler()).start();

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

class QueueHandler implements Runnable{

    static LinkedBlockingQueue<ClientAction> ServerQueue;
    static List<ObjectOutputStream> OutputStreamList;

    public static void setServerQueue(LinkedBlockingQueue<ClientAction> serverQueue) {
        ServerQueue = serverQueue;
    }

    public static void setOutputStreamList(List<ObjectOutputStream> outputStreamList) {
        OutputStreamList = outputStreamList;
    }

    @Override
    public void run() {
        try{
            while(true){
                ClientAction clientaction = ServerQueue.take();

                // Packet for client
                MazewarPacket packetToClient = new MazewarPacket();
                packetToClient.type = MazewarPacket.MAZE_EXECUTE;
                packetToClient.ClientName = clientaction.MyClient;
                packetToClient.Event = clientaction.MyEvent;
                if (clientaction.MyEvent == "X"){
                    packetToClient.type = MazewarPacket.MAZE_REMOVE;
                }

                // Broadcast to all connected clients
                for (ObjectOutputStream clientOut : OutputStreamList){
                    clientOut.writeObject(packetToClient);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }
}

class MazewarServerHandlerThread extends Thread {
    private Socket socket = null;
    static LinkedBlockingQueue<ClientAction> ServerQueue;
    static ConcurrentHashMap<String,DirectedPoint> ClientPointHash;
    static List<ObjectOutputStream> OutputStreamList;
    static List<String> ClientList;
    static int rand;

    public MazewarServerHandlerThread(Socket socket) {
        super("MazewarServerHandlerThread");
        this.socket = socket;
        System.out.println("Created new Thread to handle client");
    }
    public static void setServerQueue(LinkedBlockingQueue<ClientAction> ServerQueue){
        MazewarServerHandlerThread.ServerQueue = ServerQueue;
    }

    public static void setOutputList(List OutputStreamList){
        MazewarServerHandlerThread.OutputStreamList = OutputStreamList;
    }
    public static void setClientList(List ClientList){
        MazewarServerHandlerThread.ClientList = ClientList;
    }

    public static void setRandgen(int rand) {
        MazewarServerHandlerThread.rand = rand;
    }

    public void run() {

        try {
            */
/* stream to read from client *//*

            ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());

            MazewarPacket packetFromClient;

            */
/* stream to write back to client *//*

            ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());

            if (!OutputStreamList.contains(toClient)){
                OutputStreamList.add(toClient);
            }
            else{
                MazewarPacket packToClient = new MazewarPacket();
                packToClient.type = MazewarPacket.MAZE_ERROR;
                toClient.writeObject(packToClient);
                System.err.println("Incorrect connection");
                System.exit(1);
            }

            packetFromClient = (MazewarPacket) fromClient.readObject();

            */
/**
             * Client
             *//*


            if(packetFromClient.type != MazewarPacket.MAZE_RAND){
                System.err.println("Client must register first packet received :"+ packetFromClient.type);
                MazewarPacket packetToClient = new MazewarPacket();
                packetToClient.type = MazewarPacket.MAZE_ERROR;

                //reply to Client
                toClient.writeObject(packetToClient);

            }else{
                MazewarPacket packetToClient = new MazewarPacket();
                packetToClient.rand = rand;
                packetToClient.type = MazewarPacket.MAZE_RAND;

                //reply to client
                toClient.writeObject(packetToClient);


            }
            // read again to register
            packetFromClient = (MazewarPacket) fromClient.readObject();

            if(packetFromClient.type != MazewarPacket.MAZE_REGISTER) {
                System.err.println("Client must register first packet received :"+ packetFromClient.type);
                MazewarPacket packetToClient = new MazewarPacket();
                packetToClient.type = MazewarPacket.MAZE_ERROR;

                //reply to Client
                toClient.writeObject(packetToClient);
            }
            else{
            */
/* create a packet to send reply back to client *//*

                MazewarPacket packetToClient = new MazewarPacket();
                ClientList.add(packetFromClient.ClientName);
            */
/* send reply back to client *//*

                packetToClient = new MazewarPacket();
                packetToClient.type = MazewarPacket.MAZE_NEW;
                packetToClient.ClientName = packetFromClient.ClientName;
                packetToClient.packetClientList = ClientList;
                System.out.println("From Client: " + packetFromClient.ClientName);

              */
/* send reply back to all connected clients *//*

                for(ObjectOutputStream clientOut : OutputStreamList ){
                    System.out.println("Client List : " + packetToClient.packetClientList);
                    clientOut.writeObject(packetToClient);
                }
            }

            while (( packetFromClient = (MazewarPacket) fromClient.readObject()) != null) {
                */
/**
                 * Maze req, when client moves sends a req to the server.
                 * The server adds to the shared queue.
                 *//*


                if(packetFromClient.type == MazewarPacket.MAZE_REQUEST) {
                    */
/* add client req to the queue *//*

                    ServerQueue.offer(new ClientAction(packetFromClient.ClientName, packetFromClient.Event));
                }
                if(packetFromClient.type == MazewarPacket.MAZE_BYE){
                    // remove client from outputsteam list
                    System.out.println(" Client " + packetFromClient.ClientName + " left the game");

                    OutputStreamList.remove(toClient);
                    //Update server client list
                    ClientList.remove(packetFromClient.ClientName);
                    ServerQueue.offer(new ClientAction(packetFromClient.ClientName, "X"));
                    break;
                }
            }
            */
/* cleanup when client exits *//*

            fromClient.close();
            toClient.close();

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}*/
