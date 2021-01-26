
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

public class MessagesHandler extends SimpleChannelInboundHandler<ExchangeMessage> {

    private static final ConcurrentLinkedDeque<ChannelHandlerContext> clients = new ConcurrentLinkedDeque<>();
    private static final Logger LOG = LoggerFactory.getLogger(MessagesHandler.class);

    private Path serverPath = Paths.get(System.getProperty("user.home") + "/tmp").toAbsolutePath().normalize();
    private Path newPath = serverPath;
    private static HashSet<String> processing = new HashSet<>();

    private static int cnt = 0;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        clients.add(ctx);
        cnt++;
        LOG.info("current path is " + serverPath);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ExchangeMessage msg) throws Exception {
        LOG.info("Received " + msg.getClass().getName());
        switch (msg.getClass().getName()) {
            case "FileMessage" : processFileMessage((FileMessage) msg);
                                 break;
            case "CommandMessage": processCommandMessage((CommandMessage) msg);


        }
    }

    private void processCommandMessage(CommandMessage msg) throws IOException {
        switch (msg.getCommand()) {
            case "ls": doLs((CommandMessage<List<String>>)msg);
                       break;
            case "cd": doCd((CommandMessage<Boolean>)msg);
                       break;
            case "rm": doRm((CommandMessage<Boolean>)msg);
                       break;
        }
        for (ChannelHandlerContext client : clients) {
            client.writeAndFlush(msg);
        }
        LOG.info(String.format("Processed command %s (%s)", msg.getCommand(), msg.getParam()));
    }

    private void doRm(CommandMessage<Boolean> msg) throws IOException {
        Path tmpPath = newPath.resolve(msg.getParam());
        if (Files.exists(tmpPath)) {
            if (!Files.isDirectory(tmpPath)) {
                Files.delete(tmpPath);
                msg.setResult(true);
            }
        } else msg.setResult(false);
    }

    private void doCd(CommandMessage<Boolean> msg) {
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

    private void doLs(CommandMessage<List<String>> msg) throws IOException {
        List<String> list = new ArrayList<>();
        if (!newPath.equals(serverPath)) list.add(">>..");

        list.addAll(Files.list(newPath).map(path -> Files.isDirectory(path) ?
                ">>" + path.getFileName().toString() : path.getFileName().toString())
                    .collect(Collectors.toList()));
        msg.setResult(list);
    }

    private void processFileMessage(FileMessage msg) throws IOException {
        boolean append = processing.contains(msg.getName());
        if (!append) {
            processing.add(msg.getName());
            LOG.debug("New file: " + msg.getName());
        }
        msg.writeData(newPath.resolve(msg.getName()), append);
        LOG.debug("Writing " + msg.getName());

        if (msg.getEnd()) {
            processing.remove(msg.getName());
            LOG.debug("Finished " + msg.getName());
        }

        for (ChannelHandlerContext client : clients) {
            client.writeAndFlush(new TextMessage(String.format("Received %s part",msg.getName())));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        clients.remove(ctx);
    }
}
