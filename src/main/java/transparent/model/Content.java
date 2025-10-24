package transparent.model;

import java.time.LocalDateTime;

/**
 * Represents a piece of content in the library.  This could be a book, comic
 * or image.  The metadata stored here reflects the database schema: title,
 * file path, type, size and the date it was added to the library.
 */
public class Content {
    private int contentID;
    private String title;
    private String filePath;
    private String fileType;
    private long sizeBytes;
    private LocalDateTime dayAdded;

    public Content() {
    }

    public Content(int contentID, String title, String filePath, String fileType,
                   long sizeBytes, LocalDateTime dayAdded) {
        this.contentID = contentID;
        this.title = title;
        this.filePath = filePath;
        this.fileType = fileType;
        this.sizeBytes = sizeBytes;
        this.dayAdded = dayAdded;
    }

    public Content(String title, String filePath, String fileType, long sizeBytes) {
        this.title = title;
        this.filePath = filePath;
        this.fileType = fileType;
        this.sizeBytes = sizeBytes;
    }

    public int getContentID() {
        return contentID;
    }

    public void setContentID(int contentID) {
        this.contentID = contentID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public LocalDateTime getDayAdded() {
        return dayAdded;
    }

    public void setDayAdded(LocalDateTime dayAdded) {
        this.dayAdded = dayAdded;
    }
}