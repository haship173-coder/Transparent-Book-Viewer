package transparent.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import transparent.model.Content;
import transparent.model.Favourite;
import transparent.service.ContentService;
import transparent.service.FavouriteService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for the main library view.  Displays the list of content and
 * provides actions for searching, filtering, adding files and navigating to
 * history or favourites.  Users can open items in the reader by double
 * clicking the table rows.
 */
public class MainController {
    @FXML private TextField searchField;
    @FXML private TextField tagsField;
    @FXML private Button searchButton;
    @FXML private Button openButton;
    @FXML private Button historyButton;
    @FXML private Button favouritesButton;
    @FXML private Button themeButton;
    @FXML private Button toggleFavouriteButton;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TableView<Content> contentTable;
    @FXML private TableColumn<Content, String> colTitle;
    @FXML private TableColumn<Content, String> colType;
    @FXML private TableColumn<Content, String> colCategory;
    @FXML private TableColumn<Content, String> colTags;
    @FXML private TableColumn<Content, String> colSize;
    @FXML private TableColumn<Content, String> colAdded;
    @FXML private TableColumn<Content, String> colFavourite;

    private final ContentService contentService = new ContentService();
    private final FavouriteService favouriteService = new FavouriteService();
    private final List<Content> allContents = new ArrayList<>();
    private final Set<Integer> favouriteIds = new HashSet<>();

    private boolean darkTheme = false;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        configureTableColumns();
        bindActions();
        loadContents();
        Platform.runLater(this::applyTheme);
    }

    private void configureTableColumns() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colType.setCellValueFactory(new PropertyValueFactory<>("fileType"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colTags.setCellValueFactory(new PropertyValueFactory<>("tags"));
        colSize.setCellValueFactory(data -> Bindings.createStringBinding(
                () -> formatSize(data.getValue().getSizeBytes())));
        colAdded.setCellValueFactory(data -> Bindings.createStringBinding(
                () -> formatDate(data.getValue().getDayAdded())));
        colFavourite.setCellValueFactory(data -> Bindings.createStringBinding(
                () -> favouriteIds.contains(data.getValue().getContentID()) ? "★" : ""));
        contentTable.setRowFactory(tv -> {
            TableRow<Content> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openReader(row.getItem());
                }
            });
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    row.setContextMenu(null);
                } else {
                    ContextMenu menu = new ContextMenu();
                    MenuItem openItem = new MenuItem("Open");
                    openItem.setOnAction(e -> openReader(newItem));
                    MenuItem favouriteItem = new MenuItem("Toggle favourite");
                    favouriteItem.setOnAction(e -> toggleFavourite(newItem));
                    menu.getItems().addAll(openItem, favouriteItem);
                    row.setContextMenu(menu);
                }
            });
            return row;
        });
    }

    private void bindActions() {
        searchButton.setOnAction(e -> applyFilters());
        openButton.setOnAction(e -> addNewFile());
        historyButton.setOnAction(e -> showHistory());
        favouritesButton.setOnAction(e -> showFavourites());
        themeButton.setOnAction(e -> toggleTheme());
        toggleFavouriteButton.setOnAction(e -> {
            Content selected = contentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                toggleFavourite(selected);
            }
        });
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        tagsField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        categoryFilter.setOnAction(e -> applyFilters());
        contentTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> updateFavouriteButtonState());
    }

    private void loadContents() {
        allContents.clear();
        allContents.addAll(contentService.getAllContents());
        loadFavouriteIds();
        refreshCategoryFilter();
        applyFilters();
    }

    private void loadFavouriteIds() {
        favouriteIds.clear();
        if (CurrentUser.get() == null) {
            return;
        }
        List<Favourite> favourites = favouriteService.getFavourites(CurrentUser.get().getUserID());
        favourites.forEach(f -> favouriteIds.add(f.getContentID()));
    }

    private void refreshCategoryFilter() {
        String previous = categoryFilter.getValue();
        Set<String> categories = allContents.stream()
                .map(Content::getCategory)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
        List<String> items = new ArrayList<>();
        items.add("All categories");
        List<String> sorted = categories.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        items.addAll(sorted);
        categoryFilter.getItems().setAll(items);
        if (previous != null && items.contains(previous)) {
            categoryFilter.getSelectionModel().select(previous);
        } else if (!items.isEmpty()) {
            categoryFilter.getSelectionModel().selectFirst();
        }
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String category = categoryFilter.getValue();
        String tags = tagsField.getText() == null ? "" : tagsField.getText().trim().toLowerCase(Locale.ROOT);

        List<Content> filtered = allContents.stream()
                .filter(content -> keyword.isEmpty() || containsIgnoreCase(content.getTitle(), keyword))
                .filter(content -> matchesCategory(content, category))
                .filter(content -> matchesTags(content, tags))
                .sorted(Comparator.comparing(Content::getDayAdded, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        ObservableList<Content> observableList = FXCollections.observableArrayList(filtered);
        contentTable.setItems(observableList);
        updateFavouriteButtonState();
    }

    private boolean matchesCategory(Content content, String selectedCategory) {
        if (selectedCategory == null || "All categories".equalsIgnoreCase(selectedCategory)) {
            return true;
        }
        String category = content.getCategory();
        return category != null && category.equalsIgnoreCase(selectedCategory);
    }

    private boolean matchesTags(Content content, String tagsFilter) {
        if (tagsFilter.isEmpty()) {
            return true;
        }
        String[] tokens = java.util.Arrays.stream(tagsFilter.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (tokens.length == 0) {
            return true;
        }
        String contentTags = content.getTags();
        if (contentTags == null) {
            return false;
        }
        String lowerTags = contentTags.toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (lowerTags.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String source, String query) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(query);
    }

    private void addNewFile() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        java.io.File file = chooser.showOpenDialog(openButton.getScene().getWindow());
        if (file == null) {
            return;
        }
        String defaultTitle = file.getName();
        javafx.scene.control.TextInputDialog titleDialog = new javafx.scene.control.TextInputDialog(defaultTitle);
        titleDialog.setTitle("Add content");
        titleDialog.setHeaderText("Nhập tiêu đề hiển thị cho nội dung");
        titleDialog.setContentText("Tiêu đề:");
        Optional<String> title = titleDialog.showAndWait();
        if (title.isEmpty()) {
            return;
        }

        javafx.scene.control.TextInputDialog categoryDialog = new javafx.scene.control.TextInputDialog();
        categoryDialog.setTitle("Category");
        categoryDialog.setHeaderText("Nhập thể loại cho nội dung (ví dụ: Manga, Novel, Art)");
        categoryDialog.setContentText("Thể loại:");
        String category = categoryDialog.showAndWait().orElse("");

        javafx.scene.control.TextInputDialog tagsDialog = new javafx.scene.control.TextInputDialog();
        tagsDialog.setTitle("Tags");
        tagsDialog.setHeaderText("Nhập tags (ngăn cách bằng dấu phẩy)");
        tagsDialog.setContentText("Tags:");
        String tags = tagsDialog.showAndWait().orElse("");

        Content content = contentService.addContentFromFile(file, title.get(), category, tags);
        if (content != null) {
            allContents.add(content);
            loadFavouriteIds();
            refreshCategoryFilter();
            applyFilters();
        }
    }

    private void showHistory() {
        loadWindow("/history.fxml", "Reading History");
    }

    private void showFavourites() {
        loadWindow("/favourites.fxml", "Favourites");
    }

    private void loadWindow(String resource, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> {
                loadFavouriteIds();
                applyFilters();
            });
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openReader(Content content) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/reader.fxml"));
            Parent root = loader.load();
            ReaderController controller = loader.getController();
            controller.setContent(content);
            Stage stage = new Stage();
            stage.setTitle(content.getTitle());
            Scene scene = new Scene(root, 900, 650);
            stage.setScene(scene);
            stage.setOnHidden(e -> {
                loadFavouriteIds();
                applyFilters();
            });
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toggleFavourite(Content content) {
        if (CurrentUser.get() == null) {
            return;
        }
        favouriteService.toggleFavourite(CurrentUser.get().getUserID(), content.getContentID());
        loadFavouriteIds();
        contentTable.refresh();
        updateFavouriteButtonState();
    }

    private void updateFavouriteButtonState() {
        Content selected = contentTable.getSelectionModel().getSelectedItem();
        boolean hasSelection = selected != null;
        toggleFavouriteButton.setDisable(!hasSelection || CurrentUser.get() == null);
        if (hasSelection && CurrentUser.get() != null) {
            boolean favourite = favouriteIds.contains(selected.getContentID());
            toggleFavouriteButton.setText(favourite ? "Bỏ yêu thích" : "Thêm yêu thích");
        } else {
            toggleFavouriteButton.setText("Thêm yêu thích");
        }
    }

    private void toggleTheme() {
        darkTheme = !darkTheme;
        applyTheme();
    }

    private void applyTheme() {
        if (themeButton.getScene() == null) {
            return;
        }
        String stylesheet = darkTheme ? "/theme-dark.css" : "/theme-light.css";
        themeButton.getScene().getStylesheets().setAll(getClass().getResource(stylesheet).toExternalForm());
        themeButton.setText(darkTheme ? "Chế độ sáng" : "Chế độ tối");
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_FORMATTER.format(dateTime);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.ROOT, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
