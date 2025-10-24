package transparent.service;

import transparent.dao.FavouriteDAO;
import transparent.model.Favourite;
import transparent.repository.FileBackedLibraryRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for managing favourites.  Provides methods to toggle
 * favourites and retrieve a user's list of favourites while supporting
 * offline persistence.
 */
public class FavouriteService {

    private final FavouriteDAO favouriteDAO = new FavouriteDAO();
    private final FileBackedLibraryRepository repository = FileBackedLibraryRepository.getInstance();

    /**
     * Toggle the favourite status of a content item for a user.
     *
     * @return {@code true} if the item is now favourite, otherwise {@code false}
     */
    public boolean toggleFavourite(int userId, int contentId) {
        Favourite fav = new Favourite(userId, contentId);
        try {
            favouriteDAO.toggleFavourite(fav);
            boolean favourite = favouriteDAO.isFavourite(userId, contentId);
            repository.setFavouriteState(userId, contentId, favourite, LocalDateTime.now());
            return favourite;
        } catch (SQLException e) {
            return repository.toggleFavourite(userId, contentId);
        }
    }

    /**
     * Retrieve a user's favourites list.
     */
    public List<Favourite> getFavourites(int userId) {
        try {
            List<Favourite> favourites = favouriteDAO.getFavouritesByUser(userId);
            return repository.mergeFavouritesFromDatabase(userId, favourites);
        } catch (SQLException e) {
            return repository.getFavourites(userId);
        }
    }

    /**
     * Check whether the given content is marked as favourite by the user.
     */
    public boolean isFavourite(int userId, int contentId) {
        try {
            boolean favourite = favouriteDAO.isFavourite(userId, contentId);
            repository.setFavouriteState(userId, contentId, favourite, LocalDateTime.now());
            return favourite;
        } catch (SQLException e) {
            return repository.isFavourite(userId, contentId);
        }
    }
}