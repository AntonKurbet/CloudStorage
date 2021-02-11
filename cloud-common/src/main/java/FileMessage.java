import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

public class FileMessage implements ExchangeMessage {

    private static final Logger LOG = LoggerFactory.getLogger(FileMessage.class);

    private final String name;
    private final byte[] data;
    private boolean end;
    private final LocalDateTime createAt;

    public FileMessage(Path path) throws IOException {
        name = path.getFileName().toString();
        data = Files.readAllBytes(path);
        createAt = LocalDateTime.now();
    }

    public FileMessage(String name, byte[] data, LocalDateTime createAt, boolean end) {
        this.name = name;
        this.data = data;
        this.createAt = createAt;
        this.end = end;
    }

    public static void sendByStream(Path path, int partLength, ObjectEncoderOutputStream writer ) throws IOException {
        String name = path.getFileName().toString();
        boolean overwrite = true;
        LOG.info(String.format("Sending %s",name));
        try (InputStream is = new FileInputStream(path.toString())) {
            LocalDateTime dt = LocalDateTime.now();
            int read;
            byte[] buffer = new byte[partLength];

            while (true) {
                read = is.read(buffer);
                if (read == -1) break;
                byte[] data = new byte[read];
                System.arraycopy(buffer, 0, data, 0, read);
                writer.writeObject( new FileMessage(name, data, dt, overwrite));
                LOG.info(String.format("Sent %d bytes",read));
                overwrite = false;
            }
        }
    }

    public void writeObject(String dst) throws IOException {
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(dst));
        os.writeObject(this);
        os.close();
    }

    public void writeData(String dst) throws IOException {
        OutputStream os = new FileOutputStream(dst);
        os.write(this.data);
        os.close();
    }

    public void writeData(Path dst, boolean append) throws IOException {
        OutputStream os = new FileOutputStream(dst.toString(), append);
        os.write(this.data);
        os.close();
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public boolean getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "FileMessage{" +
                "name='" + name + '\'' +
                ", data=" + Arrays.toString(data) +
                ", createAt=" + createAt +
                '}';
    }
}
