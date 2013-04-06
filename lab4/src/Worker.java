import com.google.common.base.Joiner;
import org.apache.commons.lang.SerializationUtils;
import org.apache.zookeeper.*;
import org.zeromq.ZMQ;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 * User: jaideepbajwa
 * Date: 2013-04-02
 * Time: 5:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Worker {

    private static String zooHost;
    private static int zooPort;
    private static String myID = null;
    private static ZooKeeper zooKeeper;
    private static ZkWatcher zkWatcher;
    private static CountDownLatch zkConnected;
    private static final int ZK_TIMEOUT = 5000;
    private static String ZK_WORKER = "/worker";
    private static String ZK_FILESERVER = "/fileserver";
    private static CountDownLatch nodeDelSignal = new CountDownLatch(1);

    /* ZeroMQ */
    private static ZMQ.Context context;
    private static ZMQ.Socket socket;

    private static ArrayBlockingQueue<String> jobQueue = new ArrayBlockingQueue<String>(100);

    public Worker(){

        // connect zooKeeper client zk server
        try {
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

            // if /worker does not exists, create one
            if (zooKeeper.exists(ZK_WORKER, false) == null){
                zooKeeper.create(ZK_WORKER,
                        null,
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
            }
            // create myself
            zooKeeper.create(
                    Joiner.on("/").join(ZK_WORKER, myID),
                    "primary".getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL
            );
            // set a data watch on myself
            zooKeeper.getData(Joiner.on("/").join(ZK_WORKER, myID), zkWatcher , null );

            // initialize ZMQ
            context = ZMQ.context(1);

            // setup socket with zmq
            setSocket(new String(zooKeeper.getData(ZK_FILESERVER, zkWatcher, null)));

        } catch (Exception e) {
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

            switch (type) {
                case NodeDataChanged:
                    try {
                        if (path.equals(ZK_FILESERVER)){
                            try{
                                setSocket(new String(zooKeeper.getData(ZK_FILESERVER,false,null)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        String data = new String(zooKeeper.getData(Joiner.on("/").join(ZK_WORKER, myID), this , null ));
                        String hash = data.split(":")[0];
                        int partID = Integer.parseInt(data.split(":")[1]);
                        // connect with fileserver and get dict partition to work on
                        jobQueue.add(data);

                        System.out.println("hash got "+ hash + "partID " + partID);


                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                    break;

                case NodeDeleted:
                    if( Joiner.on("/").join(ZK_WORKER, myID).equals(path)){
                        nodeDelSignal.countDown();
                    }

                    break;
            }
        }
    }

    private void setSocket (String fileServerId){
        // setup socket with zmq
        socket = context.socket(ZMQ.REQ);
        socket.connect("tcp://"+ fileServerId);

    }
    public Runnable workerProcessor(){
        return new Runnable() {
            @Override
            public void run() {
                try {
                    while(true) {
                        String data = jobQueue.take();
                        String hash = data.split(":")[0];
                        int partID = Integer.parseInt(data.split(":")[1]);
                        // get dict partition from fileserver

                        // send packet to tracker
                        FilePacket filePacket = new FilePacket();
                        filePacket.type = FilePacket.FILE_REQ;
                        filePacket.id = partID;
                        System.out.println("To fileserver " + filePacket.type);
                        socket.send(SerializationUtils.serialize(filePacket),0);

                        // reply
                        FilePacket packetFromServer = (FilePacket) SerializationUtils.deserialize(socket.recv(0));
                        System.out.println("Packet from tracker ");
                        if (packetFromServer.type == FilePacket.FILE_ERROR){
                            System.out.println("Fileserver ERROR!");
                            continue;
                        }
                        List<String> dataList = packetFromServer.result;
                        // perform md5 hash and return result
                        String result = findHash(hash, dataList);
                        System.out.println("Result " + result);
                        if ( result != null) {
                            zooKeeper.setData(ZK_WORKER, Joiner.on(":").join("found", result).getBytes(), -1 );
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        };
    }

   /* public ArrayList<String> getDictData(int id){

        final ArrayList<String> temp1 = new ArrayList<String>() {
            {
                add("d00rst0p");
                add("w4ggons");
                add("d1shevelled");
                add("motheaten");
                add("3ncod3r");
                add("personific4tions");
            }
        };

        final ArrayList<String> temp2 = new ArrayList<String>() {
            {
                add("4llured");
                add("r34l15t1c4lly");
                add("motionless");
                add("invi0lability");
                add("borat3s");
                add("p1r0u3tt3d");
            }
        };

        final ArrayList<String> temp3 = new ArrayList<String>() {
            {
                add("p0cketing");
                add("m1gr4nt5");
                add("pla1nt");
                add("instated");
                add("hangdog");
                add("chr0n1cl3");
            }
        };
        HashMap<Integer, ArrayList<String>> map = new HashMap<Integer, ArrayList<String>>(){
            {
                put(0,temp1);
                put(1,temp2);
                put(2,temp3);
            }
        };

        return map.get(id);
    } */

    public String findHash(String hash, List<String> dataList){

        String result = null;
        for ( String word : dataList){
            String hashCal = null;

            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                BigInteger hashint = new BigInteger(1, md5.digest(word.getBytes()));
                hashCal = hashint.toString(16);
                while (hashCal.length() < 32) hashCal = "0" + hashCal;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            if ( hash.equals(hashCal)){
                result = word;
            }
        }
        return result;
    }

    public static void main (String[] args){

        if (args.length == 3){

            try{
                zooHost = args[0];
                zooPort = Integer.parseInt(args[1]);
                myID = args[2];

            } catch (Exception e){
                e.printStackTrace();
            }

        }
        else {
            System.err.println("Usage worker [zooHost] [zooPort] [myID]");
            System.exit(-1);
        }

        Worker worker = new Worker();
        new Thread(worker.workerProcessor()).start();
        try{
            nodeDelSignal.await();
        } catch ( Exception e){
            e.printStackTrace();
        }
        System.out.println("My znode deleted! exiting");
        System.exit(0);

    }
}
