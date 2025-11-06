package Project.Exceptions;

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



public abstract class CustomIT114Exception extends Exception {
    public CustomIT114Exception(String message) {
        super(message);
    }

    public CustomIT114Exception(String message, Throwable cause) {
        super(message, cause);
    }
}