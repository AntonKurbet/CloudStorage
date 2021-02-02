import java.util.List;

public class BoolCommandMessage extends CommandMessage {
    private boolean result;

    public BoolCommandMessage(ServerCommand command) {
        super(command);
    }

    public BoolCommandMessage(ServerCommand command, String params) {
        super(command, params);
    }

    public boolean isResult() {
        return result;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
}
