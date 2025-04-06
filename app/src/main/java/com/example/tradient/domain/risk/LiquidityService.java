package com.example.tradient.domain.risk;

import android.util.Log;

import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.service.ExchangeService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that calculates real liquidity data from exchange APIs.
 * Uses order book and ticker data to calculate market liquidity.
 */
public class LiquidityService {
    private static final String TAG = "LiquidityService";
    
    // Cache for calculated liquidity values to reduce redundant calculations
    private final Map<String, Double> liquidityCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdateTimestamps = new ConcurrentHashMap<>();
    
    // Cache expiry time (5 minutes)
    private static final long CACHE_EXPIRY_MS = 5 * 60 * 1000;
    
    // Base liquidity factors for common assets (higher = more liquid)
    private final Map<String, Double> BASE_LIQUIDITY_FACTORS = new HashMap<String, Double>() {{
        put("BTC", 1.0);   // Most liquid
        put("ETH", 0.95);
        put("SOL", 0.9);
        put("BNB", 0.9);
        put("XRP", 0.85);
        put("ADA", 0.8);
        put("USDT", 1.0);
        put("USDC", 0.98);
        // Default for others handled in getBaseLiquidityFactor method
    }};
    
    /**
     * Calculate liquidity factor using order book data from exchanges.
     * 
     * @param buyOrderBook Order book from buy exchange
     * @param sellOrderBook Order book from sell exchange
     * @param symbol Trading pair symbol
     * @return Liquidity factor as a decimal (0-1, higher = more liquid)
     */
    public double calculateLiquidity(OrderBook buyOrderBook, OrderBook sellOrderBook, String symbol) {
        // Check if we have a valid cached value
        if (isCacheValid(symbol)) {
            return liquidityCache.get(symbol);
        }
        
        if (buyOrderBook == null || sellOrderBook == null) {
            return 0.5; // Default medium liquidity if no data
        }
        
        try {
            // Get base asset from symbol (e.g., "BTC" from "BTC/USD")
            String baseAsset = symbol.split("/")[0];
            
            // Calculate using order book analysis
            double liquidity = calculateOrderBookLiquidity(buyOrderBook, sellOrderBook, baseAsset);
            
            // Cache the result
            liquidityCache.put(symbol, liquidity);
            lastUpdateTimestamps.put(symbol, System.currentTimeMillis());
            
            Log.d(TAG, "Calculated liquidity for " + symbol + ": " + liquidity);
            return liquidity;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating liquidity: " + e.getMessage());
            return 0.5; // Default value on error
        }
    }
    
    /**
     * Calculate liquidity based on real-time order book data.
     * 
     * @param buyExchangeService Buy exchange service
     * @param sellExchangeService Sell exchange service
     * @param symbol Trading pair symbol
     * @return Liquidity factor as a decimal
     */
    public double calculateRealTimeLiquidity(ExchangeService buyExchangeService, 
                                           ExchangeService sellExchangeService, 
                                           String symbol) {
        try {
            // Get order books from both exchanges
            OrderBook buyOrderBook = buyExchangeService.getOrderBook(symbol);
            OrderBook sellOrderBook = sellExchangeService.getOrderBook(symbol);
            
            return calculateLiquidity(buyOrderBook, sellOrderBook, symbol);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching data for liquidity calculation: " + e.getMessage());
            return 0.5; // Default value on error
        }
    }
    
    /**
     * Calculate liquidity based on order book analysis.
     */
    private double calculateOrderBookLiquidity(OrderBook buyOrderBook, OrderBook sellOrderBook, String baseAsset) {
        // Start with the base asset liquidity factor
        double baseLiquidityFactor = getBaseLiquidityFactor(baseAsset);
        
        // Get total volume in order books
        double buyVolume = 0;
        double buyValueSum = 0;
        int buyLevels = 0;
        double bidPrice = 0;
        
        // Calculate weighted liquidity for buy side (asks)
        if (buyOrderBook.getAsksAsMap() != null) {
            for (Map.Entry<Double, Double> entry : buyOrderBook.getAsksAsMap().entrySet()) {
                double price = entry.getKey();
                double volume = entry.getValue();
                
                // First price is our reference bid price
                if (buyLevels == 0) {
                    bidPrice = price;
                }
                
                // Add to total volume
                buyVolume += volume;
                
                // Calculate value at this level
                double valueAtLevel = price * volume;
                buyValueSum += valueAtLevel;
                
                buyLevels++;
                
                // Only consider first 5 price levels for better accuracy
                if (buyLevels >= 5) break;
            }
        }
        
        // Get data for sell side
        double sellVolume = 0;
        double sellValueSum = 0;
        int sellLevels = 0;
        double askPrice = 0;
        
        // Calculate weighted liquidity for sell side (bids)
        if (sellOrderBook.getBidsAsMap() != null) {
            for (Map.Entry<Double, Double> entry : sellOrderBook.getBidsAsMap().entrySet()) {
                double price = entry.getKey();
                double volume = entry.getValue();
                
                // First price is our reference ask price
                if (sellLevels == 0) {
                    askPrice = price;
                }
                
                // Add to total volume
                sellVolume += volume;
                
                // Calculate value at this level
                double valueAtLevel = price * volume;
                sellValueSum += valueAtLevel;
                
                sellLevels++;
                
                // Only consider first 5 price levels for better accuracy
                if (sellLevels >= 5) break;
            }
        }
        
        // Calculate average price of buy and sell
        double avgPrice = (bidPrice + askPrice) / 2;
        if (avgPrice <= 0) {
            return baseLiquidityFactor; // Fall back to base factor if we can't get prices
        }
        
        // Calculate total USD value of liquidity
        double totalValueUSD = buyValueSum + sellValueSum;
        
        // Calculate bid-ask spread as percentage
        double spreadPercentage = Math.abs(askPrice - bidPrice) / avgPrice;
        
        // Calculate market depth factor (0-1 scale)
        // Normalize: $1M+ is excellent liquidity (1.0), less than $10k is poor (0.1)
        double depthFactor = Math.min(1.0, Math.max(0.1, totalValueUSD / 1000000.0));
        
        // Calculate spread factor (1.0 = tight spread, 0.0 = wide spread)
        // 0.1% or less is excellent, 1%+ is poor 
        double spreadFactor = Math.min(1.0, Math.max(0.1, 1.0 - (spreadPercentage * 100)));
        
        // Final liquidity score combines base asset, depth and spread (weighted)
        double liquidityScore = (baseLiquidityFactor * 0.4) + (depthFactor * 0.4) + (spreadFactor * 0.2);
        
        // Ensure the result is between 0.1 and 1.0
        return Math.max(0.1, Math.min(1.0, liquidityScore));
    }
    
    /**
     * Get base liquidity factor for an asset.
     */
    private double getBaseLiquidityFactor(String asset) {
        if (asset == null) {
            return 0.75; // Default for unknown
        }
        
        String normalized = asset.toUpperCase();
        return BASE_LIQUIDITY_FACTORS.getOrDefault(normalized, 0.75);
    }
    
    /**
     * Check if the cached liquidity value is still valid.
     */
    private boolean isCacheValid(String symbol) {
        if (!liquidityCache.containsKey(symbol) || !lastUpdateTimestamps.containsKey(symbol)) {
            return false;
        }
        
        long lastUpdate = lastUpdateTimestamps.get(symbol);
        long currentTime = System.currentTimeMillis();
        
        return (currentTime - lastUpdate) < CACHE_EXPIRY_MS;
    }
    
    /**
     * Force clear cache for all symbols.
     */
    public void clearCache() {
        liquidityCache.clear();
        lastUpdateTimestamps.clear();
        Log.d(TAG, "Liquidity cache cleared");
    }
} 