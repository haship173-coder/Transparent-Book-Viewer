package transparent.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import transparent.model.Content;
import transparent.service.FavouriteService;
import transparent.service.ContentService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.time.format.DateTimeFormatter;

/**
 * Controller for the main library view.  Displays the list of content and
 * provides actions for searching, adding files and navigating to history or
 * favourites.  Double clicking a row is intended to open the reader view.
 */
public class MainController {
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button openButton;
    @FXML private Button readButton;
    @FXML private Button infoButton;
    @FXML private Button toggleFavouriteButton;
    @FXML private Button historyButton;
    @FXML private Button favouritesButton;
    @FXML private TableView<Content> contentTable;
    @FXML private TableColumn<Content, String> colTitle;
    @FXML private TableColumn<Content, String> colType;
    @FXML private TableColumn<Content, Number> colSize;
    @FXML private TableColumn<Content, String> colAdded;

    private final ContentService contentService = new ContentService();
    private final FavouriteService favouriteService = new FavouriteService();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        // Bind table columns to content properties
        colTitle.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTitle()));
        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getFileType()));
        colSize.setCellValueFactory(data -> new javafx.beans.property.SimpleLongProperty(data.getValue().getSizeBytes()));
        colSize.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatSize(item.longValue()));
                }
            }
        });
        colAdded.setCellValueFactory(data -> {
            if (data.getValue().getDayAdded() == null) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
            return new javafx.beans.property.SimpleStringProperty(DATE_FORMATTER.format(data.getValue().getDayAdded()));
        });
        loadContents();
        // Bind buttons to actions
        searchButton.setOnAction(e -> doSearch());
        openButton.setOnAction(e -> addNewFile());
        readButton.setOnAction(e -> Optional.ofNullable(contentTable.getSelectionModel().getSelectedItem()).ifPresent(this::openReader));
        infoButton.setOnAction(e -> Optional.ofNullable(contentTable.getSelectionModel().getSelectedItem()).ifPresent(this::showInfo));
        toggleFavouriteButton.setOnAction(e -> Optional.ofNullable(contentTable.getSelectionModel().getSelectedItem()).ifPresent(this::toggleFavourite));
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
        contentTable.setPlaceholder(new Label("No content available. Add files to begin."));
        contentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> updateSelectionState(newSel));
    }

    private void loadContents() {
        List<Content> list = contentService.getAllContents();
        contentTable.setItems(FXCollections.observableArrayList(list));
    }

    private void doSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadContents();
            return;
        }
        List<Content> list = contentService.searchContents(keyword);
        contentTable.setItems(FXCollections.observableArrayList(list));
    }

    private void addNewFile() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Add content to library");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                "Supported files", "*.pdf", "*.epub", "*.txt", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        chooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Books", "*.pdf", "*.epub", "*.txt"),
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        java.io.File file = chooser.showOpenDialog(openButton.getScene().getWindow());
        if (file != null) {
            Content content = contentService.addContentFromFile(file);
            loadContents();
            selectContent(content);
            showInformationAlert("File added", "Successfully added \"" + content.getTitle() + "\" to the library.");
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
        Objects.requireNonNull(content, "content");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reader.fxml"));
            Parent root = loader.load();
            ReaderController controller = loader.getController();
            controller.setContent(content);
            Stage stage = new Stage();
            stage.setTitle("Reading - " + content.getTitle());
            stage.initOwner(contentTable.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> loadContents());
            stage.show();
        } catch (IOException ex) {
            showErrorAlert("Unable to open reader", ex.getMessage());
        }
    }

    private void showInfo(Content content) {
        Objects.requireNonNull(content, "content");
        StringBuilder builder = new StringBuilder();
        builder.append("Type: ").append(content.getFileType()).append('\n')
               .append("Size: ").append(formatSize(content.getSizeBytes())).append('\n')
               .append("Path: ").append(content.getFilePath());
        if (content.getDayAdded() != null) {
            builder.append('\n').append("Added: ")
                   .append(DATE_FORMATTER.format(content.getDayAdded()));
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Content information");
        alert.setHeaderText(content.getTitle());
        alert.setContentText(builder.toString());
        alert.showAndWait();
    }

    private void toggleFavourite(Content content) {
        if (CurrentUser.get() == null) {
            showErrorAlert("Not logged in", "Please login to manage favourites.");
            return;
        }
        int userId = CurrentUser.get().getUserID();
        favouriteService.toggleFavourite(userId, content.getContentID());
        boolean isFavourite = favouriteService.isFavourite(userId, content.getContentID());
        updateFavouriteButtonText(isFavourite);
        showInformationAlert("Favourite updated",
                isFavourite ? "Added to favourites" : "Removed from favourites");
    }

    private void updateSelectionState(Content selected) {
        boolean hasSelection = selected != null;
        readButton.setDisable(!hasSelection);
        infoButton.setDisable(!hasSelection);
        boolean favouriteAllowed = hasSelection && CurrentUser.get() != null;
        toggleFavouriteButton.setDisable(!favouriteAllowed);
        if (favouriteAllowed) {
            boolean favourite = favouriteService.isFavourite(CurrentUser.get().getUserID(), selected.getContentID());
            updateFavouriteButtonText(favourite);
        } else {
            toggleFavouriteButton.setText("Favourite");
        }
    }

    private void updateFavouriteButtonText(boolean favourite) {
        toggleFavouriteButton.setText(favourite ? "Remove favourite" : "Add favourite");
    }

    private void selectContent(Content content) {
        if (content == null) {
            return;
        }
        for (Content item : contentTable.getItems()) {
            if (item.getContentID() == content.getContentID()) {
                contentTable.getSelectionModel().select(item);
                contentTable.scrollTo(item);
                return;
            }
        }
    }

    private void showErrorAlert(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Transparent");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInformationAlert(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Transparent");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format("%.1f %s", value, units[unitIndex]);
    }
}
