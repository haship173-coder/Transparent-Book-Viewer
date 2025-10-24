package transparent.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a piece of content in the library.  This could be a book, comic
 * or image.  The metadata stored here reflects the database schema: title,
 * file path, type, size and the date it was added to the library.  Additional
 * descriptive metadata such as author, category and tags are stored to support
 * the enhanced browsing experience and offline persistence layer.
 */
public class Content implements Serializable {
    private static final long serialVersionUID = 1L;
    private int contentID;
    private String title;
    private String filePath;
    private String fileType;
    private long sizeBytes;
    private LocalDateTime dayAdded;
    private String author;
    private String category;
    private List<String> tags = new ArrayList<>();
    private String description;
    private transient boolean favourite;

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
        this.author = other.author;
        this.category = other.category;
        setTags(other.tags);
        this.description = other.description;
        this.favourite = other.favourite;
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }
}