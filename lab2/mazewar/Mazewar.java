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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import mazewar.server.MazePacket;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static mazewar.server.MazePacket.ClientAction;
import static mazewar.server.MazePacket.PacketType;

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
    private final int mazeSeed = 1989;

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
    private ArrayList<Client> clients;

    /* Runnables for additional tasks */
    private final int QUEUE_SIZE = 1000;
    public BlockingQueue<MazePacket> packetQueue;

    /**
     * The place where all the pieces are put together.
     */
    public Mazewar(String server, int port) {
        super("ECE419 Mazewar");
        consolePrintLn("ECE419 Mazewar started!");

        // Create the maze
        maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
        assert (maze != null);

        // Have the ScoreTableModel listen to the maze to find
        // out how to adjust scores.
        ScoreTableModel scoreModel = new ScoreTableModel();
        assert (scoreModel != null);
        maze.addMazeListener(scoreModel);

        // Throw up a dialog to get the GUIClient name.
        clientId = JOptionPane.showInputDialog("Enter your name");
        if ((clientId == null) || (clientId.length() == 0)) {
            Mazewar.quit();
        }

        /* Connect to server and then add client */
        MazePacket connectPacket = null, connectResponse = null;
        try {
            mazeSocket = new Socket(server, port);
            toServer = new ObjectOutputStream(mazeSocket.getOutputStream());
            fromServer = new ObjectInputStream(mazeSocket.getInputStream());

            connectPacket = new MazePacket();
            connectPacket.type = PacketType.CONNECT;
            connectPacket.clientId = Optional.of(clientId);
            toServer.writeObject(connectPacket);

            connectResponse = (MazePacket) fromServer.readObject();

            if(connectResponse.type != PacketType.CLIENTS) {
                System.err.println("Error: " + connectResponse.error.get().getMessage());
                System.exit(1);
            }

            sequenceNumber = connectResponse.sequenceNumber.get();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        /* Initialize packet queue */
        packetQueue = new ArrayBlockingQueue<MazePacket>(QUEUE_SIZE);

        /* Inject Event Bus into Client */
        Client.setEventBus(eventBus);

        /* Loop through clients and add to maze */
        clients = new ArrayList<Client>(10);
        for(String client : connectResponse.clients.get()) {
            if(client.equals(clientId)) {
                guiClient = new GUIClient(clientId);
                clients.add(guiClient);
                maze.addClient(guiClient);
                eventBus.register(guiClient);
            } else {
                RemoteClient remoteClient = new RemoteClient(client);
                clients.add(remoteClient);
                maze.addClient(remoteClient);
                eventBus.register(remoteClient);
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

    /**
     * Listen on socket for more packets from server
     */
    @Override
    public void run() {
        try {
            while(!mazeSocket.isClosed()) {
                MazePacket packetFromServer = (MazePacket) fromServer.readObject();

                assert(packetFromServer.sequenceNumber.get() == ++sequenceNumber);

                if(packetFromServer.type == PacketType.DISCONNECT) {
                    if(clientId.equals(packetFromServer.clientId.get())) {
                        eventBus.unregister(this);
                        mazeSocket.close();
                        System.exit(0);
                    }

                    /* Search for client and remove it if isn't us */
                    for(int i = 0; i < clients.size(); i++) {
                        if(clients.get(i).getName().equals(packetFromServer.clientId.get())) {
                            System.out.println("Removing client " + packetFromServer.clientId.get());
                            maze.removeClient(clients.get(i));
                            break; /* At most one client can disconnect in a packet */
                        }
                    }
                }

                /* If received updated list of clients add them */
                if(packetFromServer.type == PacketType.CLIENTS) {
                    String[] updatedClients = packetFromServer.clients.get();
                    for(int i = 0; i < updatedClients.length; i++) {
                        if(i < clients.size()) {
                            /* Make sure we're in consistent state */
                            assert(clients.get(i).getName().equals(updatedClients[i]));
                        } else {
                            RemoteClient remoteClient = new RemoteClient(updatedClients[i]);
                            clients.add(remoteClient);
                            maze.addClient(remoteClient);
                            eventBus.register(remoteClient);
                        }
                    }
                } else {
                    /* Post any other event to Event Bus */
                    packetQueue.put(packetFromServer);
                }
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
        System.out.println("action = " + action);

        /* Send action to server */
        MazePacket actionPacket = new MazePacket();
        actionPacket.type = PacketType.ACTION;
        actionPacket.clientId = Optional.of(clientId);
        actionPacket.action = Optional.of(action);

        toServer.writeObject(actionPacket);
    }

    @Subscribe
    public void quitEvent(KeyEvent e) throws IOException {
        assert(e.getKeyCode() == KeyEvent.VK_Q);

        /* Send disconnect packet to server and close socket */
        MazePacket disconnectPacket = new MazePacket();
        disconnectPacket.type = PacketType.DISCONNECT;
        disconnectPacket.clientId = Optional.of(clientId);
        toServer.writeObject(disconnectPacket);
    }

    /* Dispatch packets from inbound queue */
    public Runnable packetDispatcher(){
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



    /**
     * Entry point for the game.
     *
     * @param args Command-line arguments.
     */
    public static void main(String args[]) {
        try {
            checkArgument(args.length == 2, "Usage: ./client.sh server port");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        String server = args[0];
        int port = Integer.parseInt(args[1]);

        eventBus = new EventBus("mazewar");

        /* Create the GUI */
        Mazewar game = new Mazewar(server, port);

        /* Register with Event Bus */
        eventBus.register(game);

        /* Listen for packets from server in new Thread */
        new Thread(game).start();

        /* Run packet dispatcher */
        new Thread(game.packetDispatcher()).start();
    }
}
