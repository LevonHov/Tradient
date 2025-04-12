package com.example.tradient.data.model;

/**
 * Represents an order in an exchange's order book
 */
public class Order {
    private double price;
    private double quantity;
    private boolean isBid; // true for buy (bid), false for sell (ask)
    
    public Order() {
    }
    
    public Order(double price, double quantity, boolean isBid) {
        this.price = price;
        this.quantity = quantity;
        this.isBid = isBid;
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
    
    public boolean isBid() {
        return isBid;
    }
    
    public void setBid(boolean bid) {
        isBid = bid;
    }
    
    /**
     * Get the total value of this order (price * quantity)
     * @return The total value
     */
    public double getTotal() {
        return price * quantity;
    }
} 