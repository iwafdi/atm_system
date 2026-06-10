package db;

import models.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Persists banking transactions. Extended with query methods in a later task.
 */
public class TransactionDAO {
    private final Connection conn;

    public TransactionDAO(Connection conn) {
        this.conn = conn;
    }

    /**
     * Inserts a transaction row using the supplied connection's current
     * transaction context (so it can participate in an atomic transfer).
     */
    public void insert(String type, String accountNumber, double amount,
                       double balanceAfter, String relatedAccount, String atmId) {
        String sql = "INSERT INTO transactions "
                + "(timestamp, type, account_number, amount, balance_after, related_account, atm_id) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, type);
            ps.setString(3, accountNumber);
            ps.setDouble(4, amount);
            ps.setDouble(5, balanceAfter);
            ps.setString(6, relatedAccount);
            ps.setString(7, atmId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert transaction: " + e.getMessage(), e);
        }
    }
}
