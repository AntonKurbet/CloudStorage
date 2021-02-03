package messages;

import tools.ServerCommand;

public class RequestFileMessage extends CommandMessage {

    public RequestFileMessage(ServerCommand cmd, String fileName) {
        super(cmd,fileName);
    }
}
