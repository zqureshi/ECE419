import java.util.HashMap;

public final class BrokerConfig {
    public static final HashMap<Integer, String> errorMessages = new HashMap<Integer, String>();
    public static final HashMap<Integer, String> messages = new HashMap<Integer, String>();

    public static enum Exchanges {
        NASDAQ,
        TSE;
    }

    static {
        errorMessages.put(BrokerPacket.ERROR_INVALID_SYMBOL, "Invalid symbol");
        errorMessages.put(BrokerPacket.ERROR_OUT_OF_RANGE, "Quote out of range");
        errorMessages.put(BrokerPacket.ERROR_SYMBOL_EXISTS, "Symbol exists");

        messages.put(BrokerPacket.EXCHANGE_ADD, "Added");
        messages.put(BrokerPacket.EXCHANGE_UPDATE, "Updated");
        messages.put(BrokerPacket.EXCHANGE_REMOVE, "Removed");
    }
}
