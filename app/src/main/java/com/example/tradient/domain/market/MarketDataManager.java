package com.example.tradient.domain.market;

import android.util.Log;

import com.example.tradient.data.model.Exchange;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.data.service.ExchangeServiceFactory;
import com.example.tradient.domain.risk.LiquidityService;
import com.example.tradient.domain.risk.VolatilityService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manager class that provides real-time market data including volatility and liquidity.
 * This follows the MVVM pattern by separating data access from presentation logic.
 */
public class MarketDataManager {
    private static final String TAG = "MarketDataManager";
    
    // Services
    private final VolatilityService volatilityService;
    private final LiquidityService liquidityService;
    private final ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutor;
    
    // Exchange services
    private final ExchangeService buyExchangeService;
    private final ExchangeService sellExchangeService;
    private final String symbol;
    
    // Data refresh interval in seconds
    private static final int REFRESH_INTERVAL_SECONDS = 10;
    
    // Tracking if we're actively collecting data
    private boolean isCollectingData = false;
    
    /**
     * Listener interface for receiving market data updates
     */
    public interface MarketDataListener {
        void onVolatilityUpdated(double volatility);
        void onLiquidityUpdated(double liquidity);
        void onOrderBooksUpdated(OrderBook buyOrderBook, OrderBook sellOrderBook);
        void onTickersUpdated(List<Ticker> buyTickers, List<Ticker> sellTickers);
        void onError(String errorMessage);
    }
    
    // List of listeners for updates
    private final List<MarketDataListener> listeners = new ArrayList<>();
    
    /**
     * Creates a new MarketDataManager with provided exchange services
     * 
     * @param buyExchangeService The exchange service for the buy side
     * @param sellExchangeService The exchange service for the sell side
     * @param symbol The trading pair symbol to track
     */
    public MarketDataManager(
            ExchangeService buyExchangeService, 
            ExchangeService sellExchangeService,
            String symbol) {
        this.buyExchangeService = buyExchangeService;
        this.sellExchangeService = sellExchangeService;
        this.symbol = symbol;
        
        volatilityService = new VolatilityService();
        liquidityService = new LiquidityService();
        executorService = Executors.newCachedThreadPool();
        
        Log.d(TAG, "Market data manager created for symbol: " + symbol);
    }
    
    /**
     * Add a listener for market data updates
     */
    public void addListener(MarketDataListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove a listener
     */
    public void removeListener(MarketDataListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Start collecting data at regular intervals
     */
    public void startDataCollection() {
        if (isCollectingData) {
            return; // Already collecting
        }
        
        if (buyExchangeService == null || sellExchangeService == null) {
            notifyError("Exchange services not initialized");
            return;
        }
        
        isCollectingData = true;
        
        // Get initial data
        fetchLatestMarketData();
        
        // Schedule regular updates
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(
                this::fetchLatestMarketData,
                REFRESH_INTERVAL_SECONDS,
                REFRESH_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
        
        Log.d(TAG, "Started collecting market data for " + symbol);
    }
    
    /**
     * Stop collecting data
     */
    public void stopDataCollection() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            scheduledExecutor = null;
        }
        
        isCollectingData = false;
        Log.d(TAG, "Stopped collecting market data");
    }
    
    /**
     * Fetch the latest market data asynchronously
     */
    public void fetchLatestMarketData() {
        if (buyExchangeService == null || sellExchangeService == null) {
            notifyError("Exchange services not initialized");
            return;
        }
        
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Fetching latest market data for " + symbol);
                
                // Fetch order books
                OrderBook buyOrderBook = buyExchangeService.getOrderBook(symbol);
                OrderBook sellOrderBook = sellExchangeService.getOrderBook(symbol);
                
                // Notify about order books
                if (buyOrderBook != null && sellOrderBook != null) {
                    notifyOrderBooksUpdated(buyOrderBook, sellOrderBook);
                    
                    // Calculate liquidity
                    double liquidity = liquidityService.calculateLiquidity(
                            buyOrderBook, sellOrderBook, symbol);
                    notifyLiquidityUpdated(liquidity);
                }
                
                // Fetch historical tickers
                List<Ticker> buyTickers = buyExchangeService.getHistoricalTickers(symbol, 24);
                List<Ticker> sellTickers = sellExchangeService.getHistoricalTickers(symbol, 24);
                
                // Notify about tickers
                if (buyTickers != null && !buyTickers.isEmpty() && 
                    sellTickers != null && !sellTickers.isEmpty()) {
                    notifyTickersUpdated(buyTickers, sellTickers);
                    
                    // Calculate volatility
                    double volatility = volatilityService.calculateVolatility(
                            buyTickers, sellTickers, symbol);
                    notifyVolatilityUpdated(volatility);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching market data: " + e.getMessage());
                notifyError("Error fetching market data: " + e.getMessage());
            }
        });
    }
    
    /**
     * Notify all listeners about updated volatility
     */
    private void notifyVolatilityUpdated(double volatility) {
        for (MarketDataListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onVolatilityUpdated(volatility);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener about volatility: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notify all listeners about updated liquidity
     */
    private void notifyLiquidityUpdated(double liquidity) {
        for (MarketDataListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onLiquidityUpdated(liquidity);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener about liquidity: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notify all listeners about updated order books
     */
    private void notifyOrderBooksUpdated(OrderBook buyOrderBook, OrderBook sellOrderBook) {
        for (MarketDataListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onOrderBooksUpdated(buyOrderBook, sellOrderBook);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener about order books: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notify all listeners about updated tickers
     */
    private void notifyTickersUpdated(List<Ticker> buyTickers, List<Ticker> sellTickers) {
        for (MarketDataListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onTickersUpdated(buyTickers, sellTickers);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener about tickers: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notify all listeners about an error
     */
    private void notifyError(String errorMessage) {
        for (MarketDataListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onError(errorMessage);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener about error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Shut down the executor service
     */
    public void shutdown() {
        stopDataCollection();
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        Log.d(TAG, "Market data manager shutdown complete");
    }
    
    /**
     * Clear all caches in the services
     */
    public void clearCaches() {
        volatilityService.clearCache();
        liquidityService.clearCache();
    }
} 