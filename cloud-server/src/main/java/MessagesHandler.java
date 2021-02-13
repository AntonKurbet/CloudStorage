import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagesHandler extends SimpleChannelInboundHandler<ExchangeMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(MessagesHandler.class);
    private static final int SEND_BUFFER_LENGTH = 65536;

    private Path serverPath = Paths.get(System.getProperty("user.home") + "/tmp").toAbsolutePath().normalize();
    private Path newPath = serverPath;
    private static HashSet<String> processing = new HashSet<>();

    private boolean authorized = false;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        //
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ExchangeMessage msg) throws Exception {
        LOG.info("Received " + msg.getClass().getName());
        if (msg instanceof FileMessage) {
            processFileMessage((FileMessage) msg, ctx);
        } else if (msg instanceof CommandMessage) {
            processCommandMessage((CommandMessage) msg, ctx);
        }
    }

    private void processCommandMessage(CommandMessage msg, ChannelHandlerContext ctx) throws IOException {
        if (!authorized && msg.getCommand() != ServerCommand.AUTH) {
            LOG.info("Not authorized");
            return;
        }

        switch (msg.getCommand()) {
            case LS:
                doLs((FileListCommandMessage) msg);
                break;
            case CD:
                doCd((SimpleCommandMessage) msg);
                break;
            case RM:
                doRm((SimpleCommandMessage) msg);
                break;
            case GET:
                doGet((SimpleCommandMessage) msg, ctx);
                break;
            case MV:
                doMv((ArrayCommandMessage) msg);
                break;
            case MKDIR:
                doMkDir((SimpleCommandMessage) msg);
                break;
            case AUTH:
                doAuthorization((AuthorizationMessage)msg);
        }
        ctx.writeAndFlush(msg);
        LOG.info(String.format("Processed command %s (%s)", msg.getCommand(), msg.getParam()));
    }

    private void doMkDir(SimpleCommandMessage msg) {
        try {
            Files.createDirectory(newPath.resolve(msg.getParam()));
            msg.setResult("");
        } catch (IOException e) {
            msg.setResult("error");
            LOG.error(e.getMessage());
        }

    }

    private void doMv(ArrayCommandMessage msg) {
        Path oldName = newPath.resolve(msg.getParamArray()[0]);
        Path newName = newPath.resolve(msg.getParamArray()[1]);
        if (Files.exists(oldName) && !Files.exists(newName)) {
            try {
                Files.copy(oldName,newName);
                Files.delete(oldName);
                msg.setResult("");
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        } else msg.setResult("File not exists or can't overwrite");
    }

    private void doAuthorization(AuthorizationMessage msg) {
        try {
            SqlClient.connect();
            String nick = SqlClient.getNickname(msg.getLogin(), msg.getPassword());
            if (nick != null && !nick.isEmpty()) {
                msg.setResult(true);
                this.authorized = true;
                Path userPath = serverPath.resolve(msg.getLogin());
                if (!Files.exists(userPath)) Files.createDirectories(userPath);
                serverPath = userPath;
                newPath = userPath;
                LOG.info("current path is " + serverPath);
            }
            else msg.setResult(false);
        } catch (RuntimeException | IOException e) {
            LOG.error(e.getMessage());
        } finally {
            SqlClient.disconnect();
        }
    }

    private void doGet(SimpleCommandMessage msg, ChannelHandlerContext ctx) {
        try {
            FileMessage.sendByStream(newPath.resolve((msg.getParam())),
                    SEND_BUFFER_LENGTH, ctx);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    private void doRm(SimpleCommandMessage msg) throws IOException {
        Path tmpPath = newPath.resolve(msg.getParam());
        if (Files.exists(tmpPath)) {
            if (!Files.isDirectory(tmpPath)) {
                Files.delete(tmpPath);
                msg.setResult("");
            }
        } else msg.setResult("File not exists");
    }

    private void doCd(SimpleCommandMessage msg) {
        Path tmpPath = newPath.resolve(msg.getParam());
        if (Files.exists(tmpPath) && Files.isDirectory(tmpPath)) {
            if (tmpPath.toAbsolutePath().normalize().startsWith(serverPath)) {
                newPath = tmpPath.toAbsolutePath().normalize();
                msg.setResult(newPath.toString().substring(serverPath.toString().length()));
            } else
                msg.setResult("Can't move there");
        } else {
            msg.setResult("Not a directory");
        }
        LOG.info("current path is " + newPath);
    }

    private void doLs(FileListCommandMessage msg) throws IOException {
        List<FileInfo> list = new ArrayList<>(Files.list(newPath).map(FileInfo::new).collect(Collectors.toList()));
        msg.setResult(list);
    }

    private void processFileMessage(FileMessage msg, ChannelHandlerContext ctx) throws IOException {
        LOG.debug("Writing " + msg.getName());
        msg.writeData(newPath.resolve(msg.getName()).toString());
        ctx.writeAndFlush(new TextMessage(String.format("Received %s part", msg.getName())));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
//
    }
}
