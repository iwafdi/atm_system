package db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Owns the single shared JDBC connection for the ATM session.
 */
public class Database {
    private static Connection connection;

    /** Connects using the default app properties file. */
    public static Connection connect() {
        return connect("db.properties");
    }

    /** Connects using the given classpath properties resource. */
    public static Connection connect(String resourceName) {
        Properties props = loadProps(resourceName);
        try {
            connection = DriverManager.getConnection(
                    props.getProperty("db.url"),
                    props.getProperty("db.user"),
                    props.getProperty("db.password"));
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database: " + e.getMessage(), e);
        }
    }

    /** Returns the live connection, or throws if connect() was not called. */
    public static Connection get() {
        if (connection == null) {
            throw new IllegalStateException("Database not connected. Call connect() first.");
        }
        return connection;
    }

    public static void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }

    private static Properties loadProps(String resourceName) {
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new RuntimeException("Properties resource not found: " + resourceName);
            }
            Properties props = new Properties();
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + resourceName, e);
        }
    }
}
