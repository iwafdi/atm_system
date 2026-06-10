package db;

import models.Card;
import util.PinHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists cards. PINs are stored as salted hashes; the model never receives
 * a plaintext PIN when loaded from the DB.
 */
public class CardDAO {
    private final Connection conn;

    public CardDAO(Connection conn) {
        this.conn = conn;
    }

    /** Inserts a card, hashing the plaintext PIN held by the model. */
    public void insert(Card card, String customerId) {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash(card.getPinForSeeding(), salt);
        String sql = "INSERT INTO cards "
                + "(card_number, expiry_date, card_type, bank_id, pin_hash, pin_salt, customer_id) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, card.getCardNumber());
            ps.setString(2, card.getExpiryDate());
            ps.setString(3, card.getCardType());
            ps.setString(4, card.getBankID());
            ps.setString(5, hash);
            ps.setString(6, salt);
            ps.setString(7, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert card: " + e.getMessage(), e);
        }
    }

    /** Returns the card (with an empty PIN — auth goes through verifyPin). */
    public Card findByCardNumber(String cardNumber) {
        String sql = "SELECT card_number, expiry_date, card_type, bank_id "
                + "FROM cards WHERE card_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cardNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find card: " + e.getMessage(), e);
        }
    }

    public String findCustomerIdByCardNumber(String cardNumber) {
        String sql = "SELECT customer_id FROM cards WHERE card_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cardNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("customer_id") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find card owner: " + e.getMessage(), e);
        }
    }

    public List<Card> findByCustomerId(String customerId) {
        String sql = "SELECT card_number, expiry_date, card_type, bank_id "
                + "FROM cards WHERE customer_id = ?";
        List<Card> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list cards: " + e.getMessage(), e);
        }
        return result;
    }

    /** Verifies the entered PIN against the stored salted hash. */
    public boolean verifyPin(String cardNumber, String enteredPin) {
        String sql = "SELECT pin_hash, pin_salt FROM cards WHERE card_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cardNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return PinHasher.verify(enteredPin, rs.getString("pin_salt"), rs.getString("pin_hash"));
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify card PIN: " + e.getMessage(), e);
        }
    }

    private Card mapRow(ResultSet rs) throws SQLException {
        return new Card(
                rs.getString("card_number"),
                rs.getString("expiry_date"),
                rs.getString("card_type"),
                rs.getString("bank_id"),
                ""); // no plaintext PIN for DB-loaded cards
    }
}
