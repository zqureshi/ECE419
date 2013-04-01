import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 * User: jaideepbajwa
 * Date: 2013-03-31
 * Time: 11:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClientDriver {
    private static String zooHost;
    private static int zooPort;
    private static ZooKeeper zooKeeper;
    private static ZkWatcher zkWatcher;
    private static CountDownLatch zkConnected;
    private static final int ZK_TIMEOUT = 5000;
    private static String ZK_TRACKER = "/tracker";
    private static CountDownLatch nodeCreatedSignal = new CountDownLatch(1);


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
            try {
                Stat stat = zooKeeper.exists(
                        ZK_TRACKER,
                        new Watcher() {       // Anonymous Watcher
                            @Override
                            public void process(WatchedEvent event) {
                                // check for event type NodeCreated
                                boolean isNodeCreated = event.getType().equals(Event.EventType.NodeCreated);
                                // verify if this is the defined znode
                                boolean isMyPath = event.getPath().equals(ZK_TRACKER);
                                if (isNodeCreated && isMyPath) {
                                    System.out.println(ZK_TRACKER + " created!");
                                    nodeCreatedSignal.countDown();
                                }
                            }
                        });
                if (stat != null ){
                    nodeCreatedSignal.countDown();
                }
            } catch(KeeperException e) {
                System.out.println(e.code());
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }

            System.out.println("Waiting for tracker to be connected ...");
            try{
                nodeCreatedSignal.await();
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }

            System.out.println("Tracker Connected!");
            // prompt user to input job
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Usage: {job [password hash]|status|quit }");
            System.out.print("> ");
            String userInput = null;
            while ((userInput = stdIn.readLine()) != null && userInput.toLowerCase().indexOf("quit") == -1){

                if (!userInput.split(" ")[0].equals("job") && ! userInput.equals("status")){
                    System.out.println("Usage: {job [password hash]|status|quit }");
                    System.out.print("> ");
                    continue;
                }
                System.out.print("> ");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
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
            System.err.println("Invalid arguments!!");
            System.exit(-1);
        }

        new ClientDriver();
    }

    /* ZooKeeper Watcher */
    class ZkWatcher implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            Event.EventType type = event.getType();
            String path = event.getPath();
            System.out.println("Path: " + path + ", Event type:" + type);

            switch (type) {
                case NodeChildrenChanged:
                    try {


                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                    break;

                case NodeDeleted:
                    String name = path.substring(path.lastIndexOf('/') + 1, path.length() - 10);


                    break;
            }
        }
    }
}
