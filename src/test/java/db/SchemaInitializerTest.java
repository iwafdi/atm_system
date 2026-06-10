package db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SchemaInitializerTest {

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void runSchemaCreatesAllSevenTables() throws Exception {
        Connection conn = TestDb.freshConnection();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT count(*) FROM information_schema.tables "
                   + "WHERE table_schema = 'public' AND table_name IN "
                   + "('customers','accounts','cards','technicians','atms',"
                   + "'maintenance_logs','transactions')")) {
            assertTrue(rs.next());
            assertEquals(7, rs.getInt(1));
        }
    }
}
