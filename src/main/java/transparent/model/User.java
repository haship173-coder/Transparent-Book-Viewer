package transparent.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a user in the system.  Users are identified by a username only
 * according to the simplified requirements, so no password is stored.
 */
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int userID;
    private String username;

    public User() {
    }

    public User(int userID, String username) {
        this.userID = userID;
        this.username = username;
    }

    public User(String username) {
        this.username = username;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User(User other) {
        this.userID = other.userID;
        this.username = other.username;
    }
}