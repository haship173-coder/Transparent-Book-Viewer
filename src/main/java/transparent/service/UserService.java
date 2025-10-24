package transparent.service;

import transparent.dao.UserDAO;
import transparent.model.User;
import transparent.repository.FileBackedLibraryRepository;

import java.sql.SQLException;

/**
 * Service layer for user operations.  Wraps the {@link UserDAO} to
 * encapsulate error handling.
 */
public class UserService {

    private final UserDAO userDAO = new UserDAO();
    private static boolean databaseAvailable = true;
    private final FileBackedLibraryRepository offlineRepo = FileBackedLibraryRepository.getInstance();

    /**
     * Find an existing user by username or create a new one if none exists.
     *
     * @param username the username provided by the user
     * @return the {@link User} or {@code null} if an error occurred
     */
    public User findOrCreateUser(String username) {
        if (databaseAvailable) {
            try {
                return userDAO.findOrCreateByUsername(username);
            } catch (SQLException e) {
                databaseAvailable = false;
                e.printStackTrace();
            }
        }
        return offlineRepo.findOrCreateUser(username);
    }
}