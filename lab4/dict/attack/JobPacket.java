package dict.attack;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: jaideepbajwa
 * Date: 2013-04-01
 * Time: 5:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class JobPacket implements Serializable {
    public static final int JOB_NULL = 0;
    public static final int JOB_REQ = 100;
    public static final int JOB_STATUS = 101;
    public static final int JOB_RESULT = 200;
    public static final int JOB_PROGRESS = 201;
    public static final int JOB_NOTFOUND = 203;
    public static final int JOB_ACCEPTED = 202;
    public static final int JOB_ERROR = 300;
    public static final int JOB_BYE = 301;

    public int type = JobPacket.JOB_NULL;

    public String hash = null;
    public String result = null;

}
