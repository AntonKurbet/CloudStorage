
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagesHandler extends SimpleChannelInboundHandler<ExchangeMessage> implements ObjectWriter {

    private static final Logger LOG = LoggerFactory.getLogger(MessagesHandler.class);
    private static final int SEND_BUFFER_LENGTH = 50;

    private Path serverPath = Paths.get(System.getProperty("user.home") + "/tmp").toAbsolutePath().normalize();
    private Path newPath = serverPath;
    private String userName = "Non Authorized";

    private static int cnt = 0;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOG.info("current path is " + serverPath);
    }

    public void writeObject(Object stream, Object obj) {
        ((ChannelHandlerContext)stream).writeAndFlush(obj);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ExchangeMessage msg) throws Exception {
        LOG.info("Received " + msg.getClass().getName());
        if (msg instanceof FileMessage) {
            processFileMessage((FileMessage) msg);
            ctx.writeAndFlush(new TextMessage(String.format("Received %s part",((FileMessage)msg).getName())));
        } else if (msg instanceof ListCommandMessage || msg instanceof BoolCommandMessage) {
            processCommandMessage((CommandMessage) msg);
            ctx.writeAndFlush(msg);
        } else if (msg instanceof RequestFileMessage) {
            FileMessage.sendByStream(newPath.resolve(((RequestFileMessage)msg).getParam()), SEND_BUFFER_LENGTH, this, ctx);
        }
    }

    private void processCommandMessage(CommandMessage msg) throws IOException {
        switch (msg.getCommand()) {
            case LS: doLs((ListCommandMessage)msg);
                     break;
            case CD: doCd((BoolCommandMessage)msg);
                     break;
            case RM: doRm((BoolCommandMessage)msg);
                     break;
            case GET: break; //Already done
            default: LOG.warn("Unsupported command");
        }
        LOG.info(String.format("Processed command %s (%s)", msg.getCommand(), msg.getParam()));
    }

    private void doRm(BoolCommandMessage msg) throws IOException {
        Path tmpPath = newPath.resolve(msg.getParam());
        if (Files.exists(tmpPath)) {
            if (!Files.isDirectory(tmpPath)) {
                Files.delete(tmpPath);
                msg.setResult(true);
            }
        } else msg.setResult(false);
    }

    private void doCd(BoolCommandMessage msg) {
        Path tmpPath = newPath.resolve(msg.getParam());
        if (Files.exists(tmpPath) && Files.isDirectory(tmpPath)) {
            if (tmpPath.toAbsolutePath().normalize().startsWith(serverPath)) {
                newPath = tmpPath.toAbsolutePath().normalize();
                msg.setResult(true);
            }
            else
                msg.setResult(false);
        } else {
            msg.setResult(false);
        }
        LOG.info("current path is " + newPath);
    }

    private void doLs(ListCommandMessage msg) throws IOException {
        List<String> list = new ArrayList<>();
        if (!newPath.equals(serverPath)) list.add(">>..");

        list.addAll(Files.list(newPath).map(path -> Files.isDirectory(path) ?
                ">>" + path.getFileName().toString() : path.getFileName().toString())
                    .collect(Collectors.toList()));
        msg.setResult(list);
    }

    private void processFileMessage(FileMessage msg) throws IOException {
        if (msg.getOverwrite())
            LOG.debug("New file: " + msg.getName());

        msg.writeData(newPath.resolve(msg.getName()));
        LOG.debug("Writing " + msg.getName());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        //
    }
}
