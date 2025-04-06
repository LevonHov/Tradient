package com.example.tradient.data.interfaces;

import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;

import java.util.List;

/**
 * Interface for market data retrieval operations.
 * Follows the Interface Segregation Principle by focusing only on market data.
 */
public interface IMarketDataProvider {
    /**
     * Get all available trading pairs from the exchange.
     * Returns cached data if available, otherwise fetches from API.
     *
     * @return List of trading pairs
     */
    List<TradingPair> getTradingPairs();
    
    /**
     * Fetch all trading pairs directly from the exchange API.
     * This method performs network calls to the exchange.
     *
     * @return List of trading pairs
     */
    List<TradingPair> fetchTradingPairs();
    
    /**
     * Get ticker data for a specific trading pair.
     * Returns cached data if available, otherwise fetches from API.
     *
     * @param symbol The trading pair symbol
     * @return Ticker data or null if not available
     */
    Ticker getTickerData(String symbol);
    
    /**
     * Fetch ticker data directly from the exchange API.
     * This method performs network calls to the exchange.
     *
     * @param symbol The trading pair symbol
     * @return Ticker data or null if not available
     */
    Ticker fetchTickerDataREST(String symbol);
    
    /**
     * Get order book for a specific trading pair.
     * Returns cached data if available, otherwise fetches from API.
     *
     * @param symbol The trading pair symbol
     * @return Order book or null if not available
     */
    OrderBook getOrderBook(String symbol);
    
    /**
     * Fetch order book directly from the exchange API.
     * This method performs network calls to the exchange.
     *
     * @param symbol The trading pair symbol
     * @return Order book or null if not available
     */
    OrderBook fetchOrderBookREST(String symbol);
    
    /**
     * Check if ticker data is stale and needs updating.
     *
     * @param ticker The ticker to check
     * @return true if stale, false otherwise
     */
    boolean isTickerStale(Ticker ticker);
    
    /**
     * Check if order book data is stale and needs updating.
     *
     * @param orderBook The order book to check
     * @return true if stale, false otherwise
     */
    boolean isOrderBookStale(OrderBook orderBook);
    
    /**
     * Determine if a trade would be executed as a maker or taker order.
     * 
     * @param symbol The trading pair
     * @param price The price at which the order would be placed
     * @param isBuy Whether this is a buy order (true) or sell order (false)
     * @return true if this would likely be a maker order, false if it would be a taker
     */
    boolean isMakerOrder(String symbol, double price, boolean isBuy);
} 