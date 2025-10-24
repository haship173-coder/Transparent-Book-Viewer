package transparent.service;

import transparent.dao.UserDAO;
import transparent.model.User;
import transparent.repository.FileBackedLibraryRepository;

import java.sql.SQLException;

/**
 * Service layer for user operations.  Wraps the {@link UserDAO} and falls back
 * to the local repository when the database is unavailable.
 */
public class UserService {

    private final UserDAO userDAO = new UserDAO();
    private final FileBackedLibraryRepository repository = FileBackedLibraryRepository.getInstance();

    /**
     * Find an existing user by username or create a new one if none exists.
     */
    public User findOrCreateUser(String username) {
        try {
            User user = userDAO.findOrCreateByUsername(username);
            if (user != null) {
                return repository.mergeUser(user);
            }
        } catch (SQLException e) {
            // fall through to offline repository
        }
        return repository.findOrCreateUser(username);
    }
}