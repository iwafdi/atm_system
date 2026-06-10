package db;

import models.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists accounts and performs atomic transfers.
 */
public class AccountDAO {
    private final Connection conn;

    public AccountDAO(Connection conn) {
        this.conn = conn;
    }

    public void insert(Account account) {
        String sql = "INSERT INTO accounts (account_number, account_type, balance, customer_id) "
                + "VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, account.getAccountType());
            ps.setDouble(3, account.getBalance());
            ps.setString(4, account.getCustomerID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert account: " + e.getMessage(), e);
        }
    }

    public Account findByNumber(String accountNumber) {
        String sql = "SELECT account_number, account_type, balance, customer_id "
                + "FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find account: " + e.getMessage(), e);
        }
    }

    public List<Account> findByCustomerId(String customerId) {
        String sql = "SELECT account_number, account_type, balance, customer_id "
                + "FROM accounts WHERE customer_id = ? ORDER BY account_number";
        List<Account> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list accounts: " + e.getMessage(), e);
        }
        return result;
    }

    public void updateBalance(String accountNumber, double newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, accountNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update balance: " + e.getMessage(), e);
        }
    }

    /**
     * Atomically debits source, credits destination, and records a TRANSFER
     * transaction. Rolls back on any failure.
     */
    public void transfer(String fromAccount, String toAccount, double amount, String atmId) {
        try {
            conn.setAutoCommit(false);
            double fromBalance = findByNumber(fromAccount).getBalance() - amount;
            double toBalance = findByNumber(toAccount).getBalance() + amount;
            updateBalance(fromAccount, fromBalance);
            updateBalance(toAccount, toBalance);
            new TransactionDAO(conn).insert("TRANSFER", fromAccount, amount,
                    fromBalance, toAccount, atmId);
            conn.commit();
        } catch (RuntimeException | SQLException e) {
            rollbackQuietly();
            throw new RuntimeException("Transfer failed and was rolled back: " + e.getMessage(), e);
        } finally {
            setAutoCommitQuietly(true);
        }
    }

    private void rollbackQuietly() {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void setAutoCommitQuietly(boolean value) {
        try {
            conn.setAutoCommit(value);
        } catch (SQLException ignored) {
        }
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        return new Account(
                rs.getString("account_number"),
                rs.getString("account_type"),
                rs.getDouble("balance"),
                rs.getString("customer_id"));
    }
}
