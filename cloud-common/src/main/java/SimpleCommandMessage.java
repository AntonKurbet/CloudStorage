public class SimpleCommandMessage extends CommandMessage<String> {
    SimpleCommandMessage(ServerCommand command, String param) {
        super(command,param);
    }
}
