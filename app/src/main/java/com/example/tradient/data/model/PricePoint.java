package com.example.tradient.data.model;

import java.util.Date;

/**
 * Data model representing a price point at a specific timestamp.
 * Used for tracking price movements and displaying them in charts.
 */
public class PricePoint {

    private final float price;
    private final Date timestamp;
    private final String exchange;
    private final String symbol;

    /**
     * Create a new price point
     *
     * @param price the price value
     * @param timestamp the time when the price was recorded
     * @param exchange the exchange where the price was recorded
     * @param symbol the trading symbol (e.g., BTC/USD)
     */
    public PricePoint(float price, Date timestamp, String exchange, String symbol) {
        this.price = price;
        this.timestamp = timestamp;
        this.exchange = exchange;
        this.symbol = symbol;
    }

    /**
     * Get the price value
     *
     * @return the price as a float
     */
    public float getPrice() {
        return price;
    }

    /**
     * Get the timestamp when the price was recorded
     *
     * @return Date object representing the timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Get the exchange where the price was recorded
     *
     * @return exchange name as string
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * Get the trading symbol
     *
     * @return symbol as string (e.g., "BTC/USD")
     */
    public String getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return "PricePoint{" +
                "price=" + price +
                ", timestamp=" + timestamp +
                ", exchange='" + exchange + '\'' +
                ", symbol='" + symbol + '\'' +
                '}';
    }
} 