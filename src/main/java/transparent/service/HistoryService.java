package transparent.service;

import transparent.dao.HistoryDAO;
import transparent.model.HistoryRecord;
import transparent.repository.FileBackedLibraryRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for managing reading history.  Provides convenience methods
 * for saving progress and retrieving a user's history.  When SQL Server is
 * unavailable the data is persisted to the local file-backed repository.
 */
public class HistoryService {

    private final HistoryDAO historyDAO = new HistoryDAO();
    private final FileBackedLibraryRepository repository = FileBackedLibraryRepository.getInstance();

    /**
     * Save or update the current reading progress for a user and content.
     */
    public void saveProgress(int userId, int contentId, int pageNumber) {
        HistoryRecord record = new HistoryRecord(userId, contentId, pageNumber);
        record.setLastReadTime(LocalDateTime.now());
        try {
            historyDAO.upsertHistory(record);
            repository.saveHistory(record);
        } catch (SQLException e) {
            repository.saveHistory(record);
        }
    }

    /**
     * Retrieve the reading history for a user.
     */
    public List<HistoryRecord> getHistory(int userId) {
        try {
            List<HistoryRecord> records = historyDAO.getHistoryByUser(userId);
            return repository.mergeHistoryFromDatabase(userId, records);
        } catch (SQLException e) {
            return repository.getHistory(userId);
        }
    }

    /**
     * Retrieve the most recent history record for a user and content pair.
     */
    public HistoryRecord getLatestEntry(int userId, int contentId) {
        try {
            HistoryRecord record = historyDAO.findHistory(userId, contentId);
            if (record != null) {
                repository.saveHistory(record);
            }
            return record;
        } catch (SQLException e) {
            return repository.findHistory(userId, contentId).orElse(null);
        }
    }
}
