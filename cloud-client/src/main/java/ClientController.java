
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
    public ListView<String> messageListView;
    private Path clientPath = Paths.get("./test_in").toAbsolutePath().normalize();

    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;

    public void sendMessage(ActionEvent event) throws IOException {
        String fileName = clientFilesListView.getSelectionModel().getSelectedItem();

        if ((fileName == null) || (fileName.isEmpty())) return;
        List<FileMessage> list = FileMessage.GenerateSequence(clientPath.resolve(fileName),SEND_BUFFER_LENGTH);
        for (FileMessage f: list) {
            os.writeObject(f);
            os.flush();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Socket socket;

            try {
                socket = new Socket("localhost", 8189);
            } catch (IOException e) {
                LOG.error("e = ", e);
                return;
            }

            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());

            initListView();

            new Thread(() -> {
                while (true) {
                    try {
                        Object obj = is.readObject();
                        if (obj.getClass().getName() == "TextMessage") {
                            TextMessage message = (TextMessage) obj;
                            messageListView.getItems().add(message.toString());
                            LOG.debug(message.toString());
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

    private void initListView() throws IOException {
        clientFilesListView.setItems(FXCollections.observableList(Files.list(clientPath).map(path -> path.getFileName().toString())
        .collect(Collectors.toList())));
    }
}
