package db;

import models.MaintenanceLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists ATM maintenance logs.
 */
public class MaintenanceLogDAO {
    private final Connection conn;

    public MaintenanceLogDAO(Connection conn) {
        this.conn = conn;
    }

    public void insert(MaintenanceLog log) {
        String sql = "INSERT INTO maintenance_logs "
                + "(log_id, timestamp, action_type, description, technician_id, atm_id) "
                + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, log.getLogID());
            ps.setTimestamp(2, Timestamp.valueOf(log.getTimestamp()));
            ps.setString(3, log.getActionType());
            ps.setString(4, log.getDescription());
            ps.setString(5, log.getTechnicianID());
            ps.setString(6, log.getAtmID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert maintenance log: " + e.getMessage(), e);
        }
    }

    public List<MaintenanceLog> findByAtmId(String atmId) {
        String sql = "SELECT log_id, action_type, description, technician_id, atm_id "
                + "FROM maintenance_logs WHERE atm_id = ? ORDER BY timestamp";
        List<MaintenanceLog> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, atmId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new MaintenanceLog(
                            rs.getString("log_id"),
                            rs.getString("action_type"),
                            rs.getString("description"),
                            rs.getString("technician_id"),
                            rs.getString("atm_id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list maintenance logs: " + e.getMessage(), e);
        }
        return result;
    }
}
