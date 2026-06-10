package models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bank customer
 */
public class Customer {
    private String customerID;
    private String name;
    private String address;
    private String phoneNumber;
    private List<Card> cards;
    private List<Account> accounts;
    
    public Customer(String customerID, String name, String address, String phoneNumber) {
        this.customerID = customerID;
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.cards = new ArrayList<>();
        this.accounts = new ArrayList<>();
    }
    
    /**
     * Adds a card to customer's cards
     */
    public void addCard(Card card) {
        this.cards.add(card);
    }
    
    /**
     * Adds an account to customer's accounts
     */
    public void addAccount(Account account) {
        this.accounts.add(account);
    }
    
    /**
     * Gets account by account number
     */
    public Account getAccount(String accountNumber) {
        for (Account account : accounts) {
            if (account.getAccountNumber().equals(accountNumber)) {
                return account;
            }
        }
        return null;
    }
    
    // Getters and Setters
    public String getCustomerID() {
        return customerID;
    }
    
    public void setCustomerID(String customerID) {
        this.customerID = customerID;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public List<Card> getCards() {
        return cards;
    }
    
    public List<Account> getAccounts() {
        return accounts;
    }
    
    @Override
    public String toString() {
        return "Customer{" +
                "customerID='" + customerID + '\'' +
                ", name='" + name + '\'' +
                ", numberOfAccounts=" + accounts.size() +
                '}';
    }
}
