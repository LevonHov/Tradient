package com.example.tradient.data.model;

/**
 * Represents a single entry in an order book (price and quantity)
 */
public class OrderBookEntry {
    private final double price;
    private final double quantity;
    
    public OrderBookEntry(double price, double quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }
    
    public double getQuantity() {
        return quantity;
    }

    /**
     * Alias for getQuantity() for backward compatibility.
     * @return The volume/quantity amount
     */
    public double getVolume() {
        return quantity;
    }
    
    /**
     * Alias for getQuantity() for backward compatibility.
     * @return The amount/quantity
     */
    public double getAmount() {
        return quantity;
    }
    
    /**
     * No-op for compatibility, as this class now uses final fields.
     * @param volume The volume to set
     * @deprecated Use constructor to set values
     */
    @Deprecated
    public void setVolume(double volume) {
        // No-op for compatibility
    }
    
    /**
     * No-op for compatibility, as this class now uses final fields.
     * @param amount The amount to set
     * @deprecated Use constructor to set values
     */
    @Deprecated
    public void setAmount(double amount) {
        // No-op for compatibility
    }

    /**
     * Optionally, override toString() for easier logging and debugging.
     */
    @Override
    public String toString() {
        return "OrderBookEntry{" +
                "price=" + price +
                ", quantity=" + quantity +
                '}';
    }
}
