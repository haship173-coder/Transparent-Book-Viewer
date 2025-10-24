package transparent.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import transparent.model.HistoryRecord;
import transparent.service.HistoryService;

import java.util.List;

/**
 * Controller for the history view.  Displays the list of recently opened
 * content and allows the user to close the window.  Reading history is
 * retrieved for the currently logged in user.
 */
public class HistoryController {
    @FXML private TableView<HistoryRecord> historyTable;
    @FXML private TableColumn<HistoryRecord, Integer> colContent;
    @FXML private TableColumn<HistoryRecord, String> colLastRead;
    @FXML private TableColumn<HistoryRecord, Integer> colPage;
    @FXML private Button backButton;

    private final HistoryService historyService = new HistoryService();

    @FXML
    private void initialize() {
        colContent.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getContentID()).asObject());
        colLastRead.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getLastReadTime().toString()));
        colPage.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getPageNumber()).asObject());
        loadHistory();
        backButton.setOnAction(e -> ((Stage) backButton.getScene().getWindow()).close());
    }

    private void loadHistory() {
        if (CurrentUser.get() == null) {
            return;
        }
        int userId = CurrentUser.get().getUserID();
        List<HistoryRecord> list = historyService.getHistory(userId);
        historyTable.setItems(javafx.collections.FXCollections.observableArrayList(list));
    }
}