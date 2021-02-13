import java.time.LocalDateTime;

abstract class CommandMessage<T> implements ExchangeMessage {
    private final ServerCommand command;
    private final String param;
    private final LocalDateTime createAt;
    private T result;

    public CommandMessage (ServerCommand command, String param) {
        this.command = command;
        this.param = param;
        this.createAt = LocalDateTime.now();
    }

    public CommandMessage(ServerCommand command) {
        this(command,null);
    }

    public T getResult() {return result;}

    public ServerCommand getCommand() {return command;}

    @Override
    public String toString() {
        return String.format("%s %s : %s",command,param,result.toString());
    }

    public String getParam() { return param;}

    public void setResult(T result) {
        this.result = result;
    }
}
