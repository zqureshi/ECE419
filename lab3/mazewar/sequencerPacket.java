import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: jaideep
 * Date: 3/21/13
 * Time: 5:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class sequencerPacket implements Serializable {

        /* define constants */
    public static final int S_NULL = 0;
    public static final int S_REQUEST = 101;
    public static final int S_REPLY = 104;
    public static final int S_ERROR   = 300;
    public static final int S_BYE   = 301;

    /* message header */
    public int type = sequencerPacket.S_NULL;

    /* Local client information */
    public String clientPath;
    public int seqNumber;
}

