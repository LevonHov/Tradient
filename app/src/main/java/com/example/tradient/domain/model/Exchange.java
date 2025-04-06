package com.example.tradient.domain.model;

import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.interfaces.IExchangeService;
import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.interfaces.ArbitrageResult;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.infrastructure.ExchangeRegistry;

import android.util.Log;

/**
 * Represents a cryptocurrency exchange with its associated data and operations
 */
public class Exchange {
    private static final String TAG = "Exchange";
    
    private String name;
    private ExchangeService exchangeService;
    private static INotificationService notificationService;
    
    static {
        // Create a simple notification service using Android logging
        notificationService = new INotificationService() {
            @Override
            public void logInfo(String message) {
                Log.i(TAG, message);
            }
            
            @Override
            public void logError(String message, Throwable throwable) {
                Log.e(TAG, message, throwable);
            }
            
            @Override
            public void logWarning(String message) {
                Log.w(TAG, message);
            }
            
            @Override
            public void logDebug(String message) {
                Log.d(TAG, message);
            }

            @Override
            public void notify(String title, String message, String type) {

            }

            @Override
            public void notifyArbitrageError(Throwable throwable) {
                Log.e(TAG, "Arbitrage error in Exchange: " + throwable.getMessage(), throwable);
            }
            
            @Override
            public void notifyArbitrageOpportunity(ArbitrageResult result) {
                Log.i(TAG, "Exchange detected arbitrage opportunity: " + result.toString());
            }
        };
    }
    
    /**
     * Create a new Exchange instance
     * @param name The name of the exchange
     */
    public Exchange(String name) {
        this.name = name;
        this.exchangeService = ExchangeRegistry.getInstance(notificationService).getExchange(name);
    }
    
    /**
     * Get the underlying exchange service
     * @return The exchange service object
     */
    public ExchangeService getExchangeService() {
        return exchangeService;
    }
    
    /**
     * Get the order book for a trading pair
     * @param tradingPair The trading pair to get the order book for
     * @return The order book for the specified trading pair
     */
    public OrderBook getOrderBook(String tradingPair) {
        if (exchangeService == null) {
            return null;
        }
        return exchangeService.getOrderBook(tradingPair);
    }
    
    /**
     * Get the name of the exchange
     * @return The exchange name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the current fee structure for this exchange
     * @return The trading fee as a percentage
     */
    public double getTradingFee() {
        if (exchangeService == null) {
            return 0.3; // Default fee
        }
        // Use deprecated method for backward compatibility
        return exchangeService.getFees();
    }
    
    /**
     * Get the estimated transfer time from this exchange
     * @return The estimated transfer time in minutes
     */
    public int getEstimatedTransferTime() {
        if (exchangeService == null) {
            return 30; // Default time in minutes
        }
        // Base transfer time on exchange type - could be configurable/dynamic in a real implementation
        return getDefaultTransferTime();
    }
    
    /**
     * Get default transfer time based on exchange name
     */
    private int getDefaultTransferTime() {
        switch (name.toLowerCase()) {
            case "binance": return 20;
            case "coinbase": return 35;
            case "kraken": return 30;
            case "bybit": return 25;
            case "okx": return 25;
            default: return 30;
        }
    }
    
    /**
     * Check if this exchange supports a specific trading pair
     * @param tradingPair The trading pair to check
     * @return True if the trading pair is supported, false otherwise
     */
    public boolean supportsTradingPair(String tradingPair) {
        if (exchangeService == null) {
            return false;
        }
        
        // Check if this symbol exists in the trading pairs list
        for (com.example.tradient.data.model.TradingPair pair : exchangeService.getTradingPairs()) {
            if (pair.getSymbol().equals(tradingPair)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "Exchange{" + name + '}';
    }
} 