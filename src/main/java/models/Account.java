package models;

/**
 * Represents a bank account with transaction capabilities
 */
public class Account {
    private String accountNumber;
    private String accountType;
    private double balance;
    private String customerID;
    
    public Account(String accountNumber, String accountType, double balance, String customerID) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.balance = balance;
        this.customerID = customerID;
    }
    
    /**
     * Gets current account balance
     */
    public double getBalance() {
        return balance;
    }
    
    /**
     * Withdraws specified amount from account
     * @param amount Amount to withdraw
     * @return true if withdrawal successful
     */
    public synchronized boolean withdraw(double amount) {
        if (amount <= 0) {
            System.out.println("Invalid withdrawal amount");
            return false;
        }
        
        if (amount > balance) {
            System.out.println("Insufficient funds");
            return false;
        }
        
        balance -= amount;
        System.out.println("Withdrawal successful. New balance: £" + balance);
        return true;
    }
    
    /**
     * Deposits specified amount into account
     * @param amount Amount to deposit
     * @return true if deposit successful
     */
    public synchronized boolean deposit(double amount) {
        if (amount <= 0) {
            System.out.println("Invalid deposit amount");
            return false;
        }
        
        balance += amount;
        System.out.println("Deposit successful. New balance: £" + balance);
        return true;
    }
    
    /**
     * Transfers funds to another account
     * @param targetAccount Destination account
     * @param amount Amount to transfer
     * @return true if transfer successful
     */
    public synchronized boolean transfer(Account targetAccount, double amount) {
        if (amount <= 0) {
            System.out.println("Invalid transfer amount");
            return false;
        }
        
        if (amount > balance) {
            System.out.println("Insufficient funds for transfer");
            return false;
        }
        
        if (this.withdraw(amount)) {
            targetAccount.deposit(amount);
            System.out.println("Transfer successful");
            return true;
        }
        
        return false;
    }
    
    /**
     * Updates account balance
     */
    public void updateBalance(double amount) {
        this.balance += amount;
    }
    
    // Getters and Setters
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getAccountType() {
        return accountType;
    }
    
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    
    public String getCustomerID() {
        return customerID;
    }
    
    public void setCustomerID(String customerID) {
        this.customerID = customerID;
    }
    
    @Override
    public String toString() {
        return "Account{" +
                "accountNumber='" + accountNumber + '\'' +
                ", accountType='" + accountType + '\'' +
                ", balance=" + balance +
                '}';
    }
}
