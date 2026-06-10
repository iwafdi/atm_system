package db;

import models.Account;
import models.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountDAOTest {

    private AccountDAO accountDAO;
    private CustomerDAO customerDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        accountDAO = new AccountDAO(conn);
        customerDAO = new CustomerDAO(conn);
        customerDAO.insert(new Customer("CUST001", "John Smith", "123 Main St", "07700900000"));
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void insertAndFindByNumber() {
        accountDAO.insert(new Account("ACC001", "Savings", 5000.0, "CUST001"));
        Account found = accountDAO.findByNumber("ACC001");
        assertNotNull(found);
        assertEquals("Savings", found.getAccountType());
        assertEquals(5000.0, found.getBalance());
        assertEquals("CUST001", found.getCustomerID());
    }

    @Test
    void findByCustomerIdReturnsAll() {
        accountDAO.insert(new Account("ACC001", "Savings", 5000.0, "CUST001"));
        accountDAO.insert(new Account("ACC002", "Current", 2500.0, "CUST001"));
        List<Account> accounts = accountDAO.findByCustomerId("CUST001");
        assertEquals(2, accounts.size());
    }

    @Test
    void updateBalancePersists() {
        accountDAO.insert(new Account("ACC001", "Savings", 5000.0, "CUST001"));
        accountDAO.updateBalance("ACC001", 4900.0);
        assertEquals(4900.0, accountDAO.findByNumber("ACC001").getBalance());
    }

    @Test
    void transferMovesFundsAtomically() {
        accountDAO.insert(new Account("ACC001", "Savings", 5000.0, "CUST001"));
        accountDAO.insert(new Account("ACC002", "Current", 2500.0, "CUST001"));
        accountDAO.transfer("ACC001", "ACC002", 1000.0, "ATM-001A");
        assertEquals(4000.0, accountDAO.findByNumber("ACC001").getBalance());
        assertEquals(3500.0, accountDAO.findByNumber("ACC002").getBalance());
    }
}
