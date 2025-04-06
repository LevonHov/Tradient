package com.example.tradient.domain.risk;

import android.util.Log;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Central service for managing slippage calculations in cryptocurrency trading.
 * 
 * This service coordinates the advanced slippage calculation system, providing
 * a unified interface for estimating price impact, tracking executions, and
 * feeding real results back into the prediction model.
 * 
 * Key functionality:
 * - Calculates expected slippage with enhanced accuracy
 * - Manages volatility data and integrates it into slippage estimates
 * - Maintains a feedback loop of prediction vs. actual results
 * - Caches recent calculations for performance optimization
 * - Tracks pending trades to correlate predictions with outcomes
 * - Performs periodic cleanup of stale data
 * 
 * The service acts as the central coordination point for all slippage-related
 * functionality in the arbitrage system.
 * 
 * Compatible with Android platform.
 */
public class SlippageManagerService {

    private static final String TAG = "SlippageManager";

    private final AdvancedSlippageCalculator slippageCalculator;
    private final VolatilityCalculator volatilityCalculator;
    
    // Cache recent slippage estimates for quick access
    private final Map<String, SlippageEstimate> slippageEstimateCache = new ConcurrentHashMap<>();
    
    // Track pending trades for feedback loop
    private final Map<String, PendingTrade> pendingTrades = new ConcurrentHashMap<>();
    
    // Cleanup scheduler
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Cache for slippage analytics by symbol
    private final Map<String, SlippageAnalyticsBuilder> slippageAnalyticsMap;
    
    // Default slippage parameters
    private final double DEFAULT_LIQUIDITY_FACTOR = 0.5;
    private final double DEFAULT_SPREAD_FACTOR = 1.5;
    private final double DEFAULT_SLIPPAGE_BASE = 0.001; // 0.1% base slippage
    
    /**
     * Creates a new slippage manager service with default calculator instances.
     */
    public SlippageManagerService() {
        this.slippageCalculator = new AdvancedSlippageCalculator();
        this.volatilityCalculator = new VolatilityCalculator();
        
        this.slippageAnalyticsMap = new ConcurrentHashMap<>();
        
        // Schedule periodic cleanup of stale data
        scheduler.scheduleAtFixedRate(this::cleanupStaleData, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Creates a new slippage manager service with custom calculator instances.
     * 
     * @param slippageCalculator The advanced slippage calculator to use
     * @param volatilityCalculator The volatility calculator to use
     */
    public SlippageManagerService(AdvancedSlippageCalculator slippageCalculator, VolatilityCalculator volatilityCalculator) {
        this.slippageCalculator = slippageCalculator;
        this.volatilityCalculator = volatilityCalculator;
        
        this.slippageAnalyticsMap = new ConcurrentHashMap<>();
        
        // Schedule periodic cleanup of stale data
        scheduler.scheduleAtFixedRate(this::cleanupStaleData, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Calculates expected slippage for a given trade size on a specific exchange.
     *
     * @param ticker The ticker data for the exchange
     * @param tradeAmount The amount of asset to trade
     * @param isBuy Whether this is a buy (true) or sell (false) operation
     * @param symbol The trading symbol
     * @return Expected slippage as a decimal (e.g., 0.002 for 0.2%)
     */
    public double calculateSlippage(Ticker ticker, double tradeAmount, boolean isBuy, String symbol) {
        if (ticker == null) {
            return DEFAULT_SLIPPAGE_BASE;
        }

        try {
            // Get or create slippage analytics for this symbol
            SlippageAnalyticsBuilder analytics = getSlippageAnalytics(symbol);
            
            // Update the analytics with the latest ticker data
            analytics.updateMarketData(ticker);
            
            // Use the analytics to calculate slippage
            return analytics.calculateSlippage(tradeAmount, isBuy);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating advanced slippage: " + e.getMessage());
            // Fall back to basic calculation
            return calculateBasicSlippage(ticker, tradeAmount, isBuy);
        }
    }
    
    /**
     * Basic slippage calculation when advanced analytics are unavailable.
     * 
     * @param ticker The ticker data
     * @param tradeAmount The amount to trade
     * @param isBuy Whether this is a buy or sell operation
     * @return Estimated slippage as a decimal
     */
    private double calculateBasicSlippage(Ticker ticker, double tradeAmount, boolean isBuy) {
        if (ticker == null || ticker.getLastPrice() <= 0) {
            return DEFAULT_SLIPPAGE_BASE;
        }
        
        // Calculate the trade value in base currency
        double tradeValue = tradeAmount * ticker.getLastPrice();
        
        // Calculate spread as a percentage
        double askPrice = ticker.getAskPrice();
        double bidPrice = ticker.getBidPrice();
        
        if (askPrice <= 0 || bidPrice <= 0) {
            return DEFAULT_SLIPPAGE_BASE;
        }
        
        double spread = (askPrice - bidPrice) / ((askPrice + bidPrice) / 2);
        
        // Calculate volume-based adjustment
        double volume = ticker.getVolume();
        double volumeRatio = tradeValue / (volume > 0 ? volume : 1000);
        double volumeAdjustment = Math.min(3.0, 1.0 + (volumeRatio * 10));
        
        // Calculate basic slippage
        double baseSlippage = (spread / 2) * DEFAULT_SPREAD_FACTOR;
        double slippage = baseSlippage * volumeAdjustment;
        
        // Add side-specific adjustment
        if (isBuy) {
            // Buys may have more slippage in bullish markets (less liquidity on ask side)
            slippage *= 1.1;
        }
        
        // Ensure slippage is within reasonable bounds
        return Math.max(0.0001, Math.min(slippage, 0.02));
    }
    
    /**
     * Gets or creates a SlippageAnalyticsBuilder for a specific symbol.
     * 
     * @param symbol The trading symbol
     * @return A SlippageAnalyticsBuilder for the symbol
     */
    private SlippageAnalyticsBuilder getSlippageAnalytics(String symbol) {
        // Create if it doesn't exist
        if (!slippageAnalyticsMap.containsKey(symbol)) {
            slippageAnalyticsMap.put(symbol, new SlippageAnalyticsBuilder(symbol));
        }
        return slippageAnalyticsMap.get(symbol);
    }
    
    /**
     * Records that a trade is about to be executed, to track for feedback loop.
     *
     * @param tradeId Unique identifier for the trade
     * @param symbol The trading symbol
     * @param tradeSize The size of the trade
     * @param isBuy Whether this is a buy (true) or sell (false) operation
     * @param predictedSlippage The predicted slippage
     */
    public void recordPendingTrade(String tradeId, String symbol, double tradeSize, 
                                 boolean isBuy, double predictedSlippage) {
        PendingTrade trade = new PendingTrade(symbol, tradeSize, isBuy, predictedSlippage, Instant.now());
        pendingTrades.put(tradeId, trade);
    }
    
    /**
     * Records the actual execution results of a trade to improve future slippage predictions.
     *
     * @param tradeId Unique identifier for the trade
     * @param actualExecutionPrice The actual execution price
     * @param expectedPrice The expected execution price before slippage
     */
    public void recordTradeExecution(String tradeId, double actualExecutionPrice, double expectedPrice) {
        PendingTrade trade = pendingTrades.remove(tradeId);
        if (trade == null) {
            return; // Unknown trade, can't record feedback
        }
        
        // Calculate actual slippage
        double actualSlippage;
        if (trade.isBuy()) {
            // For buys, slippage means paying more than expected
            actualSlippage = (actualExecutionPrice - expectedPrice) / expectedPrice;
        } else {
            // For sells, slippage means receiving less than expected
            actualSlippage = (expectedPrice - actualExecutionPrice) / expectedPrice;
        }
        
        // Only record non-negative slippage values (negative would mean price improvement)
        actualSlippage = Math.max(0, actualSlippage);
        
        // Record for feedback loop
        slippageCalculator.recordActualSlippage(trade.getSymbol(), trade.getPredictedSlippage(), actualSlippage);
    }
    
    /**
     * Gets the volatility calculator for direct use if needed.
     */
    public VolatilityCalculator getVolatilityCalculator() {
        return volatilityCalculator;
    }
    
    /**
     * Shuts down the service and its resources.
     */
    public void shutdown() {
        scheduler.shutdown();
    }
    
    /**
     * Cleans up stale data periodically.
     */
    private void cleanupStaleData() {
        Instant oneHourAgo = Instant.now().minus(1, TimeUnit.HOURS.toChronoUnit());
        
        // Clean up stale estimates
        slippageEstimateCache.entrySet().removeIf(entry -> 
            entry.getValue().getTimestamp().isBefore(oneHourAgo));
        
        // Clean up stale pending trades
        pendingTrades.entrySet().removeIf(entry -> 
            entry.getValue().getTimestamp().isBefore(oneHourAgo));
    }
    
    /**
     * Generates a key for the slippage estimate cache.
     */
    private String getEstimateKey(String symbol, double tradeSize, boolean isBuy) {
        return symbol + ":" + tradeSize + ":" + (isBuy ? "buy" : "sell");
    }
    
    /**
     * Helper class to store slippage estimates.
     */
    private static class SlippageEstimate {
        private final double slippage;
        private final double tradeSize;
        private final boolean isBuy;
        private final Instant timestamp;
        
        public SlippageEstimate(double slippage, double tradeSize, boolean isBuy, Instant timestamp) {
            this.slippage = slippage;
            this.tradeSize = tradeSize;
            this.isBuy = isBuy;
            this.timestamp = timestamp;
        }
        
        public double getSlippage() {
            return slippage;
        }
        
        public double getTradeSize() {
            return tradeSize;
        }
        
        public boolean isBuy() {
            return isBuy;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * Helper class to track pending trades.
     */
    private static class PendingTrade {
        private final String symbol;
        private final double tradeSize;
        private final boolean isBuy;
        private final double predictedSlippage;
        private final Instant timestamp;
        
        public PendingTrade(String symbol, double tradeSize, boolean isBuy, 
                          double predictedSlippage, Instant timestamp) {
            this.symbol = symbol;
            this.tradeSize = tradeSize;
            this.isBuy = isBuy;
            this.predictedSlippage = predictedSlippage;
            this.timestamp = timestamp;
        }
        
        public String getSymbol() {
            return symbol;
        }
        
        public double getTradeSize() {
            return tradeSize;
        }
        
        public boolean isBuy() {
            return isBuy;
        }
        
        public double getPredictedSlippage() {
            return predictedSlippage;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * Inner class that builds and maintains slippage analytics for a specific symbol.
     */
    private class SlippageAnalyticsBuilder {
        private final String symbol;
        private Ticker latestTicker;
        private long lastUpdateTime;
        private double historicalVolatility = 0.02; // Initial estimate
        
        // Parameter tuning factors
        private double spreadFactor = 1.5;
        private double volumeFactor = 0.7;
        private double volatilityFactor = 1.2;
        
        /**
         * Constructor initializing the builder for a specific symbol.
         * 
         * @param symbol The trading symbol
         */
        public SlippageAnalyticsBuilder(String symbol) {
            this.symbol = symbol;
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        /**
         * Updates market data with the latest ticker.
         * 
         * @param ticker The latest ticker data
         */
        public void updateMarketData(Ticker ticker) {
            if (ticker != null) {
                // Calculate volatility update if we have a previous ticker
                if (latestTicker != null) {
                    long timeGap = System.currentTimeMillis() - lastUpdateTime;
                    if (timeGap > 0) {
                        // Estimate short-term volatility
                        double priceDiff = Math.abs(ticker.getLastPrice() - latestTicker.getLastPrice()) / latestTicker.getLastPrice();
                        double annualizedVol = priceDiff * Math.sqrt(365 * 24 * 60 * 60 * 1000.0 / timeGap);
                        
                        // Exponential moving average of volatility
                        double alpha = 0.1; // Weight for new observation
                        historicalVolatility = (alpha * annualizedVol) + ((1 - alpha) * historicalVolatility);
                    }
                }
                
                // Update ticker and timestamp
                latestTicker = ticker;
                lastUpdateTime = System.currentTimeMillis();
                
                // Adjust factors based on market conditions
                adjustFactors();
            }
        }
        
        /**
         * Adjusts calculation factors based on current market conditions.
         */
        private void adjustFactors() {
            if (latestTicker != null) {
                // Adjust spread factor based on bid-ask spread
                double relativeBidAskSpread = (latestTicker.getAskPrice() - latestTicker.getBidPrice()) / latestTicker.getLastPrice();
                
                // Wider spreads require higher spread factor
                if (relativeBidAskSpread > 0.005) {
                    spreadFactor = 2.0;
                } else if (relativeBidAskSpread > 0.002) {
                    spreadFactor = 1.7;
                } else {
                    spreadFactor = 1.5;
                }
                
                // Adjust volume factor based on 24h volume
                if (latestTicker.getVolume() > 50000) {
                    volumeFactor = 0.5; // Less slippage for high volume
                } else if (latestTicker.getVolume() > 10000) {
                    volumeFactor = 0.7;
                } else {
                    volumeFactor = 1.0; // More slippage for low volume
                }
                
                // Adjust volatility factor based on historical volatility
                if (historicalVolatility > 0.05) { // Very volatile
                    volatilityFactor = 1.5;
                } else if (historicalVolatility > 0.02) { // Moderately volatile
                    volatilityFactor = 1.2;
                } else {
                    volatilityFactor = 1.0; // Less volatile
                }
            }
        }
        
        /**
         * Calculates slippage for a given trade using the latest analytics.
         * 
         * @param tradeAmount The amount to trade
         * @param isBuy Whether this is a buy or sell
         * @return Calculated slippage as a decimal
         */
        public double calculateSlippage(double tradeAmount, boolean isBuy) {
            if (latestTicker == null || latestTicker.getLastPrice() <= 0) {
                return DEFAULT_SLIPPAGE_BASE;
            }
            
            // Calculate trade value
            double tradeValue = tradeAmount * latestTicker.getLastPrice();
            double dayVolume = latestTicker.getVolume() * latestTicker.getLastPrice();
            
            // Volume-based component
            double volumeRatio = tradeValue / (dayVolume > 0 ? dayVolume : 1000);
            double volumeComponent = Math.min(0.01, volumeRatio * volumeFactor);
            
            // Spread-based component
            double spread = (latestTicker.getAskPrice() - latestTicker.getBidPrice()) / latestTicker.getLastPrice();
            double spreadComponent = (spread / 2) * spreadFactor;
            
            // Volatility component
            double volatilityComponent = historicalVolatility * volatilityFactor * volumeRatio;
            
            // Side-specific adjustment
            double sideMultiplier = isBuy ? 1.1 : 1.0;
            
            // Combine components
            double slippage = (spreadComponent + volumeComponent + volatilityComponent) * sideMultiplier;
            
            // Ensure slippage is within reasonable bounds
            return Math.max(0.0001, Math.min(slippage, 0.05));
        }
    }
    
    /**
     * Calculates expected slippage for a trade with enhanced accuracy and dynamic calibration.
     * This method is for backward compatibility with code that passes an OrderBook.
     * 
     * @param ticker The market ticker data
     * @param orderBook The order book data (will be ignored in this implementation)
     * @param tradeSize The size of the trade to execute
     * @param isBuy Whether this is a buy (true) or sell (false) operation
     * @param symbol The trading symbol
     * @return Expected slippage as a decimal (e.g., 0.002 for 0.2%)
     */
    public double calculateSlippage(Ticker ticker, OrderBook orderBook, double tradeSize, 
                                  boolean isBuy, String symbol) {
        // Ignore the order book and delegate to the simpler method
        return calculateSlippage(ticker, tradeSize, isBuy, symbol);
    }
} 