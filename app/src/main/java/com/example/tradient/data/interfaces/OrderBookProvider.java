package com.example.tradient.data.interfaces;

import com.example.tradient.domain.slippage.OrderBookSnapshot;

/**
 * Interface for accessing current order book data.
 */
public interface OrderBookProvider {
    
    /**
     * Gets the current order book for a specific symbol and exchange.
     * 
     * @param symbol The trading symbol
     * @param exchange The exchange name
     * @return The current order book snapshot
     */
    OrderBookSnapshot getOrderBook(String symbol, String exchange);
} 