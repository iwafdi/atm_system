package db;

import models.Account;
import models.Customer;
import models.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionDAOTest {

    private TransactionDAO transactionDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        new CustomerDAO(conn).insert(new Customer("CUST001", "John", "addr", "phone"));
        new AccountDAO(conn).insert(new Account("ACC001", "Savings", 5000.0, "CUST001"));
        transactionDAO = new TransactionDAO(conn);
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void insertAndFindByAccount() {
        transactionDAO.insert("WITHDRAWAL", "ACC001", 100.0, 4900.0, null, "ATM-001A");
        List<Transaction> txns = transactionDAO.findByAccount("ACC001");
        assertEquals(1, txns.size());
        Transaction t = txns.get(0);
        assertEquals("WITHDRAWAL", t.getType());
        assertEquals(100.0, t.getAmount());
        assertEquals(4900.0, t.getBalanceAfter());
        assertTrue(t.getTransactionId() > 0);
    }
}
