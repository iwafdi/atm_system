package db;

import models.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persists customers. Extended with query methods in a later task.
 */
public class CustomerDAO {
    private final Connection conn;

    public CustomerDAO(Connection conn) {
        this.conn = conn;
    }

    public void insert(Customer customer) {
        String sql = "INSERT INTO customers (customer_id, name, address, phone_number) "
                + "VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getCustomerID());
            ps.setString(2, customer.getName());
            ps.setString(3, customer.getAddress());
            ps.setString(4, customer.getPhoneNumber());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert customer: " + e.getMessage(), e);
        }
    }
}
