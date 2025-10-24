package transparent.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import transparent.model.Favourite;
import transparent.service.FavouriteService;

import java.util.List;

/**
 * Controller for the favourites view.  Displays the list of content that
 * the current user has marked as favourite.
 */
public class FavouritesController {
    @FXML private TableView<Favourite> favouritesTable;
    @FXML private TableColumn<Favourite, Integer> colContent;
    @FXML private TableColumn<Favourite, String> colAdded;
    @FXML private Button backButton;

    private final FavouriteService favouriteService = new FavouriteService();

    @FXML
    private void initialize() {
        colContent.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getContentID()).asObject());
        colAdded.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getAddedDate().toString()));
        loadFavourites();
        backButton.setOnAction(e -> ((Stage) backButton.getScene().getWindow()).close());
    }

    private void loadFavourites() {
        if (CurrentUser.get() == null) {
            return;
        }
        int userId = CurrentUser.get().getUserID();
        List<Favourite> list = favouriteService.getFavourites(userId);
        favouritesTable.setItems(javafx.collections.FXCollections.observableArrayList(list));
    }
}