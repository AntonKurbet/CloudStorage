import java.util.List;

public class FileListCommandMessage extends CommandMessage<List<FileInfo>> {
    FileListCommandMessage(ServerCommand command) {
        super(command,null);
    }
}
