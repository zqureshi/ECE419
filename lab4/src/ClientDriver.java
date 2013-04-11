
import org.apache.commons.lang.SerializationUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import org.zeromq.ZMQ;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 * User: jaideepbajwa
 * Date: 2013-03-31
 * Time: 11:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClientDriver  {
    private static EventBus eventBus;

    private static String zooHost;
    private static int zooPort;
    private static ZooKeeper zooKeeper;
    private static ZkWatcher zkWatcher;
    private static CountDownLatch zkConnected;
    private static final int ZK_TIMEOUT = 5000;
    private static String ZK_TRACKER = "/tracker";
    private static CountDownLatch nodeCreatedSignal = new CountDownLatch(1);

    /* ZeroMQ */
    private static ZMQ.Context context;
    private static ZMQ.Socket socket;

    public ClientDriver(){

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

            // Successfully connected, now get tracker information from /tracker
            // wait until tracker information is provide in zookeeper

            Stat stat = zooKeeper.exists( ZK_TRACKER, zkWatcher);
            if (stat != null ){
                nodeCreatedSignal.countDown();
            }


            System.out.println("Waiting for tracker to be connected ...");

            nodeCreatedSignal.await();

            System.out.println("Tracker Connected!");

            // setup connection with tracker

            // initialize ZMQ
            context = ZMQ.context(1);

            // setup socket with zmq
            setSocket(new String(zooKeeper.getData(ZK_TRACKER,false,null)));

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Subscribe
    public void handleJobPacket(JobPacket jobPacket){
        // send packet to tracker
        System.out.println("To tracker " + jobPacket.type);
        socket.send(SerializationUtils.serialize(jobPacket),0);

        // reply
        JobPacket packetFromServer = (JobPacket) SerializationUtils.deserialize(socket.recv(0));
        System.out.println("Packet from tracker "+ packetFromServer.type);

        if (packetFromServer.type == JobPacket.JOB_RESULT){
            String result = packetFromServer.result;
            if (result == null){
                System.out.println("Password doesn't exists!! ");
            }
            else {
                System.out.println("Result Found: " + result);
            }
        }
        if (packetFromServer.type == JobPacket.JOB_PROGRESS){
            System.out.println("Job in progress, please wait!");
        }
        if (packetFromServer.type == JobPacket.JOB_NOTFOUND){
            System.out.println("No such Job, please enter your job again!");
        }
        if (packetFromServer.type == JobPacket.JOB_ACCEPTED){
            System.out.println("Job accepted! Please check status in a bit!");
        }
        System.out.print("> ");

    }


    private void setSocket (String jobtrackerId){
        // setup socket with zmq
        System.out.println("re-set connection!");
        socket = context.socket(ZMQ.REQ);
        socket.connect("tcp://"+ jobtrackerId);
        System.out.print("> ");

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
                        if (path.equals(ZK_TRACKER)){
                            try{
                                setSocket(new String(zooKeeper.getData(ZK_TRACKER,false,null)));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                    break;

                case NodeDeleted:

                    break;

                case NodeCreated:
                    // verify if this is the defined znode
                    try {
                        boolean isMyPath = event.getPath().equals(ZK_TRACKER);
                        if (isMyPath) {
                            System.out.println(ZK_TRACKER + " created!");

                            nodeCreatedSignal.countDown();
                        }
                    } catch ( Exception e) {
                        e.printStackTrace();
                    }
            }
        }
    }

    public static void main (String[] args){

        if (args.length == 2){

            try{
                zooHost = args[0];
                zooPort = Integer.parseInt(args[1]);

            } catch (Exception e){
                e.printStackTrace();
            }

        }
        else {
            System.err.println("Usage client [zooHost] [zooPort]");
            System.exit(-1);
        }

        eventBus = new EventBus("Client");
        ClientDriver c = new ClientDriver();
        eventBus.register(c);

        // prompt user to input job
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Usage: {job [password hash]|status|quit }");
        System.out.print("> ");
        String userInput = null;

        try{
            while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("quit") == -1){

                if (!userInput.split(" ")[0].equals("job") && ! userInput.split(" ")[0].equals("status")){
                    System.out.println("Usage: {job [password hash]|[status hash] |quit }");
                    System.out.print("> ");
                    continue;
                }
                if (userInput.split(" ")[0].equals("job")){
                    String hash = userInput.split(" ")[1];
                    System.out.println("Hash =" + hash);
                    JobPacket jobPacket = new JobPacket();
                    jobPacket.type = JobPacket.JOB_REQ;
                    jobPacket.hash = hash;
                    eventBus.post(jobPacket);
                }
                if (userInput.split(" ")[0].equals("status")){
                    String hash = userInput.split(" ")[1];
                    System.out.println("Checking status");
                    JobPacket jobPacket = new JobPacket();
                    jobPacket.type = JobPacket.JOB_STATUS;
                    jobPacket.hash = hash;
                    eventBus.post(jobPacket);
                }

            }
            stdIn.close();
            System.out.println("Exiting!");
            System.exit(0);
        } catch ( Exception e){
            e.printStackTrace();
        }

    }
}
