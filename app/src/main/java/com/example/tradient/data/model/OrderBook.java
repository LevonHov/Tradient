package com.example.tradient.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the order book for a given trading pair.
 * <p>
 * This class stores the list of bid and ask orders along with a timestamp.
 * Additional helper methods provide quick access to key values such as:
 * - The best bid and best ask orders.
 * - The volume available at the best bid or ask.
 * - The spread between the best ask and best bid prices.
 */
public class OrderBook {

    // The trading pair symbol (e.g., "BTC/USD")
    private String symbol;

    // Lists of order book entries for bids and asks.
    private List<OrderBookEntry> bids;
    private List<OrderBookEntry> asks;

    // The timestamp when the order book was last updated.
    private long timestamp;

    private String exchangeName;
    private Ticker ticker;

    // Add a map to store metadata about the order book
    private Map<String, Object> metadata;

    /**
     * Constructor to initialize the OrderBook.
     *
     * @param symbol    The trading pair symbol.
     * @param bids      The list of bid entries (typically sorted descending by price).
     * @param asks      The list of ask entries (typically sorted ascending by price).
     * @param timestamp The timestamp of the order book snapshot.
     */
    public OrderBook(String symbol, List<OrderBookEntry> bids, List<OrderBookEntry> asks, long timestamp) {
        this.symbol = symbol;
        this.bids = bids;
        this.asks = asks;
        this.timestamp = timestamp;
        this.metadata = new HashMap<>();
    }

    /**
     * Constructor with Date timestamp for backward compatibility.
     *
     * @param symbol    The trading pair symbol.
     * @param bids      The list of bid entries.
     * @param asks      The list of ask entries.
     * @param timestamp The timestamp as a Date object.
     */
    public OrderBook(String symbol, List<OrderBookEntry> bids, List<OrderBookEntry> asks, Date timestamp) {
        this(symbol, bids, asks, timestamp.getTime());
    }

    public OrderBook() {
        this.bids = new ArrayList<>();
        this.asks = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Returns the trading pair symbol associated with this order book.
     *
     * @return The symbol (e.g., "BTC/USD").
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns the list of bid entries.
     *
     * @return A list of OrderBookEntry objects for bids.
     */
    public List<OrderBookEntry> getBids() {
        return bids;
    }

    /**
     * Returns the list of ask entries.
     *
     * @return A list of OrderBookEntry objects for asks.
     */
    public List<OrderBookEntry> getAsks() {
        return asks;
    }

    /**
     * Returns the timestamp of this order book snapshot.
     *
     * @return The timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the timestamp as a Date object (for backward compatibility).
     *
     * @return The timestamp as a Date.
     */
    public Date getTimestampAsDate() {
        return new Date(timestamp);
    }

    /**
     * For backward compatibility - processes that expect to call getTime() on the result.
     */
    public Date getTimestamp_ForBackwardCompatibility() {
        return new Date(timestamp);
    }

    /**
     * Retrieves the best bid entry.
     * <p>
     * Assumes that the bids list is sorted in descending order by price.
     *
     * @return The highest bid entry, or null if there are no bids.
     */
    public OrderBookEntry getBestBid() {
        return (bids != null && !bids.isEmpty()) ? bids.get(0) : null;
    }

    /**
     * Retrieves the best ask entry.
     * <p>
     * Assumes that the asks list is sorted in ascending order by price.
     *
     * @return The lowest ask entry, or null if there are no asks.
     */
    public OrderBookEntry getBestAsk() {
        return (asks != null && !asks.isEmpty()) ? asks.get(0) : null;
    }

    /**
     * Retrieves the volume available at the best bid price.
     *
     * @return The volume of the best bid, or 0 if no bids are available.
     */
    public double getBestBidVolume() {
        OrderBookEntry bestBid = getBestBid();
        return bestBid != null ? bestBid.getVolume() : 0;
    }

    /**
     * Retrieves the volume available at the best ask price.
     *
     * @return The volume of the best ask, or 0 if no asks are available.
     */
    public double getBestAskVolume() {
        OrderBookEntry bestAsk = getBestAsk();
        return bestAsk != null ? bestAsk.getVolume() : 0;
    }

    /**
     * Calculates the spread between the best ask and the best bid prices.
     *
     * @return The spread (best ask price minus best bid price), or 0 if either side is missing.
     */
    public double getSpread() {
        OrderBookEntry bestBid = getBestBid();
        OrderBookEntry bestAsk = getBestAsk();
        if (bestBid != null && bestAsk != null) {
            return bestAsk.getPrice() - bestBid.getPrice();
        }
        return 0;
    }

    /**
     * Updates the bids list.
     *
     * @param bids The new list of bid entries.
     */
    public void setBids(List<OrderBookEntry> bids) {
        this.bids = bids;
    }

    /**
     * Updates the asks list.
     *
     * @param asks The new list of ask entries.
     */
    public void setAsks(List<OrderBookEntry> asks) {
        this.asks = asks;
    }

    /**
     * Set timestamp using a Date object (for backward compatibility).
     *
     * @param timestamp The Date timestamp to set.
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp != null ? timestamp.getTime() : System.currentTimeMillis();
    }

    /**
     * Get bids as a map for backward compatibility
     */
    public Map<Double, Double> getBidsAsMap() {
        Map<Double, Double> bidsMap = new TreeMap<>((a, b) -> Double.compare(b, a)); // Descending order for bids
        for (OrderBookEntry entry : bids) {
            bidsMap.put(entry.getPrice(), entry.getQuantity());
        }
        return bidsMap;
    }

    /**
     * Get asks as a map for backward compatibility
     */
    public Map<Double, Double> getAsksAsMap() {
        Map<Double, Double> asksMap = new TreeMap<>(); // Ascending order for asks
        for (OrderBookEntry entry : asks) {
            asksMap.put(entry.getPrice(), entry.getQuantity());
        }
        return asksMap;
    }

    /**
     * Get the exchange name
     */
    public String getExchangeName() {
        return exchangeName;
    }

    /**
     * Set the exchange name
     */
    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    /**
     * Get the associated ticker
     */
    public Ticker getTicker() {
        return ticker;
    }

    /**
     * Set the associated ticker
     */
    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
    }

    /**
     * Store metadata about the order book
     * 
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
    
    /**
     * Get metadata value as Object
     * 
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public Object getMetadata(String key) {
        if (metadata == null) {
            return null;
        }
        return metadata.get(key);
    }
    
    /**
     * Get metadata value as Double
     * 
     * @param key The metadata key
     * @param defaultValue The default value to return if not found or not a Double
     * @return The metadata value as Double, or defaultValue if not found
     */
    public double getMetadataDouble(String key, double defaultValue) {
        Object value = getMetadata(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    /**
     * Get all metadata
     * 
     * @return Map of all metadata
     */
    public Map<String, Object> getAllMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return Collections.unmodifiableMap(metadata);
    }

}
