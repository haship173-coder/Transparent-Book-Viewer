package transparent.dao;

import transparent.db.DBConnectionManager;
import transparent.model.HistoryRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object for {@link HistoryRecord}.  Handles inserting and
 * updating history entries and retrieving a user's reading history.
 */
public class HistoryDAO {

    /**
     * Insert a new history record or update an existing one.  If a record for
     * the given user and content already exists, its page number and last
     * access time are updated.  Otherwise a new record is created.
     *
     * @param record the history record to upsert
     * @throws SQLException if a database error occurs
     */
    public void upsertHistory(HistoryRecord record) throws SQLException {
        String checkSql = "SELECT HistoryID FROM History WHERE UserID = ? AND ContentID = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setInt(1, record.getUserID());
            check.setInt(2, record.getContentID());
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                // Update existing
                String updateSql = "UPDATE History SET LastReadTime = GETDATE(), PageNumber = ? WHERE HistoryID = ?";
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setInt(1, record.getPageNumber());
                    update.setInt(2, rs.getInt("HistoryID"));
                    update.executeUpdate();
                }
            } else {
                // Insert new
                String insertSql = "INSERT INTO History (UserID, ContentID, LastReadTime, PageNumber) VALUES (?, ?, GETDATE(), ?)";
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    insert.setInt(1, record.getUserID());
                    insert.setInt(2, record.getContentID());
                    insert.setInt(3, record.getPageNumber());
                    insert.executeUpdate();
                }
            }
        }
    }

    /**
     * Retrieve all history records for a given user, ordered by last read time
     * descending.
     *
     * @param userId the ID of the user
     * @return a list of history records
     * @throws SQLException if a database error occurs
     */
    public List<HistoryRecord> getHistoryByUser(int userId) throws SQLException {
        String sql = "SELECT * FROM History WHERE UserID = ? ORDER BY LastReadTime DESC";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            List<HistoryRecord> list = new ArrayList<>();
            while (rs.next()) {
                HistoryRecord rec = new HistoryRecord(
                        rs.getInt("HistoryID"),
                        rs.getInt("UserID"),
                        rs.getInt("ContentID"),
                        rs.getTimestamp("LastReadTime").toLocalDateTime(),
                        rs.getInt("PageNumber")
                );
                list.add(rec);
            }
            return list;
        }
    }
}