import java.io.IOException;
import java.net.Socket;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.Optional;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Thread.sleep;

public class ClientController {

    private static final Logger LOG = LoggerFactory.getLogger(ClientController.class);
    private static final int SEND_BUFFER_LENGTH = 65535;

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
            localPC.setCurrentPath(Paths.get("./test_in").toAbsolutePath().normalize());

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
            msg.writeData(localPC.getCurrentPath().resolve(msg.getName()).toString());
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
            case MKDIR:
                cmdObj = new SimpleCommandMessage(cmd, params[0]);
                break;
            case MV:
                cmdObj = new ArrayCommandMessage(cmd, params);
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
            case LS:
                List<FileInfo> files = ((FileListCommandMessage) cmd).getResult();
                if (files != null) Platform.runLater(() -> {
                    remotePC.setFiles(files);
                    remotePC.updateList();
                });
        }
    }

    private void refreshClientListView() {
        Platform.runLater(() -> localPC.updateList());
    }

    public void exitOnAction(ActionEvent actionEvent) {
        connected = false;
        Platform.runLater(() -> System.exit(0));
    }

    public void copyOnAction(ActionEvent actionEvent) {
        if (!selectedFile()) return;

        try {
            if (localPC.getSelectedFilename() != null) {
                FileMessage.sendByStream(localPC.getCurrentPath().resolve(localPC.getSelectedFilename()), SEND_BUFFER_LENGTH, os);
                sendCommand(ServerCommand.LS);
            } else {
                sendCommand(ServerCommand.GET, remotePC.getSelectedFilename());
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    private boolean selectedFile() {
        if (localPC.getSelectedFilename() == null && remotePC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "No files selected", ButtonType.OK);
            alert.showAndWait();
            return false;
        }
        return true;
    }

    public void initSocket(Socket socket) {
        this.socket = socket;
        connected = true;
    }

    public void renameOnAction(ActionEvent actionEvent) {
        if (!selectedFile()) return;

        String oldFilename;
        String newFilename;

        try {
            if (localPC.getSelectedFilename() != null) {
                oldFilename = localPC.getSelectedFilename();
                newFilename = confirmRename(oldFilename);
                if (newFilename != null) {
                    Files.copy(localPC.getCurrentPath().resolve(oldFilename),
                            localPC.getCurrentPath().resolve(newFilename));
                    Files.delete(localPC.getCurrentPath().resolve(oldFilename));
                    refreshClientListView();
                }
            } else {
                oldFilename = remotePC.getSelectedFilename();
                newFilename = confirmRename(oldFilename);
                sendCommand(ServerCommand.MV, new String[]{remotePC.getSelectedFilename(), newFilename});
                sendCommand(ServerCommand.LS);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

    }

    private String confirmRename(String oldFilename) {
        TextInputDialog dialog = new TextInputDialog(oldFilename);
        dialog.setTitle("Rename");
        dialog.setHeaderText("You want to rename file");
        dialog.setContentText("Please enter new name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String newFilename = result.get();
            if (!newFilename.equals(oldFilename))
                return newFilename;
            else
                return null;
        }
        return null;
    }

    public void deleteOnAction(ActionEvent actionEvent) {
        if (!selectedFile()) return;

        try {
            if (localPC.getSelectedFilename() != null) {
                Files.delete(localPC.getCurrentPath().resolve(localPC.getSelectedFilename()));
                localPC.updateList();
            } else {
                sendCommand(ServerCommand.RM, remotePC.getSelectedFilename());
                sendCommand(ServerCommand.LS);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    private String getDirName() {
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("Make dir");
        dialog.setHeaderText("You want to make dir");
        dialog.setContentText("Please enter dir name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            return result.get();
        }
        return null;
    }

    public void mkdirOnAction(ActionEvent actionEvent) {
        String newDirName = getDirName();
        if (newDirName != null) {
            try {
                if (localPC.getSelectedFilename() != null) {
                    Files.createDirectory(localPC.getCurrentPath().resolve(newDirName));
                    localPC.updateList();
                } else {
                    sendCommand(ServerCommand.MKDIR, newDirName);
                    sendCommand(ServerCommand.LS);
                }
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
    }
}
