package transparent.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A simple connection manager for SQL Server.
 *
 * <p>The connection string here uses the local instance of SQL Server Express.  You
 * should update the URL, username and password to match your own database
 * configuration.  When encryption is disabled (encrypt=false) the driver will
 * connect without TLS.  Adjust as necessary for your environment.</p>
 */
public final class DBConnectionManager {
    private static final String URL =
            "jdbc:sqlserver://localhost:1433;databaseName=TransparentDB;encrypt=false";
    private static final String USER = "sa";
    private static final String PASSWORD = "yourStrongPassword";

    private DBConnectionManager() {
        // prevent instantiation
    }

    /**
     * Obtain a new JDBC connection.
     *
     * @return an open {@link Connection}
     * @throws SQLException if a connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}