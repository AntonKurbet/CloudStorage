import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

import static java.lang.Thread.sleep;

public class LoginController {
    @FXML
    private TextField userField;
    @FXML
    private TextField passField;

    private Socket socket;
    private Scene scene;

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);
    private Stage primaryStage;
    private boolean connected;
    private boolean authorized;
    private ObjectEncoderOutputStream os;
    private ObjectDecoderInputStream is;

    public void exitOnAction(ActionEvent actionEvent) {
        Platform.runLater(() -> System.exit(0));
    }

    public void authorizeOnAction(ActionEvent actionEvent) {
        try {
            os.writeObject(new AuthorizationMessage(userField.getText(), passField.getText()));
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        try {
            sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (authorized) showMainView();
    }

    private void showMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("clientLayout.fxml"));
            Parent parent = loader.load();
            ClientController controller = loader.getController();
            controller.initSocket(socket);
            primaryStage.setScene(new Scene(parent));
            primaryStage.setTitle("Cloud Storage Client");
            primaryStage.show();

        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    public void initialize() {
        try {
            socket = new Socket("localhost", 8189);
            LOG.info("Connected to server...");
            connected = true;
        } catch (IOException e) {
            LOG.error("e = ", e);
            return;
        }

        try {
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }


        new Thread(() -> {
            while (!authorized) {
                try {
                    Object obj = is.readObject();
                    LOG.info("Received " + obj.getClass().getName());
                    if (obj instanceof AuthorizationMessage) {
                        if (!((AuthorizationMessage) obj).getResult()) {
                            LOG.info("Authorization failed");
                            return;
                        }
                        LOG.info("Authorized user");
                        authorized = true;
                    }
                } catch (Exception e) {
                    LOG.error("e = ", e);
                    break;
                }
            }
        }).start();

    }

    public void initStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}

