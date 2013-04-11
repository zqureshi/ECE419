package dict.attack;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import org.apache.commons.lang.SerializationUtils;
import org.apache.zookeeper.*;
import org.zeromq.ZMQ;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
    private static String ZK_JOBS = "/jobs";
    private static String ZK_RESULT = "/result";
    private static String ZK_FILESERVER = "/fileserver";
    private static CountDownLatch nodeDelSignal = new CountDownLatch(1);

    // hashmap to store already calculated hash:passwd
    private static HashMap<String, String> cacheJobs = new HashMap<String, String>();
    private static HashMap<String, List<Integer>> cachePartId = new HashMap<String, List<Integer>>();
    private static Map<String, String> currJobs = new ConcurrentHashMap<String, String>();

    /* ZeroMQ */
    private static ZMQ.Context context;
    private static ZMQ.Socket socket;

    private static ArrayBlockingQueue<String> jobQueue = new ArrayBlockingQueue<String>(100);

    private static Gson gson = new Gson();

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

            // set watch on /jobs's children
            zooKeeper.getChildren(ZK_JOBS, zkWatcher);

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

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                    break;

                case NodeChildrenChanged:
                    /* get children of /jobs, which are currently active jobs
                    *  check in your local data structure if you have already worked
                    *  on that job, if not work on it else leave it*/
                    try {
                        if (path.equals(ZK_JOBS)){
                            List<String> nodeList = zooKeeper.getChildren(ZK_JOBS, false);
                            for ( String node : nodeList){
                                // checking cache
                                System.out.println("node" + node);
                                if (cacheJobs.containsKey(node)){
                                    setResult(node, cacheJobs.get(node));
                                }
                                else if ( !currJobs.containsKey(node)){
                                    String data = new String(zooKeeper.getData(Joiner.on("/").join(ZK_JOBS, node), false, null));

                                    System.out.println("data "+data);
                                    jobQueue.add(data);
                                    currJobs.put(node, data);
                                }
                            }
                        }

                        // re-set watch on /jobs for new jobs to come
                        System.out.println("Re-set watch on existing jobs");
                        zooKeeper.getChildren(ZK_JOBS, zkWatcher);

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }

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
        System.out.println("Connection re-set");

    }

    private void setResult (String hash , String result){
        try {
            byte[] res = null;
            if (result != null)
                res = result.getBytes();

            // Create znode in /result with results and delete it from /jobs
            if ( zooKeeper.exists(Joiner.on("/").join(ZK_RESULT, hash), false) == null )
                zooKeeper.create(
                        Joiner.on("/").join(ZK_RESULT, hash) ,
                        res,
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );

            //zooKeeper.setData(Joiner.on("/").join(ZK_JOBS, hash), null , -1);
            zooKeeper.delete(
                   Joiner.on("/").join(ZK_JOBS, hash) ,
                    -1
            );
        } catch ( Exception e) {
            e.printStackTrace();
        }
    }
    // not found on this worker

    private void resultNotFound (String hash, List<Integer> partIdList) {
        try {
            while (true){

                // no such job therefore exit
                if ( zooKeeper.exists(Joiner.on("/").join(ZK_JOBS, hash), false) == null)
                    break;

                String currData = new String(zooKeeper.getData(Joiner.on("/").join(ZK_JOBS, hash), false, null));
                int currVersion = zooKeeper.exists(Joiner.on("/").join(ZK_JOBS, hash), false).getVersion();
                System.out.println("Version curr" + currVersion);

                // de-serialize
                WorkerInfo workerInfo = gson.fromJson(currData, WorkerInfo.class);
                HashMap<String, List<Integer>> newMap = workerInfo.getWorkerInfo();

                // if the original list is updated then do not do any thing
                if(!newMap.get(myID).equals(partIdList))
                   break;
                // delete myself
                newMap.remove(myID);

                if (newMap.isEmpty()){
                    // set result with data as null and break
                    setResult(hash, null);
                    break;
                }
                else {
                    // update the work assigned to the current worker
                    WorkerInfo newWorkerInfo = new WorkerInfo(newMap, hash);

                    // serialize
                    String newData = gson.toJson(newWorkerInfo);

                    // setdata on the znode /jobs/<hash>

                    try {
                        if ( zooKeeper.setData(Joiner.on("/").join(ZK_JOBS, hash), newData.getBytes() , currVersion) != null) {
                            System.out.println("Update data removed " + myID);
                            break;
                        }
                    } catch (KeeperException e){
                        // Ignore
                    }
                }
            }
        } catch ( Exception e){
            throw Throwables.propagate(e);
        }
    }

    // connect with fileserver and get dict partition to work on

    public Runnable workerProcessor(){
        return new Runnable() {
            @Override
            public void run() {
                try {
                    while(true) {
                        String data = jobQueue.take();

                        // de-serialize
                        WorkerInfo workerInfo = gson.fromJson(data, WorkerInfo.class);

                        String hash = workerInfo.getHash();

                        // Update current jobs
                        currJobs.remove(hash);

                        List<Integer> partIdList = workerInfo.getWorkerInfo().get(myID);

                        // Already worked on this job
                        if (partIdList == null)
                            continue;

                        List<Integer> alreadySeen = new ArrayList<Integer>();

                        if ( cachePartId.containsKey(hash))
                            alreadySeen = cachePartId.get(hash);
                        else
                            cachePartId.put(hash, alreadySeen);

                        // get dict partition from fileserver
                        String result = null;

                        for ( Integer partID : partIdList ){

                            if ( alreadySeen.contains(partID))
                                continue;
                            else
                                alreadySeen.add(partID);
                                cachePartId.put(hash, alreadySeen);

                            //Thread.sleep(5000);
                            // send packet to tracker
                            FilePacket filePacket = new FilePacket();
                            filePacket.type = FilePacket.FILE_REQ;
                            filePacket.id = partID;
                            System.out.println("To fileserver " + filePacket.id);
                            socket.send(SerializationUtils.serialize(filePacket),0);

                            // reply from fileserver
                            FilePacket packetFromServer = (FilePacket) SerializationUtils.deserialize(socket.recv(0));
                            System.out.println("Packet from tracker ");
                            if (packetFromServer.type == FilePacket.FILE_ERROR){
                                System.out.println("Fileserver ERROR!");
                                break;
                            }
                            List<String> dataList = packetFromServer.result;
                            // perform md5 hash and return result
                            result = findHash(hash, dataList);
                            System.out.println("Result " + result);
                            if ( result != null) {
                                setResult(hash, result);
                                break;
                            }
                        }

                        // call this method if passwd not found on this worker
                        if (result == null){
                            resultNotFound(hash , cachePartId.get(hash));
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        };
    }


    public Runnable periodCheck(){
        return new Runnable() {
            @Override
            public void run() {
                try {
                    while(true) {
                        Thread.sleep(10000);
                        if (currJobs.isEmpty() )
                            continue;

                        List<String> jobList = zooKeeper.getChildren(ZK_JOBS, false);
                        for (String job : jobList){

                            String data = new String(zooKeeper.getData(Joiner.on("/").join(ZK_JOBS, job), false, null));
                            WorkerInfo workerInfoZk = gson.fromJson(data, WorkerInfo.class);
                            List<Integer> PartIdListZk = workerInfoZk.getWorkerInfo().get(myID);
                            if (PartIdListZk == null)
                                continue;

                            WorkerInfo workerInfo = gson.fromJson(currJobs.get(job), WorkerInfo.class);
                            List<Integer> PartIdList = workerInfo.getWorkerInfo().get(myID);

                            /*byte[] dataBytes = zooKeeper.getData(Joiner.on("/").join(ZK_JOBS, hash), false, null);
                            if (dataBytes == null)
                               continue;*/

                            PartIdListZk.removeAll(PartIdList);

                            if ( PartIdListZk.isEmpty())
                                continue;

                            // Update the local record
                            currJobs.put(job, data);

                            jobQueue.add(data);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        };
    }

    public String findHash(String hash, List<String> dataList){

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
            // add hashes onto the cache
            cacheJobs.put(hashCal, word);
            if ( hash.equals(hashCal)){
                return word;
            }
        }
        return null;
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
        new Thread(worker.periodCheck()).start();
        try{
            nodeDelSignal.await();
        } catch ( Exception e){
            e.printStackTrace();
        }
        System.out.println("My znode deleted! exiting");
        System.exit(0);

    }
}
