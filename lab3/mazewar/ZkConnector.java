/**
 * Created with IntelliJ IDEA.
 * User: jaideepbajwa
 * Date: 2013-03-18
 * Time: 6:11 PM
 * To change this template use File | Settings | File Templates.
 */
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.Watcher.Event.EventType;

import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.io.IOException;

public class ZkConnector implements Watcher{
    // ZooKeeper Object
    ZooKeeper zooKeeper;

    // To block any operation until ZooKeeper is connected. It's initialized
    // with count 1, that is, ZooKeeper connect state.
    CountDownLatch connectedSignal = new CountDownLatch(1);

    // ACL, set to Completely Open
    protected static final List<ACL> acl = Ids.OPEN_ACL_UNSAFE;

    /**
     * Connects to ZooKeeper servers specified by hosts.
     */
    public void connect(String hosts) throws IOException, InterruptedException {

        zooKeeper = new ZooKeeper(
                hosts, // ZooKeeper service hosts
                5000,  // Session timeout in milliseconds
                this); // watcher - see process method for callbacks
        connectedSignal.await();
    }

    /**
     * Closes connection with ZooKeeper
     */
    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    /**
     * @return the zooKeeper
     */
    public ZooKeeper getZooKeeper() {
        // Verify ZooKeeper's validity
        if (null == zooKeeper || !zooKeeper.getState().equals(States.CONNECTED)) {
            throw new IllegalStateException ("ZooKeeper is not connected.");
        }
        return zooKeeper;
    }

    public Watcher getWatcher() {
        // Verify ZooKeeper's validity
        return this;
    }

    protected Stat exists(String path, Watcher watch) {

        Stat stat =null;
        try {
            stat = zooKeeper.exists(path, watch);
        } catch(Exception e) {
        }

        return stat;
    }

    protected KeeperException.Code create(String path, String data, CreateMode mode) {
        try {
            byte[] byteData = null;
            if(data != null) {
                byteData = data.getBytes();
            }
            zooKeeper.create(path, byteData, acl, mode);

        } catch(KeeperException e) {
            return e.code();
        } catch(Exception e) {
            return KeeperException.Code.SYSTEMERROR;
        }

        return KeeperException.Code.OK;
    }

    public void process(WatchedEvent event) {
        // release lock if ZooKeeper is connected.

        EventType type = event.getType();
        String path = event.getPath();
        System.out.println("Path:" + path + ", Event type:" + type);

        if (event.getState() == KeeperState.SyncConnected) {
            connectedSignal.countDown();
        }
        if(type == EventType.NodeChildrenChanged){
            System.out.println("In zkconnet with path " + path + "and Event " + event);
            if (path != null){
                Mazewar.nodeCreated(path);
            }
        }
        if (type == EventType.NodeDeleted){
            System.out.println("deleting " + path);
            Mazewar.nodeDeleted(path);
        }
        try{
            if (path != null){
                //re-set data watch
                zooKeeper.exists(path, true);
                // re-set child watch
                zooKeeper.getChildren("/root", true);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}