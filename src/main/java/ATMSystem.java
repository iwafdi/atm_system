import models.*;
import db.*;
import java.sql.Connection;
import java.util.List;
import java.util.Scanner;

/**
 * Main ATM System Application (PostgreSQL-backed).
 */
public class ATMSystem {
    private static Scanner scanner = new Scanner(System.in);

    // Global session state
    private static ATM currentATM = null;
    private static Customer currentCustomer = null;
    private static Card currentCard = null;
    private static Technician currentTechnician = null;

    // DAOs (initialized after the DB connection opens)
    private static AccountDAO accountDAO;
    private static CustomerDAO customerDAO;
    private static CardDAO cardDAO;
    private static TechnicianDAO technicianDAO;
    private static AtmDAO atmDAO;
    private static MaintenanceLogDAO logDAO;
    private static TransactionDAO transactionDAO;

    private static final String ATM_ID = "ATM-001A";
    private static final int MAX_PIN_ATTEMPTS = 3;

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("   WELCOME TO PING-SOURCE ATM SYSTEM");
        System.out.println("===========================================\n");

        if (!startup()) {
            return;
        }

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

        Database.close();
        System.out.println("\nThank you for using Ping-Source ATM!");
        scanner.close();
    }

    /**
     * Connects to PostgreSQL, ensures the schema exists, seeds sample data on
     * first run, builds the DAOs, and loads the ATM. Returns false on failure.
     */
    private static boolean startup() {
        try {
            Connection conn = Database.connect();
            SchemaInitializer.runSchema(conn);
            SchemaInitializer.seedIfEmpty(conn);

            accountDAO = new AccountDAO(conn);
            customerDAO = new CustomerDAO(conn);
            cardDAO = new CardDAO(conn);
            technicianDAO = new TechnicianDAO(conn);
            atmDAO = new AtmDAO(conn);
            logDAO = new MaintenanceLogDAO(conn);
            transactionDAO = new TransactionDAO(conn);

            currentATM = atmDAO.findById(ATM_ID);
            if (currentATM == null) {
                System.out.println("❌ ATM " + ATM_ID + " not found in database.");
                return false;
            }
            System.out.println("Connected to database. ATM " + ATM_ID + " ready.\n");
            return true;
        } catch (RuntimeException e) {
            System.out.println("❌ Could not start ATM: " + e.getMessage());
            System.out.println("   Check that PostgreSQL is running and db.properties is correct.");
            return false;
        }
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

        Technician tech = technicianDAO.findById(authInput);
        if (tech != null) {
            return authenticateTechnician(tech);
        }

        return authenticateCustomer(authInput);
    }

    /**
     * Handle Technician Authentication
     */
    private static boolean authenticateTechnician(Technician tech) {
        int attempts = 0;
        while (attempts < MAX_PIN_ATTEMPTS) {
            System.out.print("Enter Technician PIN: ");
            String pin = scanner.nextLine();

            if (technicianDAO.verifyPin(tech.getTechnicianID(), pin)) {
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
        recordLog("TECHNICIAN_LOGIN", "Technician logged in successfully.");
        return true;
    }

    /**
     * Authenticate Customer with card input
     */
    private static boolean authenticateCustomer(String cardInput) {
        Card card = cardDAO.findByCardNumber(cardInput);
        if (card == null) {
            System.out.println("❌ Invalid card number. Card returned.");
            return true;
        }
        if (!card.validate()) {
            System.out.println("❌ Invalid card format. Card returned.");
            return true;
        }
        if (card.isExpired()) {
            System.out.println("❌ Card has expired. Please contact your bank.");
            return true;
        }

        int attempts = 0;
        while (attempts < MAX_PIN_ATTEMPTS) {
            System.out.print("Enter PIN: ");
            String pin = scanner.nextLine();

            if (cardDAO.verifyPin(cardInput, pin)) {
                currentCard = card;
                currentCustomer = loadCustomer(cardDAO.findCustomerIdByCardNumber(cardInput));
                System.out.println("✓ Authentication successful!\n");
                return true;
            }
            attempts++;
            if (attempts < MAX_PIN_ATTEMPTS) {
                System.out.println("❌ Incorrect PIN. " + (MAX_PIN_ATTEMPTS - attempts) + " attempts remaining.");
            }
        }
        System.out.println("❌ Maximum PIN attempts exceeded. Card blocked.");
        return true;
    }

    /** Loads a customer with its accounts and cards attached. */
    private static Customer loadCustomer(String customerId) {
        Customer customer = customerDAO.findById(customerId);
        for (Account account : accountDAO.findByCustomerId(customerId)) {
            customer.addAccount(account);
        }
        for (Card card : cardDAO.findByCustomerId(customerId)) {
            customer.addCard(card);
        }
        return customer;
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
                    recordLog("DIAGNOSTICS", "Ran ATM self-diagnostics.");
                    break;
                case "2":
                    handleCashReplenishment();
                    break;
                case "3":
                    currentATM.replenishInkAndPaper();
                    atmDAO.update(currentATM);
                    recordLog("REPLENISH", "Replenished Ink and Paper levels.");
                    break;
                case "4":
                    handleSystemUpgrade();
                    break;
                case "5":
                    printMaintenanceLogs();
                    break;
                case "6":
                    sessionActive = false;
                    recordLog("LOGOUT", "Technician logged out.");
                    System.out.println("\n✓ Technician Session ended.");
                    break;
                default:
                    System.out.println("❌ Invalid option. Please try again.");
            }
        }
    }

    /** Prints maintenance logs read back from the database. */
    private static void printMaintenanceLogs() {
        List<MaintenanceLog> logs = logDAO.findByAtmId(ATM_ID);
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

    private static void handleCashReplenishment() {
        System.out.print("Enter amount £ to replenish: ");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            currentATM.replenishCash(amount);
            atmDAO.update(currentATM);
            recordLog("CASH_REPLENISH", "Added £" + amount + " to ATM Inventory.");
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
        atmDAO.update(currentATM);
        recordLog("SYSTEM_UPGRADE", "Upgraded " + typeName + " to " + version);
    }

    /** Records a maintenance log both in the ATM object and the database. */
    private static void recordLog(String type, String description) {
        String technicianId = currentTechnician != null ? currentTechnician.getTechnicianID() : "SYSTEM";
        MaintenanceLog log = new MaintenanceLog(
                "LOG-" + System.currentTimeMillis(), type, description, technicianId, ATM_ID);
        currentATM.addMaintenanceLog(log);
        logDAO.insert(log);
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

    private static void withdrawCash() {
        System.out.println("\n--- WITHDRAW CASH ---");
        Account account = selectAccount();
        if (account == null) {
            return;
        }
        System.out.print("Enter amount to withdraw: £");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            if (currentATM.getCashInventory() < amount) {
                System.out.println("❌ ATM has insufficient funds to fulfill this withdrawal.");
                return;
            }
            if (account.withdraw(amount)) {
                if (currentATM.dispenseCash(amount)) {
                    accountDAO.updateBalance(account.getAccountNumber(), account.getBalance());
                    atmDAO.update(currentATM);
                    transactionDAO.insert("WITHDRAWAL", account.getAccountNumber(), amount,
                            account.getBalance(), null, ATM_ID);
                    System.out.println("\n✓ Please collect your cash");
                    printReceipt("Withdrawal", account, amount, account.getBalance());
                } else {
                    account.deposit(amount); // rollback in-memory
                    System.out.println("❌ ATM Hardware failure during dispense. Rolled back.");
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid amount entered");
        }
    }

    private static void depositFunds() {
        System.out.println("\n--- DEPOSIT FUNDS ---");
        Account account = selectAccount();
        if (account == null) {
            return;
        }
        System.out.print("Enter amount to deposit: £");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            if (account.deposit(amount)) {
                currentATM.acceptCash(amount);
                accountDAO.updateBalance(account.getAccountNumber(), account.getBalance());
                atmDAO.update(currentATM);
                transactionDAO.insert("DEPOSIT", account.getAccountNumber(), amount,
                        account.getBalance(), null, ATM_ID);
                printReceipt("Deposit", account, amount, account.getBalance());
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid amount entered");
        }
    }

    private static void transferFunds() {
        System.out.println("\n--- TRANSFER FUNDS ---");
        System.out.println("Select source account:");
        Account sourceAccount = selectAccount();
        if (sourceAccount == null) {
            return;
        }
        System.out.println("\nSelect destination account:");
        Account destinationAccount = selectAccount();
        if (destinationAccount == null) {
            return;
        }
        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) {
            System.out.println("❌ Cannot transfer to the same account");
            return;
        }
        System.out.print("Enter amount to transfer: £");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            if (amount <= 0) {
                System.out.println("❌ Invalid transfer amount");
                return;
            }
            if (amount > sourceAccount.getBalance()) {
                System.out.println("❌ Insufficient funds for transfer");
                return;
            }
            accountDAO.transfer(sourceAccount.getAccountNumber(),
                    destinationAccount.getAccountNumber(), amount, ATM_ID);
            // Refresh in-memory balances from the persisted truth
            double newSourceBalance = accountDAO.findByNumber(sourceAccount.getAccountNumber()).getBalance();
            sourceAccount.updateBalance(newSourceBalance - sourceAccount.getBalance());
            System.out.println("✓ Transfer successful");
            printReceipt("Transfer", sourceAccount, amount, newSourceBalance);
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid amount entered");
        } catch (RuntimeException e) {
            System.out.println("❌ " + e.getMessage());
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
