package transparent;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import transparent.ui.ThemeManager;

/**
 * Entry point for the JavaFX application.  Launches the login view.
 */
public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/login.fxml"));
        stage.setTitle("Transparent - Login");
        Scene scene = new Scene(root);
        ThemeManager.getInstance().register(scene);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}