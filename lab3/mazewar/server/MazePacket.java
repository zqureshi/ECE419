package mazewar.server;

import com.google.common.base.Optional;

import java.io.Serializable;

public class MazePacket implements Serializable {
    public static enum PacketType {
        ERROR,
        CONNECT,
        DISCONNECT,
        CLIENTS,
        ACTION
    }

    public static enum PacketErrorCode {
        CLIENT_EXISTS("Client already exists"),
        GAME_STARTED("Game already started!");

        String message;

        PacketErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static enum ClientAction {
        FORWARD,
        BACKUP,
        LEFT,
        RIGHT,
        FIRE
    }

    public PacketType type;

    /* Sequence number to be set on all operations by server */
    public Integer sequenceNumber;

    /* When an ERROR occurs */
    public Optional<PacketErrorCode> error;

    /* For CONNECT, DISCONNECT and ACTION */
    public Optional<String> clientId;

    /* For CLIENTS */
    public Optional<String[]> clients;
    public Optional<Long> seed;

    /* For ACTION */
    public Optional<ClientAction> action;
}
