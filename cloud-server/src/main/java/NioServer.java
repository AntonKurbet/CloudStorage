import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class NioServer {

    private final ServerSocketChannel serverChannel = ServerSocketChannel.open();
    private final Selector selector = Selector.open();
    private final ByteBuffer buffer = ByteBuffer.allocate(5);
    private Path serverPath = Paths.get("./").toAbsolutePath().normalize();
    private Path newPath = serverPath;

    public NioServer() throws IOException {
        serverChannel.bind(new InetSocketAddress(8189));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (serverChannel.isOpen()) {
            selector.select(); // block
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new NioServer();
//        Path serverPath = Paths.get("./");
//        System.out.println(serverPath); // .
//        System.out.println(serverPath.toAbsolutePath().normalize()); // C:\Users\Admin\IdeaProjects\CloudStorage
//        Path newPath = serverPath.resolve("src");
//        System.out.println(newPath.toAbsolutePath().normalize()); // C:\Users\Admin\IdeaProjects\CloudStorage\src
//        System.out.println(serverPath.relativize(newPath)); // src
    }

    //HW2 Взять с урока НИО сервак и поддержать команды пользователя:
    // cd path
    // ls +
    // cat filePath
    // touch fileName, mkdir dirName

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        int read = 0;
        StringBuilder msg = new StringBuilder();
        while ((read = channel.read(buffer)) > 0) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                msg.append((char) buffer.get());
            }
            buffer.clear();
        }

        try {
            String[] command = msg.toString().replaceAll("[\n|\r]", "").split(" ");
            byte[] response;
            switch (command[0]) {
                case "ls":
                    response = doLs();
                    break;
                case "cd":
                    response = doCd(command[1]);
                    break;
                case "cat":
                    response = doCat(command[1]);
                    break;
                case "exit":
                    throw new IOException("EOT");
                case "touch":
                    response = doTouch(command[1]);
                    break;
                case "mkdir":
                    response = doMkdir(command[1]);
                    break;
                default:
                    response = Arrays.deepToString(command).getBytes(StandardCharsets.UTF_8);
            }
            channel.write(ByteBuffer.wrap(response));
            channel.write(ByteBuffer.wrap("\n\r".getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private byte[] doMkdir(String command) {
        Path tmpPath = newPath.resolve(command);
        try {
            Files.createDirectories(tmpPath);
            return toBytes(serverPath.relativize(tmpPath).toString());
        } catch (IOException e) {
            return toBytes("Error creating directory");
        }
    }

    private byte[] doTouch(String command) {
        Path tmpPath = newPath.resolve(command);
        try {
            if (!Files.exists(tmpPath))
                Files.createFile(tmpPath);
            else
                Files.setLastModifiedTime(tmpPath,FileTime.fromMillis(System.currentTimeMillis()));

            return toBytes(serverPath.relativize(tmpPath).toString());
        } catch (IOException e) {
            return toBytes("Error creating files");
        }
    }

    private byte[] getRelativePathBytes() {
        return getRelativePath().toString().getBytes(StandardCharsets.UTF_8);
    }

    private Path getRelativePath() {
        return serverPath.relativize(newPath);
    }

    private byte[] doCat(String command) throws IOException {
        Path tmpPath = newPath.resolve(command);
        if (Files.exists(tmpPath) && Files.isRegularFile(tmpPath)) {
            return Files.readAllBytes(tmpPath);
        } else {
            return toBytes(command + " not exists");
        }
    }

    private byte[] doCd(String command)  {
        Path tmpPath = newPath.resolve(command);
        if (Files.exists(tmpPath) && Files.isDirectory(tmpPath)) {
            if (tmpPath.toAbsolutePath().normalize().startsWith(serverPath)) {
                newPath = tmpPath.toAbsolutePath().normalize();
                return getRelativePathBytes();
            }
            else
                return toBytes("Cannot change directory");
        } else {
            return toBytes(command + " not exists");
        }
    }

    private byte[] toBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] doLs() throws IOException {
        String files = Files.list(newPath)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.joining("\n\r"));
        files += "\n\r";
        return toBytes(files);
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
    }
}
