package com.example.tradient.domain.market;

import android.util.Log;

import com.example.tradient.data.model.Exchange;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.data.service.ExchangeServiceFactory;
import com.example.tradient.domain.risk.LiquidityService;
import com.example.tradient.domain.risk.VolatilityService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    
    // Exchange services
    private ExchangeService buyExchangeService;
    private ExchangeService sellExchangeService;
    
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
    
    private MarketDataListener listener;
    
    /**
     * Creates a new MarketDataManager
     */
    public MarketDataManager() {
        volatilityService = new VolatilityService();
        liquidityService = new LiquidityService();
        executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * Set the listener for market data updates
     */
    public void setListener(MarketDataListener listener) {
        this.listener = listener;
    }
    
    /**
     * Initialize exchange services for the given exchanges
     */
    public void initializeExchanges(String buyExchangeName, String sellExchangeName) {
        try {
            Exchange buyExchange = Exchange.valueOf(buyExchangeName.toUpperCase());
            Exchange sellExchange = Exchange.valueOf(sellExchangeName.toUpperCase());
            
            buyExchangeService = ExchangeServiceFactory.getExchangeService(buyExchange);
            sellExchangeService = ExchangeServiceFactory.getExchangeService(sellExchange);
            
            Log.d(TAG, "Exchange services initialized for " + buyExchange + " and " + sellExchange);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error initializing exchange services: " + e.getMessage());
            if (listener != null) {
                listener.onError("Error initializing exchange services: " + e.getMessage());
            }
        }
    }
    
    /**
     * Fetch the latest market data asynchronously
     */
    public void fetchLatestMarketData(String symbol) {
        if (buyExchangeService == null || sellExchangeService == null) {
            if (listener != null) {
                listener.onError("Exchange services not initialized");
            }
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Fetch order books
                OrderBook buyOrderBook = buyExchangeService.getOrderBook(symbol);
                OrderBook sellOrderBook = sellExchangeService.getOrderBook(symbol);
                
                // Fetch historical tickers
                List<Ticker> buyTickers = buyExchangeService.getHistoricalTickers(symbol, 24);
                List<Ticker> sellTickers = sellExchangeService.getHistoricalTickers(symbol, 24);
                
                // Calculate volatility and liquidity
                double volatility = volatilityService.calculateVolatility(buyTickers, sellTickers, symbol);
                double liquidity = liquidityService.calculateLiquidity(buyOrderBook, sellOrderBook, symbol);
                
                // Notify listener
                if (listener != null) {
                    listener.onVolatilityUpdated(volatility);
                    listener.onLiquidityUpdated(liquidity);
                    listener.onOrderBooksUpdated(buyOrderBook, sellOrderBook);
                    listener.onTickersUpdated(buyTickers, sellTickers);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching market data: " + e.getMessage());
                if (listener != null) {
                    listener.onError("Error fetching market data: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Shut down the executor service
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * Clear all caches in the services
     */
    public void clearCaches() {
        volatilityService.clearCache();
        liquidityService.clearCache();
    }
} 