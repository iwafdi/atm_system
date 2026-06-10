package db;

import models.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public List<Transaction> findByAccount(String accountNumber) {
        String sql = "SELECT transaction_id, timestamp, type, account_number, amount, "
                + "balance_after, related_account, atm_id FROM transactions "
                + "WHERE account_number = ? ORDER BY transaction_id";
        List<Transaction> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Transaction(
                            rs.getInt("transaction_id"),
                            rs.getTimestamp("timestamp").toLocalDateTime(),
                            rs.getString("type"),
                            rs.getString("account_number"),
                            rs.getDouble("amount"),
                            rs.getDouble("balance_after"),
                            rs.getString("related_account"),
                            rs.getString("atm_id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list transactions: " + e.getMessage(), e);
        }
        return result;
    }
}
