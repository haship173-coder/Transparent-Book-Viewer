package transparent.service;

import transparent.dao.ContentDAO;
import transparent.model.Content;
import transparent.repository.FileBackedLibraryRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Service layer for managing {@link Content}.  Wraps the DAO calls and
 * encapsulates business logic for interacting with content entries.  When the
 * SQL Server database is not reachable the service automatically falls back to
 * the {@link FileBackedLibraryRepository} so the application keeps working
 * offline.
 */
public class ContentService {

    private final ContentDAO contentDAO = new ContentDAO();
    private static boolean databaseAvailable = true;
    private final FileBackedLibraryRepository offlineRepo = FileBackedLibraryRepository.getInstance();

    /**
     * Retrieve all content entries from the database or offline store.
     *
     * @return list of content entries or an empty list on failure
     */
    public List<Content> getAllContents() {
        if (databaseAvailable) {
            try {
                return contentDAO.getAllContents();
            } catch (SQLException e) {
                databaseAvailable = false;
                e.printStackTrace();
            }
        }
        return offlineRepo.getAllContents();
    }

    /**
     * Search content entries by title.
     *
     * @param keyword the search keyword
     * @return list of matching content entries or an empty list on error
     */
    public List<Content> searchContents(String keyword) {
        if (databaseAvailable) {
            try {
                return contentDAO.searchByTitle(keyword);
            } catch (SQLException e) {
                databaseAvailable = false;
                e.printStackTrace();
            }
        }
        return offlineRepo.searchContents(keyword);
    }

    /**
     * Create a new content entry from a file on disk.  This extracts the
     * filename, file type and size automatically.  The file is not copied;
     * only its path is stored.
     *
     * @param file     the file selected by the user
     * @param category optional category provided by the user
     * @param tags     optional comma separated tags
     * @return the {@link Content} object with its ID populated
     */
    public Content addContentFromFile(java.io.File file, String titleOverride, String category, String tags) {
        String displayName = (titleOverride == null || titleOverride.isBlank())
                ? file.getName()
                : titleOverride.trim();
        String type = "";
        String originalName = file.getName();
        int dot = originalName.lastIndexOf('.');
        if (dot > 0) {
            type = originalName.substring(dot + 1).toUpperCase(Locale.ROOT);
        }
        long size = file.length();
        Content content = new Content(displayName, file.getAbsolutePath(), type, size);
        content.setDayAdded(LocalDateTime.now());
        content.setCategory(category == null || category.isBlank() ? "Uncategorized" : category.trim());
        content.setTags(tags);
        if (databaseAvailable) {
            try {
                contentDAO.insertContent(content);
                return content;
            } catch (SQLException e) {
                databaseAvailable = false;
                e.printStackTrace();
            }
        }
        return offlineRepo.addContentFromFile(file, displayName, category, tags);
    }

    /**
     * Retrieve a content entry by its ID.
     *
     * @param contentId the identifier to look up
     * @return the content or {@code null} if not found
     */
    public Content getContentById(int contentId) {
        if (databaseAvailable) {
            try {
                return contentDAO.getContentById(contentId);
            } catch (SQLException e) {
                databaseAvailable = false;
                e.printStackTrace();
            }
        }
        return offlineRepo.getContentById(contentId);
    }
}
