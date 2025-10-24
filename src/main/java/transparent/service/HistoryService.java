package transparent.service;

import transparent.dao.HistoryDAO;
import transparent.model.HistoryRecord;

import java.sql.SQLException;
import java.util.List;

/**
 * Service layer for managing reading history.  Provides convenience methods
 * for saving progress and retrieving a user's history.
 */
public class HistoryService {

    private final HistoryDAO historyDAO = new HistoryDAO();

    /**
     * Save or update the current reading progress for a user and content.
     *
     * @param userId    the user ID
     * @param contentId the content ID
     * @param pageNumber the last page number read
     */
    public void saveProgress(int userId, int contentId, int pageNumber) {
        HistoryRecord record = new HistoryRecord(userId, contentId, pageNumber);
        try {
            historyDAO.upsertHistory(record);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the reading history for a user.
     *
     * @param userId the user ID
     * @return list of history records or empty list on error
     */
    public List<HistoryRecord> getHistory(int userId) {
        try {
            return historyDAO.getHistoryByUser(userId);
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Retrieve the most recent history record for a user and content pair.
     *
     * @param userId    the user ID
     * @param contentId the content ID
     * @return the {@link HistoryRecord} if it exists, otherwise {@code null}
     */
    public HistoryRecord getLatestEntry(int userId, int contentId) {
        try {
            return historyDAO.findHistory(userId, contentId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
