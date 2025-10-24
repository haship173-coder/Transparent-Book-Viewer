package transparent.service;

import transparent.dao.UserDAO;
import transparent.model.User;

import java.sql.SQLException;

/**
 * Service layer for user operations.  Wraps the {@link UserDAO} to
 * encapsulate error handling.
 */
public class UserService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Find an existing user by username or create a new one if none exists.
     *
     * @param username the username provided by the user
     * @return the {@link User} or {@code null} if an error occurred
     */
    public User findOrCreateUser(String username) {
        try {
            return userDAO.findOrCreateByUsername(username);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}