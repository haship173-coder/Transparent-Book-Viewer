package transparent.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a history entry for a user reading a piece of content.  It
 * records the last page read and the last time the content was opened.
 */
public class HistoryRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private int historyID;
    private int userID;
    private int contentID;
    private LocalDateTime lastReadTime;
    private int pageNumber;

    public HistoryRecord() {
    }

    public HistoryRecord(int historyID, int userID, int contentID,
                         LocalDateTime lastReadTime, int pageNumber) {
        this.historyID = historyID;
        this.userID = userID;
        this.contentID = contentID;
        this.lastReadTime = lastReadTime;
        this.pageNumber = pageNumber;
    }

    public HistoryRecord(int userID, int contentID, int pageNumber) {
        this.userID = userID;
        this.contentID = contentID;
        this.pageNumber = pageNumber;
    }

    public HistoryRecord(HistoryRecord other) {
        this.historyID = other.historyID;
        this.userID = other.userID;
        this.contentID = other.contentID;
        this.lastReadTime = other.lastReadTime;
        this.pageNumber = other.pageNumber;
    }

    public int getHistoryID() {
        return historyID;
    }

    public void setHistoryID(int historyID) {
        this.historyID = historyID;
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

    public LocalDateTime getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(LocalDateTime lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }
}