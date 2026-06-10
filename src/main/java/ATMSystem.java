import models.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Main ATM System Application
 * Demonstrates UML class diagram implementation with Technician Extension
 */
public class ATMSystem {
    private static Scanner scanner = new Scanner(System.in);

    // Global States
    private static ATM currentATM = null;
    private static Customer currentCustomer = null;
    private static Card currentCard = null;
    private static Technician currentTechnician = null;

    // Data Storage
    private static List<Technician> technicians = new ArrayList<>();
    private static List<Customer> customers = new ArrayList<>();

    private static final int MAX_PIN_ATTEMPTS = 3;

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("   WELCOME TO PING-SOURCE ATM SYSTEM");
        System.out.println("===========================================\n");

        // Initialize sample data
        initializeSampleData();

        // Start ATM operation
        boolean running = true;
        while (running) {
            running = authenticateUser();
            if (running) {
                if (currentTechnician != null) {
                    showTechnicianMenu();
                } else if (currentCustomer != null) {
                    showMainMenu();
                }
            }
        }

        System.out.println("\nThank you for using Ping-Source ATM!");
        scanner.close();
    }

    /**
     * Initialize sample customer, account data, and ATM states
     */
    private static void initializeSampleData() {
        // Initialize ATM
        currentATM = new ATM("ATM-001A", "Main Branch", 10000.00);

        // Create sample customer
        currentCustomer = new Customer("CUST001", "John Smith", "123 Main St", "07700900000");

        // Create sample accounts
        Account savingsAccount = new Account("ACC001", "Savings", 5000.00, "CUST001");
        Account currentAccount = new Account("ACC002", "Current", 2500.00, "CUST001");

        currentCustomer.addAccount(savingsAccount);
        currentCustomer.addAccount(currentAccount);

        // Create sample card
        Card card = new Card("1234567890123456", "12/25", "Debit", "BANK001", "1234");
        currentCustomer.addCard(card);

        // Create sample technician
        technicians.add(new Technician("TECH001", "Alice Tech", "Level 3", "9999"));

        customers.add(currentCustomer);

        System.out.println("Sample data initialized:");
        System.out.println("Customer Card: 1234567890123456 (PIN: 1234)");
        System.out.println("Accounts: Savings (£5000.00), Current (£2500.00)");
        System.out.println("Technician ID: TECH001 (PIN: 9999)\n");
    }

    /**
     * Authenticate user (Customer or Technician)
     */
    private static boolean authenticateUser() {
        currentCustomer = null;
        currentTechnician = null;
        currentCard = null;

        System.out.println("\n--- AUTHENTICATION ---");
        System.out.print("Insert card or Enter Technician ID (or 'exit' to quit): ");
        String authInput = scanner.nextLine();

        if (authInput.equalsIgnoreCase("exit")) {
            return false;
        }

        // Check if input is a Technician ID or PIN
        for (Technician tech : technicians) {
            if (tech.getTechnicianID().equalsIgnoreCase(authInput) || tech.authenticate(authInput)) {
                return authenticateTechnician(tech, authInput);
            }
        }

        // Otherwise, assume it's a Customer Card
        return authenticateCustomer(authInput);
    }

    /**
     * Handle Technician Authentication
     */
    private static boolean authenticateTechnician(Technician tech, String initialInput) {
        // If they already entered the PIN instead of the ID in the first prompt,
        // auto-authenticate
        if (tech.authenticate(initialInput)) {
            return finalizeTechnicianLogin(tech);
        }

        int attempts = 0;
        while (attempts < MAX_PIN_ATTEMPTS) {
            System.out.print("Enter Technician PIN: ");
            String pin = scanner.nextLine();

            if (tech.authenticate(pin)) {
                return finalizeTechnicianLogin(tech);
            }
            attempts++;
            if (attempts < MAX_PIN_ATTEMPTS) {
                System.out.println("❌ Incorrect PIN. " + (MAX_PIN_ATTEMPTS - attempts) + " attempts remaining.");
            }
        }
        System.out.println("❌ Maximum PIN attempts exceeded. Technician account locked.");
        return true;
    }

    private static boolean finalizeTechnicianLogin(Technician tech) {
        System.out.println("✓ Technician Authentication successful!\n");
        currentTechnician = tech;

        // Log the login event
        currentATM.addMaintenanceLog(new MaintenanceLog(
                "LOG-" + System.currentTimeMillis(),
                "TECHNICIAN_LOGIN",
                "Technician logged in successfully.",
                tech.getTechnicianID(),
                currentATM.getAtmID()));
        return true;
    }

    /**
     * Authenticate Customer with card input
     */
    private static boolean authenticateCustomer(String cardInput) {
        // Find card in known customers
        for (Customer cust : customers) {
            for (Card card : cust.getCards()) {
                if (card.getCardNumber().equals(cardInput)) {
                    currentCard = card;
                    currentCustomer = cust;
                    break;
                }
            }
            if (currentCard != null)
                break;
        }

        if (currentCard == null) {
            System.out.println("❌ Invalid card number. Card returned.");
            return true;
        }

        if (!currentCard.validate()) {
            System.out.println("❌ Invalid card format. Card returned.");
            return true;
        }

        if (currentCard.isExpired()) {
            System.out.println("❌ Card has expired. Please contact your bank.");
            return true;
        }

        // PIN validation with retry limit
        int attempts = 0;
        while (attempts < MAX_PIN_ATTEMPTS) {
            System.out.print("Enter PIN: ");
            String pin = scanner.nextLine();

            if (currentCard.validatePIN(pin)) {
                System.out.println("✓ Authentication successful!\n");
                return true; // customer is preserved in global state as it is pre-loaded
            }

            attempts++;
            if (attempts < MAX_PIN_ATTEMPTS) {
                System.out.println("❌ Incorrect PIN. " + (MAX_PIN_ATTEMPTS - attempts) + " attempts remaining.");
            }
        }

        System.out.println("❌ Maximum PIN attempts exceeded. Card blocked.");
        return true;
    }

    /**
     * Display main menu and handle transaction selection for Customers
     */
    private static void showMainMenu() {
        boolean sessionActive = true;

        while (sessionActive) {
            System.out.println("\n========== MAIN MENU ==========");
            System.out.println("1. Check Balance");
            System.out.println("2. Withdraw Cash");
            System.out.println("3. Deposit Funds");
            System.out.println("4. Transfer Funds");
            System.out.println("5. Exit");
            System.out.println("================================");
            System.out.print("Select option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    checkBalance();
                    break;
                case "2":
                    withdrawCash();
                    break;
                case "3":
                    depositFunds();
                    break;
                case "4":
                    transferFunds();
                    break;
                case "5":
                    sessionActive = false;
                    System.out.println("\n✓ Session ended. Card returned.");
                    break;
                default:
                    System.out.println("❌ Invalid option. Please try again.");
            }
        }
    }

    /**
     * Display Technician Menu
     */
    private static void showTechnicianMenu() {
        boolean sessionActive = true;

        while (sessionActive) {
            System.out.println("\n========== TECHNICIAN MENU ==========");
            System.out.println("1. Run Diagnostics");
            System.out.println("2. Replenish Cash Inventory");
            System.out.println("3. Replenish Ink and Paper");
            System.out.println("4. Upgrade Firmware/Software");
            System.out.println("5. View Maintenance Logs");
            System.out.println("6. Exit");
            System.out.println("=====================================");
            System.out.print("Select action: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    currentATM.runSelfDiagnostics();
                    logTechnicianAction("DIAGNOSTICS", "Ran ATM self-diagnostics.");
                    break;
                case "2":
                    handleCashReplenishment();
                    break;
                case "3":
                    currentATM.replenishInkAndPaper();
                    logTechnicianAction("REPLENISH", "Replenished Ink and Paper levels.");
                    break;
                case "4":
                    handleSystemUpgrade();
                    break;
                case "5":
                    currentATM.printMaintenanceLogs();
                    break;
                case "6":
                    sessionActive = false;
                    logTechnicianAction("LOGOUT", "Technician logged out.");
                    System.out.println("\n✓ Technician Session ended.");
                    break;
                default:
                    System.out.println("❌ Invalid option. Please try again.");
            }
        }
    }

    private static void handleCashReplenishment() {
        System.out.print("Enter amount £ to replenish: ");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            currentATM.replenishCash(amount);
            logTechnicianAction("CASH_REPLENISH", "Added £" + amount + " to ATM Inventory.");
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid amount format.");
        }
    }

    private static void handleSystemUpgrade() {
        System.out.println("Upgrade Type: 1. Firmware, 2. Software");
        System.out.print("Select: ");
        String typeChoice = scanner.nextLine();
        String typeName = typeChoice.equals("1") ? "Firmware" : typeChoice.equals("2") ? "Software" : null;

        if (typeName == null) {
            System.out.println("❌ Invalid selection");
            return;
        }

        System.out.print("Enter new version string (e.g., v2.1.0): ");
        String version = scanner.nextLine();
        currentATM.applyUpgrade(typeName, version);
        logTechnicianAction("SYSTEM_UPGRADE", "Upgraded " + typeName + " to " + version);
    }

    /**
     * Helper to log actions
     */
    private static void logTechnicianAction(String type, String description) {
        currentATM.addMaintenanceLog(new MaintenanceLog(
                "LOG-" + System.currentTimeMillis(),
                type,
                description,
                currentTechnician.getTechnicianID(),
                currentATM.getAtmID()));
    }

    /**
     * Check account balance
     */
    private static void checkBalance() {
        System.out.println("\n--- CHECK BALANCE ---");
        Account account = selectAccount();

        if (account != null) {
            System.out.println("\n✓ Account: " + account.getAccountType());
            System.out.println("✓ Current Balance: £" + String.format("%.2f", account.getBalance()));
            printReceipt("Balance Inquiry", account, 0, account.getBalance());
        }
    }

    /**
     * Withdraw cash from account
     */
    private static void withdrawCash() {
        System.out.println("\n--- WITHDRAW CASH ---");
        Account account = selectAccount();

        if (account != null) {
            System.out.print("Enter amount to withdraw: £");
            try {
                double amount = Double.parseDouble(scanner.nextLine());

                // First check if ATM has enough physical cash
                if (currentATM.getCashInventory() >= amount) {
                    // Then logic with customer account
                    if (account.withdraw(amount)) {
                        // Finally physically dispense
                        if (currentATM.dispenseCash(amount)) {
                            System.out.println("\n✓ Please collect your cash");
                            printReceipt("Withdrawal", account, amount, account.getBalance());
                        } else {
                            // rollback since ATM is out of sync or failed
                            account.deposit(amount);
                            System.out.println("❌ ATM Hardware failure during dispense. Rolled back.");
                        }
                    }
                } else {
                    System.out.println("❌ ATM has insufficient funds to fulfill this withdrawal.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid amount entered");
            }
        }
    }

    /**
     * Deposit funds into account
     */
    private static void depositFunds() {
        System.out.println("\n--- DEPOSIT FUNDS ---");
        Account account = selectAccount();

        if (account != null) {
            System.out.print("Enter amount to deposit: £");
            try {
                double amount = Double.parseDouble(scanner.nextLine());

                if (account.deposit(amount)) {
                    currentATM.acceptCash(amount);
                    printReceipt("Deposit", account, amount, account.getBalance());
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid amount entered");
            }
        }
    }

    /**
     * Transfer funds between accounts
     */
    private static void transferFunds() {
        System.out.println("\n--- TRANSFER FUNDS ---");
        System.out.println("Select source account:");
        Account sourceAccount = selectAccount();

        if (sourceAccount != null) {
            System.out.println("\nSelect destination account:");
            Account destinationAccount = selectAccount();

            if (destinationAccount != null && !sourceAccount.equals(destinationAccount)) {
                System.out.print("Enter amount to transfer: £");
                try {
                    double amount = Double.parseDouble(scanner.nextLine());

                    if (sourceAccount.transfer(destinationAccount, amount)) {
                        printReceipt("Transfer", sourceAccount, amount, sourceAccount.getBalance());
                    }
                } catch (NumberFormatException e) {
                    System.out.println("❌ Invalid amount entered");
                }
            } else if (sourceAccount.equals(destinationAccount)) {
                System.out.println("❌ Cannot transfer to the same account");
            }
        }
    }

    /**
     * Select an account from customer's accounts
     */
    private static Account selectAccount() {
        if (currentCustomer.getAccounts().isEmpty()) {
            System.out.println("❌ No accounts found");
            return null;
        }

        System.out.println("\nAvailable accounts:");
        for (int i = 0; i < currentCustomer.getAccounts().size(); i++) {
            Account acc = currentCustomer.getAccounts().get(i);
            System.out.println((i + 1) + ". " + acc.getAccountType() +
                    " (" + acc.getAccountNumber() + ")");
        }

        System.out.print("Select account (1-" + currentCustomer.getAccounts().size() + "): ");
        try {
            int choice = Integer.parseInt(scanner.nextLine());
            if (choice > 0 && choice <= currentCustomer.getAccounts().size()) {
                return currentCustomer.getAccounts().get(choice - 1);
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid selection");
        }

        return null;
    }

    /**
     * Print transaction receipt
     */
    private static void printReceipt(String transactionType, Account account,
            double amount, double balance) {
        System.out.println("\n========== RECEIPT ==========");
        System.out.println("Transaction: " + transactionType);
        System.out.println("Account: " + account.getAccountNumber());
        System.out.println("Account Type: " + account.getAccountType());
        if (amount > 0) {
            System.out.println("Amount: £" + String.format("%.2f", amount));
        }
        System.out.println("Balance: £" + String.format("%.2f", balance));
        System.out.println("Date: " + java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        System.out.println("=============================\n");
    }
}
