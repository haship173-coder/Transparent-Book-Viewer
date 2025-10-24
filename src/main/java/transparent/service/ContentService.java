package transparent.service;

import transparent.dao.ContentDAO;
import transparent.model.Content;
import transparent.repository.FileBackedLibraryRepository;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for managing {@link Content}.  Wraps the DAO calls and
 * transparently falls back to a local file-backed repository when the database
 * is unreachable.
 */
public class ContentService {

    private final ContentDAO contentDAO = new ContentDAO();
    private final FileBackedLibraryRepository repository = FileBackedLibraryRepository.getInstance();

    /**
     * Retrieve content entries according to the supplied {@link ContentQuery}.
     * Results are sourced from the database when available, otherwise the local
     * offline repository is used.
     */
    public List<Content> listContents(ContentQuery query) {
        List<Content> contents = fetchContents();
        return applyFilters(contents, query);
    }

    public List<Content> getAllContents() {
        return listContents(new ContentQuery());
    }

    private List<Content> fetchContents() {
        try {
            List<Content> dbContents = contentDAO.getAllContents();
            return repository.mergeFromDatabase(dbContents);
        } catch (SQLException e) {
            return repository.getAllContents();
        }
    }

    private List<Content> applyFilters(List<Content> contents, ContentQuery query) {
        if (query == null) {
            return contents;
        }
        return contents.stream()
                .filter(content -> matchesKeyword(content, query))
                .filter(content -> matchesCategory(content, query))
                .filter(content -> matchesTags(content, query))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean matchesKeyword(Content content, ContentQuery query) {
        if (!query.hasKeyword()) {
            return true;
        }
        String keyword = query.getKeyword().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(content.getTitle(), keyword)
                || containsIgnoreCase(content.getAuthor(), keyword)
                || content.getTags().stream().anyMatch(tag -> containsIgnoreCase(tag, keyword));
    }

    private boolean matchesCategory(Content content, ContentQuery query) {
        if (!query.hasCategory()) {
            return true;
        }
        String category = query.getCategory();
        return category.equalsIgnoreCase("All")
                || containsIgnoreCase(content.getCategory(), category);
    }

    private boolean matchesTags(Content content, ContentQuery query) {
        if (!query.hasTags()) {
            return true;
        }
        List<String> tags = content.getTags().stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        return tags.containsAll(query.getTags());
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    /**
     * Build a {@link Content} instance from a selected file.  This method does
     * not persist the content; call {@link #saveContent(Content)} to store it.
     */
    public Content createContentFromFile(File file) {
        String name = file.getName();
        String type = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            type = name.substring(dot + 1).toUpperCase(Locale.ROOT);
        }
        long size = file.length();
        Content content = new Content(name, file.getAbsolutePath(), type, size);
        content.setDayAdded(LocalDateTime.now());
        return content;
    }

    /**
     * Persist content metadata, attempting to write to the database first and
     * falling back to the local repository if required.
     */
    public Content saveContent(Content content) {
        try {
            if (content.getContentID() <= 0) {
                contentDAO.insertContent(content);
            }
            repository.updateContentMetadata(content);
            return content;
        } catch (SQLException e) {
            Content stored = repository.saveOfflineContent(content);
            content.setContentID(stored.getContentID());
            return stored;
        }
    }

    public void updateMetadata(Content content) {
        repository.updateContentMetadata(content);
    }

    public Optional<Content> findContent(int contentId) {
        return repository.findContent(contentId);
    }

    public List<String> getKnownCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("All");
        categories.addAll(repository.listCategories());
        return categories;
    }

    public List<String> getKnownTags() {
        return repository.listTags();
    }
}