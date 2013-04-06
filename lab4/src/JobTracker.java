import com.google.common.base.Joiner;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang.SerializationUtils;
import org.apache.zookeeper.*;
import org.zeromq.ZMQ;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 * User: jaideepbajwa
 * Date: 2013-04-01
 * Time: 5:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class JobTracker extends Thread{

    private static EventBus eventBus;
    private static ArrayBlockingQueue<JobPacket> packetQueue = null;

    private static ZkWatcher zkWatcher;
    private static CountDownLatch zkConnected;
    private static ZooKeeper zooKeeper;
    private static final int ZK_TIMEOUT = 5000;
    private static final int ARRAY_SIZE = 10;
    private static String ZK_TRACKER = "/tracker";
    private static String ZK_WORKER = "/worker";
    private static String zooHost;
    private static int zooPort;
    private static List<String> finalResult = new ArrayList<String>(1);

    /* ZeroMQ */
    private ZMQ.Context context;
    private ZMQ.Socket socket;

    public JobTracker(int port, String myID, ZMQ.Context context, ZMQ.Socket socket) {

        this.context = context;
        this.socket = socket;

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

            // if /tracker does not exists, create one
            if (zooKeeper.exists(ZK_TRACKER, false) == null){
                zooKeeper.create(ZK_TRACKER,
                        Joiner.on(":").join(InetAddress.getLocalHost().getHostAddress(), port).getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
                // create myself as leader
                zooKeeper.create(
                        Joiner.on("/").join(ZK_TRACKER, myID),
                        "primary".getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL_SEQUENTIAL
                );
            }
            else {
                // if no children then create myself as leader
                System.out.println("tracker has no children");
                if (zooKeeper.getChildren(ZK_TRACKER, false, null).isEmpty()){
                    // create myself as leader and update data of /tracker
                    zooKeeper.setData(ZK_TRACKER,
                            Joiner.on(":").join(InetAddress.getLocalHost().getHostAddress(), port).getBytes(),
                            -1
                    );
                    zooKeeper.create(
                            Joiner.on("/").join(ZK_TRACKER, myID),
                            "primary".getBytes(),
                            ZooDefs.Ids.OPEN_ACL_UNSAFE,
                            CreateMode.EPHEMERAL_SEQUENTIAL
                    );
                }
                /* else if there is a child then
                   become the backup */
                else{
                    zooKeeper.create(
                            Joiner.on("/").join(ZK_TRACKER, myID),
                            "backup".getBytes(),
                            ZooDefs.Ids.OPEN_ACL_UNSAFE,
                            CreateMode.EPHEMERAL_SEQUENTIAL
                    );
                }

            }
            // set watch at /worker
            zooKeeper.getChildren(ZK_WORKER, zkWatcher);
            zooKeeper.getData(ZK_WORKER, zkWatcher, null);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void setWatchWorkers(){
        try {
        List<String> workerList = zooKeeper.getChildren(ZK_WORKER, zkWatcher);
        for(String worker : workerList) {
            zooKeeper.exists(ZK_WORKER + "/" + worker, zkWatcher);
        }
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
                // set watch on workers
                setWatchWorkers();

                switch (type) {
                    case NodeDataChanged:
                        try {
                            String data = new String(zooKeeper.getData(path, zkWatcher , null ));
                            String status = data.split(":")[0];
                            String result = data.split(":")[1];
                            // connect with fileserver and get dict partition to work on

                            System.out.println(status + " result " + result);
                            if ( status.equals("found")){
                                finalResult.add(result);

                                // Stop all the workers!
                            }

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


    @Override
    public void run(){
        //ZMQ.Socket socket = context.socket(ZMQ.REP);
        System.out.println("Going in");
        //socket.connect("worker");

        while (true){
            // wait for client req then respond
            JobPacket packetFromServer = (JobPacket) SerializationUtils.deserialize(socket.recv(0));
            System.out.println("From client" + packetFromServer.type);
            eventBus.post(packetFromServer);

        }

    }
    @Subscribe
    public void handleJob(JobPacket jobPacket) throws Exception{
        JobPacket packetToClient = new JobPacket();
        if (jobPacket.type == JobPacket.JOB_REQ){
            new Thread(this.manageWorker(jobPacket.hash)).start();
            packetToClient.type = JobPacket.JOB_ACCEPTED;
            packetToClient.result = "none";

        }
        if (jobPacket.type == JobPacket.JOB_STATUS){

            if ( finalResult.isEmpty()) {
                System.out.println("Job in progress, please wait!");
                packetToClient.type = JobPacket.JOB_PROGRESS;
                packetToClient.result = "none";
            }
            else {
                System.out.println("Result found!");
                packetToClient.type = JobPacket.JOB_RESULT;
                packetToClient.result = finalResult.remove(0);
            }
        }
        socket.send(SerializationUtils.serialize(packetToClient),0);

    }
    public Runnable manageWorker(final String hash) {

        return new Runnable() {

            @Override
            public void run(){
                Random randomGen = new Random( 9834 );
                List<Integer> partIdList = new ArrayList<Integer>(ARRAY_SIZE);
                for (int i = 0; i < ARRAY_SIZE; i++){
                    partIdList.add(i,i);
                }

                try{
                    List<String> workerList = zooKeeper.getChildren(ZK_WORKER, zkWatcher);
                    System.out.println("Connecting with worker and sending hash :" + hash + "worker list" + workerList + "partID" + partIdList);

                    while(!partIdList.isEmpty()){
                        for (String worker : workerList){
                            int n = partIdList.size();
                            if (n==0){
                                break;
                            }
                            System.out.println("worker and size"  + worker + "  n= " +n);
                            Thread.sleep(100);
                            zooKeeper.setData(Joiner.on("/").join(ZK_WORKER,worker),
                                    Joiner.on(":").join(hash, partIdList.remove(randomGen.nextInt(n))).getBytes(),
                                    -1);
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        };
    }

    public static void main (String[] args){
        int myPort = 0;
        String myID = null;
        if (args.length == 4){

            try{
                zooHost = args[0];
                zooPort = Integer.parseInt(args[1]);
                myPort = Integer.parseInt(args[2]);
                myID = args[3];

            } catch (Exception e){
                e.printStackTrace();
            }

        }
        else {
            System.err.println("Usage tracker [zooHost] [zooPort] [myPort] [myID]");
            System.exit(-1);
        }
        // initialize ZMQ
        ZMQ.Context context = ZMQ.context(1);
        ZMQ.Socket clients = context.socket(ZMQ.REP);
        clients.bind ("tcp://*:"+ myPort);


            eventBus = new EventBus("Tracker");
            Thread t = new JobTracker(myPort, myID, context, clients);
            eventBus.register(t);
            System.out.println("Starting thread");
            t.start();
        //}
    }
}
