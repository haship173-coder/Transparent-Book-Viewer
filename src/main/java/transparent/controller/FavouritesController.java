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
import transparent.model.Favourite;
import transparent.service.ContentService;
import transparent.service.FavouriteService;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller for the favourites view.  Displays the list of content that the
 * current user has marked as favourite and allows reopening it directly.
 */
public class FavouritesController {
    @FXML private TableView<Favourite> favouritesTable;
    @FXML private TableColumn<Favourite, String> colContent;
    @FXML private TableColumn<Favourite, String> colAdded;
    @FXML private Button backButton;

    private final FavouriteService favouriteService = new FavouriteService();
    private final ContentService contentService = new ContentService();
    private final Map<Integer, Content> contentLookup = new HashMap<>();
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault());

    @FXML
    private void initialize() {
        colContent.setCellValueFactory(data -> Bindings.createStringBinding(
                () -> resolveTitle(data.getValue().getContentID())));
        colAdded.setCellValueFactory(data -> Bindings.createStringBinding(
                () -> data.getValue().getAddedDate() == null
                        ? ""
                        : FORMATTER.format(data.getValue().getAddedDate())));
        favouritesTable.setRowFactory(tv -> {
            TableRow<Favourite> row = new TableRow<>();
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
        loadFavourites();
        backButton.setOnAction(e -> ((Stage) backButton.getScene().getWindow()).close());
    }

    private void loadFavourites() {
        if (CurrentUser.get() == null) {
            favouritesTable.setItems(FXCollections.emptyObservableList());
            return;
        }
        contentLookup.clear();
        contentService.getAllContents().forEach(content ->
                contentLookup.put(content.getContentID(), content));
        List<Favourite> list = favouriteService.getFavourites(CurrentUser.get().getUserID());
        favouritesTable.setItems(FXCollections.observableArrayList(list));
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
