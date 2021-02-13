import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("loginLayout.fxml"));
        Parent parent = loader.load();
        LoginController controller = loader.getController();
        controller.initStage(primaryStage);
        primaryStage.setScene(new Scene(parent));
        primaryStage.setTitle("Cloud Storage");
        primaryStage.show();
    }
}
