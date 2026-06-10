package db;

import models.Technician;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class TechnicianDAOTest {

    private TechnicianDAO technicianDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        technicianDAO = new TechnicianDAO(conn);
        technicianDAO.insert(new Technician("TECH001", "Alice Tech", "Level 3", "9999"));
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void findByIdReturnsTechnician() {
        Technician t = technicianDAO.findById("TECH001");
        assertNotNull(t);
        assertEquals("Alice Tech", t.getName());
        assertEquals("Level 3", t.getClearanceLevel());
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        assertNull(technicianDAO.findById("NOPE"));
    }

    @Test
    void verifyPinAcceptsCorrectAndRejectsWrong() {
        assertTrue(technicianDAO.verifyPin("TECH001", "9999"));
        assertFalse(technicianDAO.verifyPin("TECH001", "0000"));
    }
}
