import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FileInfo implements Serializable {
    public enum FileType {
        FILE("F"), DIR("D");
        private String name;

        public String getName() {
            return name;
        }

        FileType(String name) {
            this.name = name;
        }
    }

    private String name;
    private FileType type;
    private long size;
    private LocalDateTime modified;

    public FileInfo(Path path) {
        try {
            this.name = path.getFileName().toString();
            this.type = Files.isDirectory(path) ? FileType.DIR : FileType.FILE;
            this.size = type == FileType.FILE ? Files.size(path) : -1L;
            this.modified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(0));
        } catch (IOException e) {
            throw new RuntimeException("Can't create file info");
        }
    }

    public String getName() {
        return name;
    }

    public FileType getType() {
        return type;
    }

    public long getSize() {
        return size;
    }

    public LocalDateTime getModified() {
        return modified;
    }
}
