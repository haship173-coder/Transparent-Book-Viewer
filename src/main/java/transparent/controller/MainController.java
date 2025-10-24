package transparent.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import transparent.model.Content;
import transparent.service.ContentService;

import java.util.List;

/**
 * Controller for the main library view.  Displays the list of content and
 * provides actions for searching, adding files and navigating to history or
 * favourites.  Double clicking a row is intended to open the reader view.
 */
public class MainController {
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button openButton;
    @FXML private Button historyButton;
    @FXML private Button favouritesButton;
    @FXML private TableView<Content> contentTable;
    @FXML private TableColumn<Content, String> colTitle;
    @FXML private TableColumn<Content, String> colType;
    @FXML private TableColumn<Content, Number> colSize;

    private final ContentService contentService = new ContentService();

    @FXML
    private void initialize() {
        // Bind table columns to content properties
        colTitle.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTitle()));
        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFileType()));
        colSize.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().getSizeBytes()));
        loadContents();
        // Bind buttons to actions
        searchButton.setOnAction(e -> doSearch());
        openButton.setOnAction(e -> addNewFile());
        historyButton.setOnAction(e -> showHistory());
        favouritesButton.setOnAction(e -> showFavourites());
        // Double click row to open reader (not implemented)
        contentTable.setRowFactory(tv -> {
            TableRow<Content> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openReader(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadContents() {
        List<Content> list = contentService.getAllContents();
        contentTable.setItems(FXCollections.observableArrayList(list));
    }

    private void doSearch() {
        String keyword = searchField.getText().trim();
        List<Content> list = contentService.searchContents(keyword);
        contentTable.setItems(FXCollections.observableArrayList(list));
    }

    private void addNewFile() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        java.io.File file = chooser.showOpenDialog(openButton.getScene().getWindow());
        if (file != null) {
            contentService.addContentFromFile(file);
            loadContents();
        }
    }

    private void showHistory() {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/history.fxml"));
            stage.setTitle("Reading History");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showFavourites() {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/favourites.fxml"));
            stage.setTitle("Favourites");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openReader(Content content) {
        // Reader functionality is not yet implemented.  You could pass the
        // content object to another controller to display the file and save
        // reading progress.
    }
}