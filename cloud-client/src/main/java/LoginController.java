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

public class LoginController {
    @FXML
    private TextField userField;
    @FXML
    private TextField passPass;

    private Socket socket;
    private Scene scene;

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);
    private Stage primaryStage;

    public void exitOnAction(ActionEvent actionEvent) {
        Platform.runLater(() -> System.exit(0));
    }

    public void authorizeOnAction(ActionEvent actionEvent) {
        showMainView(null);
    }

    private void showMainView(Socket socket) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("clientLayout.fxml"));
            Parent parent = loader.load();
            primaryStage.setScene(new Scene(parent));
            primaryStage.setTitle("Cloud Storage Client");
            ClientController controller = loader.getController();
            controller.initSocket(socket);
            primaryStage.show();

        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    public void initialize() {
    }

    public void initStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}

