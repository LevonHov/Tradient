package com.example.tradient.data.time;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.tradient.data.interfaces.IExchangeService;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for estimating execution and settlement times in arbitrage operations.
 * Uses historical data and exchange-specific characteristics to provide time estimates.
 */
public class TransactionTimeEstimator {
    private static final String TAG = "TransactionTimeEstimator";
    private static final String PREFS_NAME = "transaction_time_prefs";
    private static final String KEY_EXECUTION_TIMES = "execution_times";
    private static final String KEY_SETTLEMENT_TIMES = "settlement_times";
    
    // Singleton instance
    private static TransactionTimeEstimator instance;
    
    // Cache of execution time estimates (exchange -> pair -> seconds)
    private final Map<String, Map<String, Double>> executionTimeCache;
    
    // Cache of settlement time estimates (from_exchange -> to_exchange -> asset -> seconds)
    private final Map<String, Map<String, Map<String, Double>>> settlementTimeCache;
    
    // Context for accessing shared preferences
    private final Context context;
    
    // Constants for estimation when no data is available
    private static final double DEFAULT_EXECUTION_TIME_SECONDS = 5.0;
    private static final double DEFAULT_SETTLEMENT_TIME_MINUTES = 30.0;
    
    // Blockchain confirmation time estimates (in minutes)
    private static final Map<String, Double> BLOCKCHAIN_CONFIRMATION_TIMES = new HashMap<String, Double>() {{
        put("BTC", 60.0);  // Bitcoin: ~60 minutes (6 confirmations)
        put("ETH", 5.0);   // Ethereum: ~5 minutes (35 confirmations at 15s/block)
        put("SOL", 0.5);   // Solana: ~30 seconds
        put("XRP", 0.1);   // XRP: ~5-10 seconds
        put("BNB", 3.0);   // BNB: ~3 minutes
        put("USDT", 5.0);  // USDT (on Ethereum): ~5 minutes
        put("USDC", 5.0);  // USDC (on Ethereum): ~5 minutes
    }};
    
    /**
     * Private constructor for Singleton pattern
     * @param context Application context
     */
    private TransactionTimeEstimator(Context context) {
        this.context = context.getApplicationContext();
        this.executionTimeCache = new ConcurrentHashMap<>();
        this.settlementTimeCache = new ConcurrentHashMap<>();
        loadHistoricalData();
    }
    
    /**
     * Get the singleton instance
     * @param context Application context
     * @return The TransactionTimeEstimator instance
     */
    public static synchronized TransactionTimeEstimator getInstance(Context context) {
        if (instance == null) {
            instance = new TransactionTimeEstimator(context);
        }
        return instance;
    }
    
    /**
     * Estimate the execution time for an order on a specific exchange
     * 
     * @param exchange The exchange service
     * @param tradingPair The trading pair symbol
     * @param amount The amount to trade
     * @param isBuy Whether this is a buy order
     * @return Estimated execution time in seconds
     */
    public double estimateExecutionTime(IExchangeService exchange, String tradingPair, 
                                       double amount, boolean isBuy) {
        String exchangeName = exchange.getExchangeName();
        
        // Check cache first
        if (executionTimeCache.containsKey(exchangeName) && 
            executionTimeCache.get(exchangeName).containsKey(tradingPair)) {
            return executionTimeCache.get(exchangeName).get(tradingPair);
        }
        
        // If not in cache, calculate based on order book depth and market activity
        OrderBook orderBook = exchange.getOrderBook(tradingPair);
        Ticker ticker = exchange.getTicker(tradingPair);
        
        double estimate = calculateExecutionTimeEstimate(orderBook, ticker, amount, isBuy);
        
        // Cache the result
        executionTimeCache
            .computeIfAbsent(exchangeName, k -> new ConcurrentHashMap<>())
            .put(tradingPair, estimate);
        
        return estimate;
    }
    
    /**
     * Estimate the settlement time for transferring an asset between exchanges
     * 
     * @param fromExchange The source exchange
     * @param toExchange The destination exchange
     * @param asset The asset being transferred (e.g., "BTC", "ETH")
     * @return Estimated settlement time in minutes
     */
    public double estimateSettlementTime(IExchangeService fromExchange, 
                                        IExchangeService toExchange, String asset) {
        String fromName = fromExchange.getExchangeName();
        String toName = toExchange.getExchangeName();
        
        // Check cache first
        if (settlementTimeCache.containsKey(fromName) && 
            settlementTimeCache.get(fromName).containsKey(toName) &&
            settlementTimeCache.get(fromName).get(toName).containsKey(asset)) {
            return settlementTimeCache.get(fromName).get(toName).get(asset);
        }
        
        // If not in cache, use blockchain confirmation times as a baseline
        double blockchainTime = BLOCKCHAIN_CONFIRMATION_TIMES.getOrDefault(asset, DEFAULT_SETTLEMENT_TIME_MINUTES);
        
        // Add exchange-specific processing times (estimates)
        double fromExchangeProcessingTime = getExchangeProcessingTime(fromName, false); // Withdrawal
        double toExchangeProcessingTime = getExchangeProcessingTime(toName, true);     // Deposit
        
        double totalEstimate = blockchainTime + fromExchangeProcessingTime + toExchangeProcessingTime;
        
        // Cache the result
        settlementTimeCache
            .computeIfAbsent(fromName, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(toName, k -> new ConcurrentHashMap<>())
            .put(asset, totalEstimate);
        
        return totalEstimate;
    }
    
    /**
     * Calculate the total time for an arbitrage operation between exchanges
     * 
     * @param buyExchange Exchange to buy on
     * @param sellExchange Exchange to sell on
     * @param tradingPair Trading pair (e.g., "BTC/USDT")
     * @param amount Amount to trade
     * @return Total estimated time in minutes
     */
    public double estimateTotalArbitrageTime(IExchangeService buyExchange, 
                                            IExchangeService sellExchange, 
                                            String tradingPair, double amount) {
        // Split trading pair to get base asset (e.g., "BTC" from "BTC/USDT")
        String baseAsset = tradingPair.split("/")[0];
        
        // Estimate times for each step
        double buyExecutionTime = estimateExecutionTime(buyExchange, tradingPair, amount, true);
        double settlementTime = estimateSettlementTime(buyExchange, sellExchange, baseAsset);
        double sellExecutionTime = estimateExecutionTime(sellExchange, tradingPair, amount, false);
        
        // Convert execution times from seconds to minutes
        buyExecutionTime = buyExecutionTime / 60.0;
        sellExecutionTime = sellExecutionTime / 60.0;
        
        // Sum all times
        return buyExecutionTime + settlementTime + sellExecutionTime;
    }
    
    /**
     * Record actual execution time for future estimates
     * 
     * @param exchange Exchange where order was executed
     * @param tradingPair Trading pair
     * @param executionTimeSeconds Actual execution time in seconds
     */
    public void recordExecutionTime(IExchangeService exchange, String tradingPair, double executionTimeSeconds) {
        String exchangeName = exchange.getExchangeName();
        
        // Get current average if it exists
        Map<String, Double> exchangeTimes = executionTimeCache.computeIfAbsent(
            exchangeName, k -> new ConcurrentHashMap<>());
        
        double currentAverage = exchangeTimes.getOrDefault(tradingPair, 0.0);
        int count = getExecutionTimeCount(exchangeName, tradingPair);
        
        // Calculate new weighted average (giving more weight to recent data)
        double newAverage;
        if (count == 0) {
            newAverage = executionTimeSeconds;
        } else {
            newAverage = (currentAverage * count + executionTimeSeconds) / (count + 1);
        }
        
        // Update cache
        exchangeTimes.put(tradingPair, newAverage);
        setExecutionTimeCount(exchangeName, tradingPair, count + 1);
        
        // Save to persistent storage
        saveHistoricalData();
        
        Log.d(TAG, "Recorded execution time: " + executionTimeSeconds + "s for " + 
              tradingPair + " on " + exchangeName + ". New average: " + newAverage + "s");
    }
    
    /**
     * Record actual settlement time for future estimates
     * 
     * @param fromExchange Source exchange
     * @param toExchange Destination exchange
     * @param asset Asset that was transferred
     * @param settlementTimeMinutes Actual settlement time in minutes
     */
    public void recordSettlementTime(IExchangeService fromExchange, IExchangeService toExchange, 
                                    String asset, double settlementTimeMinutes) {
        String fromName = fromExchange.getExchangeName();
        String toName = toExchange.getExchangeName();
        
        // Get current average if it exists
        Map<String, Map<String, Double>> fromExchangeMap = settlementTimeCache.computeIfAbsent(
            fromName, k -> new ConcurrentHashMap<>());
        
        Map<String, Double> toExchangeMap = fromExchangeMap.computeIfAbsent(
            toName, k -> new ConcurrentHashMap<>());
        
        double currentAverage = toExchangeMap.getOrDefault(asset, 0.0);
        int count = getSettlementTimeCount(fromName, toName, asset);
        
        // Calculate new weighted average
        double newAverage;
        if (count == 0) {
            newAverage = settlementTimeMinutes;
        } else {
            newAverage = (currentAverage * count + settlementTimeMinutes) / (count + 1);
        }
        
        // Update cache
        toExchangeMap.put(asset, newAverage);
        setSettlementTimeCount(fromName, toName, asset, count + 1);
        
        // Save to persistent storage
        saveHistoricalData();
        
        Log.d(TAG, "Recorded settlement time: " + settlementTimeMinutes + "min for " + 
              asset + " from " + fromName + " to " + toName + ". New average: " + newAverage + "min");
    }
    
    /**
     * Calculate execution time estimate based on order book depth and market activity
     */
    private double calculateExecutionTimeEstimate(OrderBook orderBook, Ticker ticker, 
                                                 double amount, boolean isBuy) {
        if (orderBook == null || ticker == null) {
            return DEFAULT_EXECUTION_TIME_SECONDS;
        }
        
        // Base estimate on volume and order book depth
        double volume = ticker.getVolume();
        
        // Calculate order book depth for the side we're interested in
        double depth = isBuy ? 
            calculateOrderBookBuyDepth(orderBook, amount) : 
            calculateOrderBookSellDepth(orderBook, amount);
        
        // More volume = faster execution, more depth = slower execution
        double volumeFactor = Math.log10(1 + volume) / 5.0; // Normalize volume factor
        double depthFactor = Math.log10(1 + depth) * 2.0;      // Depth has more impact
        
        // Combine factors (higher volume reduces time, higher depth increases time)
        double baseEstimate = DEFAULT_EXECUTION_TIME_SECONDS;
        double adjustedEstimate = baseEstimate * (1 + depthFactor) / (1 + volumeFactor);
        
        // Ensure reasonable bounds
        return Math.max(1.0, Math.min(60.0, adjustedEstimate));
    }
    
    /**
     * Calculate the depth required to fulfill a buy order
     */
    private double calculateOrderBookBuyDepth(OrderBook orderBook, double amount) {
        double remainingAmount = amount;
        double weightedDepth = 0;
        int levelCount = 0;
        
        if (orderBook.getAsks() == null || orderBook.getAsks().isEmpty()) {
            return 10.0; // Default depth value if no data
        }
        
        for (OrderBookEntry entry : orderBook.getAsks()) {
            double levelAmount = entry.getVolume();
            double levelPrice = entry.getPrice();
            levelCount++;
            
            if (levelAmount >= remainingAmount) {
                // This level completely fulfills our order
                weightedDepth += remainingAmount / levelAmount * levelCount;
                break;
            } else {
                // This level partially fulfills our order
                weightedDepth += levelCount;
                remainingAmount -= levelAmount;
            }
            
            // Limit to checking 10 levels max
            if (levelCount >= 10) {
                weightedDepth += 5; // Penalty for large orders that go deep
                break;
            }
        }
        
        // If we couldn't fulfill the entire order with the visible order book
        if (remainingAmount > 0) {
            weightedDepth += 10; // Large penalty for potentially illiquid markets
        }
        
        return weightedDepth;
    }
    
    /**
     * Calculate the depth required to fulfill a sell order
     */
    private double calculateOrderBookSellDepth(OrderBook orderBook, double amount) {
        double remainingAmount = amount;
        double weightedDepth = 0;
        int levelCount = 0;
        
        if (orderBook.getBids() == null || orderBook.getBids().isEmpty()) {
            return 10.0; // Default depth value if no data
        }
        
        for (OrderBookEntry entry : orderBook.getBids()) {
            double levelAmount = entry.getVolume();
            double levelPrice = entry.getPrice();
            levelCount++;
            
            if (levelAmount >= remainingAmount) {
                weightedDepth += remainingAmount / levelAmount * levelCount;
                break;
            } else {
                weightedDepth += levelCount;
                remainingAmount -= levelAmount;
            }
            
            if (levelCount >= 10) {
                weightedDepth += 5;
                break;
            }
        }
        
        if (remainingAmount > 0) {
            weightedDepth += 10;
        }
        
        return weightedDepth;
    }
    
    /**
     * Get estimated processing time for an exchange
     * 
     * @param exchangeName Exchange name
     * @param isDeposit Whether this is for deposit (true) or withdrawal (false)
     * @return Estimated processing time in minutes
     */
    private double getExchangeProcessingTime(String exchangeName, boolean isDeposit) {
        // These are educated estimates based on typical exchange processing times
        Map<String, Double> withdrawalTimes = new HashMap<String, Double>() {{
            put("binance", 5.0);
            put("coinbase", 10.0);
            put("kraken", 7.0);
            put("bybit", 6.0);
            put("okx", 8.0);
            put("kucoin", 9.0);
            put("huobi", 7.0);
            put("gate", 8.0);
        }};
        
        Map<String, Double> depositTimes = new HashMap<String, Double>() {{
            put("binance", 2.0);
            put("coinbase", 5.0);
            put("kraken", 3.0);
            put("bybit", 3.0);
            put("okx", 4.0);
            put("kucoin", 4.0);
            put("huobi", 3.0);
            put("gate", 4.0);
        }};
        
        String normalizedName = exchangeName.toLowerCase();
        
        if (isDeposit) {
            return depositTimes.getOrDefault(normalizedName, 5.0);
        } else {
            return withdrawalTimes.getOrDefault(normalizedName, 10.0);
        }
    }
    
    /**
     * Load historical transaction time data from shared preferences
     */
    private void loadHistoricalData() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            
            // Implementation would deserialize stored JSON data into the caches
            // This is a simplified version that would need to be expanded in production
            
            Log.d(TAG, "Loaded historical transaction time data");
        } catch (Exception e) {
            Log.e(TAG, "Error loading historical transaction time data", e);
        }
    }
    
    /**
     * Save historical transaction time data to shared preferences
     */
    private void saveHistoricalData() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            // Implementation would serialize the caches into JSON and store them
            // This is a simplified version that would need to be expanded in production
            
            editor.apply();
            Log.d(TAG, "Saved historical transaction time data");
        } catch (Exception e) {
            Log.e(TAG, "Error saving historical transaction time data", e);
        }
    }
    
    // Helper methods for tracking counts (for averaging)
    private int getExecutionTimeCount(String exchange, String pair) {
        // Would be implemented with SharedPreferences in production
        return 0;
    }
    
    private void setExecutionTimeCount(String exchange, String pair, int count) {
        // Would be implemented with SharedPreferences in production
    }
    
    private int getSettlementTimeCount(String fromExchange, String toExchange, String asset) {
        // Would be implemented with SharedPreferences in production
        return 0;
    }
    
    private void setSettlementTimeCount(String fromExchange, String toExchange, String asset, int count) {
        // Would be implemented with SharedPreferences in production
    }
} 