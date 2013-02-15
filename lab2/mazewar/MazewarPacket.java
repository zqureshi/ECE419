/**
 * Created with IntelliJ IDEA.
 * User: jaideep
 * Date: 2/15/13
 * Time: 1:14 AM
 * To change this template use File | Settings | File Templates.
 */
import java.io.Serializable;

public class MazewarPacket implements Serializable {
    /* define constants */

    public static final int MAZE_NULL    = 0;

    public static final int MAZE_REQUEST    = 101;
    public static final int MAZE_REPLY = 102;
    public static final int MAZE_RECEIVED    = 103;
    public static final int MAZE_EXECUTE    = 104;

    /* message header */
    public int type = MazewarPacket.MAZE_NULL;


    public String ClientName;
    public String Event;

 }