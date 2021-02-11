
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import io.netty.channel.ChannelHandlerContext;
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
                refreshClientListView();
                sendCommand(ServerCommand.LS, null);
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }

            new Thread(() -> {
                while (true) {
                    try {
                        Object obj = is.readObject();
                        LOG.info("Received " + obj.getClass().getName());
                        if (obj instanceof TextMessage) {
                            LOG.info(obj.toString());
                        } else if (obj instanceof CommandMessage) {
                            ProcessCommandResult(obj);
                        } else if (obj instanceof FileMessage) {
                            ProcessFileMessage((FileMessage) obj);
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

    private void ProcessFileMessage(FileMessage msg) {
        LOG.debug("Writing " + msg.getName());
        try {
            msg.writeData(clientPath.resolve(msg.getName()).toString());
            refreshClientListView();
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    public void sendCommand(ServerCommand cmd) throws IOException {
        sendCommand(cmd, null);
    }

    public void sendCommand(ServerCommand cmd, String params) throws IOException {
        Object cmdObj;
        switch (cmd) {
            case CD:
            case RM:
            case GET:
                cmdObj = new SimpleCommandMessage(cmd, params);
                break;
            case LS:
                cmdObj = new FileListCommandMessage(cmd);
                break;
            default:
                return;
        }
        os.writeObject(cmdObj);
        os.flush();
    }

    private void ProcessCommandResult(Object cmd) {
        switch (((CommandMessage) cmd).getCommand()) {
            case CD:
            case RM:
                break;
            case LS:
                List<String> files = ((FileListCommandMessage) cmd).getResult();
                if (files != null) Platform.runLater(() -> {
                    serverFilesListView.setItems(FXCollections.observableList(files));
                });
                break;
        }
    }

    private void refreshClientListView() {
        Platform.runLater(() -> {
            try {
                clientFilesListView.setItems(
                        FXCollections.observableList(Files.list(clientPath).map(path -> path.getFileName().toString())
                                .collect(Collectors.toList())));
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        });
    }

    public void changeDir(ActionEvent actionEvent) {
        String dirName = serverFilesListView.getSelectionModel().getSelectedItem();
        if ((dirName == null) || (dirName.isEmpty()) || !dirName.startsWith(">>")) return;

        try {
            sendCommand(ServerCommand.CD, dirName.substring(2));
            sendCommand(ServerCommand.LS);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    public void deleteFile(ActionEvent actionEvent) {
        String fileName = serverFilesListView.getSelectionModel().getSelectedItem();
        if ((fileName == null) || (fileName.isEmpty()) || fileName.startsWith(">>")) return;

        try {
            sendCommand(ServerCommand.RM, fileName);
            sendCommand(ServerCommand.LS);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    public void sendFile(ActionEvent event) throws IOException {
        String fileName = clientFilesListView.getSelectionModel().getSelectedItem();

        if ((fileName == null) || (fileName.isEmpty())) return;

        FileMessage.sendByStream(clientPath.resolve(fileName), SEND_BUFFER_LENGTH, os);

        sendCommand(ServerCommand.LS);
    }

    public void getFile(ActionEvent actionEvent) throws IOException {
        String fileName = serverFilesListView.getSelectionModel().getSelectedItem();

        if ((fileName == null) || (fileName.isEmpty())) return;

        sendCommand(ServerCommand.GET, fileName);
    }
}
