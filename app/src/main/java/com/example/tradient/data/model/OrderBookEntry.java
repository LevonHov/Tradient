package com.example.tradient.data.model;

/**
 * Represents an entry in an order book with price and quantity
 */
public class OrderBookEntry {
    private double price;
    private double quantity;
    
    public OrderBookEntry() {
    }
    
    public OrderBookEntry(double price, double quantity) {
        this.price = price;
        this.quantity = quantity;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    
    /**
     * Get the volume of this entry (same as quantity)
     * @return The quantity
     */
    public double getVolume() {
        return quantity;
    }
    
    /**
     * Set the volume of this entry (same as quantity)
     * @param volume The volume/quantity to set
     */
    public void setVolume(double volume) {
        this.quantity = volume;
    }
    
    /**
     * Calculate the total value of this entry (price * quantity)
     * @return The total value
     */
    public double getTotal() {
        return price * quantity;
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
