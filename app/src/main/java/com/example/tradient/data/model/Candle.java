package com.example.tradient.data.model;

/**
 * Represents a price candle for historical data analysis
 */
public class Candle {
    private long openTime;
    private long closeTime;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
    
    public Candle() {
    }
    
    public Candle(long openTime, long closeTime, double open, double high, double low, double close, double volume) {
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
    
    public long getOpenTime() {
        return openTime;
    }
    
    public void setOpenTime(long openTime) {
        this.openTime = openTime;
    }
    
    public long getCloseTime() {
        return closeTime;
    }
    
    public void setCloseTime(long closeTime) {
        this.closeTime = closeTime;
    }
    
    public double getOpen() {
        return open;
    }
    
    public void setOpen(double open) {
        this.open = open;
    }
    
    public double getHigh() {
        return high;
    }
    
    public void setHigh(double high) {
        this.high = high;
    }
    
    public double getLow() {
        return low;
    }
    
    public void setLow(double low) {
        this.low = low;
    }
    
    public double getClose() {
        return close;
    }
    
    public void setClose(double close) {
        this.close = close;
    }
    
    public double getVolume() {
        return volume;
    }
    
    public void setVolume(double volume) {
        this.volume = volume;
    }
    
    /**
     * Calculate the candle's price range as a percentage of the closing price
     * @return The price range as a percentage
     */
    public double getPriceRangePercent() {
        if (close == 0) return 0;
        return (high - low) / close * 100.0;
    }
    
    /**
     * Calculate the candle's price change as a percentage
     * @return The price change as a percentage
     */
    public double getPriceChangePercent() {
        if (open == 0) return 0;
        return (close - open) / open * 100.0;
    }
} 