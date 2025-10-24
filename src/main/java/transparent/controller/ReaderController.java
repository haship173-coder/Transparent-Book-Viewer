package transparent.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller for the reader view.  Currently this is a placeholder.  In a
 * complete implementation you would render the selected file (PDF, image,
 * etc.), allow navigation between pages and save the reading progress when
 * closing the window.
 */
public class ReaderController {
    @FXML private Button closeButton;

    @FXML
    private void initialize() {
        closeButton.setOnAction(e -> handleClose());
    }

    private void handleClose() {
        // Save reading progress here (not yet implemented)
        // Close the window
        closeButton.getScene().getWindow().hide();
    }
}