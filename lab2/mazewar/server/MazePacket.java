package mazewar.server;

import com.google.common.base.Optional;

import java.io.Serializable;

public class MazePacket implements Serializable {
    public static enum PacketType {
        ERROR,
        CONNECT,
        DISCONNECT,
        NEWCLIENTS,
        ACTION
    }

    public static enum PacketErrorCode {
        CLIENT_EXISTS("Client already existis");

        String message;

        PacketErrorCode(String message) {
            this.message = message;
        }
    }

    public static enum PacketAction {
        FORWARD,
        BACKUP,
        LEFT,
        RIGHT,
        FIRE
    }

    public PacketType type;

    /* Sequence number to be set on all operations by server */
    public Optional<Integer> sequenceNumber;

    /* When an ERROR occurs */
    public Optional<PacketErrorCode> error;

    /* For CONNECT and ACTION packet */
    public Optional<String> clientId;

    /* For CONNECT */

    /* For ACTION */
    public Optional<PacketAction> action;
}
