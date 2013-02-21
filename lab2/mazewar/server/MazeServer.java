package mazewar.server;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;

import static mazewar.server.MazePacket.PacketType;
import static mazewar.server.MazePacket.PacketErrorCode;

public class MazeServer {
    private static EventBus eventBus;
    private static BlockingQueue<MazePacket> packetQueue;
    private static CopyOnWriteArraySet<String> clients;
    private static AtomicBoolean gameStarted;
    private static AtomicInteger sequenceNumber;

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

        private void connectError(PacketErrorCode error) throws IOException {
            MazePacket connectResponse = new MazePacket();
            connectResponse.type = PacketType.ERROR;
            connectResponse.error = Optional.of(error);

            toClient.writeObject(connectResponse);
            socket.close();
        }

        @Override
        public void run() {
            try {
                /* Process CONNECT Request from Client */
                MazePacket connectPacket = (MazePacket) fromClient.readObject();
                checkArgument(connectPacket.type == PacketType.CONNECT,
                        "First packet from client must be CONNECT");
                clientId = connectPacket.clientId.get();

                /* If game already started then can't join */
                if(gameStarted.get()) {
                    connectError(PacketErrorCode.GAME_STARTED);
                    return;
                }

                /**
                 * Synchronize on clients to prevent interleaving of connect calls
                 * and thus break ordering of sequence numbers.
                 */
                synchronized (clients) {
                    /* If given clientId already exists then send error back */
                    if(!clients.add(clientId)) {
                        connectError(PacketErrorCode.CLIENT_EXISTS);
                        return;
                    }

                    /* Finally register object with Event Bus */
                    eventBus.register(this);

                    /* Put list of connected clients on dispatch queue */
                    MazePacket clientList = new MazePacket();
                    clientList.type = PacketType.CLIENTS;
                    clientList.clients = Optional.of(clients.toArray(new String[]{}));
                    packetQueue.put(clientList);
                }

                /* Wait for first packet from client after connect */
                MazePacket packetFromClient = (MazePacket) fromClient.readObject();

                /* If received a packet from client post-connect means game has started */
                gameStarted.set(true);
                /*System.out.println("Game started");*/

                while(!socket.isClosed()) {
                    /* Put packet and queue which will be handled by dispatcher */
                    packetQueue.put(packetFromClient);

                    if(packetFromClient.type == PacketType.DISCONNECT) {
                        return;
                    } else {
                        packetFromClient = (MazePacket) fromClient.readObject();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Subscribe
        public void clientEvent(MazePacket packet) throws IOException {
            /*System.out.printf("Dispatching %s to %s\n", packet.type, clientId);*/
            toClient.writeObject(packet);

            if(packet.type == PacketType.DISCONNECT &&
                    clientId.equals(packet.clientId.get())) {
                eventBus.unregister(this);
                socket.close();
                clients.remove(clientId);
                System.out.println(clientId + " Disconnected");

                if(clients.isEmpty()) {
                    System.out.println("All clients disconnected: Game Over");
                    System.exit(0);
                }
            }
        }
    }

    /**
     * Single threaded dispatcher that picks packets from queue and posts to event bus.
     */
    private static class PacketDispatcher implements Runnable {
        @Override
        public synchronized void run() {
            try {
                while(true) {
                    MazePacket nextPacket = packetQueue.take();

                    /* Attach sequence number to packet and post */
                    nextPacket.sequenceNumber = Optional.of(sequenceNumber.getAndIncrement());
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
        eventBus = new EventBus("mazeserver");

        /* Initialize Packet Queue */
        packetQueue = new ArrayBlockingQueue<MazePacket>(QUEUE_SIZE);

        /* Initialize Client Set */
        clients = new CopyOnWriteArraySet<String>();

        /* Start Dispatcher */
        new Thread(new PacketDispatcher()).start();

        /* Starting new game, so set started to false */
        gameStarted = new AtomicBoolean(false);

        /* Initialize Sequence Number */
        sequenceNumber = new AtomicInteger();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                handlerExecutor.shutdown();
                System.out.println("Server shutting down.");
            }
        }));

        /* Listen for new connections from clients */
        while(serverSocket.isBound()) {
            handlerExecutor.execute(new ServerHandler(serverSocket.accept()));
        }

        serverSocket.close();
    }
}
