package com.example.tradient.domain.engine;

import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;

import java.util.List;

/**
 * Interface for exchange services in the domain model.
 * This provides a consistent interface for interacting with exchange services.
 */
public interface ExchangeService {

    /**
     * Get the exchange name
     * 
     * @return The name of the exchange
     */
    String getExchangeName();
    
    /**
     * Get a ticker for a specific trading pair
     * 
     * @param symbol The trading pair symbol
     * @return The ticker data
     */
    Ticker getTicker(String symbol);
    
    /**
     * Get an order book for a specific trading pair
     * 
     * @param symbol The trading pair symbol
     * @return The order book data
     */
    OrderBook getOrderBook(String symbol);
    
    /**
     * Get all available trading pairs
     * 
     * @return List of available trading pairs
     */
    List<TradingPair> getTradingPairs();
    
    /**
     * Fetch trading pairs from the exchange API
     * 
     * @return List of trading pairs
     */
    List<TradingPair> fetchTradingPairs();
    
    /**
     * Get the trading fee for a specific symbol
     * 
     * @param symbol The trading pair symbol
     * @param isMaker Whether the order is a maker order
     * @return The fee as a percentage
     */
    double getTradingFee(String symbol, boolean isMaker);
    
    /**
     * Initialize WebSocket connection
     * 
     * @param symbols List of symbols to subscribe to
     * @return true if WebSocket initialization was successful
     */
    boolean initializeWebSocket(List<String> symbols);
    
    /**
     * Check if WebSocket is connected
     * 
     * @return true if WebSocket is connected
     */
    boolean isWebSocketConnected();
    
    /**
     * Close WebSocket connection
     */
    void closeWebSocket();
} 