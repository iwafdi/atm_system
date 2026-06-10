package models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the physical ATM machine and its internal states
 */
public class ATM {
    private String atmID;
    private String location;

    // Physical Supplies
    private double cashInventory;
    private int inkLevel; // Percentage 0-100
    private int paperLevel; // Percentage 0-100

    // Software states
    private String firmwareVersion;
    private String softwareVersion;
    private String hardwareStatus;

    private List<MaintenanceLog> logs;

    public ATM(String atmID, String location, double initialCash) {
        this.atmID = atmID;
        this.location = location;
        this.cashInventory = initialCash;
        this.inkLevel = 100;
        this.paperLevel = 100;
        this.firmwareVersion = "v1.0.0";
        this.softwareVersion = "v1.0.0";
        this.hardwareStatus = "OPERATIONAL";
        this.logs = new ArrayList<>();
    }

    /** Full-state constructor used when loading an ATM from the database. */
    public ATM(String atmID, String location, double cashInventory, int inkLevel,
               int paperLevel, String firmwareVersion, String softwareVersion,
               String hardwareStatus) {
        this.atmID = atmID;
        this.location = location;
        this.cashInventory = cashInventory;
        this.inkLevel = inkLevel;
        this.paperLevel = paperLevel;
        this.firmwareVersion = firmwareVersion;
        this.softwareVersion = softwareVersion;
        this.hardwareStatus = hardwareStatus;
        this.logs = new java.util.ArrayList<>();
    }

    /**
     * Dispenses cash from ATM Inventory
     */
    public boolean dispenseCash(double amount) {
        if (amount > cashInventory) {
            System.out.println("❌ ATM has insufficient cash inventory.");
            return false;
        }
        cashInventory -= amount;
        return true;
    }

    /**
     * Accepts cash into ATM Inventory
     */
    public void acceptCash(double amount) {
        cashInventory += amount;
    }

    /**
     * Technician action: Replenish Cash
     */
    public void replenishCash(double amount) {
        cashInventory += amount;
        System.out.println("Cash inventory replenished. New total: £" + String.format("%.2f", cashInventory));
    }

    /**
     * Technician action: Replenish Ink and Paper
     */
    public void replenishInkAndPaper() {
        this.inkLevel = 100;
        this.paperLevel = 100;
        System.out.println("Ink and Paper replenished to 100%.");
    }

    /**
     * Technician action: Apply System Upgrade
     */
    public void applyUpgrade(String type, String version) {
        if (type.equalsIgnoreCase("firmware")) {
            this.firmwareVersion = version;
            System.out.println("Firmware upgraded to " + version);
        } else if (type.equalsIgnoreCase("software")) {
            this.softwareVersion = version;
            System.out.println("Software upgraded to " + version);
        } else {
            System.out.println("Unknown upgrade type.");
        }
    }

    /**
     * Technician action: Run Self Diagnostics
     */
    public void runSelfDiagnostics() {
        System.out.println("\n--- ATM DIAGNOSTICS REPORT ---");
        System.out.println("ATM ID: " + atmID);
        System.out.println("Location: " + location);
        System.out.println("Cash Inventory: £" + String.format("%.2f", cashInventory));
        System.out.println("Ink Level: " + inkLevel + "%");
        System.out.println("Paper Level: " + paperLevel + "%");
        System.out.println("Firmware Version: " + firmwareVersion);
        System.out.println("Software Version: " + softwareVersion);
        System.out.println("Hardware Status: " + hardwareStatus);
        System.out.println("------------------------------");
    }

    /**
     * Adds a maintenance log
     */
    public void addMaintenanceLog(MaintenanceLog log) {
        this.logs.add(log);
    }

    /**
     * Prints all maintenance logs
     */
    public void printMaintenanceLogs() {
        if (logs.isEmpty()) {
            System.out.println("No maintenance logs found.");
            return;
        }
        System.out.println("\n--- MAINTENANCE LOGS ---");
        for (MaintenanceLog log : logs) {
            System.out.println(log.toString());
        }
        System.out.println("------------------------");
    }

    // Getters and Setters
    public String getAtmID() {
        return atmID;
    }

    public String getLocation() {
        return location;
    }

    public double getCashInventory() {
        return cashInventory;
    }

    public int getInkLevel() {
        return inkLevel;
    }

    public int getPaperLevel() {
        return paperLevel;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public String getHardwareStatus() {
        return hardwareStatus;
    }
}
