import java.time.LocalDateTime;

public class CommandMessage implements ExchangeMessage{
    private final ServerCommand command;
    private final String param;
    private final LocalDateTime createAt;

    public CommandMessage (ServerCommand command, String param) {
        this.command = command;
        this.param = param;
        this.createAt = LocalDateTime.now();
    }

    public CommandMessage(ServerCommand command) {
        this(command,null);
    }

    public ServerCommand getCommand() {return command;}

    @Override
    public String toString() {
        return String.format("%s %s",command,param);
    }

    public String getParam() { return param;}
}
