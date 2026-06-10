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

    public Customer findById(String customerId) {
        String sql = "SELECT customer_id, name, address, phone_number "
                + "FROM customers WHERE customer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                            rs.getString("customer_id"),
                            rs.getString("name"),
                            rs.getString("address"),
                            rs.getString("phone_number"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find customer: " + e.getMessage(), e);
        }
    }
}
