package Project.Common;

import java.util.HashMap;

import Project.Exceptions.CustomIT114Exception;
import Project.Exceptions.DuplicateRoomException;
import Project.Exceptions.RoomNotFoundException;
import Project.Common.User;
import Project.Common.TextFX;
import Project.Common.RoomAction;
import Project.Common.PayloadType;
import Project.Common.Payload;
import Project.Common.Constants;
import Project.Common.ConnectionPayload;
import Project.Common.Command;


public enum Command {
    QUIT("quit"),
    DISCONNECT("disconnect"),
    LOGOUT("logout"),
    LOGOFF("logoff"),
    REVERSE("reverse"),
    CREATE_ROOM("createroom"),
    LEAVE_ROOM("leaveroom"),
    JOIN_ROOM("joinroom"),
    NAME("name"),
    LIST_USERS("users");

    private static final HashMap<String, Command> BY_COMMAND = new HashMap<>();
    static {
        for (Command e : values()) {
            BY_COMMAND.put(e.command, e);
        }
    }
    public final String command;

    private Command(String command) {
        this.command = command;
    }

    public static Command stringToCommand(String command) {
        return BY_COMMAND.get(command);
    }
}