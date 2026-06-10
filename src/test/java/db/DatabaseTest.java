package db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void connectReturnsUsableConnection() throws Exception {
        Connection conn = Database.connect("db.test.properties");
        assertNotNull(conn);
        assertFalse(conn.isClosed());
        assertTrue(conn.isValid(2));
    }

    @Test
    void getBeforeConnectThrows() {
        Database.close();
        assertThrows(IllegalStateException.class, Database::get);
    }
}
