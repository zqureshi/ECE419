package mazewar.server;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import org.omg.PortableInterceptor.USER_EXCEPTION;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkArgument;

import static mazewar.server.MazePacket.PacketType;
import static mazewar.server.MazePacket.PacketErrorCode;

public class MazeServer {
    private static AsyncEventBus eventBus;
    private static BlockingQueue<MazePacket> packetQueue;
    private static ConcurrentSkipListSet<String> clients;

    private static final int QUEUE_SIZE = 1000;

    private static class ServerHandler implements Runnable {
        private final Socket socket;
        private final ObjectInputStream fromClient;
        private final ObjectOutputStream toClient;
        private String clientId;

        private ServerHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.toClient = new ObjectOutputStream(socket.getOutputStream());
            this.fromClient = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            eventBus.register(this);

            try {
                /* Process CONNECT Request from Client */
                MazePacket connectPacket = (MazePacket) fromClient.readObject();
                checkArgument(connectPacket.type == PacketType.CONNECT,
                        "First packet from client must be CONNECT");
                clientId = connectPacket.clientId.get();

                /* If given clientId already exists then send error back */
                if(!clients.add(clientId)) {
                    MazePacket connectResponse = new MazePacket();
                    connectResponse.type = PacketType.ERROR;
                    connectResponse.error = Optional.of(PacketErrorCode.CLIENT_EXISTS);

                    toClient.writeObject(connectResponse);
                    socket.close();
                    return;
                }

                packetQueue.put(connectPacket);

                while(socket.isConnected()) {
                    MazePacket packetFromClient = (MazePacket) fromClient.readObject();
                    packetQueue.put(packetFromClient);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Subscribe
        public void clientEvent(MazePacket packet) throws IOException {
            System.out.println("Dispatching event to clientId = " + clientId);
            toClient.writeObject(packet);
        }
    }

    private static class PacketDispatcher implements Runnable {
        private int sequenceNumber = 0;

        @Override
        public synchronized void run() {
            try {
                while(true) {
                    MazePacket nextPacket = packetQueue.take();
                    nextPacket.sequenceNumber = Optional.of(sequenceNumber++);
                    eventBus.post(nextPacket);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        final ExecutorService handlerExecutor = Executors.newCachedThreadPool();
        final ExecutorService eventDispatcher = Executors.newSingleThreadExecutor();

        try {
            checkArgument(args.length == 1, "Usage: ./server.sh port");
            int serverPort = Integer.parseInt(args[0]);

            serverSocket = new ServerSocket(serverPort);
            System.out.println("Server listening on port: " + serverPort);
        } catch(IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Could not bind to port!");
            System.exit(1);
        }

        /* Initialize Event Bus */
        eventBus = new AsyncEventBus("mazeserver", eventDispatcher);

        /* Initialize Packet Queue */
        packetQueue = new ArrayBlockingQueue<MazePacket>(QUEUE_SIZE);

        /* Initialize Client Set */
        clients = new ConcurrentSkipListSet<String>();

        /* Start Dispatcher */
        new Thread(new PacketDispatcher()).start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                handlerExecutor.shutdown();
                eventDispatcher.shutdown();
                System.out.println("Server shutting down.");
            }
        }));

        while(serverSocket.isBound()) {
            handlerExecutor.execute(new ServerHandler(serverSocket.accept()));
        }

        serverSocket.close();
    }
}
