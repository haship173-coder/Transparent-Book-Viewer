package transparent.dao;

import transparent.db.DBConnectionManager;
import transparent.model.Content;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object for {@link Content}.  Provides CRUD operations for
 * managing content records.  Only insert and query operations are implemented
 * here as part of the assignment scaffold.
 */
public class ContentDAO {

    /**
     * Insert a new piece of content into the database.  The contentID will be
     * set on the provided {@link Content} object upon successful insertion.
     *
     * @param content the content to insert
     * @throws SQLException if a database error occurs
     */
    public void insertContent(Content content) throws SQLException {
        String sql = "INSERT INTO Contents (Title, FilePath, FileType, Size, DayAdded) " +
                     "VALUES (?, ?, ?, ?, GETDATE())";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, content.getTitle());
            ps.setString(2, content.getFilePath());
            ps.setString(3, content.getFileType());
            ps.setLong(4, content.getSizeBytes());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                content.setContentID(keys.getInt(1));
            }
        }
    }

    /**
     * Retrieve all content entries from the database.
     *
     * @return a list of {@link Content}
     * @throws SQLException if a database error occurs
     */
    public List<Content> getAllContents() throws SQLException {
        String sql = "SELECT * FROM Contents ORDER BY DayAdded DESC";
        try (Connection conn = DBConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Content> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    /**
     * Search content records by title.  Uses a LIKE query to match partial titles.
     *
     * @param keyword the keyword to search for
     * @return a list of matching {@link Content}
     * @throws SQLException if a database error occurs
     */
    public List<Content> searchByTitle(String keyword) throws SQLException {
        String sql = "SELECT * FROM Contents WHERE Title LIKE ? ORDER BY DayAdded DESC";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            List<Content> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    /**
     * Retrieve a single content entry by ID.
     *
     * @param contentId the identifier of the content
     * @return the content or {@code null} if not found
     * @throws SQLException if a database error occurs
     */
    public Content getContentById(int contentId) throws SQLException {
        String sql = "SELECT * FROM Contents WHERE ContentID = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;
        }
    }

    private Content mapRow(ResultSet rs) throws SQLException {
        Content content = new Content(
                rs.getInt("ContentID"),
                rs.getString("Title"),
                rs.getString("FilePath"),
                rs.getString("FileType"),
                rs.getLong("Size"),
                rs.getTimestamp("DayAdded").toLocalDateTime()
        );
        content.setCategory("Uncategorized");
        return content;
    }
}