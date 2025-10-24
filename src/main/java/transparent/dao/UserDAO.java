package transparent.dao;

import transparent.db.DBConnectionManager;
import transparent.model.User;

import java.sql.*;

/**
 * Data access object for {@link User}.  Provides methods for looking up
 * existing users or creating new ones by username.  Since the system only
 * requires a username (no password), this DAO does not handle authentication
 * and assumes names are unique.
 */
public class UserDAO {

    /**
     * Find a user by username or create a new user if none exists.
     *
     * @param username the username to find or create
     * @return the {@link User} object representing the found or newly created user
     * @throws SQLException if a database error occurs
     */
    public User findOrCreateByUsername(String username) throws SQLException {
        // First, attempt to find an existing user
        String selectSql = "SELECT UserID, Username FROM Users WHERE Username = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("UserID"), rs.getString("Username"));
            }
        }

        // No existing user found; insert a new user
        String insertSql = "INSERT INTO Users (Username) OUTPUT INSERTED.UserID VALUES (?)";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int newId = rs.getInt(1);
                return new User(newId, username);
            }
        }
        throw new SQLException("Unable to create user");
    }
}