package com.example.tradient.data.interfaces;

import java.util.List;

/**
 * Interface for WebSocket data streaming functionality.
 * Separated from core exchange services to follow Interface Segregation Principle.
 */
public interface IWebSocketProvider {
    /**
     * Initialize WebSocket connection for real-time data.
     *
     * @param symbols List of symbols to subscribe to
     * @return true if successfully connected, false otherwise
     */
    boolean initializeWebSocket(List<String> symbols);
    
    /**
     * Close WebSocket connection.
     */
    void closeWebSocket();
    
    /**
     * Check if WebSocket is connected.
     *
     * @return true if connected, false otherwise
     */
    boolean isWebSocketConnected();
    
    /**
     * Add a listener for WebSocket updates.
     *
     * @param listener The WebSocket update listener
     */
    void addWebSocketListener(IWebSocketUpdateListener listener);
    
    /**
     * Remove a WebSocket update listener.
     *
     * @param listener The WebSocket update listener to remove
     */
    void removeWebSocketListener(IWebSocketUpdateListener listener);
} 