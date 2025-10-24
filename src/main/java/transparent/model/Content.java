package transparent.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a piece of content in the library.  This could be a book, comic
 * or image.  The metadata stored here reflects the database schema: title,
 * file path, type, size and the date it was added to the library.  Additional
 * metadata such as category and tags are also supported for the offline
 * storage mode.
 */
public class Content implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int contentID;
    private String title;
    private String filePath;
    private String fileType;
    private long sizeBytes;
    private LocalDateTime dayAdded;
    private String category;
    private String tags;

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

    public Content(Content other) {
        this.contentID = other.contentID;
        this.title = other.title;
        this.filePath = other.filePath;
        this.fileType = other.fileType;
        this.sizeBytes = other.sizeBytes;
        this.dayAdded = other.dayAdded;
        this.category = other.category;
        this.tags = other.tags;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * Utility method that returns the tags as an array, splitting on comma.
     */
    public String[] getTagArray() {
        if (tags == null || tags.isBlank()) {
            return new String[0];
        }
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}