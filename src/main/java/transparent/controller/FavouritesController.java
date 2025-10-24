package transparent.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import transparent.model.Content;
import transparent.model.Favourite;
import transparent.service.ContentService;
import transparent.service.FavouriteService;
import transparent.ui.ThemeManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the favourites view.  Displays the list of content that
 * the current user has marked as favourite.
 */
public class FavouritesController {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML private TableView<Favourite> favouritesTable;
    @FXML private TableColumn<Favourite, String> colTitle;
    @FXML private TableColumn<Favourite, String> colAdded;
    @FXML private Button backButton;

    private final FavouriteService favouriteService = new FavouriteService();
    private final ContentService contentService = new ContentService();

    @FXML
    private void initialize() {
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(resolveTitle(data.getValue().getContentID())));
        colAdded.setCellValueFactory(data -> new SimpleStringProperty(formatTimestamp(data.getValue())));
        favouritesTable.setPlaceholder(new Label("No favourites yet."));
        loadFavourites();
        backButton.setOnAction(e -> ((Stage) backButton.getScene().getWindow()).close());
        Platform.runLater(() -> ThemeManager.getInstance().register(favouritesTable.getScene()));
    }

    private void loadFavourites() {
        if (CurrentUser.get() == null) {
            favouritesTable.setItems(FXCollections.emptyObservableList());
            return;
        }
        int userId = CurrentUser.get().getUserID();
        List<Favourite> list = favouriteService.getFavourites(userId);
        favouritesTable.setItems(FXCollections.observableArrayList(list));
    }

    private String resolveTitle(int contentId) {
        return contentService.findContent(contentId)
                .map(Content::getTitle)
                .orElse("Content #" + contentId);
    }

    private String formatTimestamp(Favourite favourite) {
        return favourite.getAddedDate() == null ? "-" : FORMATTER.format(favourite.getAddedDate());
    }
}