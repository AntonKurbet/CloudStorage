
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Client {
    private static final String IP_ADDR = "127.0.0.1";
    private static final int PORT = 32000;
    private static final Logger LOGGER = LogManager.getLogger(Server.class);
    private static final String SEND_FILE = "TestFileToSend.txt";
    private static byte START_BYTE = 31;

    private static Socket connect() {
        try {
            return new Socket(IP_ADDR, PORT);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

//    private static void sendByProtocol(Socket socket) throws IOException, URISyntaxException {
//        FileMessage obj = new FileMessage(Paths.get(SEND_FILE));
//        OutputStream out = socket.getOutputStream();
//        BufferedOutputStream byteOut = new BufferedOutputStream(out);
//        byteOut.write(START_BYTE);
//        byte[] filename = obj.getName().getBytes(StandardCharsets.UTF_8);
//        byteOut.write(filename.length);
//        byteOut.write(filename);
//        byteOut.write(obj.getData().length);
//        byteOut.write(obj.getData());
//        LOGGER.info("Sent object: " + obj.toString());
//    }

    private static void sendObjectSequence(Socket socket) throws IOException, URISyntaxException {
        ArrayList<FileMessage> list = FileMessage.GenerateSequence(Paths.get(SEND_FILE),50);
        OutputStream out = socket.getOutputStream();
        for (int i = 0; i < list.size(); i++) {
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeObject(list.get(i));
            LOGGER.info(String.format("Sent object part: %d", i));
        }
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        try {
            Socket socket = connect();
            if (socket != null) {
                LOGGER.info("Connected to " + socket.getInetAddress() + ":" + socket.getPort());
                sendObjectSequence(socket);
                //sendByProtocol(socket);
            }
            else LOGGER.error("Not connected");
        } catch (IOException | NullPointerException | URISyntaxException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
