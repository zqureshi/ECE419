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

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import java.net.Socket;
import java.io.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {

    /**
     * The default width of the {@link Maze}.
     */
    private final int mazeWidth = 20;

    private static Socket Mysocket = null;

    /**
     * The default height of the {@link Maze}.
     */
    private final int mazeHeight = 10;

    /**
     * The default random seed for the {@link Maze}.
     * All implementations of the same protocol must use
     * the same seed value, or your mazes will be different.
     */
    private final int mazeSeed = 42;

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
     * @param msg The {@link String} to print.
     */
    public static synchronized void consolePrintLn(String msg) {
        console.setText(console.getText()+msg+"\n");
    }

    /**
     * Write a message to the console.
     * @param msg The {@link String} to print.
     */
    public static synchronized void consolePrint(String msg) {
        console.setText(console.getText()+msg);
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
        try{
            Mazewar.Mysocket.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    static ConcurrentHashMap<String,Client> ClientHash;

    /**
     * The place where all the pieces are put together.
     */
    public Mazewar(String hostname, int port)  {
        super("ECE419 Mazewar");
        consolePrintLn("ECE419 Mazewar started!");

        // Initialize hash map
        ClientHash = new ConcurrentHashMap<String, Client>();
        Socket MazewarSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        MazewarPacket packetFromServer = null;
        try {

            MazewarSocket = new Socket(hostname, port);
            // Store socket in Mazewar variable
            this.Mysocket = MazewarSocket;
            out = new ObjectOutputStream(MazewarSocket.getOutputStream());
            in = new ObjectInputStream(MazewarSocket.getInputStream());

            // Packet to Server
            MazewarPacket packetToServer = new MazewarPacket();

            packetToServer.type = MazewarPacket.MAZE_RAND;
            out.writeObject(packetToServer);

            // reply from server
            packetFromServer = (MazewarPacket) in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        if(packetFromServer.type == MazewarPacket.MAZE_RAND){
            // Create the maze
            maze = new MazeImpl(new Point(mazeWidth, mazeHeight), packetFromServer.rand);
            assert(maze != null);

        }else{
            System.err.println("No random number found !");
            System.exit(1);

        }
        // Have the ScoreTableModel listen to the maze to find
        // out how to adjust scores.
        ScoreTableModel scoreModel = new ScoreTableModel();
        assert(scoreModel != null);
        maze.addMazeListener(scoreModel);

        // Throw up a dialog to get the GUIClient name.
        String name = JOptionPane.showInputDialog("Enter your name");
        if((name == null) || (name.length() == 0)) {
            Mazewar.quit();
        }
        // You may want to put your network initialization code somewhere in
        // here.

        try {
             // Packet to Server
            MazewarPacket packetToServer = new MazewarPacket();
            packetToServer.ClientName = name;
            packetToServer.type = MazewarPacket.MAZE_REGISTER;

            System.out.println("To server " + packetToServer.ClientName);
            out.writeObject(packetToServer);

            // reply from server
            packetFromServer = (MazewarPacket) in.readObject();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        if(packetFromServer.type == MazewarPacket.MAZE_NEW){
            // Create the GUIClient and connect it to the KeyListener queue
            List<String> clientlist = packetFromServer.packetClientList;
            System.out.println(" From server Client list = " + clientlist.toString());
            for(int i =0; i< clientlist.size(); i++ ){
                if(clientlist.get(i).equals(name)){
                    guiClient = new GUIClient(name, out);
                    maze.addClient(guiClient);
                    ClientHash.put(packetFromServer.ClientName, guiClient);
                    this.addKeyListener(guiClient);
                }
                else{
                    Client R = new RemoteClient(clientlist.get(i));
                    maze.addClient(R);
                    ClientHash.put(clientlist.get(i), R);
                }
            }
        }
        // TODO: handle ERROR from server

        // Dependencies taken care of
        mazewarthreadhandler.setClientHash(ClientHash);
        mazewarthreadhandler.setIn(in);
        mazewarthreadhandler.setMyname(name);
        mazewarthreadhandler.setMaze(maze);

        // Create the panel that will display the maze.

        overheadPanel = new OverheadMazePanel(maze, guiClient);
        assert(overheadPanel != null);
        maze.addMazeListener(overheadPanel);

        // Don't allow editing the console from the GUI
        console.setEditable(false);
        console.setFocusable(false);
        console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));

        // Allow the console to scroll by putting it in a scrollpane
        JScrollPane consoleScrollPane = new JScrollPane(console);
        assert(consoleScrollPane != null);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));

        // Create the score table
        scoreTable = new JTable(scoreModel);
        assert(scoreTable != null);
        scoreTable.setFocusable(false);
        scoreTable.setRowSelectionAllowed(false);

        // Allow the score table to scroll too.
        JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
        assert(scoreScrollPane != null);
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
     * Entry point for the game.
     * @param args Command-line arguments.
     */
    public static void main(String args[]) throws IOException,
            ClassNotFoundException {

        /* variables for hostname/port */
        String hostname = "";
        int port = 0;

        if(args.length == 2 ) {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
        } else {
            System.err.println("ERROR: Invalid arguments!");
            System.exit(-1);
        }
             /* Create the GUI */
        new Mazewar(hostname, port);

        // Create thread
        new mazewarthreadhandler().start();
    }
}

class mazewarthreadhandler extends Thread{

    private static String myname;
    private static Maze maze;
    static ConcurrentHashMap<String, Client> ClientHash;
    private static ObjectInputStream in;

    public static void setIn(ObjectInputStream in) {
        mazewarthreadhandler.in = in;
    }

    public static void setMaze(Maze maze) {
        mazewarthreadhandler.maze = maze;
    }

    public static void setMyname(String myname) {
        mazewarthreadhandler.myname = myname;
    }

    public mazewarthreadhandler(){
        super("mazewarthreadhandler");
        System.out.println("Created new Thread Client " + myname);
    }

    public static void setClientHash(ConcurrentHashMap<String, Client> ClientHash){
        mazewarthreadhandler.ClientHash = ClientHash;
    }

    public void run(){
        boolean bye = true;
        // reply from server
        while(bye){
            MazewarPacket packetFromServer = null;
            try{
                packetFromServer = (MazewarPacket) in.readObject();

            }catch (IOException e){
                System.err.println("ERROR: Couldn't get I/O for the connection.");
                System.exit(1);
            }catch (ClassNotFoundException e){
                System.err.println("ERROR: Class not found");
                System.exit(1);

            }
            System.out.println("FROM server " + packetFromServer.ClientName + "ACTION " + packetFromServer.Event);
            if(packetFromServer.type == MazewarPacket.MAZE_NEW){
                assert(!packetFromServer.ClientName.equals(myname));

                // When a new Client joins the server, a new remote client is created
                Client R = new RemoteClient(packetFromServer.ClientName);
                maze.addClient(R);
                ClientHash.put(packetFromServer.ClientName, R);
            }

            if(packetFromServer.type == MazewarPacket.MAZE_EXECUTE){

                if ( ClientHash.get(packetFromServer.ClientName) != null){
                    Client currentClient = ClientHash.get(packetFromServer.ClientName);
                    String Action = packetFromServer.Event;

                    if (Action.equals("F")){
                        currentClient.forward();
                    }
                    if (Action.equals("B")){
                        currentClient.backup();
                    }
                    if (Action.equals("L")){
                        currentClient.turnLeft();
                    }
                    if (Action.equals("R")){
                        currentClient.turnRight();
                    }
                    if(Action.equals("S")){
                        currentClient.fire();
                    }
                }
            }
            if (packetFromServer.type == MazewarPacket.MAZE_REMOVE) {
                if (ClientHash.get(packetFromServer.ClientName) != null){
                    Client removeClient = ClientHash.get(packetFromServer.ClientName);
                    ClientHash.remove(packetFromServer.ClientName);
                    maze.removeClient(removeClient);
                }
            }
            if (packetFromServer.type == MazewarPacket.MAZE_ERROR) {
                System.out.println("Error From server ! Quiting");
                bye = false;
            }
        }
    }
}
