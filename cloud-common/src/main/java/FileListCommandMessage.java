import java.util.List;

public class FileListCommandMessage extends CommandMessage<List<String>> {
    FileListCommandMessage(ServerCommand command) {
        super(command,null);
    }
}
