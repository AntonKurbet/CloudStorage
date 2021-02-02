
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
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientController implements Initializable, ObjectWriter {

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
                updateClientListView();
                sendCommand(ServerCommand.LS,null);
            } catch (IOException e) {
                e.printStackTrace();
            }

            new Thread(() -> {
                while (true) {
                    try {
                        Object obj = is.readObject();
                        LOG.info("Received " + obj.getClass().getName());
                        if (obj instanceof TextMessage) {
                            LOG.info(obj.toString());
                        } else if (obj instanceof CommandMessage) {
                            ProcessCommandResult((CommandMessage)obj);
                        } else if (obj instanceof FileMessage) {
                            ((FileMessage) obj).writeData(clientPath);
                            updateClientListView();
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

    public void sendCommand(ServerCommand cmd) throws IOException {sendCommand(cmd, null);}

    public void sendCommand(ServerCommand cmd, String params) throws IOException {
        Object cmdObj;
        switch (cmd) {
            case CD : cmdObj = new BoolCommandMessage(ServerCommand.CD,params);
                break;
            case LS : cmdObj = new ListCommandMessage(ServerCommand.LS);
                break;
            case RM : cmdObj = new BoolCommandMessage(ServerCommand.RM,params);
                break;
            case GET: cmdObj = new RequestFileMessage(ServerCommand.GET,params);
                break;
            default: return;
        }

        os.writeObject(cmdObj);
        os.flush();
    }

    private void ProcessCommandResult(CommandMessage cmd) {
        switch (cmd.getCommand()) {
            case CD :
            case RM :
            case GET: break;
            case LS : List<String> files = ((ListCommandMessage)cmd).getResult();
                        if (files != null) Platform.runLater(() ->
                                serverFilesListView.setItems(FXCollections.observableList(files)));
                        break;
        }
    }

    private void updateClientListView() throws IOException {
        Platform.runLater(() -> {
            try {
                clientFilesListView.setItems(
                        FXCollections.observableList(Files.list(clientPath)
                                .map(path -> path.getFileName().toString())
                                .collect(Collectors.toList())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void writeObject(Object stream, Object obj)  {
        try {
            ((ObjectEncoderOutputStream)stream).writeObject(obj);
            ((ObjectEncoderOutputStream)stream).flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void uploadFile(ActionEvent event) throws IOException {
        String fileName = clientFilesListView.getSelectionModel().getSelectedItem();

        if ((fileName == null) || (fileName.isEmpty())) return;
        FileMessage.sendByStream(clientPath.resolve(fileName),SEND_BUFFER_LENGTH,this, os);
        sendCommand(ServerCommand.LS);
    }

    public void downloadFile(ActionEvent actionEvent) throws IOException {
        String fileName = serverFilesListView.getSelectionModel().getSelectedItem();

        if ((fileName == null) || (fileName.isEmpty())) return;
        sendCommand(ServerCommand.GET,fileName);
    }

    public void deleteFile(ActionEvent actionEvent) {
        String fileName = serverFilesListView.getSelectionModel().getSelectedItem();
        if ((fileName == null) || (fileName.isEmpty()) || fileName.startsWith(">>")) return;

        try {
            sendCommand(ServerCommand.RM,fileName);
            sendCommand(ServerCommand.LS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serverFilesOnMouseClicked(MouseEvent mouseEvent) {
        String dirName = serverFilesListView.getSelectionModel().getSelectedItem();
        if ((dirName == null) || (dirName.isEmpty()) || !dirName.startsWith(">>")) return;

        try {
            sendCommand(ServerCommand.CD,dirName.substring(2));
            sendCommand(ServerCommand.LS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
