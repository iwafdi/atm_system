package models;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Represents an ATM card used for customer authentication
 */
public class Card {
    private String cardNumber;
    private String expiryDate;
    private String cardType;
    private String bankID;
    private String pin;
    
    public Card(String cardNumber, String expiryDate, String cardType, String bankID, String pin) {
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.cardType = cardType;
        this.bankID = bankID;
        this.pin = pin;
    }
    
    /**
     * Validates the card number format
     */
    public boolean validate() {
        if (cardNumber == null || cardNumber.length() != 16) {
            return false;
        }
        return cardNumber.matches("\\d{16}");
    }
    
    /**
     * Checks if the card has expired
     */
    public boolean isExpired() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
            LocalDate expiry = LocalDate.parse("01/" + expiryDate, 
                                             DateTimeFormatter.ofPattern("dd/MM/yy"));
            return expiry.isBefore(LocalDate.now());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Validates PIN
     */
    public boolean validatePIN(String enteredPIN) {
        return this.pin.equals(enteredPIN);
    }
    
    // Getters and Setters
    public String getCardNumber() {
        return cardNumber;
    }
    
    public String getExpiryDate() {
        return expiryDate;
    }
    
    public String getCardType() {
        return cardType;
    }
    
    public String getBankID() {
        return bankID;
    }
    
    @Override
    public String toString() {
        return "Card{" +
                "cardNumber='" + maskCardNumber() + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", cardType='" + cardType + '\'' +
                '}';
    }
    
    private String maskCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "************" + cardNumber.substring(12);
    }
}
