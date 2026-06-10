package db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the schema (idempotent) and, later, seeds sample data when empty.
 */
public class SchemaInitializer {

    /** Executes schema.sql against the given connection. */
    public static void runSchema(Connection conn) {
        String sql = readResource("schema.sql");
        try (Statement st = conn.createStatement()) {
            for (String statement : sql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    st.execute(statement);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to run schema.sql: " + e.getMessage(), e);
        }
    }

    private static String readResource(String name) {
        try (InputStream in = SchemaInitializer.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new RuntimeException("Resource not found: " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + name, e);
        }
    }
}
