import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDate;

public class Server {

    private static final int PORT = 32000;
    private static final int TIMEOUT = 1000;
    private static boolean connected = false;
    private static Socket client;
    private static final Logger LOGGER = LogManager.getLogger(Server.class);
    public static final String OUT_PATH = "test_out/";
    private static byte START_BYTE = 31;

    public static void main(String[] args) {
        BasicConfigurator.configure();
        connect();
        if (client != null) {
            getObjectSequence();
            //getByProtocol();
        }
        else LOGGER.error("Not connected");
    }

//    private static void getByProtocol() {
//        try (InputStream input = client.getInputStream();
//             DataInputStream byteInput = new DataInputStream(input)) {
//            while (byteInput.readByte() != START_BYTE) ;
//            int size = byteInput.readInt();
//            byte[] nameBytes = new byte[size];
//            byteInput.read(nameBytes);
//            size = byteInput.readInt();
//            byte[] dataBytes = new byte[size];
//            byteInput.read(dataBytes);
//
//            FileMessage obj = new FileMessage(nameBytes.toString(),dataBytes, LocalDate.now(),true);
//            LOGGER.info(String.format("Received object: %s",obj.toString()));
//            obj.writeData("file.txt");
//        } catch (IOException e) {
//            LOGGER.error(e.getMessage());
//        } catch (EOFException e) {
//            continue;
//        }
//    }

    private static void getObjectSequence() {
        try (InputStream input = client.getInputStream()) {
            int i = 0;
            while (true) {
                ObjectInputStream objInput = new ObjectInputStream(input);
                // Если поместить в трай с ресурсами, то получим ошибку
                // Как сделать правильно, чтобы поток закрывался
                FileMessage obj = (FileMessage) objInput.readObject();
                LOGGER.info(String.format("Received file %s part: %d data length: %d", obj.getName(), i++, obj.getData().length));
                obj.writeData(OUT_PATH + obj.getName(), i != 1);
                if (obj.getEnd()) break;
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private static void connect() {
        try (ServerSocket server = new ServerSocket(PORT)) {
            LOGGER.info(String.format("Starting server on port %d",PORT));
            server.setSoTimeout(TIMEOUT);
            while (!connected) {
                try {
                    client = server.accept();
                    connected = true;
                    LOGGER.info(String.format("Connected client from %s",client.getInetAddress()));
                } catch (SocketTimeoutException e) {
                    //continue;
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
