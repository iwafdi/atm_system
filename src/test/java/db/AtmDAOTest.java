package db;

import models.ATM;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class AtmDAOTest {

    private AtmDAO atmDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        atmDAO = new AtmDAO(conn);
        atmDAO.insert(new ATM("ATM-001A", "Main Branch", 10000.0));
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void insertAndFindById() {
        ATM atm = atmDAO.findById("ATM-001A");
        assertNotNull(atm);
        assertEquals("Main Branch", atm.getLocation());
        assertEquals(10000.0, atm.getCashInventory());
        assertEquals(100, atm.getInkLevel());
        assertEquals("OPERATIONAL", atm.getHardwareStatus());
    }

    @Test
    void updatePersistsNewState() {
        ATM atm = atmDAO.findById("ATM-001A");
        atm.replenishCash(5000.0);     // cash -> 15000
        atm.applyUpgrade("firmware", "v2.0.0");
        atmDAO.update(atm);

        ATM reloaded = atmDAO.findById("ATM-001A");
        assertEquals(15000.0, reloaded.getCashInventory());
        assertEquals("v2.0.0", reloaded.getFirmwareVersion());
    }
}
