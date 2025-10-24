package transparent.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import transparent.model.User;
import transparent.service.UserService;

/**
 * Controller for the login view.  Prompts the user to enter a username and
 * transitions to the main library view upon successful login.
 */
public class LoginController {
    @FXML private TextField usernameField;
    @FXML private Button loginButton;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        loginButton.setOnAction(e -> handleLogin());
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            // In a real app you would show an error message here
            return;
        }
        User user = userService.findOrCreateUser(username);
        if (user == null) {
            return;
        }
        CurrentUser.set(user);
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
            stage.setTitle("Transparent - Library");
            stage.setScene(new Scene(root));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}