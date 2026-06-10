package models;

import java.time.LocalDateTime;

/**
 * Represents a log of ATM maintenance or repair activities
 */
public class MaintenanceLog {
    private String logID;
    private LocalDateTime timestamp;
    private String actionType;
    private String description;
    private String technicianID;
    private String atmID;
    
    public MaintenanceLog(String logID, String actionType, String description, String technicianID, String atmID) {
        this.logID = logID;
        this.timestamp = LocalDateTime.now();
        this.actionType = actionType;
        this.description = description;
        this.technicianID = technicianID;
        this.atmID = atmID;
    }
    
    public String getLogID() {
        return logID;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getTechnicianID() {
        return technicianID;
    }
    
    public String getAtmID() {
        return atmID;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s (Tech: %s, ATM: %s)", 
            timestamp.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
            actionType, description, technicianID, atmID);
    }
}
