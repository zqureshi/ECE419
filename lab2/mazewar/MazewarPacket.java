/**
 * Created with IntelliJ IDEA.
 * User: jaideep
 * Date: 2/15/13
 * Time: 1:14 AM
 * To change this template use File | Settings | File Templates.
 */
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MazewarPacket implements Serializable {
    /* define constants */

    public static final int MAZE_NULL    = 0;

    public static final int MAZE_REQUEST    = 101;
    public static final int MAZE_EXECUTE    = 104;
    public static final int MAZE_REGISTER    = 105;
    public static final int MAZE_NEW   = 106;
    public static final int MAZE_REMOVE = 107;
    public static final int MAZE_RAND = 108;



    public static final int MAZE_BYE   = 200;

    public static final int MAZE_ERROR   = 300;
    /* message header */
    public int type = MazewarPacket.MAZE_NULL;

    /* Local client information */
    public String ClientName;

    List packetClientList = Collections.synchronizedList(new LinkedList<String>());

    public int rand;


    public String Event;

 }