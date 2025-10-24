package transparent.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import transparent.model.Content;
import transparent.model.HistoryRecord;
import transparent.service.ContentService;
import transparent.service.HistoryService;
import transparent.ui.ThemeManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the history view.  Displays the list of recently opened
 * content and allows the user to close the window.  Reading history is
 * retrieved for the currently logged in user.
 */
public class HistoryController {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML private TableView<HistoryRecord> historyTable;
    @FXML private TableColumn<HistoryRecord, String> colTitle;
    @FXML private TableColumn<HistoryRecord, String> colLastRead;
    @FXML private TableColumn<HistoryRecord, Integer> colPage;
    @FXML private Button backButton;

    private final HistoryService historyService = new HistoryService();
    private final ContentService contentService = new ContentService();

    @FXML
    private void initialize() {
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(resolveTitle(data.getValue().getContentID())));
        colLastRead.setCellValueFactory(data -> new SimpleStringProperty(formatTimestamp(data.getValue())));
        colPage.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getPageNumber()).asObject());
        historyTable.setPlaceholder(new Label("No reading history available."));
        loadHistory();
        backButton.setOnAction(e -> ((Stage) backButton.getScene().getWindow()).close());
        Platform.runLater(() -> ThemeManager.getInstance().register(historyTable.getScene()));
    }

    private void loadHistory() {
        if (CurrentUser.get() == null) {
            historyTable.setItems(FXCollections.emptyObservableList());
            return;
        }
        int userId = CurrentUser.get().getUserID();
        List<HistoryRecord> list = historyService.getHistory(userId);
        historyTable.setItems(FXCollections.observableArrayList(list));
    }

    private String resolveTitle(int contentId) {
        return contentService.findContent(contentId)
                .map(Content::getTitle)
                .orElse("Content #" + contentId);
    }

    private String formatTimestamp(HistoryRecord record) {
        return record.getLastReadTime() == null
                ? "-"
                : FORMATTER.format(record.getLastReadTime());
    }
}