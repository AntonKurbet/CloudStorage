
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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientController.class);
    private static final int SEND_BUFFER_LENGTH = 65535;

    private Path clientPath = Paths.get("./test_in").toAbsolutePath().normalize();

    private Socket socket;
    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;

    @FXML
    VBox localFilesPanel, remoteFilesPanel;

    private ClientPanelController localPC;
    private ServerPanelController remotePC;

    private boolean connected;

    @FXML
    public void exitApplication(ActionEvent event) {
        Platform.exit();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {

            try {
                socket = new Socket("localhost", 8189);
                LOG.info("Connected to server...");
                connected = true;
            } catch (IOException e) {
                LOG.error("e = ", e);
                return;
            }

            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());

            try {
                refreshClientListView();
                sendCommand(ServerCommand.AUTH, new String[]{"user01", "Pass0!"});

                localPC = (ClientPanelController) localFilesPanel.getProperties().get("controller");
                remotePC = (ServerPanelController) remoteFilesPanel.getProperties().get("controller");
                localPC.setMainController(this);
                remotePC.setMainController(this);
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
        sendCommand(cmd, (String) null);
    }

    public void sendCommand(ServerCommand cmd, String params) throws IOException {
        sendCommand(cmd, new String[]{params});
    }

    public void sendCommand(ServerCommand cmd, String[] params) throws IOException {
        Object cmdObj;
        switch (cmd) {
            case CD:
            case RM:
            case GET:
                cmdObj = new SimpleCommandMessage(cmd, params[0]);
                break;
            case LS:
                cmdObj = new FileListCommandMessage(cmd);
                break;
            case AUTH:
                cmdObj = new AuthorizationMessage(params[0], params[1]);
                break;
            default:
                return;
        }
        os.writeObject(cmdObj);
        os.flush();
    }

    private void ProcessCommandResult(Object cmd) throws IOException {
        switch (((CommandMessage) cmd).getCommand()) {
            case CD:
                String path = ((SimpleCommandMessage) cmd).getResult();
                remotePC.setServerPath(path);
                break;
            case RM:
                break;
            case LS:
                List<FileInfo> files = ((FileListCommandMessage) cmd).getResult();
                if (files != null) Platform.runLater(() -> {
                    remotePC.setFiles(files);
                    remotePC.updateList();
                });
                break;
            case AUTH:
                if (!((AuthorizationMessage) cmd).getResult()) {
                    LOG.info("Authorization failed");
                    return;
                }
                sendCommand(ServerCommand.LS);
        }
    }

    private void refreshClientListView() {
        Platform.runLater(() -> localPC.updateList(clientPath));
    }

//    public void changeDir(ActionEvent actionEvent) {
//        FileInfo dir = remotePC.getSelectedItem();
//        if (dir.getType() != FileInfo.FileType.DIR) return;
//
//        try {
//            sendCommand(ServerCommand.CD, dir.getName());
//            sendCommand(ServerCommand.LS);
//        } catch (IOException e) {
//            LOG.error(e.getMessage());
//        }
//    }

    public void exitOnAction(ActionEvent actionEvent) {
        connected = false;
        Platform.runLater(() -> System.exit(0));
    }

    public void copyOnAction(ActionEvent actionEvent) {
//TODO:
    }

    public void initSocket(Socket socket) {
        this.socket = socket;
    }


//    public void deleteFile(ActionEvent actionEvent) {
//        String fileName = serverFilesListView.getSelectionModel().getSelectedItem();
//        if ((fileName == null) || (fileName.isEmpty()) || fileName.startsWith(">>")) return;
//
//        try {
//            sendCommand(ServerCommand.RM, fileName);
//            sendCommand(ServerCommand.LS);
//        } catch (IOException e) {
//            LOG.error(e.getMessage());
//        }
//    }
//
//    public void sendFile(ActionEvent event) throws IOException {
//        String fileName = clientFilesListView.getSelectionModel().getSelectedItem();
//
//        if ((fileName == null) || (fileName.isEmpty())) return;
//
//        FileMessage.sendByStream(clientPath.resolve(fileName), SEND_BUFFER_LENGTH, os);
//
//        sendCommand(ServerCommand.LS);
//    }
//
//    public void getFile(ActionEvent actionEvent) throws IOException {
//        String fileName = serverFilesListView.getSelectionModel().getSelectedItem();
//
//        if ((fileName == null) || (fileName.isEmpty())) return;
//
//        sendCommand(ServerCommand.GET, fileName);
//    }
}
