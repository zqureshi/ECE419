
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class Sequencer{
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        Socket socket = null;
        boolean listening = true;
        String parent = "/game";
        String zooHostname = "";
        String hosts = "";
        String myIp = "";
        int zooPort = 0;
        int myPort = 0;
        ZkConnector zkc = null;

        //atomic integer
        AtomicInteger atomicCounter = new AtomicInteger(0);

        try{
            myIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            if(args.length == 3) {
                zooHostname = args[0];
                zooPort = Integer.parseInt(args[1]);
                myPort = Integer.parseInt(args[2]);
                serverSocket = new ServerSocket(myPort);
            } else {
                System.err.println("ERROR: Invalid arguments!");
                System.exit(-1);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not listen on port!");
            System.exit(-1);
        }

        // Connect with zookeeper
        hosts = zooHostname + ":" + zooPort;
        zkc = new ZkConnector();
        try{
            zkc.connect(hosts);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        try {
            Stat ret = zkc.exists(
                    parent,
                    zkc.getWatcher());
            // Create root with sequencer IP address
            if (ret == null){
                System.out.println("Creating root !");
                zkc.create(
                        parent,
                        myIp+":"+myPort,
                        CreateMode.PERSISTENT
                );
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }

        socket = serverSocket.accept();

        ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
        sequencerPacket packetFromClient;
        sequencerPacket packetToClient;
        try {
            // Listen socket
            while (listening && ( packetFromClient = (sequencerPacket) fromClient.readObject()) != null) {

                packetFromClient = (sequencerPacket) fromClient.readObject();

                if(packetFromClient.type == sequencerPacket.S_REQUEST){
                    // Increate the atomicCounter and send it to client
                    packetToClient = new sequencerPacket();
                    packetToClient.seqNumber = atomicCounter.incrementAndGet();
                    packetToClient.type = sequencerPacket.S_REPLY;

                    // reply to client
                    toClient.writeObject(packetToClient);
                }

                if(packetFromClient.type == sequencerPacket.S_BYE){

                    listening = false;
                    break;
                }
            }
        }          catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        socket.close();
    }
}