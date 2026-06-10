package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single banking transaction (withdrawal, deposit, or transfer).
 */
public class Transaction {
    private int transactionId;
    private LocalDateTime timestamp;
    private String type;
    private String accountNumber;
    private double amount;
    private double balanceAfter;
    private String relatedAccount; // transfer destination; null otherwise
    private String atmId;

    public Transaction(int transactionId, LocalDateTime timestamp, String type,
                       String accountNumber, double amount, double balanceAfter,
                       String relatedAccount, String atmId) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.type = type;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.relatedAccount = relatedAccount;
        this.atmId = atmId;
    }

    public int getTransactionId() { return transactionId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public String getAccountNumber() { return accountNumber; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
    public String getRelatedAccount() { return relatedAccount; }
    public String getAtmId() { return atmId; }

    @Override
    public String toString() {
        return String.format("[%s] %s %s £%.2f -> balance £%.2f%s",
                timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                type, accountNumber, amount, balanceAfter,
                relatedAccount == null ? "" : " (to " + relatedAccount + ")");
    }
}
