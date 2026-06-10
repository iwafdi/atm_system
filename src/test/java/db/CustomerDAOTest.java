package db;

import models.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class CustomerDAOTest {

    private CustomerDAO customerDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        customerDAO = new CustomerDAO(conn);
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void insertAndFindById() {
        customerDAO.insert(new Customer("CUST001", "John Smith", "123 Main St", "07700900000"));
        Customer found = customerDAO.findById("CUST001");
        assertNotNull(found);
        assertEquals("John Smith", found.getName());
        assertEquals("123 Main St", found.getAddress());
        assertEquals("07700900000", found.getPhoneNumber());
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        assertNull(customerDAO.findById("NOPE"));
    }
}
