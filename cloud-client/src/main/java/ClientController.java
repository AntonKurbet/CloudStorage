import java.io.IOException;
import java.net.Socket;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.sleep;

public class ClientController {

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

    public void initialize() {
        try {
            refreshClientListView();

            localPC = (ClientPanelController) localFilesPanel.getProperties().get("controller");
            remotePC = (ServerPanelController) remoteFilesPanel.getProperties().get("controller");
            localPC.setMainController(this);
            remotePC.setMainController(this);

            new Thread(() -> {

                while (!connected) {
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        LOG.error(e.getMessage());
                    }
                }

                try {
                    os = new ObjectEncoderOutputStream(socket.getOutputStream());
                    is = new ObjectDecoderInputStream(socket.getInputStream());

                    sendCommand(ServerCommand.LS);
                } catch (IOException e) {
                    LOG.error(e.getMessage());
                }

                while (connected) {
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
            default:
                return;
        }
        os.writeObject(cmdObj);
        os.flush();
    }

    private void ProcessCommandResult(Object cmd) {
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
        connected = true;
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
