package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Test helper: connects to the test database and resets the schema to a clean state.
 */
public final class TestDb {

    private TestDb() {
    }

    /** Connects to the test DB and drops + recreates all tables. Returns the connection. */
    public static Connection freshConnection() {
        Connection conn = Database.connect("db.test.properties");
        dropAll(conn);
        SchemaInitializer.runSchema(conn);
        return conn;
    }

    private static void dropAll(Connection conn) {
        String sql = "DROP TABLE IF EXISTS transactions, maintenance_logs, cards, "
                + "accounts, technicians, atms, customers CASCADE";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to drop test tables", e);
        }
    }
}
