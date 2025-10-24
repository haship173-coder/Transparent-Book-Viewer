package transparent.service;

import transparent.dao.FavouriteDAO;
import transparent.model.Favourite;

import java.sql.SQLException;
import java.util.List;

/**
 * Service layer for managing favourites.  Provides methods to toggle
 * favourites and retrieve a user's list of favourites.
 */
public class FavouriteService {

    private final FavouriteDAO favouriteDAO = new FavouriteDAO();

    /**
     * Toggle the favourite status of a content item for a user.
     *
     * @param userId the user ID
     * @param contentId the content ID
     */
    public void toggleFavourite(int userId, int contentId) {
        Favourite fav = new Favourite(userId, contentId);
        try {
            favouriteDAO.toggleFavourite(fav);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve a user's favourites list.
     *
     * @param userId the user ID
     * @return list of favourites or empty list on error
     */
    public List<Favourite> getFavourites(int userId) {
        try {
            return favouriteDAO.getFavouritesByUser(userId);
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }
}