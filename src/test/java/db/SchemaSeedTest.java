package db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SchemaSeedTest {

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void seedsSampleDataWhenEmptyAndIsIdempotent() throws Exception {
        Connection conn = TestDb.freshConnection();

        SchemaInitializer.seedIfEmpty(conn);
        SchemaInitializer.seedIfEmpty(conn); // second call must not duplicate

        assertEquals(1, count(conn, "customers"));
        assertEquals(2, count(conn, "accounts"));
        assertEquals(1, count(conn, "cards"));
        assertEquals(1, count(conn, "technicians"));
        assertEquals(1, count(conn, "atms"));

        // Seeded PINs verify through the DAOs (hashed, not plaintext)
        assertTrue(new CardDAO(conn).verifyPin("1234567890123456", "1234"));
        assertTrue(new TechnicianDAO(conn).verifyPin("TECH001", "9999"));
    }

    private int count(Connection conn, String table) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
