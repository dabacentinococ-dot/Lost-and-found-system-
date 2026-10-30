package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Database connection: opens the application's CRMS MySQL connection.
public final class DatabaseConnection {

    private static final String DATABASE_URL = 
            "jdbc:mysql://localhost:3306/CRMS?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private DatabaseConnection() {
    }

    // Database service: returns a new connection for a query.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
    }
}
