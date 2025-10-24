package transparent.service;

import transparent.dao.ContentDAO;
import transparent.model.Content;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for managing {@link Content}.  Wraps the DAO calls and
 * encapsulates business logic (currently minimal) for interacting with
 * content entries.
 */
public class ContentService {

    private final ContentDAO contentDAO = new ContentDAO();

    /**
     * Retrieve all content entries from the database.
     *
     * @return list of content entries or an empty list on failure
     */
    public List<Content> getAllContents() {
        try {
            return contentDAO.getAllContents();
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Search content entries by title.
     *
     * @param keyword the search keyword
     * @return list of matching content entries or an empty list on error
     */
    public List<Content> searchContents(String keyword) {
        try {
            return contentDAO.searchByTitle(keyword);
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Create a new content entry from a file on disk.  This extracts the
     * filename, file type and size automatically.  The file is not copied;
     * only its path is stored.
     *
     * @param file the file selected by the user
     * @return the {@link Content} object with its ID populated
     */
    public Content addContentFromFile(java.io.File file) {
        String name = file.getName();
        String type = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            type = name.substring(dot + 1).toUpperCase();
        }
        long size = file.length();
        Content content = new Content(name, file.getAbsolutePath(), type, size);
        try {
            contentDAO.insertContent(content);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        content.setDayAdded(LocalDateTime.now());
        return content;
    }
}