package db;

import models.Card;
import models.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardDAOTest {

    private CardDAO cardDAO;

    @BeforeEach
    void setUp() {
        Connection conn = TestDb.freshConnection();
        new CustomerDAO(conn).insert(
                new Customer("CUST001", "John Smith", "123 Main St", "07700900000"));
        cardDAO = new CardDAO(conn);
        cardDAO.insert(new Card("1234567890123456", "12/25", "Debit", "BANK001", "1234"), "CUST001");
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void findByCardNumberReturnsCardWithoutPlaintextPin() {
        Card card = cardDAO.findByCardNumber("1234567890123456");
        assertNotNull(card);
        assertEquals("12/25", card.getExpiryDate());
        assertEquals("Debit", card.getCardType());
        assertEquals("BANK001", card.getBankID());
    }

    @Test
    void findByCardNumberReturnsNullWhenMissing() {
        assertNull(cardDAO.findByCardNumber("0000000000000000"));
    }

    @Test
    void findCustomerIdByCardNumber() {
        assertEquals("CUST001", cardDAO.findCustomerIdByCardNumber("1234567890123456"));
    }

    @Test
    void findByCustomerIdReturnsCards() {
        List<Card> cards = cardDAO.findByCustomerId("CUST001");
        assertEquals(1, cards.size());
    }

    @Test
    void verifyPinAcceptsCorrectAndRejectsWrong() {
        assertTrue(cardDAO.verifyPin("1234567890123456", "1234"));
        assertFalse(cardDAO.verifyPin("1234567890123456", "0000"));
    }

    @Test
    void pinIsNotStoredInPlaintext() throws Exception {
        Connection conn = Database.get();
        try (var st = conn.createStatement();
             var rs = st.executeQuery("SELECT pin_hash FROM cards WHERE card_number = '1234567890123456'")) {
            assertTrue(rs.next());
            assertNotEquals("1234", rs.getString("pin_hash"));
        }
    }
}
