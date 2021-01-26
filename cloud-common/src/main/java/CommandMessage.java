import java.time.LocalDateTime;

public class CommandMessage<T> implements ExchangeMessage{
    private final String command;
    private final String param;
    private final LocalDateTime createAt;
    private T result;

    public CommandMessage (String command, String param) {
        this.command = command;
        this.param = param;
        this.createAt = LocalDateTime.now();
    }

    public CommandMessage(String command) {
        this(command,null);
    }

    public T getResult() {return result;}

    public String getCommand() {return command;}

    @Override
    public String toString() {
        return String.format("%s %s : %s",command,param,result.toString());
    }

    public String getParam() { return param;}

    public void setResult(T result) {
        this.result = result;
    }
}
