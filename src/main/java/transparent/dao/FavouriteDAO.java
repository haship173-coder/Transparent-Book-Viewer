package transparent.dao;

import transparent.db.DBConnectionManager;
import transparent.model.Favourite;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object for {@link Favourite}.  Allows adding or removing
 * favourites and retrieving a user's favourites list.
 */
public class FavouriteDAO {

    /**
     * Toggle a favourite entry.  If the user has already favourited the
     * content then the favourite is removed; otherwise it is inserted.
     *
     * @param fav the favourite entry containing the user and content IDs
     * @throws SQLException if a database error occurs
     */
    public void toggleFavourite(Favourite fav) throws SQLException {
        String checkSql = "SELECT FavouriteID FROM Favourites WHERE UserID = ? AND ContentID = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setInt(1, fav.getUserID());
            check.setInt(2, fav.getContentID());
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                // Remove existing favourite
                String deleteSql = "DELETE FROM Favourites WHERE FavouriteID = ?";
                try (PreparedStatement delete = conn.prepareStatement(deleteSql)) {
                    delete.setInt(1, rs.getInt("FavouriteID"));
                    delete.executeUpdate();
                }
            } else {
                // Insert new favourite
                String insertSql = "INSERT INTO Favourites (UserID, ContentID, AddedDate) VALUES (?, ?, GETDATE())";
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    insert.setInt(1, fav.getUserID());
                    insert.setInt(2, fav.getContentID());
                    insert.executeUpdate();
                }
            }
        }
    }

    /**
     * Retrieve all favourites for a given user ordered by date added descending.
     *
     * @param userId the ID of the user
     * @return a list of favourites
     * @throws SQLException if a database error occurs
     */
    public List<Favourite> getFavouritesByUser(int userId) throws SQLException {
        String sql = "SELECT * FROM Favourites WHERE UserID = ? ORDER BY AddedDate DESC";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            List<Favourite> list = new ArrayList<>();
            while (rs.next()) {
                Favourite fav = new Favourite(
                        rs.getInt("FavouriteID"),
                        rs.getInt("UserID"),
                        rs.getInt("ContentID"),
                        rs.getTimestamp("AddedDate").toLocalDateTime()
                );
                list.add(fav);
            }
            return list;
        }
    }

    /**
     * Determine whether a user has marked the given content as a favourite.
     *
     * @param userId    the user ID
     * @param contentId the content ID
     * @return {@code true} if a favourite entry exists
     * @throws SQLException if a database error occurs
     */
    public boolean isFavourite(int userId, int contentId) throws SQLException {
        String sql = "SELECT 1 FROM Favourites WHERE UserID = ? AND ContentID = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, contentId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }
}