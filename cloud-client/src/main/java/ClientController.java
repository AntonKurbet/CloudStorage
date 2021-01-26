
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientController.class);
    private static final int SEND_BUFFER_LENGTH = 50;

    public ListView<String> clientFilesListView;
    public ListView<String> serverFilesListView;
    private Path clientPath = Paths.get("./test_in").toAbsolutePath().normalize();

    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;

    public void sendFile(ActionEvent event) throws IOException {
        String fileName = clientFilesListView.getSelectionModel().getSelectedItem();

        if ((fileName == null) || (fileName.isEmpty())) return;
        List<FileMessage> list = FileMessage.GenerateSequence(clientPath.resolve(fileName),SEND_BUFFER_LENGTH);
        for (FileMessage f: list) {
            os.writeObject(f);
            os.flush();
        }
        sendCommand("ls");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket;

            try {
                socket = new Socket("localhost", 8189);
                LOG.info("Connected to server...");
            } catch (IOException e) {
                LOG.error("e = ", e);
                return;
            }

            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());

            try {
                initListView();
                sendCommand("ls",null);
            } catch (IOException e) {
                e.printStackTrace();
            }

            new Thread(() -> {
                while (true) {
                    try {
                        Object obj = is.readObject();
                        LOG.info("Received " + obj.getClass().getName());
                        switch (obj.getClass().getName()) {
                            case "TextMessage": LOG.info(obj.toString());
                                                break;
                            case "CommandMessage": ProcessCommandResult(obj);
                        }
                    } catch (Exception e) {
                        LOG.error("e = ", e);
                        break;
                    }
                }
            }).start();
        } catch (Exception e) {
            LOG.error("e = ", e);
        }
    }

    public void sendCommand(String cmd) throws IOException {sendCommand(cmd, null);}

    public void sendCommand(String cmd, String params) throws IOException {
        Object cmdObj;
        switch (cmd) {
            case "cd" : cmdObj = new CommandMessage<Boolean>("cd",params);
                break;
            case "ls" : cmdObj = new CommandMessage<List<String>>("ls");
                break;
            case "rm" : cmdObj = new CommandMessage<Boolean>("rm",params);
                break;
            default: return;
        }

        os.writeObject(cmdObj);
        os.flush();
    }

    private void ProcessCommandResult(Object cmd) {
        switch (((CommandMessage)cmd).getCommand()) {
            case "cd" :
            case "rm" :
                        break;
            case "ls" : List<String> files = ((CommandMessage<List<String>>)cmd).getResult();
                        if (files != null) Platform.runLater(() -> {
                            serverFilesListView.setItems(FXCollections.observableList(files));
                        });
                        break;
        }
    }

    private void initListView() throws IOException {
        clientFilesListView.setItems(
                FXCollections.observableList(Files.list(clientPath).map(path -> path.getFileName().toString())
                             .collect(Collectors.toList())));
    }

    public void changeDir(ActionEvent actionEvent) {
        String dirName = serverFilesListView.getSelectionModel().getSelectedItem();
        if ((dirName == null) || (dirName.isEmpty()) || !dirName.startsWith(">>")) return;

        try {
            sendCommand("cd",dirName.substring(2));
            sendCommand("ls");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteFile(ActionEvent actionEvent) {
        String fileName = serverFilesListView.getSelectionModel().getSelectedItem();
        if ((fileName == null) || (fileName.isEmpty()) || fileName.startsWith(">>")) return;

        try {
            sendCommand("rm",fileName);
            sendCommand("ls");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
