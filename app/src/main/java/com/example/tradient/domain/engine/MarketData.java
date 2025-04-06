package com.example.tradient.domain.engine;

/**
 * Domain model representing market data for a specific symbol on a specific exchange.
 * Contains order book information and other market metrics.
 */
public class MarketData {
    private String symbol;
    private String exchange;
    private double bestBidPrice;
    private double bestBidVolume;
    private double bestAskPrice;
    private double bestAskVolume;
    private double bidDepth;
    private double askDepth;
    private double volume24h;
    private long timestamp;
    
    // Getters and setters
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getExchange() {
        return exchange;
    }
    
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }
    
    public double getBestBidPrice() {
        return bestBidPrice;
    }
    
    public void setBestBidPrice(double bestBidPrice) {
        this.bestBidPrice = bestBidPrice;
    }
    
    public double getBestBidVolume() {
        return bestBidVolume;
    }
    
    public void setBestBidVolume(double bestBidVolume) {
        this.bestBidVolume = bestBidVolume;
    }
    
    public double getBestAskPrice() {
        return bestAskPrice;
    }
    
    public void setBestAskPrice(double bestAskPrice) {
        this.bestAskPrice = bestAskPrice;
    }
    
    public double getBestAskVolume() {
        return bestAskVolume;
    }
    
    public void setBestAskVolume(double bestAskVolume) {
        this.bestAskVolume = bestAskVolume;
    }
    
    public double getBidDepth() {
        return bidDepth;
    }
    
    public void setBidDepth(double bidDepth) {
        this.bidDepth = bidDepth;
    }
    
    public double getAskDepth() {
        return askDepth;
    }
    
    public void setAskDepth(double askDepth) {
        this.askDepth = askDepth;
    }
    
    public double getVolume24h() {
        return volume24h;
    }
    
    public void setVolume24h(double volume24h) {
        this.volume24h = volume24h;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Get the bid price (alias for getBestBidPrice)
     * @return The best bid price
     */
    public double getBidPrice() {
        return getBestBidPrice();
    }
    
    /**
     * Get the ask price (alias for getBestAskPrice)
     * @return The best ask price
     */
    public double getAskPrice() {
        return getBestAskPrice();
    }
} 