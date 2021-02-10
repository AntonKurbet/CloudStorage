package messages;

import tools.ServerCommand;

import java.util.List;

public class ListCommandMessage extends CommandMessage {
    private List<String> result;

    public ListCommandMessage(ServerCommand command) {
        super(command);
    }

    public List<String> getResult() {
        return result;
    }

    public void setResult(List<String> result) {
        this.result = result;
    }
}
