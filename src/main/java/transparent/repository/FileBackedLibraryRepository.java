package transparent.repository;

import transparent.model.Content;
import transparent.model.Favourite;
import transparent.model.HistoryRecord;
import transparent.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * A lightweight repository backed by a local file.  The repository is designed
 * to mirror the subset of data that the UI relies upon so that the
 * application can continue to operate when the SQL Server database is
 * unavailable.  Data is serialised using Java's built in object streams which
 * keeps the implementation dependency free.
 */
public final class FileBackedLibraryRepository {
    private static final Path STORAGE_PATH = Paths.get(
            System.getProperty("user.home"), ".transparent", "library-store.bin");

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<java.util.function.Consumer<String>> themeListeners = new CopyOnWriteArrayList<>();
    private LibraryState state;

    private FileBackedLibraryRepository() {
        load();
    }

    private static final class Holder {
        private static final FileBackedLibraryRepository INSTANCE = new FileBackedLibraryRepository();
    }

    public static FileBackedLibraryRepository getInstance() {
        return Holder.INSTANCE;
    }

    private void load() {
        lock.writeLock().lock();
        try {
            if (Files.exists(STORAGE_PATH)) {
                try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(STORAGE_PATH))) {
                    Object obj = in.readObject();
                    if (obj instanceof LibraryState loaded) {
                        state = loaded;
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    state = new LibraryState();
                }
            }
            if (state == null) {
                state = new LibraryState();
            }
        } catch (IOException ioException) {
            state = new LibraryState();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void persist() {
        lock.writeLock().lock();
        try {
            Files.createDirectories(STORAGE_PATH.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(
                    Files.newOutputStream(STORAGE_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                out.writeObject(state);
            }
        } catch (IOException ex) {
            // Persisting is best-effort; swallow exceptions to avoid impacting the UI.
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<Content> snapshotContentsLocked() {
        List<Content> snapshot = new ArrayList<>();
        for (Content content : state.contents) {
            snapshot.add(new Content(content));
        }
        return snapshot;
    }

    private List<HistoryRecord> snapshotHistoryLocked(int userId) {
        List<HistoryRecord> snapshot = new ArrayList<>();
        for (HistoryRecord record : state.history) {
            if (record.getUserID() == userId) {
                snapshot.add(new HistoryRecord(record));
            }
        }
        snapshot.sort(Comparator.comparing(HistoryRecord::getLastReadTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return snapshot;
    }

    private List<Favourite> snapshotFavouritesLocked(int userId) {
        List<Favourite> snapshot = new ArrayList<>();
        for (Favourite favourite : state.favourites) {
            if (favourite.getUserID() == userId) {
                snapshot.add(new Favourite(favourite));
            }
        }
        snapshot.sort(Comparator.comparing(Favourite::getAddedDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return snapshot;
    }

    private Optional<Content> findContentLocked(int contentId) {
        return state.contents.stream().filter(c -> c.getContentID() == contentId).findFirst();
    }

    private Optional<User> findUserLocked(int userId) {
        return state.users.stream().filter(u -> u.getUserID() == userId).findFirst();
    }

    private Optional<User> findUserByUsernameLocked(String username) {
        return state.users.stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
    }

    private Optional<Favourite> findFavouriteLocked(int userId, int contentId) {
        return state.favourites.stream()
                .filter(f -> f.getUserID() == userId && f.getContentID() == contentId)
                .findFirst();
    }

    /**
     * Merge database contents into the local repository, returning an immutable
     * snapshot of the merged state.
     */
    public List<Content> mergeFromDatabase(List<Content> databaseContents) {
        lock.writeLock().lock();
        try {
            boolean changed = false;
            for (Content dbContent : databaseContents) {
                Content copy = new Content(dbContent);
                copy.setFavourite(false);
                changed |= upsertContentLocked(copy, false);
            }
            if (changed) {
                persist();
            }
            return snapshotContentsLocked();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean upsertContentLocked(Content incoming, boolean allowNewId) {
        Optional<Content> existingOpt = findContentLocked(incoming.getContentID());
        if (existingOpt.isEmpty() && allowNewId) {
            incoming.setContentID(state.nextContentId--);
            if (incoming.getDayAdded() == null) {
                incoming.setDayAdded(LocalDateTime.now());
            }
            state.contents.add(incoming);
            return true;
        } else if (existingOpt.isEmpty()) {
            if (incoming.getDayAdded() == null) {
                incoming.setDayAdded(LocalDateTime.now());
            }
            state.contents.add(incoming);
            return true;
        }
        Content existing = existingOpt.get();
        boolean changed = false;
        if (!equals(existing.getTitle(), incoming.getTitle())) {
            existing.setTitle(incoming.getTitle());
            changed = true;
        }
        if (!equals(existing.getFilePath(), incoming.getFilePath())) {
            existing.setFilePath(incoming.getFilePath());
            changed = true;
        }
        if (!equals(existing.getFileType(), incoming.getFileType())) {
            existing.setFileType(incoming.getFileType());
            changed = true;
        }
        if (existing.getSizeBytes() != incoming.getSizeBytes()) {
            existing.setSizeBytes(incoming.getSizeBytes());
            changed = true;
        }
        if (incoming.getDayAdded() != null && !incoming.getDayAdded().equals(existing.getDayAdded())) {
            existing.setDayAdded(incoming.getDayAdded());
            changed = true;
        }
        if (!equals(existing.getAuthor(), incoming.getAuthor())) {
            existing.setAuthor(incoming.getAuthor());
            changed = true;
        }
        if (!equals(existing.getCategory(), incoming.getCategory())) {
            existing.setCategory(incoming.getCategory());
            changed = true;
        }
        if (!existing.getTags().equals(incoming.getTags())) {
            existing.setTags(incoming.getTags());
            changed = true;
        }
        if (!equals(existing.getDescription(), incoming.getDescription())) {
            existing.setDescription(incoming.getDescription());
            changed = true;
        }
        return changed;
    }

    private static boolean equals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Persist a new or updated content entry when operating offline.
     */
    public Content saveOfflineContent(Content content) {
        lock.writeLock().lock();
        try {
            Content copy = new Content(content);
            if (copy.getContentID() == 0) {
                copy.setContentID(state.nextContentId--);
            }
            if (copy.getDayAdded() == null) {
                copy.setDayAdded(LocalDateTime.now());
            }
            upsertContentLocked(copy, true);
            persist();
            return new Content(copy);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Update metadata associated with a content entry.  This is used both when
     * editing metadata locally and when synchronising data retrieved from the
     * database.
     */
    public void updateContentMetadata(Content content) {
        lock.writeLock().lock();
        try {
            Content copy = new Content(content);
            upsertContentLocked(copy, true);
            persist();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Content> getAllContents() {
        lock.readLock().lock();
        try {
            return snapshotContentsLocked();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<Content> findContent(int contentId) {
        lock.readLock().lock();
        try {
            return findContentLocked(contentId).map(Content::new);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<String> listCategories() {
        lock.readLock().lock();
        try {
            return state.contents.stream()
                    .map(Content::getCategory)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toCollection(() -> new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER)))
                    .stream()
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<String> listTags() {
        lock.readLock().lock();
        try {
            return state.contents.stream()
                    .flatMap(content -> content.getTags().stream())
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(tag -> tag.trim().toLowerCase())
                    .collect(Collectors.toCollection(() -> new java.util.TreeSet<>()))
                    .stream()
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<HistoryRecord> mergeHistoryFromDatabase(int userId, List<HistoryRecord> records) {
        lock.writeLock().lock();
        try {
            Map<Integer, HistoryRecord> byContent = new HashMap<>();
            for (HistoryRecord record : state.history) {
                if (record.getUserID() == userId) {
                    byContent.put(record.getContentID(), record);
                }
            }
            for (HistoryRecord record : records) {
                HistoryRecord copy = new HistoryRecord(record);
                HistoryRecord existing = byContent.get(copy.getContentID());
                if (existing == null) {
                    state.history.add(copy);
                } else {
                    existing.setLastReadTime(copy.getLastReadTime());
                    existing.setPageNumber(copy.getPageNumber());
                    existing.setHistoryID(copy.getHistoryID());
                }
            }
            persist();
            return snapshotHistoryLocked(userId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void saveHistory(HistoryRecord record) {
        lock.writeLock().lock();
        try {
            HistoryRecord copy = new HistoryRecord(record);
            if (copy.getHistoryID() == 0) {
                copy.setHistoryID(state.nextHistoryId--);
            }
            boolean updated = false;
            for (HistoryRecord existing : state.history) {
                if (existing.getUserID() == copy.getUserID() && existing.getContentID() == copy.getContentID()) {
                    existing.setPageNumber(copy.getPageNumber());
                    existing.setLastReadTime(copy.getLastReadTime());
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                if (copy.getLastReadTime() == null) {
                    copy.setLastReadTime(LocalDateTime.now());
                }
                state.history.add(copy);
            }
            persist();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<HistoryRecord> getHistory(int userId) {
        lock.readLock().lock();
        try {
            return snapshotHistoryLocked(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<HistoryRecord> findHistory(int userId, int contentId) {
        lock.readLock().lock();
        try {
            return state.history.stream()
                    .filter(record -> record.getUserID() == userId && record.getContentID() == contentId)
                    .findFirst()
                    .map(HistoryRecord::new);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean toggleFavourite(int userId, int contentId) {
        lock.writeLock().lock();
        try {
            Optional<Favourite> existing = findFavouriteLocked(userId, contentId);
            if (existing.isPresent()) {
                state.favourites.remove(existing.get());
                persist();
                return false;
            }
            Favourite favourite = new Favourite(userId, contentId);
            favourite.setFavouriteID(state.nextFavouriteId--);
            favourite.setAddedDate(LocalDateTime.now());
            state.favourites.add(favourite);
            persist();
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setFavouriteState(int userId, int contentId, boolean favourite, LocalDateTime timestamp) {
        lock.writeLock().lock();
        try {
            Optional<Favourite> existing = findFavouriteLocked(userId, contentId);
            if (favourite) {
                Favourite target = existing.orElseGet(() -> {
                    Favourite fav = new Favourite(userId, contentId);
                    fav.setFavouriteID(state.nextFavouriteId--);
                    state.favourites.add(fav);
                    return fav;
                });
                target.setAddedDate(timestamp != null ? timestamp : LocalDateTime.now());
            } else {
                existing.ifPresent(state.favourites::remove);
            }
            persist();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isFavourite(int userId, int contentId) {
        lock.readLock().lock();
        try {
            return findFavouriteLocked(userId, contentId).isPresent();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Favourite> mergeFavouritesFromDatabase(int userId, List<Favourite> favourites) {
        lock.writeLock().lock();
        try {
            Set<Integer> seenContent = new HashSet<>();
            for (Favourite favourite : favourites) {
                Favourite copy = new Favourite(favourite);
                seenContent.add(copy.getContentID());
                Optional<Favourite> existing = findFavouriteLocked(userId, copy.getContentID());
                if (existing.isEmpty()) {
                    state.favourites.add(copy);
                } else {
                    existing.get().setAddedDate(copy.getAddedDate());
                    existing.get().setFavouriteID(copy.getFavouriteID());
                }
            }
            state.favourites.removeIf(fav -> fav.getUserID() == userId && !seenContent.contains(fav.getContentID()));
            persist();
            return snapshotFavouritesLocked(userId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Favourite> getFavourites(int userId) {
        lock.readLock().lock();
        try {
            return snapshotFavouritesLocked(userId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public User mergeUser(User user) {
        lock.writeLock().lock();
        try {
            Optional<User> existingById = findUserLocked(user.getUserID());
            Optional<User> existingByName = findUserByUsernameLocked(user.getUsername());
            if (existingById.isPresent()) {
                existingById.get().setUsername(user.getUsername());
                persist();
                return new User(existingById.get());
            }
            if (existingByName.isPresent()) {
                existingByName.get().setUserID(user.getUserID());
                persist();
                return new User(existingByName.get());
            }
            User copy = new User(user);
            state.users.add(copy);
            persist();
            return new User(copy);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public User findOrCreateUser(String username) {
        lock.writeLock().lock();
        try {
            Optional<User> existing = findUserByUsernameLocked(username);
            if (existing.isPresent()) {
                return new User(existing.get());
            }
            User user = new User(username);
            user.setUserID(state.nextUserId--);
            state.users.add(user);
            persist();
            return new User(user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getTheme() {
        lock.readLock().lock();
        try {
            return state.theme;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setTheme(String theme) {
        lock.writeLock().lock();
        try {
            if (!equals(state.theme, theme)) {
                state.theme = theme;
                persist();
            }
        } finally {
            lock.writeLock().unlock();
        }
        notifyThemeListeners(theme);
    }

    public void addThemeListener(java.util.function.Consumer<String> listener) {
        themeListeners.add(listener);
    }

    private void notifyThemeListeners(String theme) {
        for (java.util.function.Consumer<String> listener : themeListeners) {
            listener.accept(theme);
        }
    }

    private static final class LibraryState implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<Content> contents = new ArrayList<>();
        private List<HistoryRecord> history = new ArrayList<>();
        private List<Favourite> favourites = new ArrayList<>();
        private List<User> users = new ArrayList<>();
        private int nextContentId = -1;
        private int nextHistoryId = -1;
        private int nextFavouriteId = -1;
        private int nextUserId = -1;
        private String theme = "LIGHT";
    }
}
