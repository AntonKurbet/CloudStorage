import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

public class FileMessage implements ExchangeMessage {

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

    public static ArrayList<FileMessage> GenerateSequence(Path path, int partLength)
            throws IOException {
        ArrayList<FileMessage> result = new ArrayList<>();

            String name = path.getFileName().toString();
            InputStream is = new FileInputStream(name);
            LocalDateTime dt = LocalDateTime.now();
            int read;
            byte[] buffer = new byte[partLength];

            while (true) {
                read = is.read(buffer);
                if (read == -1) break;
                byte[] data = new byte[read];
                System.arraycopy(buffer,0,data,0,read);
                result.add(new FileMessage(name,data,dt,false));
            }
            result.get(result.size() - 1).end = true;
            is.close();
            return result;
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
