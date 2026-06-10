package models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void gettersReturnConstructorValues() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 10, 12, 0);
        Transaction t = new Transaction(1, now, "WITHDRAWAL", "ACC001",
                100.0, 4900.0, null, "ATM-001A");
        assertEquals(1, t.getTransactionId());
        assertEquals(now, t.getTimestamp());
        assertEquals("WITHDRAWAL", t.getType());
        assertEquals("ACC001", t.getAccountNumber());
        assertEquals(100.0, t.getAmount());
        assertEquals(4900.0, t.getBalanceAfter());
        assertNull(t.getRelatedAccount());
        assertEquals("ATM-001A", t.getAtmId());
    }
}
