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

import com.sun.xml.internal.ws.util.StringUtils;
import org.apache.zookeeper.KeeperException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

//zookeeper
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;

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
    static private Maze maze = null;

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
        System.out.println("Exiting !");
        Mazewar.deleteMe();
        try{
//            Mazewar.Mysocket.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }
    // Sort children list
    protected static List<String> sortList (List<String> list){
        // if empty just return it back
        if (list.isEmpty()){
            return list;
        }
        List<String> retList = new ArrayList<String>();
        HashMap<String, sortNode> tempHash = new HashMap<String, sortNode>();
        for (String ele : list){
            //String name = ele.substring(0,(ele.length()-10));
            Integer number = Integer.parseInt(ele.substring(ele.length() - 10));

            sortNode temp = new sortNode(ele, number);
            tempHash.put(temp.getName(), temp);
            //System.out.println("Split " + number + "element " + name);
        }
        List<sortNode> s = new ArrayList<sortNode>(tempHash.values());

        Collections.sort(s, new Comparator<sortNode>() {
            @Override
            public int compare(sortNode o1, sortNode o2) {
                return o1.getNum() - o2.getNum();  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        for (sortNode n: s){
            //System.out.println("mee " + n.getName());
            retList.add(n.getName());
        }
        return retList;

    }
    public static void nodeCreated(String path){
        // if node created then create a remote client
        System.out.println("In node created! ");
        Scanner s = null;
        List<String> nodeList = null;

        try{
            nodeList = sortList(zooKeeper.getChildren(path, true));
            System.out.println("Node list " + nodeList);
        } catch (Exception e){
            e.printStackTrace();
        }
        String child = nodeList.get(nodeList.size()-1);
        System.out.println("child creating : " + child);
        try{
            // reading data from each znode
            String data = new String(zooKeeper.getData(parent + "/" + child, true, null));
            System.out.println(data);
            s = new Scanner(data);
        } catch (Exception e){
            e.printStackTrace();
        }

        s.useDelimiter(":");
        String remoteName = s.next();
        System.out.println("SIZES " + nodeList.size()+","+ClientHash.size());
        if (!remoteName.equals(name) && (nodeList.size() > ClientHash.size() )){
            Client R = new RemoteClient(remoteName);
            ClientHash.put(remoteName,R);
            remoteHash.put(parent + "/" + child, remoteName);
            maze.addClient(R);
        }
    }

    public static void nodeDeleted(String path){
        // if node created then create a remote client
        System.out.println("In node deleted! ");
        maze.removeClient(ClientHash.get(remoteHash.get(path)));
    }

    public static void deleteMe(){
        // if node created then create a remote client
        List<String> nodeList = null;
        Scanner s = null;
        try{
            nodeList = zooKeeper.getChildren(parent, true);
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println(nodeList);
        for (String remote : nodeList){
            try{
                // reading data from each znode, setting watcher to false as we don't want data watches
                String data = new String(zooKeeper.getData(parent + "/" + remote, true , null));
                System.out.println(data);
                s = new Scanner(data);
            } catch (Exception e){
                e.printStackTrace();
            }

            s.useDelimiter(":");
            String myName = s.next();
            if (myName.equals(name)){
                try{
                    zooKeeper.delete(parent + "/" + remote, -1);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    static ConcurrentHashMap<String,Client> ClientHash;
    static ConcurrentHashMap<String,String> remoteHash;
    final static String parent = "/root";
    static ZooKeeper zooKeeper;
    static ZkConnector zkc;
    static String hosts;
    static String name;
    static CountDownLatch nodeCreatedSignal = new CountDownLatch(1);


    /**
     * The place where all the pieces are put together.
     */
    public Mazewar(String hostname, int zooPort, int port)  {
        super("ECE419 Mazewar");
        consolePrintLn("ECE419 Mazewar started!");

        // Initialize hash map
        ClientHash = new ConcurrentHashMap<String, Client>();
        remoteHash = new ConcurrentHashMap<String, String>();
        List<String> nodeList = null;

        Scanner s = null;
        String myIp = null;
        int myPort = port;
        try{
            myIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e){
            e.printStackTrace();
        }


        maze = new MazeImpl(new Point(mazeWidth, mazeHeight), 42);
        assert(maze != null);

        // Have the ScoreTableModel listen to the maze to find
        // out how to adjust scores.
        ScoreTableModel scoreModel = new ScoreTableModel();
        assert(scoreModel != null);
        maze.addMazeListener(scoreModel);

        // Throw up a dialog to get the GUIClient name.
        name = JOptionPane.showInputDialog("Enter your name");
        //String name = "jaideep";
        if((name == null) || (name.length() == 0)) {
            Mazewar.quit();
        }
        // You may want to put your network initialization code somewhere in
        // here.
        // connect with zookeeper and get a list of current users, then draw clients in the
        // specific order

        hosts = hostname + ":" + zooPort;

        zkc = new ZkConnector();
        try{
             zkc.connect(hosts);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        zooKeeper = zkc.getZooKeeper();

        try {
            Stat ret = zkc.exists(
                    parent,
                    zkc.getWatcher());
            if (ret == null){
                System.out.println("Creating root !");
                zkc.create(
                        parent,
                        "parent",
                        CreateMode.PERSISTENT
                );
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        try{
            nodeList = sortList(zooKeeper.getChildren(parent, true));
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println(nodeList);



        // create remote nodes if any
        if (!nodeList.isEmpty()){
            for (String remote : nodeList){
                try{
                    // reading data from each znode, setting watcher to false as we don't want data watches

                    String data = new String(zooKeeper.getData(parent + "/" + remote, true , null));
                    System.out.println(data);
                    s = new Scanner(data);
                } catch (Exception e){
                    e.printStackTrace();
                }

                s.useDelimiter(":");
                String remoteName = s.next();
                Client R = new RemoteClient(remoteName);
                ClientHash.put(remoteName, R);
                remoteHash.put(parent + "/" +remote, remoteName);
                maze.addClient(R);
            }
        }

        System.out.println("creating myself!");
        // create znode, store in the form "name:ipaddress:port"
        String myPath = parent + "/" + name;
        zkc.create(
                myPath,
                name + ":" + myIp + ":" + myPort,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );
        guiClient = new GUIClient(name);
        maze.addClient(guiClient);
        ClientHash.put(name,guiClient);
        this.addKeyListener(guiClient);



        // set dependency

        mazewarthreadhandler.setMyname(name);
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
        String zooHostname = "";
        int zooPort = 0;
        int port = 0;

        if(args.length == 3 ) {
            zooHostname = args[0];
            zooPort = Integer.parseInt(args[1]);
            port = Integer.parseInt(args[2]);
        } else {
            System.err.println("ERROR: Invalid arguments!");
            System.exit(-1);
        }
             /* Create the GUI */
        new Mazewar(zooHostname, zooPort, port);
        new mazewarthreadhandler().start();

        // open Socket for other clients to connect
        ServerSocket serverSocket = null;
        boolean listening = true;

        try {
             serverSocket = new ServerSocket(port);
             nodeCreatedSignal.await();

        } catch (Exception e) {
            System.err.println("ERROR: Could not listen on port!");
            e.printStackTrace();
            System.exit(-1);
        }

        while (listening) {
            new MazewarServerHandlerThread(serverSocket.accept()).start();
        }
        serverSocket.close();
    }
}

class mazewarthreadhandler extends Thread{

    private static String myname;

    public static void setMyname(String myname) {
        mazewarthreadhandler.myname = myname;
    }
    public mazewarthreadhandler(){
        super("mazewarthreadhandler");
        System.out.println("Created new Thread Client " + myname);
    }

    public void run(){

    }
}

class MazewarServerHandlerThread extends Thread {
    private Socket socket = null;

    public MazewarServerHandlerThread(Socket socket) {
        super("MazewarServerHandlerThread");
        this.socket = socket;
        System.out.println("Created new Thread to handle client");
    }
    public void run() {

    }

}
