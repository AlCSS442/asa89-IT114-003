
package Project.Common;

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


public abstract class Constants {
    final public static String COMMAND_TRIGGER = "/";
    final public static String SINGLE_SPACE = " ";
    final public static long DEFAULT_CLIENT_ID = -1;
}
