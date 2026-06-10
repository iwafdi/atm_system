package models;

/**
 * Represents an ATM Technician capable of maintenance and repairs
 */
public class Technician {
    private String technicianID;
    private String name;
    private String clearanceLevel;
    private String pin;

    public Technician(String technicianID, String name, String clearanceLevel, String pin) {
        this.technicianID = technicianID;
        this.name = name;
        this.clearanceLevel = clearanceLevel;
        this.pin = pin;
    }

    /**
     * Validates Technician PIN
     */
    public boolean authenticate(String enteredPIN) {
        return this.pin.equals(enteredPIN);
    }

    // Getters and Setters
    public String getTechnicianID() {
        return technicianID;
    }

    public void setTechnicianID(String technicianID) {
        this.technicianID = technicianID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClearanceLevel() {
        return clearanceLevel;
    }

    public void setClearanceLevel(String clearanceLevel) {
        this.clearanceLevel = clearanceLevel;
    }

    /** Plaintext PIN, used only when seeding a technician into the database. */
    public String getPinForSeeding() {
        return pin;
    }

    @Override
    public String toString() {
        return "Technician{" +
                "technicianID='" + technicianID + '\'' +
                ", name='" + name + '\'' +
                ", clearanceLevel='" + clearanceLevel + '\'' +
                '}';
    }
}
