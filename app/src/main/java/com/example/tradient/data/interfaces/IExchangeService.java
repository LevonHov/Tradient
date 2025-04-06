package com.example.tradient.data.interfaces;

import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;
import com.example.tradient.data.fee.Fee;
import com.example.tradient.data.fee.FeeTracker;

import java.util.List;

/**
 * Core interface for exchange services that combines all specialized interfaces.
 * This follows the Interface Segregation Principle by extending specific interfaces
 * rather than containing all methods directly.
 */
public interface IExchangeService extends IMarketDataProvider, IFeeProvider, IWebSocketProvider {
    /**
     * Gets the name of the exchange.
     *
     * @return The exchange name
     */
    String getExchangeName();
    
    /**
     * Get all available trading pairs from the exchange.
     * Returns cached data if available, otherwise fetches from API.
     *
     * @return List of trading pairs
     */
    List<TradingPair> getTradingPairs();
    
    /**
     * Fetch all trading pairs from the exchange API.
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
     * Get order book for a specific trading pair.
     * Returns cached data if available, otherwise fetches from API.
     *
     * @param symbol The trading pair symbol
     * @return Order book or null if not available
     */
    OrderBook getOrderBook(String symbol);
    
    /**
     * Initialize WebSocket connection for real-time data.
     *
     * @param symbols List of symbols to subscribe to
     * @return true if successful, false otherwise
     */
    boolean initializeWebSocket(List<String> symbols);
    
    /**
     * Close WebSocket connection.
     */
    void closeWebSocket();
    
    /**
     * Get the maker fee for this exchange.
     *
     * @return The maker fee
     */
    Fee getMakerFee();
    
    /**
     * Get the taker fee for this exchange.
     *
     * @return The taker fee
     */
    Fee getTakerFee();
    
    /**
     * Update fee tiers based on trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume
     */
    void updateFeesTiers(double thirtyDayVolume);
    
    /**
     * Calculate and track a fee for a transaction.
     *
     * @param tradingPair The trading pair
     * @param amount The transaction amount
     * @param isMaker Whether this is a maker order
     * @return The calculated fee amount
     */
    double calculateAndTrackFee(String tradingPair, double amount, boolean isMaker);
    
    /**
     * Get the fee tracker for this exchange.
     *
     * @return The fee tracker
     */
    FeeTracker getFeeTracker();
    
    /**
     * Check if WebSocket is connected.
     *
     * @return true if connected, false otherwise
     */
    boolean isWebSocketConnected();
    
    /**
     * Set the notification service for this exchange.
     *
     * @param notificationService The notification service to use
     */
    void setNotificationService(INotificationService notificationService);

    /**
     * Gets ticker data for a specific trading pair
     *
     * @param tradingPair The trading pair symbol
     * @return The ticker data or null if not available
     */
    Ticker getTicker(String tradingPair);

    /**
     * Gets fee percentage for a specific trading pair based on maker/taker status
     * 
     * @param tradingPair The trading pair symbol
     * @param isMaker Whether it's a maker order (true) or taker order (false)
     * @return The fee percentage as a decimal (e.g., 0.001 for 0.1%)
     */
    double getFeePercentage(String tradingPair, boolean isMaker);
} 