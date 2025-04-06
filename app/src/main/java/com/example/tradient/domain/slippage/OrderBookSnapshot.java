package com.example.tradient.domain.slippage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a snapshot of an order book at a specific moment in time.
 * Provides the foundation for slippage calculations.
 */
public class OrderBookSnapshot {
    private final String symbol;
    private final String exchange;
    private final Instant timestamp;
    private final List<PriceLevel> asks = new ArrayList<>();
    private final List<PriceLevel> bids = new ArrayList<>();
    
    public OrderBookSnapshot(String symbol, String exchange, Instant timestamp) {
        this.symbol = symbol;
        this.exchange = exchange;
        this.timestamp = timestamp;
    }
    
    public void addAsk(double price, double volume) {
        asks.add(new PriceLevel(price, volume));
    }
    
    public void addBid(double price, double volume) {
        bids.add(new PriceLevel(price, volume));
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getExchange() {
        return exchange;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public List<PriceLevel> getAsks() {
        return asks;
    }
    
    public List<PriceLevel> getBids() {
        return bids;
    }
    
    /**
     * Inner class representing a price level in the order book.
     */
    public static class PriceLevel {
        private final double price;
        private final double volume;
        
        public PriceLevel(double price, double volume) {
            this.price = price;
            this.volume = volume;
        }
        
        public double getPrice() {
            return price;
        }
        
        public double getVolume() {
            return volume;
        }
    }
} 