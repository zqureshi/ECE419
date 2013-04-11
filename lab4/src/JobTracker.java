import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import org.apache.commons.lang.SerializationUtils;
import org.apache.zookeeper.*;
import org.zeromq.ZMQ;

import java.net.InetAddress;
import java.util.*;
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
    private static final int ARRAY_SIZE = 100;
    private static String ZK_TRACKER = "/tracker";
    private static String ZK_WORKER = "/worker";
    private static String ZK_JOBS = "/jobs";
    private static String ZK_RESULT = "/result";
    private static String zooHost;
    private static int zooPort;
    private static int myPort =0;
    private static Random randGen = new Random(897);
    private static ArrayBlockingQueue<String> jobQueue = new ArrayBlockingQueue<String>(100);

    private static Gson gson = new Gson();

    /* ZeroMQ */
    private static ZMQ.Context context;
    private static ZMQ.Socket socket;

    public JobTracker(String myID) {

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

            // Create /jobs if it doesn't exists
            if (zooKeeper.exists(ZK_JOBS, false) == null){
                zooKeeper.create(ZK_JOBS,
                        null,
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
            }

            // Create /result if it doesn't exists, to stores already processed jobs with results
            if (zooKeeper.exists(ZK_RESULT, false) == null){
                zooKeeper.create(ZK_RESULT,
                        null,
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
            }


            // if /tracker does not exists, create one
            if (zooKeeper.exists(ZK_TRACKER, false) == null){
                zooKeeper.create(ZK_TRACKER,
                        Joiner.on(":").join(InetAddress.getLocalHost().getHostAddress(), myPort).getBytes(),
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
                if (zooKeeper.getChildren(ZK_TRACKER, zkWatcher, null).isEmpty()){
                    // create myself as leader and update data of /tracker
                    zooKeeper.setData(ZK_TRACKER,
                            Joiner.on(":").join(InetAddress.getLocalHost().getHostAddress(), myPort).getBytes(),
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
                    // set watch on "primary"
                    zooKeeper.exists(Joiner.on("/").join(ZK_TRACKER,zooKeeper.getChildren(ZK_TRACKER, zkWatcher).get(0)), zkWatcher);
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

                    case NodeDeleted:
                        try{
                            // Check if node deleted is from /tracker
                            if (path.contains(ZK_TRACKER)){
                                String node = zooKeeper.getChildren(ZK_TRACKER, false).get(0);
                                // If primary is dead then I become primary
                                if ( !zooKeeper.getData(Joiner.on("/").join(ZK_TRACKER, node ), false, null).equals("primary")){
                                    zooKeeper.setData(Joiner.on("/").join(ZK_TRACKER, node ), "primary".getBytes(), -1);
                                    zooKeeper.setData(ZK_TRACKER,
                                            Joiner.on(":").join(InetAddress.getLocalHost().getHostAddress(), myPort).getBytes(),
                                            -1
                                    );
                                }
                            }
                            if (path.contains(ZK_WORKER)){
                                List<String> currJobs = zooKeeper.getChildren(ZK_JOBS, false);
                                // /worker/<id>
                                String workerId = path.split("/")[2];
                                System.out.println("Dead worker id " + workerId);
                                for ( String job : currJobs ){

                                    while (true){
                                        String currData = new String(zooKeeper.getData(Joiner.on("/").join(ZK_JOBS, job), false, null));
                                        int currVersion = zooKeeper.exists(Joiner.on("/").join(ZK_JOBS, job), false).getVersion();
                                        System.out.println("Version curr" + currVersion);

                                        // de-serialize
                                        WorkerInfo workerInfo = gson.fromJson(currData, WorkerInfo.class);
                                        HashMap<String, List<Integer>> newMap = workerInfo.getWorkerInfo();
                                        List<Integer> deadWorkerList = newMap.get(workerId);
                                        System.out.println("dead worker List" +deadWorkerList);
                                        if (deadWorkerList == null){
                                            break;
                                        }
                                        // remove it from current info as its dead
                                        newMap.remove(workerId);

                                        List<String> currWorker = zooKeeper.getChildren(ZK_WORKER, false);

                                        // Pick a worker ramdomly and assign the task to it by adding to its current task list
                                        int rand = randGen.nextInt(currWorker.size());
                                        List<Integer> newWorkerList = newMap.get(currWorker.get(rand));
                                        if (newWorkerList == null)
                                            newWorkerList = deadWorkerList;
                                        else
                                            newWorkerList.addAll(deadWorkerList);
                                        newMap.put(currWorker.get(rand), newWorkerList);

                                        WorkerInfo newWorkerInfo = new WorkerInfo(newMap, job);

                                        // serialize
                                        String newData = gson.toJson(newWorkerInfo);

                                        // setdata on the znode /jobs/<hash>

                                        try {
                                            if ( zooKeeper.setData(Joiner.on("/").join(ZK_JOBS, job), newData.getBytes() , currVersion) != null) {
                                                System.out.println("Update data for " + workerId + " on to " + currWorker.get(rand));
                                                break;
                                            }
                                        } catch (KeeperException e){
                                            // Ignore
                                        }

                                    }

                                }
                            }

                        } catch ( Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
    }

    @Subscribe
    public void handleJob(JobPacket jobPacket) throws Exception{
        JobPacket packetToClient = new JobPacket();
        if (jobPacket.type == JobPacket.JOB_REQ){
            jobQueue.add(jobPacket.hash);
            packetToClient.type = JobPacket.JOB_ACCEPTED;
            packetToClient.result = "none";

        }
        if (jobPacket.type == JobPacket.JOB_STATUS){

            // check under /result/<hash>
            try {
                if ( (zooKeeper.exists(Joiner.on("/").join(ZK_JOBS, jobPacket.hash), false) != null) && (zooKeeper.exists(Joiner.on("/").join(ZK_RESULT, jobPacket.hash), false) == null )){
                    System.out.println("Job in progress, please wait!");
                    packetToClient.type = JobPacket.JOB_PROGRESS;
                    packetToClient.result = "none";
                }
                if ( (zooKeeper.exists(Joiner.on("/").join(ZK_JOBS, jobPacket.hash), false) == null) && (zooKeeper.exists(Joiner.on("/").join(ZK_RESULT, jobPacket.hash), false) == null)){
                    System.out.println("No such Job, please enter your job again!");
                    packetToClient.type = JobPacket.JOB_NOTFOUND;
                    packetToClient.result = "none";
                }
                if (zooKeeper.exists(Joiner.on("/").join(ZK_RESULT, jobPacket.hash), false) != null) {
                    byte[] data = zooKeeper.getData(Joiner.on("/").join(ZK_RESULT, jobPacket.hash), false, null);
                    packetToClient.type = JobPacket.JOB_RESULT;
                    if ( data == null) {
                        System.out.println("Result not found!");
                        packetToClient.result = null;
                    }
                    else {
                        String result = new String(data);
                        packetToClient.result = result;
                        System.out.println("Result found!");
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        socket.send(SerializationUtils.serialize(packetToClient),0);

    }
    public Runnable manageWorker() {

        return new Runnable() {

            @Override
            public void run(){

                while (true){
                    try{
                        String hash = jobQueue.take();
                        List<Integer> partIdList = new ArrayList<Integer>(ARRAY_SIZE);
                        HashMap<String, List<Integer>> workerIds = new HashMap<String, List<Integer>>();
                        for (int i = 0; i < ARRAY_SIZE; i++){
                            partIdList.add(i,i);
                        }

                        List<String> workerList = zooKeeper.getChildren(ZK_WORKER, zkWatcher);

                        // Create n sub lists, where n = number of workers
                        List<List<Integer>> subPartId = Lists.partition(partIdList, (int) Math.ceil((float)ARRAY_SIZE / workerList.size()));
                        System.out.println("Connecting with worker and sending hash :" + hash + "worker list" + workerList + "partID" + subPartId);

                        int i = 0;
                        for ( List<Integer> subList : subPartId){
                            System.out.println(subList);
                            workerIds.put(workerList.get(i), subList);
                            i++;
                        }

                        WorkerInfo workerInfo = new WorkerInfo(workerIds, hash);
                        // Now store this in /jobs/<hash>
                        // Serialize into json

                        String workerInfoJson = gson.toJson(workerInfo);

                        // Create /jobs/<hash>
                        zooKeeper.create(Joiner.on("/").join(ZK_JOBS, hash),
                                workerInfoJson.getBytes(),
                                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                CreateMode.PERSISTENT
                        );
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }
        };
    }

    public static void main (String[] args){
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
        context = ZMQ.context(1);
        socket = context.socket(ZMQ.REP);
        socket.bind ("tcp://*:"+ myPort);

        eventBus = new EventBus("Tracker");
        JobTracker t = new JobTracker(myID);
        eventBus.register(t);
        System.out.println("Starting thread");
        new Thread(t.manageWorker()).start();

        while (true){
            // wait for client req then respond
            JobPacket packetFromServer = (JobPacket) SerializationUtils.deserialize(socket.recv(0));
            System.out.println("From client" + packetFromServer.type);
            eventBus.post(packetFromServer);
        }
    }
}
