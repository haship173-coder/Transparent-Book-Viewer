package transparent.model;

import java.time.LocalDateTime;

/**
 * Represents a favourite entry linking a user to a piece of content.  When a
 * user marks content as a favourite the current date and time is stored.
 */
public class Favourite {
    private int favouriteID;
    private int userID;
    private int contentID;
    private LocalDateTime addedDate;

    public Favourite() {
    }

    public Favourite(int favouriteID, int userID, int contentID, LocalDateTime addedDate) {
        this.favouriteID = favouriteID;
        this.userID = userID;
        this.contentID = contentID;
        this.addedDate = addedDate;
    }

    public Favourite(int userID, int contentID) {
        this.userID = userID;
        this.contentID = contentID;
    }

    public int getFavouriteID() {
        return favouriteID;
    }

    public void setFavouriteID(int favouriteID) {
        this.favouriteID = favouriteID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public int getContentID() {
        return contentID;
    }

    public void setContentID(int contentID) {
        this.contentID = contentID;
    }

    public LocalDateTime getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(LocalDateTime addedDate) {
        this.addedDate = addedDate;
    }
}