package mazewar.server;

import com.google.common.base.Optional;

import java.io.Serializable;

enum PacketType {
    ERROR,
    CONNECT,
    DISCONNECT,
    ACTION
}

enum PacketErrorCode {
    USER_CONNECTED
}

enum PacketAction {
    FORWARD,
    BACKUP,
    LEFT,
    RIGHT,
    FIRE
}

public class MazePacket implements Serializable {
    public PacketType type;

    /* When an ERROR occurs */
    public Optional<PacketErrorCode> error;

    /* For CONNECT and ACTION packet */
    public Optional<String> clientId;

    /* For Client ACTION */
    public Optional<PacketAction> action;
}
