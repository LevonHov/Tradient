package com.example.tradient.data.interfaces;

import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;

/**
 * Interface for listeners of WebSocket updates.
 * Follows the Observer pattern to decouple WebSocket data reception from processing.
 */
public interface IWebSocketUpdateListener {
    /**
     * Called when a ticker update is received.
     *
     * @param symbol The trading pair symbol
     * @param ticker The updated ticker data
     */
    void onTickerUpdate(String symbol, Ticker ticker);
    
    /**
     * Called when an order book update is received.
     *
     * @param symbol The trading pair symbol
     * @param orderBook The updated order book
     */
    void onOrderBookUpdate(String symbol, OrderBook orderBook);
    
    /**
     * Called when a WebSocket connection is established.
     *
     * @param provider The WebSocket provider that connected
     */
    void onWebSocketConnected(IWebSocketProvider provider);
    
    /**
     * Called when a WebSocket connection is closed.
     *
     * @param provider The WebSocket provider that disconnected
     * @param code The disconnect code
     * @param reason The reason for disconnection
     */
    void onWebSocketDisconnected(IWebSocketProvider provider, int code, String reason);
    
    /**
     * Called when a WebSocket error occurs.
     *
     * @param provider The WebSocket provider that encountered an error
     * @param error The error that occurred
     */
    void onWebSocketError(IWebSocketProvider provider, Throwable error);
} 