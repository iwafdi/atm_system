package db;

import models.ATM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persists ATM hardware/software state.
 */
public class AtmDAO {
    private final Connection conn;

    public AtmDAO(Connection conn) {
        this.conn = conn;
    }

    public void insert(ATM atm) {
        String sql = "INSERT INTO atms (atm_id, location, cash_inventory, ink_level, "
                + "paper_level, firmware_version, software_version, hardware_status) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, atm);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert ATM: " + e.getMessage(), e);
        }
    }

    public ATM findById(String atmId) {
        String sql = "SELECT atm_id, location, cash_inventory, ink_level, paper_level, "
                + "firmware_version, software_version, hardware_status FROM atms WHERE atm_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, atmId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ATM(
                            rs.getString("atm_id"),
                            rs.getString("location"),
                            rs.getDouble("cash_inventory"),
                            rs.getInt("ink_level"),
                            rs.getInt("paper_level"),
                            rs.getString("firmware_version"),
                            rs.getString("software_version"),
                            rs.getString("hardware_status"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find ATM: " + e.getMessage(), e);
        }
    }

    public void update(ATM atm) {
        String sql = "UPDATE atms SET location = ?, cash_inventory = ?, ink_level = ?, "
                + "paper_level = ?, firmware_version = ?, software_version = ?, "
                + "hardware_status = ? WHERE atm_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, atm.getLocation());
            ps.setDouble(2, atm.getCashInventory());
            ps.setInt(3, atm.getInkLevel());
            ps.setInt(4, atm.getPaperLevel());
            ps.setString(5, atm.getFirmwareVersion());
            ps.setString(6, atm.getSoftwareVersion());
            ps.setString(7, atm.getHardwareStatus());
            ps.setString(8, atm.getAtmID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update ATM: " + e.getMessage(), e);
        }
    }

    private void bind(PreparedStatement ps, ATM atm) throws SQLException {
        ps.setString(1, atm.getAtmID());
        ps.setString(2, atm.getLocation());
        ps.setDouble(3, atm.getCashInventory());
        ps.setInt(4, atm.getInkLevel());
        ps.setInt(5, atm.getPaperLevel());
        ps.setString(6, atm.getFirmwareVersion());
        ps.setString(7, atm.getSoftwareVersion());
        ps.setString(8, atm.getHardwareStatus());
    }
}
