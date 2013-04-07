import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jaideepbajwa
 * Date: 2013-04-07
 * Time: 6:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class WorkerInfo {

    private HashMap<String, List<Integer>> workerInfo = new HashMap<String, List<Integer>>();
    private String hash  = null;

    WorkerInfo(HashMap<String, List<Integer>> workerInfo, String hash) {
        this.workerInfo = workerInfo;
        this.hash = hash;
    }

    HashMap<String, List<Integer>> getWorkerInfo() {
        return workerInfo;
    }

    String getHash() {
        return hash;
    }

}
