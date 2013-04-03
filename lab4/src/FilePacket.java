import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jaideepbajwa
 * Date: 2013-04-03
 * Time: 3:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class FilePacket implements Serializable {
    public static final int FILE_NULL = 0;
    public static final int FILE_REQ = 100;
    public static final int FILE_RESULT = 200;
    public static final int FILE_ERROR = 300;
    public static final int FILE_BYE = 301;

    public int type = FilePacket.FILE_NULL;

    public int id = 0;
    public ArrayList<String> result = new ArrayList<String>();

}
