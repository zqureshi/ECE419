package mazewar;

/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import mazewar.server.MazePacket;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static mazewar.server.MazePacket.ClientAction;
import static mazewar.server.MazePacket.PacketType;
import static org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 *
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame implements Runnable {

    /**
     * The default width of the {@link Maze}.
     */
    private final int mazeWidth = 20;

    /**
     * The default height of the {@link Maze}.
     */
    private final int mazeHeight = 10;

    /**
     * The default random seed for the {@link Maze}.
     * All implementations of the same protocol must use
     * the same seed value, or your mazes will be different.
     */
    private long mazeSeed = 1989;

    /**
     * The {@link Maze} that the game uses.
     */
    private Maze maze = null;

    /**
     * The {@link GUIClient} for the game.
     */
    private GUIClient guiClient = null;

    /**
     * The panel that displays the {@link Maze}.
     */
    private OverheadMazePanel overheadPanel = null;

    /**
     * The table the displays the scores.
     */
    private JTable scoreTable = null;

    /**
     * Create the textpane statically so that we can
     * write to it globally using
     * the static consolePrint methods
     */
    private static final JTextPane console = new JTextPane();

    /**
     * Write a message to the console followed by a newline.
     *
     * @param msg The {@link String} to print.
     */
    public static synchronized void consolePrintLn(String msg) {
        console.setText(console.getText() + msg + "\n");
    }

    /**
     * Write a message to the console.
     *
     * @param msg The {@link String} to print.
     */
    public static synchronized void consolePrint(String msg) {
        console.setText(console.getText() + msg);
    }

    /**
     * Clear the console.
     */
    public static synchronized void clearConsole() {
        console.setText("");
    }

    /**
     * Static method for performing cleanup before exiting the game.
     */
    public static void quit() {
        // Put any network clean-up code you might have here.
        // (inform other implementations on the network that you have
        //  left, etc.)


        System.exit(0);
    }

    /* Event Bus to implement action pub/sub */
    private static EventBus eventBus;

    /* Socket to communicate with server */
    private Socket mazeSocket;
    private ObjectOutputStream toServer;
    private ObjectInputStream fromServer;
    private int sequenceNumber;

    /* Client details */
    private String clientId;
    private ConcurrentHashMap<String, Client> clients;

    /* Runnables for additional tasks */
    private final int QUEUE_SIZE = 1000;
    public BlockingQueue<MazePacket> packetQueue;

    /* ZooKeeper Connection */
    private static final String ZK_PARENT = "/game";
    private static final int ZK_TIMEOUT = 1000;
    private String clientPath;
    private static ZooKeeper zooKeeper;
    private static ZkWatcher zkWatcher;
    private static CountDownLatch zkConnected;

    /**
     * The place where all the pieces are put together.
     */
    public Mazewar(String zkServer, int zkPort, int port, String name) {
        super("ECE419 Mazewar");
        consolePrintLn("ECE419 Mazewar started!");

        // Throw up a dialog to get the GUIClient name.
        if(name != null) {
            clientId = name;
        } else {
            clientId = JOptionPane.showInputDialog("Enter your name");
        }
        if ((clientId == null) || (clientId.length() == 0)) {
            Mazewar.quit();
        }

        /* Connect to ZooKeeper and get sequencer details */
        List<ClientNode> nodeList = null;
        try {
            zkWatcher = new ZkWatcher();
            zkConnected = new CountDownLatch(1);
            zooKeeper = new ZooKeeper(zkServer + ":" + zkPort, ZK_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    /* Release Lock if ZooKeeper is connected */
                    if (event.getState() == SyncConnected) {
                        zkConnected.countDown();
                    } else {
                        System.err.println("Could not connect to ZooKeeper!");
                        System.exit(0);
                    }
                }
            });
            zkConnected.await();

            /* Successfully connected, now create our node on ZooKeeper */
            zooKeeper.create(
                Joiner.on('/').join(ZK_PARENT, clientId),
                Joiner.on(':').join(clientId, InetAddress.getLocalHost().getHostAddress(), port).getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
            );

            /* Get list of nodes */
            nodeList = ClientNode.sortList(zooKeeper.getChildren(ZK_PARENT, false));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Create the maze
        maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
        assert (maze != null);

        // Have the ScoreTableModel listen to the maze to find
        // out how to adjust scores.
        ScoreTableModel scoreModel = new ScoreTableModel();
        assert (scoreModel != null);
        maze.addMazeListener(scoreModel);

        /* Initialize packet queue */
        packetQueue = new ArrayBlockingQueue<MazePacket>(QUEUE_SIZE);

        /* Inject Event Bus into Client */
        Client.setEventBus(eventBus);

        clients = new ConcurrentHashMap<String, Client>();
        for(ClientNode client : nodeList) {
            if(client.getName().equals(clientId)) {
                clientPath = ZK_PARENT + "/" + client.getPath();
                guiClient = new GUIClient(clientId);
                clients.put(clientId, guiClient);
                maze.addClient(guiClient);
                eventBus.register(guiClient);
            } else {
                addRemoteClient(client);
            }
        }

        checkNotNull(guiClient, "Should have received our clientId in CLIENTS list!");

        // Create the GUIClient and connect it to the KeyListener queue
        this.addKeyListener(guiClient);

        // Use braces to force constructors not to be called at the beginning of the
        // constructor.
        /*{
            maze.addClient(new RobotClient("Norby"));
            maze.addClient(new RobotClient("Robbie"));
            maze.addClient(new RobotClient("Clango"));
            maze.addClient(new RobotClient("Marvin"));
        }*/


        // Create the panel that will display the maze.
        overheadPanel = new OverheadMazePanel(maze, guiClient);
        assert (overheadPanel != null);
        maze.addMazeListener(overheadPanel);

        // Don't allow editing the console from the GUI
        console.setEditable(false);
        console.setFocusable(false);
        console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));

        // Allow the console to scroll by putting it in a scrollpane
        JScrollPane consoleScrollPane = new JScrollPane(console);
        assert (consoleScrollPane != null);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));

        // Create the score table
        scoreTable = new JTable(scoreModel);
        assert (scoreTable != null);
        scoreTable.setFocusable(false);
        scoreTable.setRowSelectionAllowed(false);

        // Allow the score table to scroll too.
        JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
        assert (scoreScrollPane != null);
        scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));

        // Create the layout manager
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        getContentPane().setLayout(layout);

        // Define the constraints on the components.
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 3.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(overheadPanel, c);
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.weightx = 2.0;
        c.weighty = 1.0;
        layout.setConstraints(consoleScrollPane, c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        layout.setConstraints(scoreScrollPane, c);

        // Add the components
        getContentPane().add(overheadPanel);
        getContentPane().add(consoleScrollPane);
        getContentPane().add(scoreScrollPane);

        // Pack everything neatly.
        pack();

        // Let the magic begin.
        setVisible(true);
        overheadPanel.repaint();
        this.requestFocusInWindow();
    }

    private void addRemoteClient(ClientNode client) {
        RemoteClient remoteClient = new RemoteClient(client.getName());
        clients.put(client.getName(), remoteClient);
        maze.addClient(remoteClient);
        eventBus.register(remoteClient);
    }

    /**
     * Listen on socket for more packets from server
     */
    @Override
    public void run() {
        try {
            while(mazeSocket != null && !mazeSocket.isClosed()) {
                MazePacket packetFromServer = (MazePacket) fromServer.readObject();
                /* Post any other event to Event Bus */
                packetQueue.put(packetFromServer);
            }
        } catch (EOFException e) {
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Subscribe
    public void keyEvent(ClientAction action) throws IOException {
        /*System.out.println("action = " + action);*/

        /* Send action to server */
        MazePacket actionPacket = new MazePacket();
        actionPacket.type = PacketType.ACTION;
        actionPacket.clientId = Optional.of(clientId);
        actionPacket.action = Optional.of(action);

        toServer.writeObject(actionPacket);
    }

    @Subscribe
    public void quitEvent(KeyEvent e) throws Exception {
        assert(e.getKeyCode() == KeyEvent.VK_Q);

        eventBus.unregister(this);
        zooKeeper.delete(clientPath, -1);
        System.exit(0);
    }

    /* Dispatch packets from inbound queue */
    public Runnable packetDispatcher() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    while(true) {
                        MazePacket packet = packetQueue.take();
                        eventBus.post(packet);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        };
    }

    /* ZooKeeper Watcher */
    class ZkWatcher implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            Event.EventType type = event.getType();
            String path = event.getPath();
            System.out.println("Path: " + path + ", Event type:" + type);

            switch (type) {
                case NodeChildrenChanged:
                    try {
                        List<ClientNode> nodeList = ClientNode.sortList(zooKeeper.getChildren(ZK_PARENT, zkWatcher));

                        for(ClientNode client : nodeList) {
                            if(clients.containsKey(client.getName())) {
                                continue;
                            }

                            addRemoteClient(client);
                            zooKeeper.exists(ZK_PARENT + "/" + client.getPath(), zkWatcher);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                    break;

                case NodeDeleted:
                    String name = path.substring(path.lastIndexOf('/') + 1, path.length() - 10);
                    Client client = clients.remove(name);
                    eventBus.unregister(client);
                    maze.removeClient(client);

                    if(clients.size() == 1) {
                        System.err.println("Only one left in game, quitting!");
                        System.exit(0);
                    }
                    break;
            }
        }
    }

    /**
     * Class to represent ZkNodes ordered via sequence numbers.
     */
    private static class ClientNode implements Comparable<ClientNode> {
        private String path;
        private String name;
        private Integer sequence;

        private ClientNode(String path) {
            this.path = path;
            this.name =  path.substring(0, path.length() - 10);
            this.sequence = Integer.parseInt(path.substring(path.length() - 10));
        }

        @Override
        public int compareTo(ClientNode n) {
            return sequence - n.sequence;
        }

        public String toString() {
            return path;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

        public Integer getSequence() {
            return sequence;
        }

        private static List<ClientNode> sortList(List<String> list) {
            List<ClientNode> nodeList = new ArrayList<ClientNode>(list.size());
            for (String path : list) {
                nodeList.add(new ClientNode(path));
            }

            Collections.sort(nodeList);

            return nodeList;
        }
    }


    /**
     * Entry point for the game.
     *
     * @param args Command-line arguments.
     */
    public static void main(String args[]) throws Exception {
        try {
            checkArgument(args.length >= 3, "Usage: ./client.sh zkServer zkPort port [name]");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        String zkServer = args[0];
        int zkPort = Integer.parseInt(args[1]);
        int port = Integer.parseInt(args[2]);
        String name = null;

        if(args.length == 4) {
            name = args[3];
        }

        eventBus = new EventBus("mazewar");

        /* Create the GUI */
        Mazewar game = new Mazewar(zkServer, zkPort, port, name);

        /* Register with Event Bus */
        eventBus.register(game);

        /* Listen for packets from server in new Thread */
        new Thread(game).start();

        /* Run packet dispatcher */
        new Thread(game.packetDispatcher()).start();

        /* Set watch on siblings */
        List<String> nodeList = zooKeeper.getChildren(ZK_PARENT, zkWatcher);
        for(String node : nodeList) {
            zooKeeper.exists(ZK_PARENT + "/" + node, zkWatcher);
        }
    }
}
