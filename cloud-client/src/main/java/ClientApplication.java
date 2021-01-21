import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent parent = FXMLLoader.load(getClass().getResource("clientLayout.fxml"));
        primaryStage.setScene(new Scene(parent));
        primaryStage.setTitle("Чат");
        primaryStage.show();
    }
}
