package transparent.repository;

import transparent.model.Content;
import transparent.model.Favourite;
import transparent.model.HistoryRecord;
import transparent.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A lightweight repository that stores library data on disk using standard
 * Java serialization.  This class acts as a drop-in replacement for the SQL
 * Server based DAOs whenever a database connection is not available, enabling
 * the application to run fully offline (for example in the packaged Windows
 * executable).
 */
public final class FileBackedLibraryRepository {

    private static final FileBackedLibraryRepository INSTANCE = new FileBackedLibraryRepository();

    private final Path dataFile;
    private LibraryState state;

    private FileBackedLibraryRepository() {
        Path dataDir = Path.of(System.getProperty("user.home"), ".transparent");
        this.dataFile = dataDir.resolve("library.dat");
        this.state = load();
    }

    public static FileBackedLibraryRepository getInstance() {
        return INSTANCE;
    }

    private synchronized LibraryState load() {
        try {
            if (Files.exists(dataFile)) {
                try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(dataFile))) {
                    Object obj = in.readObject();
                    if (obj instanceof LibraryState loaded) {
                        return loaded;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new LibraryState();
    }

    private synchronized void persist() {
        try {
            Files.createDirectories(dataFile.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(dataFile))) {
                out.writeObject(state);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized User findOrCreateUser(String username) {
        Optional<User> existing = state.users.values().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
        if (existing.isPresent()) {
            return new User(existing.get());
        }
        User user = new User(state.nextUserId++, username);
        state.users.put(user.getUserID(), user);
        persist();
        return new User(user);
    }

    public synchronized List<Content> getAllContents() {
        return state.contents.values().stream()
                .sorted(Comparator.comparing(Content::getDayAdded, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(Content::new)
                .collect(Collectors.toList());
    }

    public synchronized List<Content> searchContents(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllContents();
        }
        String lower = keyword.toLowerCase(Locale.ROOT);
        return state.contents.values().stream()
                .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase(Locale.ROOT).contains(lower))
                .sorted(Comparator.comparing(Content::getDayAdded, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(Content::new)
                .collect(Collectors.toList());
    }

    public synchronized Content addContentFromFile(java.io.File file, String title, String category, String tags) {
        Content content = new Content();
        content.setContentID(state.nextContentId++);
        content.setTitle(title == null || title.isBlank() ? file.getName() : title.trim());
        content.setFilePath(file.getAbsolutePath());
        String type = "";
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            type = name.substring(dot + 1).toUpperCase(Locale.ROOT);
        }
        content.setFileType(type);
        content.setSizeBytes(file.length());
        content.setDayAdded(LocalDateTime.now());
        content.setCategory(normalizeCategory(category));
        content.setTags(normalizeTags(tags));
        state.contents.put(content.getContentID(), content);
        persist();
        return new Content(content);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "Uncategorized";
        }
        return category.trim();
    }

    private String normalizeTags(String tags) {
        if (tags == null) {
            return null;
        }
        String normalized = java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
        return normalized.isEmpty() ? null : normalized;
    }

    public synchronized List<String> getAllCategories() {
        List<String> categories = state.contents.values().stream()
                .map(Content::getCategory)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toCollection(ArrayList::new));
        if (!categories.contains("Uncategorized")) {
            categories.add(0, "Uncategorized");
        }
        return categories;
    }

    public synchronized boolean toggleFavourite(int userId, int contentId) {
        String key = key(userId, contentId);
        Favourite existing = state.favourites.get(key);
        if (existing != null) {
            state.favourites.remove(key);
            persist();
            return false;
        }
        Favourite favourite = new Favourite(state.nextFavouriteId++, userId, contentId, LocalDateTime.now());
        state.favourites.put(key, favourite);
        persist();
        return true;
    }

    public synchronized boolean isFavourite(int userId, int contentId) {
        return state.favourites.containsKey(key(userId, contentId));
    }

    public synchronized List<Favourite> getFavourites(int userId) {
        return state.favourites.values().stream()
                .filter(f -> f.getUserID() == userId)
                .sorted(Comparator.comparing(Favourite::getAddedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(Favourite::new)
                .collect(Collectors.toList());
    }

    public synchronized void saveProgress(int userId, int contentId, int pageNumber) {
        String key = key(userId, contentId);
        HistoryRecord record = state.history.get(key);
        if (record == null) {
            record = new HistoryRecord(state.nextHistoryId++, userId, contentId, LocalDateTime.now(), pageNumber);
            state.history.put(key, record);
        } else {
            record.setPageNumber(pageNumber);
            record.setLastReadTime(LocalDateTime.now());
        }
        persist();
    }

    public synchronized List<HistoryRecord> getHistory(int userId) {
        return state.history.values().stream()
                .filter(h -> h.getUserID() == userId)
                .sorted(Comparator.comparing(HistoryRecord::getLastReadTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(HistoryRecord::new)
                .collect(Collectors.toList());
    }

    public synchronized Content getContentById(int contentId) {
        Content content = state.contents.get(contentId);
        return content == null ? null : new Content(content);
    }

    private String key(int userId, int contentId) {
        return userId + ":" + contentId;
    }

    /**
     * Serializable container storing the in-memory state of the library.
     */
    private static class LibraryState implements java.io.Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private int nextUserId = 1;
        private int nextContentId = 1;
        private int nextHistoryId = 1;
        private int nextFavouriteId = 1;

        private final Map<Integer, User> users = new HashMap<>();
        private final Map<Integer, Content> contents = new HashMap<>();
        private final Map<String, Favourite> favourites = new HashMap<>();
        private final Map<String, HistoryRecord> history = new HashMap<>();
    }
}
