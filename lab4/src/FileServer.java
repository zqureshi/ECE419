import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.zookeeper.*;
import org.zeromq.ZMQ;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 * User: jaideepbajwa
 * Date: 2013-04-03
 * Time: 3:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class FileServer {
    private static EventBus eventBus;

    private HashMap<Integer, ArrayList<String>> map = new HashMap<Integer, ArrayList<String>>();
    private static final int FILE_LENGTH = 1000;

    private static ZkWatcher zkWatcher;
    private static CountDownLatch zkConnected;
    private static final int ZK_TIMEOUT = 5000;
    private static ZooKeeper zooKeeper;
    private static String ZK_FILESERVER = "/fileserver";
    private static String zooHost;
    private static int zooPort;
    private static String pathtofile = "/Users/jaideepbajwa/development/ECE419/lab4/src";

    /* ZeroMQ */
    private static ZMQ.Context context;
    private static ZMQ.Socket socket;

    public FileServer(String myID, String fileName, int port){

        // read the dictionary file and load it onto memory
        try {
            List<String> lines = Files.readLines(new File(Joiner.on("/").join(pathtofile, fileName)), Charsets.UTF_8);
            List<List<String>> chunks =  Lists.partition(lines, FILE_LENGTH/10);
            int i = 0;
            for (List<String> chunk : chunks){
                ArrayList<String> temp = new ArrayList<String>(chunk);
                map.put(i, temp);
                i++;
            }

            // connect with zooKeeper

            zkWatcher = new ZkWatcher();
            zkConnected = new CountDownLatch(1);
            zooKeeper = new ZooKeeper(zooHost + ":" + zooPort, ZK_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    /* Release Lock if ZooKeeper is connected */
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        zkConnected.countDown();
                    } else {
                        System.err.println("Could not connect to ZooKeeper!");
                        System.exit(0);
                    }
                }
            });
            zkConnected.await();

            // if /fileserver does not exists, create one
            if (zooKeeper.exists(ZK_FILESERVER, false) == null){
                zooKeeper.create(ZK_FILESERVER,
                        Joiner.on(":").join(InetAddress.getLocalHost().getHostAddress(), port).getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
                // create myself as leader
                zooKeeper.create(
                        Joiner.on("/").join(ZK_FILESERVER, myID),
                        "primary".getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL_SEQUENTIAL
                );
            }
            else {
                // if no children then create myself as leader
                System.out.println("tracker has no children");
                if (zooKeeper.getChildren(ZK_FILESERVER, false, null).isEmpty()){
                    // create myself as leader and update data of /fileserver
                    zooKeeper.setData(ZK_FILESERVER,
                            Joiner.on(":").join(InetAddress.getLocalHost().getHostAddress(), port).getBytes(),
                            -1
                    );
                    zooKeeper.create(
                            Joiner.on("/").join(ZK_FILESERVER, myID),
                            "primary".getBytes(),
                            ZooDefs.Ids.OPEN_ACL_UNSAFE,
                            CreateMode.EPHEMERAL_SEQUENTIAL
                    );
                }
                /* else if there is a child then
                   become the backup */
                else{
                    zooKeeper.create(
                            Joiner.on("/").join(ZK_FILESERVER, myID),
                            "backup".getBytes(),
                            ZooDefs.Ids.OPEN_ACL_UNSAFE,
                            CreateMode.EPHEMERAL_SEQUENTIAL
                    );
                }

            }

        } catch (Exception e){
            e.printStackTrace();
        }


    }

    /* ZooKeeper Watcher */
    class ZkWatcher implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            Event.EventType type = event.getType();
            String path = event.getPath();
            System.out.println("Path: " + path + ", Event type:" + type);
            // set watch on workers


            switch (type) {
                case NodeDataChanged:
                    try {

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                    break;

                case NodeDeleted:
                    break;
            }
        }
    }
    public Runnable workerReq() {

        return new Runnable() {

            @Override
            public void run(){
                //ZMQ.Socket socket = context.socket(ZMQ.REP);
                System.out.println("Fileserver!");
                //socket.connect("worker");

                while (true){
                    // wait for client req then respond
                    FilePacket packetFromServer = (FilePacket) SerializationUtils.deserialize(socket.recv(0));
                    System.out.println("From client" + packetFromServer.type);
                    eventBus.post(packetFromServer);

                }

            }
        };
    }
    @Subscribe
    public void handleJob(FilePacket filePacket) throws Exception{
        FilePacket packetToClient = new FilePacket();
        if (filePacket.type == FilePacket.FILE_REQ){
            packetToClient.type = FilePacket.FILE_RESULT;
            packetToClient.result = map.get(filePacket.id);
            if ( packetToClient.result == null){
                packetToClient.type = FilePacket.FILE_ERROR;
            }
        }

        socket.send(SerializationUtils.serialize(packetToClient),0);

    }

    public static void main (String[] args) {
        String myID = null;
        String fileName = null;
        int myPort = 0;

        if (args.length == 5){

            try{
                zooHost = args[0];
                zooPort = Integer.parseInt(args[1]);
                myID = args[2];
                fileName = args[3];
                myPort = Integer.parseInt(args[4]);
            } catch (Exception e){
                e.printStackTrace();
            }

        }
        else {
            System.err.println("Usage fileserver [zooHost] [zooPort] [myID] [filename] [myPort]");
            System.exit(-1);
        }

        // initialize ZMQ
        context = ZMQ.context(1);
        socket = context.socket(ZMQ.REP);
        socket.bind ("tcp://*:"+ myPort);

        eventBus = new EventBus("fileserver");
        FileServer fileServer = new FileServer(myID, fileName, myPort);
        eventBus.register(fileServer);

        new Thread(fileServer.workerReq()).start();

    }
}
