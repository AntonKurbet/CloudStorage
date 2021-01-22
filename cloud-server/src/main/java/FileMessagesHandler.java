
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedDeque;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class FileMessagesHandler extends SimpleChannelInboundHandler<FileMessage> {

    private static final ConcurrentLinkedDeque<ChannelHandlerContext> clients = new ConcurrentLinkedDeque<>();

    private Path serverPath = Paths.get("./test_out").toAbsolutePath().normalize();
    private static HashSet<String> processing = new HashSet<>();

    private static int cnt = 0;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        clients.add(ctx);
        cnt++;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FileMessage msg) throws Exception {
        boolean append = processing.contains(msg.getName());
        if (!append) processing.add(msg.getName());
        msg.writeData(serverPath.resolve(msg.getName()), append);
        if (msg.getEnd()) processing.remove(msg.getName());
    // TODO: Server Response
//        for (ChannelHandlerContext client : clients) {
//            client.writeAndFlush(msg);
//        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        clients.remove(ctx);
    }
}
