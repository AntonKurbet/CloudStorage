import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class NioServer {

  private final ServerSocketChannel serverChannel = ServerSocketChannel.open();
  private final Selector selector = Selector.open();
  private final ByteBuffer buffer = ByteBuffer.allocate(5);
  private Path serverPath = Paths.get("./");

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

    String[] command = msg.toString().replaceAll("[\n|\r]", "").split(" ");
    if (command[0].equals("ls")) {
      String files = Files.list(serverPath)
              .map(path -> path.getFileName().toString())
              .collect(Collectors.joining("\n\r"));
      files += "\n\r";
      channel.write(ByteBuffer.wrap(files.getBytes(StandardCharsets.UTF_8)));
      }
    else if (command[0].equals("cd")) {
      Path newPath = serverPath.resolve(command[1]);
      if (Files.exists(newPath)) {
        serverPath = newPath;
        channel.write(ByteBuffer.wrap(serverPath.toString().getBytes(StandardCharsets.UTF_8)));
      } else {
        channel.write(ByteBuffer.wrap((command[1] + " not exists").getBytes(StandardCharsets.UTF_8)));
      }
      channel.write(ByteBuffer.wrap("\n\r".getBytes(StandardCharsets.UTF_8)));
    }

  }

  private void handleAccept(SelectionKey key) throws IOException {
    SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
    channel.configureBlocking(false);
    channel.register(selector, SelectionKey.OP_READ);
  }
}
