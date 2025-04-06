package com.example.tradient.repository;

import android.content.Context;
import android.util.Log;

import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.data.interfaces.ArbitrageResult;
import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.model.ExchangeConfiguration;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;
import com.example.tradient.data.service.BinanceExchangeService;
import com.example.tradient.data.service.BybitV5ExchangeService;
import com.example.tradient.data.service.CoinbaseExchangeService;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.data.service.KrakenExchangeService;
import com.example.tradient.data.service.OkxExchangeService;
import com.example.tradient.infrastructure.ExchangeRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository responsible for managing exchange services and data access
 */
public class ExchangeRepository {
    private static final String TAG = "ExchangeRepository";
    
    private final Context context;
    private final ExecutorService executorService;
    private final Map<String, ExchangeService> exchangeServices = new ConcurrentHashMap<>();
    private final Map<String, List<TradingPair>> tradingPairsCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Ticker>> tickerCache = new ConcurrentHashMap<>();
    
    private INotificationService notificationService;
    private ExchangeRegistry exchangeRegistry;
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();
    
    /**
     * Simple implementation of INotificationService that logs messages
     */
    private class LoggingNotificationService implements INotificationService {
        @Override
        public void logInfo(String message) {
            Log.i(TAG, message);
        }
        
        @Override
        public void logWarning(String message) {
            Log.w(TAG, message);
        }
        
        @Override
        public void logError(String message, Throwable error) {
            Log.e(TAG, message, error);
        }
        
        @Override
        public void logDebug(String message) {
            Log.d(TAG, message);
        }
        
        @Override
        public void notify(String title, String message, String type) {
            Log.i(TAG, "Notification (" + type + "): " + title + " - " + message);
        }
        
        @Override
        public void notifyArbitrageOpportunity(ArbitrageResult opportunity) {
            Log.i(TAG, "Arbitrage opportunity found: " + opportunity);
        }
        
        @Override
        public void notifyArbitrageError(Throwable error) {
            Log.e(TAG, "Arbitrage error", error);
        }
    }
    
    /**
     * Constructs a new ExchangeRepository
     * @param context Application context for accessing system resources
     */
    public ExchangeRepository(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        initializeRegistry();
    }
    
    /**
     * Initialize the exchange registry
     */
    private void initializeRegistry() {
        try {
            // Create a notification service adapter for logging
            INotificationService notificationService = new LoggingNotificationService();
            this.exchangeRegistry = ExchangeRegistry.getInstance(notificationService);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing exchange registry", e);
        }
    }
    
    public void setNotificationService(INotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Initializes and returns all enabled exchanges
     */
    public CompletableFuture<List<ExchangeService>> getEnabledExchanges() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ExchangeService> exchanges = new ArrayList<>();
                ExchangeConfiguration config = ConfigurationFactory.getExchangeConfig();
                
                // Initialize Binance if enabled
                if (config.isExchangeEnabled("binance")) {
                    BinanceExchangeService binance = new BinanceExchangeService(config.getExchangeFee("binance"));
                    binance.setLogoResource("binance_logo");
                    if (notificationService != null) {
                        binance.setNotificationService(notificationService);
                    }
                    exchangeServices.put("binance", binance);
                    exchanges.add(binance);
                    Log.i(TAG, "Binance exchange service initialized");
                }
                
                // Initialize Coinbase if enabled
                if (config.isExchangeEnabled("coinbase")) {
                    CoinbaseExchangeService coinbase = new CoinbaseExchangeService(config.getExchangeFee("coinbase"));
                    coinbase.setLogoResource("coinbase_logo");
                    if (notificationService != null) {
                        coinbase.setNotificationService(notificationService);
                    }
                    exchangeServices.put("coinbase", coinbase);
                    exchanges.add(coinbase);
                    Log.i(TAG, "Coinbase exchange service initialized");
                }
                
                // Initialize Kraken if enabled
                if (config.isExchangeEnabled("kraken")) {
                    KrakenExchangeService kraken = new KrakenExchangeService(config.getExchangeFee("kraken"));
                    kraken.setLogoResource("kraken_logo");
                    if (notificationService != null) {
                        kraken.setNotificationService(notificationService);
                    }
                    exchangeServices.put("kraken", kraken);
                    exchanges.add(kraken);
                    Log.i(TAG, "Kraken exchange service initialized");
                }
                
                // Initialize Bybit if enabled
                if (config.isExchangeEnabled("bybit")) {
                    BybitV5ExchangeService bybit = new BybitV5ExchangeService(config.getExchangeFee("bybit"));
                    bybit.setLogoResource("bybit_logo");
                    if (notificationService != null) {
                        bybit.setNotificationService(notificationService);
                    }
                    exchangeServices.put("bybit", bybit);
                    exchanges.add(bybit);
                    Log.i(TAG, "Bybit exchange service initialized");
                }
                
                // Initialize OKX if enabled
                if (config.isExchangeEnabled("okx")) {
                    OkxExchangeService okx = new OkxExchangeService(config.getExchangeFee("okx"));
                    okx.setLogoResource("okx_logo");
                    if (notificationService != null) {
                        okx.setNotificationService(notificationService);
                    }
                    exchangeServices.put("okx", okx);
                    exchanges.add(okx);
                    Log.i(TAG, "OKX exchange service initialized");
                }
                
                Log.i(TAG, "Initialized " + exchanges.size() + " exchange services");
                return exchanges;
                
            } catch (Exception e) {
                Log.e(TAG, "Error initializing exchanges", e);
                throw new RuntimeException("Error initializing exchanges", e);
            }
        }, executorService);
    }
    
    /**
     * Gets trading pairs for a specific exchange
     */
    public CompletableFuture<List<TradingPair>> getTradingPairs(ExchangeService exchange) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String exchangeName = exchange.getExchangeName();
                
                // Check cache first
                List<TradingPair> cachedPairs = tradingPairsCache.get(exchangeName);
                if (cachedPairs != null && !cachedPairs.isEmpty()) {
                    Log.i(TAG, "Using cached trading pairs for " + exchangeName);
                    return cachedPairs;
                }
                
                // Fetch from exchange
                Log.i(TAG, "Fetching trading pairs from " + exchangeName);
                List<TradingPair> pairs = exchange.fetchTradingPairs();
                
                // Cache the result
                if (pairs != null && !pairs.isEmpty()) {
                    tradingPairsCache.put(exchangeName, pairs);
                    Log.i(TAG, "Cached " + pairs.size() + " trading pairs for " + exchangeName);
                }
                
                return pairs;
            } catch (Exception e) {
                Log.e(TAG, "Error fetching trading pairs for " + exchange.getExchangeName(), e);
                throw new RuntimeException("Error fetching trading pairs", e);
            }
        }, executorService);
    }
    
    /**
     * Gets ticker for a specific symbol on an exchange
     */
    public CompletableFuture<Ticker> getTicker(ExchangeService exchange, String symbol) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Cache key for this ticker
                String cacheKey = exchange.getExchangeName() + ":" + symbol + ":ticker";
                
                // Check cache first
                if (cache.containsKey(cacheKey)) {
                    Ticker cachedTicker = (Ticker) cache.get(cacheKey);
                    
                    // If cache entry is less than 5 seconds old, use it
                    if (System.currentTimeMillis() - cachedTicker.getTimestamp().getTime() < 5000) {
                        return cachedTicker;
                    }
                }
                
                // Get fresh ticker data from exchange
                Ticker ticker = exchange.getTicker(symbol);
                
                // Update cache
                if (ticker != null) {
                    cache.put(cacheKey, ticker);
                }
                
                return ticker;
            } catch (Exception e) {
                Log.e(TAG, "Error getting ticker for " + symbol + " on " + 
                        exchange.getExchangeName(), e);
                return null;
            }
        }, executorService);
    }
    
    /**
     * Update ticker data from WebSocket
     */
    public void updateTickerFromWebSocket(String exchangeName, String symbol, double bidPrice, double askPrice) {
        try {
            // Validate data
            if (bidPrice <= 0 || askPrice <= 0 || symbol == null || exchangeName == null) {
                return;
            }
            
            // Get the exchange cache map
            Map<String, Ticker> exchangeTickers = tickerCache.computeIfAbsent(
                exchangeName, k -> new ConcurrentHashMap<>());
            
            // Create or update ticker
            Ticker ticker = exchangeTickers.computeIfAbsent(symbol, k -> new Ticker());
            ticker.setBidPrice(bidPrice);
            ticker.setAskPrice(askPrice);
            ticker.setTimestamp(new java.util.Date());
            
            Log.d(TAG, "Updated WebSocket ticker for " + symbol + " on " + exchangeName);
        } catch (Exception e) {
            Log.e(TAG, "Error updating WebSocket ticker", e);
        }
    }
    
    /**
     * Initialize WebSockets for real-time data
     */
    public CompletableFuture<Void> initializeWebSockets(ExchangeService exchange, List<String> symbols) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Implementation would depend on the exchange API 
                // This is a stub that would be implemented based on the exchange capabilities
                Log.i(TAG, "Would initialize WebSockets for " + exchange.getExchangeName() + 
                     " with " + symbols.size() + " symbols");
                
                // In an actual implementation, you'd call exchange-specific methods
                // exchange.connectWebSocket(symbols);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing WebSockets for " + exchange.getExchangeName(), e);
            }
        }, executorService);
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        executorService.shutdownNow();
        
        // Close WebSocket connections
        for (ExchangeService exchange : exchangeServices.values()) {
            try {
                // Implement proper WebSocket closing logic here
                Log.i(TAG, "Closing connection to " + exchange.getExchangeName());
            } catch (Exception e) {
                Log.e(TAG, "Error closing connection for " + exchange.getExchangeName(), e);
            }
        }
    }
} 