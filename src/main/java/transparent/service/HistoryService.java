package transparent.service;

import transparent.dao.HistoryDAO;
import transparent.model.HistoryRecord;
import transparent.repository.FileBackedLibraryRepository;

import java.sql.SQLException;
import java.util.List;

/**
 * Service layer for managing reading history.  Provides convenience methods
 * for saving progress and retrieving a user's history.
 */
public class HistoryService {

    private final HistoryDAO historyDAO = new HistoryDAO();
    private static boolean databaseAvailable = true;
    private final FileBackedLibraryRepository offlineRepo = FileBackedLibraryRepository.getInstance();

    /**
     * Save or update the current reading progress for a user and content.
     *
     * @param userId    the user ID
     * @param contentId the content ID
     * @param pageNumber the last page number read
     */
    public void saveProgress(int userId, int contentId, int pageNumber) {
        HistoryRecord record = new HistoryRecord(userId, contentId, pageNumber);
        if (databaseAvailable) {
            try {
                historyDAO.upsertHistory(record);
                return;
            } catch (SQLException e) {
                databaseAvailable = false;
                e.printStackTrace();
            }
        }
        offlineRepo.saveProgress(userId, contentId, pageNumber);
    }

    /**
     * Retrieve the reading history for a user.
     *
     * @param userId the user ID
     * @return list of history records or empty list on error
     */
    public List<HistoryRecord> getHistory(int userId) {
        if (databaseAvailable) {
            try {
                return historyDAO.getHistoryByUser(userId);
            } catch (SQLException e) {
                databaseAvailable = false;
                e.printStackTrace();
            }
        }
        return offlineRepo.getHistory(userId);
    }
}