package db;

import models.Technician;
import util.PinHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persists technicians. PINs are stored as salted hashes.
 */
public class TechnicianDAO {
    private final Connection conn;

    public TechnicianDAO(Connection conn) {
        this.conn = conn;
    }

    public void insert(Technician technician) {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash(technician.getPinForSeeding(), salt);
        String sql = "INSERT INTO technicians "
                + "(technician_id, name, clearance_level, pin_hash, pin_salt) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, technician.getTechnicianID());
            ps.setString(2, technician.getName());
            ps.setString(3, technician.getClearanceLevel());
            ps.setString(4, hash);
            ps.setString(5, salt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert technician: " + e.getMessage(), e);
        }
    }

    public Technician findById(String technicianId) {
        String sql = "SELECT technician_id, name, clearance_level "
                + "FROM technicians WHERE technician_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, technicianId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Technician(
                            rs.getString("technician_id"),
                            rs.getString("name"),
                            rs.getString("clearance_level"),
                            ""); // no plaintext PIN for DB-loaded technicians
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find technician: " + e.getMessage(), e);
        }
    }

    public boolean verifyPin(String technicianId, String enteredPin) {
        String sql = "SELECT pin_hash, pin_salt FROM technicians WHERE technician_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, technicianId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return PinHasher.verify(enteredPin, rs.getString("pin_salt"), rs.getString("pin_hash"));
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify technician PIN: " + e.getMessage(), e);
        }
    }
}
