package transparent.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import transparent.model.Content;
import transparent.model.Favourite;
import transparent.service.ContentQuery;
import transparent.service.ContentService;
import transparent.service.FavouriteService;
import transparent.ui.ThemeManager;
import transparent.ui.ThemeManager.Theme;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Controller for the main library view.  Displays the list of content and
 * provides actions for searching, adding files and navigating to history or
 * favourites.  Double clicking a row is intended to open the reader view.
 */
public class MainController {
    @FXML private TextField searchField;
    @FXML private TextField tagField;
    @FXML private ChoiceBox<String> categoryChoice;
    @FXML private ChoiceBox<Theme> themeChoice;
    @FXML private Button searchButton;
    @FXML private Button clearFiltersButton;
    @FXML private Button openButton;
    @FXML private Button readButton;
    @FXML private Button infoButton;
    @FXML private Button metadataButton;
    @FXML private Button toggleFavouriteButton;
    @FXML private Button historyButton;
    @FXML private Button favouritesButton;
    @FXML private TableView<Content> contentTable;
    @FXML private TableColumn<Content, Boolean> colFavourite;
    @FXML private TableColumn<Content, String> colTitle;
    @FXML private TableColumn<Content, String> colType;
    @FXML private TableColumn<Content, Number> colSize;
    @FXML private TableColumn<Content, String> colAdded;
    @FXML private TableColumn<Content, String> colCategory;
    @FXML private TableColumn<Content, String> colTags;

    private final ContentService contentService = new ContentService();
    private final FavouriteService favouriteService = new FavouriteService();
    private final ContentQuery currentQuery = new ContentQuery();
    private final Set<Integer> favouriteIds = new HashSet<>();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        // Bind table columns to content properties
        colFavourite.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(
                favouriteIds.contains(data.getValue().getContentID())));
        colFavourite.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || !item) {
                    setText(null);
                } else {
                    setText("★");
                }
                setTooltip(item != null && item ? new Tooltip("Favourite") : null);
                setStyle("-fx-alignment: CENTER;");
            }
        });
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
        colCategory.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getCategory() == null ? "" : data.getValue().getCategory()));
        colTags.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.join(", ", data.getValue().getTags())));
        setupCategoryChoice();
        setupThemeChoice();
        loadContents();
        // Bind buttons to actions
        searchButton.setOnAction(e -> doSearch());
        searchField.setOnAction(e -> doSearch());
        tagField.setOnAction(e -> doSearch());
        clearFiltersButton.setOnAction(e -> clearFilters());
        openButton.setOnAction(e -> addNewFile());
        readButton.setOnAction(e -> Optional.ofNullable(contentTable.getSelectionModel().getSelectedItem()).ifPresent(this::openReader));
        infoButton.setOnAction(e -> Optional.ofNullable(contentTable.getSelectionModel().getSelectedItem()).ifPresent(this::showInfo));
        metadataButton.setOnAction(e -> Optional.ofNullable(contentTable.getSelectionModel().getSelectedItem()).ifPresent(this::editMetadata));
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
        Platform.runLater(() -> ThemeManager.getInstance().register(contentTable.getScene()));
    }

    private void loadContents() {
        refreshFavouritesCache();
        List<Content> list = contentService.listContents(currentQuery);
        for (Content content : list) {
            content.setFavourite(favouriteIds.contains(content.getContentID()));
        }
        ObservableList<Content> items = FXCollections.observableArrayList(list);
        contentTable.setItems(items);
        populateCategoryChoice();
        contentTable.refresh();
    }

    private void doSearch() {
        currentQuery.setKeyword(searchField.getText());
        currentQuery.setTags(parseTags(tagField.getText()));
        String selectedCategory = categoryChoice.getSelectionModel().getSelectedItem();
        currentQuery.setCategory(selectedCategory == null || selectedCategory.equalsIgnoreCase("All") ? "" : selectedCategory);
        loadContents();
    }

    private void clearFilters() {
        searchField.clear();
        tagField.clear();
        if (!categoryChoice.getItems().isEmpty()) {
            categoryChoice.getSelectionModel().selectFirst();
        }
        currentQuery.setKeyword("");
        currentQuery.setCategory("");
        currentQuery.setTags(List.of());
        loadContents();
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
            Content content = contentService.createContentFromFile(file);
            ContentMetadataDialog dialog = new ContentMetadataDialog(content, "Add to library");
            Optional<Content> result = dialog.showAndWait();
            if (result.isPresent()) {
                Content saved = contentService.saveContent(result.get());
                loadContents();
                selectContent(saved);
                showInformationAlert("File added", "Successfully added \"" + saved.getTitle() + "\" to the library.");
            }
        }
    }

    private void showHistory() {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/history.fxml"));
            stage.setTitle("Reading History");
            Scene scene = new Scene(root);
            ThemeManager.getInstance().register(scene);
            stage.setScene(scene);
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
            Scene scene = new Scene(root);
            ThemeManager.getInstance().register(scene);
            stage.setScene(scene);
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
            Scene scene = new Scene(root);
            ThemeManager.getInstance().register(scene);
            stage.setScene(scene);
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
        if (content.getAuthor() != null && !content.getAuthor().isBlank()) {
            builder.append('\n').append("Author: ").append(content.getAuthor());
        }
        if (content.getCategory() != null && !content.getCategory().isBlank()) {
            builder.append('\n').append("Category: ").append(content.getCategory());
        }
        if (!content.getTags().isEmpty()) {
            builder.append('\n').append("Tags: ").append(String.join(", ", content.getTags()));
        }
        if (content.getDescription() != null && !content.getDescription().isBlank()) {
            builder.append('\n').append("Description: ").append(content.getDescription());
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
        boolean isFavourite = favouriteService.toggleFavourite(userId, content.getContentID());
        content.setFavourite(isFavourite);
        if (isFavourite) {
            favouriteIds.add(content.getContentID());
        } else {
            favouriteIds.remove(content.getContentID());
        }
        updateFavouriteButtonText(isFavourite);
        contentTable.refresh();
        showInformationAlert("Favourite updated",
                isFavourite ? "Added to favourites" : "Removed from favourites");
    }

    private void updateSelectionState(Content selected) {
        boolean hasSelection = selected != null;
        readButton.setDisable(!hasSelection);
        infoButton.setDisable(!hasSelection);
        metadataButton.setDisable(!hasSelection);
        boolean favouriteAllowed = hasSelection && CurrentUser.get() != null;
        toggleFavouriteButton.setDisable(!favouriteAllowed);
        if (favouriteAllowed) {
            boolean favourite = favouriteIds.contains(selected.getContentID());
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

    private void setupCategoryChoice() {
        categoryChoice.setItems(FXCollections.observableArrayList("All"));
        categoryChoice.getSelectionModel().selectFirst();
        categoryChoice.setOnAction(e -> doSearch());
    }

    private void populateCategoryChoice() {
        List<String> categories = contentService.getKnownCategories();
        String previous = categoryChoice.getSelectionModel().getSelectedItem();
        categoryChoice.setItems(FXCollections.observableArrayList(categories));
        if (previous != null && categories.contains(previous)) {
            categoryChoice.getSelectionModel().select(previous);
        } else {
            categoryChoice.getSelectionModel().selectFirst();
        }
    }

    private void setupThemeChoice() {
        themeChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(Theme object) {
                return object == Theme.DARK ? "Dark" : "Light";
            }

            @Override
            public Theme fromString(String string) {
                return "Dark".equalsIgnoreCase(string) ? Theme.DARK : Theme.LIGHT;
            }
        });
        themeChoice.setItems(FXCollections.observableArrayList(Theme.values()));
        themeChoice.getSelectionModel().select(ThemeManager.getInstance().getActiveTheme());
        themeChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldTheme, newTheme) -> {
            if (newTheme != null) {
                ThemeManager.getInstance().setTheme(newTheme);
            }
        });
        ThemeManager.getInstance().addListener(theme -> {
            if (themeChoice.getSelectionModel().getSelectedItem() != theme) {
                themeChoice.getSelectionModel().select(theme);
            }
        });
    }

    private List<String> parseTags(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] parts = text.split(",");
        List<String> tags = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }
        return tags;
    }

    private void refreshFavouritesCache() {
        favouriteIds.clear();
        if (CurrentUser.get() == null) {
            return;
        }
        int userId = CurrentUser.get().getUserID();
        List<Favourite> favourites = favouriteService.getFavourites(userId);
        for (Favourite favourite : favourites) {
            favouriteIds.add(favourite.getContentID());
        }
    }

    private void editMetadata(Content content) {
        ContentMetadataDialog dialog = new ContentMetadataDialog(new Content(content), "Edit metadata");
        Optional<Content> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            copyMetadata(content, updated);
            contentService.updateMetadata(updated);
            loadContents();
            selectContent(updated);
        });
    }

    private void copyMetadata(Content target, Content source) {
        target.setTitle(source.getTitle());
        target.setFilePath(source.getFilePath());
        target.setFileType(source.getFileType());
        target.setSizeBytes(source.getSizeBytes());
        target.setDayAdded(source.getDayAdded());
        target.setAuthor(source.getAuthor());
        target.setCategory(source.getCategory());
        target.setTags(source.getTags());
        target.setDescription(source.getDescription());
    }
}
