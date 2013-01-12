import java.io.Serializable;
 /**
 * BrokerPacket
 * ============
 * 
 * Packet format of the packets exchanged between the Broker and the Client
 * 
 */


/* inline class to describe host/port combo */
class BrokerLocation implements Serializable {
	public String  broker_host;
	public Integer broker_port;
	
	/* constructor */
	public BrokerLocation(String host, Integer port) {
		this.broker_host = host;
		this.broker_port = port;
	}
	
	/* printable output */
	public String toString() {
		return " HOST: " + broker_host + " PORT: " + broker_port; 
	}
	
}

public class BrokerPacket implements Serializable {

	/* define constants */
	/* for part 1/2/3 */
	public static final int BROKER_NULL    = 0;
	public static final int BROKER_REQUEST = 101;
	public static final int BROKER_QUOTE   = 102;
	public static final int BROKER_ERROR   = 103;
	public static final int BROKER_FORWARD = 104;
	public static final int BROKER_BYE     = 199;
	
	/* for part 2/3 */
	public static final int EXCHANGE_ADD    = 201;
	public static final int EXCHANGE_UPDATE = 202;
	public static final int EXCHANGE_REMOVE = 203;
	public static final int EXCHANGE_REPLY  = 204;
	
	
	/* for part 3 */
	public static final int LOOKUP_REQUEST  = 301;
	public static final int LOOKUP_REPLY    = 302;
	public static final int LOOKUP_REGISTER = 303;
	
	/* error codes */
	/* for part 2/3 */
	public static final int ERROR_INVALID_SYMBOL   = -101;
	public static final int ERROR_OUT_OF_RANGE     = -102;
	public static final int ERROR_SYMBOL_EXISTS    = -103;
	public static final int ERROR_INVALID_EXCHANGE = -104;
	
	/* message header */
	/* for part 1/2/3 */
	public int type = BrokerPacket.BROKER_NULL;
	
	/* request quote */
	/* for part 1/2/3 */
	public String symbol;
	
	/* quote */
	/* for part 1/2/3 */
	public Long quote;
	
	/* report errors */
	/* for part 2/3 */
	public int error_code;
	
	/* exchange lookup */
	/* for part 3 */
	public String         exchange;
	public int            num_locations;
	public BrokerLocation locations[];
	
}
