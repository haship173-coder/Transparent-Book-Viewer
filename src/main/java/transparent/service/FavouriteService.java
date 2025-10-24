package transparent.service;

import transparent.dao.FavouriteDAO;
import transparent.model.Favourite;
import transparent.repository.FileBackedLibraryRepository;

import java.sql.SQLException;
import java.util.List;

/**
 * Service layer for managing favourites.  Provides methods to toggle
 * favourites and retrieve a user's list of favourites.
 */
public class FavouriteService {

    private final FavouriteDAO favouriteDAO = new FavouriteDAO();
    private static boolean databaseAvailable = true;
    private final FileBackedLibraryRepository offlineRepo = FileBackedLibraryRepository.getInstance();

    /**
     * Toggle the favourite status of a content item for a user.
     *
     * @param userId the user ID
     * @param contentId the content ID
     */
    public boolean toggleFavourite(int userId, int contentId) {
        Favourite fav = new Favourite(userId, contentId);
        if (databaseAvailable) {
            try {
                return favouriteDAO.toggleFavourite(fav);
            } catch (SQLException e) {
                databaseAvailable = false;
                e.printStackTrace();
            }
        }
        return offlineRepo.toggleFavourite(userId, contentId);
    }

    /**
     * Retrieve a user's favourites list.
     *
     * @param userId the user ID
     * @return list of favourites or empty list on error
     */
    public List<Favourite> getFavourites(int userId) {
        if (databaseAvailable) {
            try {
                return favouriteDAO.getFavouritesByUser(userId);
            } catch (SQLException e) {
                databaseAvailable = false;
                e.printStackTrace();
            }
        }
        return offlineRepo.getFavourites(userId);
    }

    /**
     * Determine whether the given content is a favourite for the user.
     */
    public boolean isFavourite(int userId, int contentId) {
        if (databaseAvailable) {
            try {
                return favouriteDAO.isFavourite(userId, contentId);
            } catch (SQLException e) {
                databaseAvailable = false;
                e.printStackTrace();
            }
        }
        return offlineRepo.isFavourite(userId, contentId);
    }
}