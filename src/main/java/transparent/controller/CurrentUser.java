package transparent.controller;

import transparent.model.User;

/**
 * A simple holder for the currently logged in user.  Using a static field
 * allows access across different controllers without a more complex session
 * management system.  In a larger application you would avoid statics and
 * use dependency injection or a proper context.
 */
public final class CurrentUser {
    private static User current;

    private CurrentUser() {
        // prevent instantiation
    }

    /**
     * Set the current user.
     *
     * @param user the user that has logged in
     */
    public static void set(User user) {
        current = user;
    }

    /**
     * Get the current user.
     *
     * @return the logged in user or {@code null} if none
     */
    public static User get() {
        return current;
    }
}