package transparent.controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import transparent.model.Content;
import transparent.model.HistoryRecord;
import transparent.service.ContentService;
import transparent.service.HistoryService;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for the history view.  Displays the list of recently opened
 * content and allows the user to reopen items with a double click.
 */
public class HistoryController {
    @FXML private TableView<HistoryRecord> historyTable;
    @FXML private TableColumn<HistoryRecord, String> colContent;
    @FXML private TableColumn<HistoryRecord, String> colLastRead;
    @FXML private TableColumn<HistoryRecord, Number> colPage;
    @FXML private Button backButton;

    private final HistoryService historyService = new HistoryService();
    private final ContentService contentService = new ContentService();
    private final Map<Integer, Content> contentLookup = new HashMap<>();
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault());

    @FXML
    private void initialize() {
        colContent.setCellValueFactory(data -> Bindings.createStringBinding(
                () -> resolveTitle(data.getValue().getContentID())));
        colLastRead.setCellValueFactory(data -> Bindings.createStringBinding(
                () -> data.getValue().getLastReadTime() == null
                        ? ""
                        : FORMATTER.format(data.getValue().getLastReadTime())));
        colPage.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(
                data.getValue().getPageNumber()).asObject());
        historyTable.setRowFactory(tv -> {
            TableRow<HistoryRecord> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Content content = contentLookup.get(row.getItem().getContentID());
                    if (content != null) {
                        openReader(content);
                    }
                }
            });
            return row;
        });
        loadHistory();
        backButton.setOnAction(e -> ((Stage) backButton.getScene().getWindow()).close());
    }

    private void loadHistory() {
        if (CurrentUser.get() == null) {
            historyTable.setItems(FXCollections.emptyObservableList());
            return;
        }
        contentLookup.clear();
        contentService.getAllContents().forEach(content ->
                contentLookup.put(content.getContentID(), content));
        List<HistoryRecord> list = historyService.getHistory(CurrentUser.get().getUserID());
        historyTable.setItems(FXCollections.observableArrayList(list));
    }

    private String resolveTitle(int contentId) {
        Content content = contentLookup.get(contentId);
        if (content == null) {
            return "#" + contentId;
        }
        return content.getTitle();
    }

    private void openReader(Content content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reader.fxml"));
            Parent root = loader.load();
            ReaderController controller = loader.getController();
            controller.setContent(content);
            Stage stage = new Stage();
            stage.setTitle(content.getTitle());
            stage.setScene(new Scene(root, 900, 650));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
