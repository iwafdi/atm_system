package db;

import models.ATM;
import models.MaintenanceLog;
import models.Technician;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceLogDAOTest {

    private MaintenanceLogDAO logDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        new AtmDAO(conn).insert(new ATM("ATM-001A", "Main Branch", 10000.0));
        new TechnicianDAO(conn).insert(new Technician("TECH001", "Alice Tech", "Level 3", "9999"));
        logDAO = new MaintenanceLogDAO(conn);
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void insertAndFindByAtmId() {
        logDAO.insert(new MaintenanceLog("LOG-1", "DIAGNOSTICS", "Ran diagnostics",
                "TECH001", "ATM-001A"));
        List<MaintenanceLog> logs = logDAO.findByAtmId("ATM-001A");
        assertEquals(1, logs.size());
        assertEquals("DIAGNOSTICS", logs.get(0).getActionType());
        assertEquals("TECH001", logs.get(0).getTechnicianID());
    }
}
