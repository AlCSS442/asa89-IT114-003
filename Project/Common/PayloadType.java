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


public enum PayloadType {
    CLIENT_CONNECT, // client requesting to connect to server (passing of initialization data
                    // [name])
    CLIENT_ID, // server sending client id
    SYNC_CLIENT, // silent syncing of clients in room
    DISCONNECT, // distinct disconnect action
    ROOM_CREATE,
    ROOM_JOIN,
    ROOM_LEAVE,
    REVERSE,
    MESSAGE // sender and message
}