package com.example.tradient.util;

import android.util.Log;

import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.util.TimeEstimationUtil.MarketVolatility;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for advanced market analysis and metrics
 * including volatility estimation, liquidity analysis, and correlation tracking.
 */
public class MarketAnalysisUtil {
    private static final String TAG = "MarketAnalysisUtil";
    
    // Cache for market volatility (asset -> volatility level)
    private static final Map<String, MarketVolatility> volatilityCache = new ConcurrentHashMap<>();
    
    // Cache for liquidity scores (asset -> liquidity score 0-1)
    private static final Map<String, Double> liquidityCache = new ConcurrentHashMap<>();
    
    // Market correlation matrix (asset1:asset2 -> correlation coefficient)
    private static final Map<String, Double> correlationMatrix = new ConcurrentHashMap<>();
    
    // Classification thresholds for volatility (24h price change %)
    private static final double[] VOLATILITY_THRESHOLDS = {
        1.0,   // < 1% = VERY_LOW
        2.5,   // < 2.5% = LOW
        5.0,   // < 5% = MEDIUM
        10.0   // < 10% = HIGH, >= 10% = VERY_HIGH
    };
    
    // Base liquidity classifications (higher = more liquid)
    private static final Map<String, Double> BASE_LIQUIDITY_FACTORS = new HashMap<String, Double>() {{
        put("BTC", 1.0);   // Most liquid
        put("ETH", 0.95);
        put("SOL", 0.9);
        put("BNB", 0.9);
        put("XRP", 0.85);
        put("ADA", 0.8);
        put("USDT", 1.0);
        put("USDC", 0.98);
        // Default for others handled in getBaseLiquidityFactor
    }};
    
    /**
     * Estimate market volatility based on ticker data
     * 
     * @param asset Asset symbol (e.g., "BTC")
     * @param ticker Ticker data
     * @return Volatility level
     */
    public static MarketVolatility estimateVolatility(String asset, Ticker ticker) {
        // Check cache for recent estimate
        if (volatilityCache.containsKey(asset)) {
            return volatilityCache.get(asset);
        }
        
        MarketVolatility result = MarketVolatility.MEDIUM; // Default
        
        if (ticker != null) {
            try {
                // Calculate volatility from ticker data
                double volatilityPercentage = calculateTickerVolatility(ticker);
                
                // Classify volatility level
                result = classifyVolatility(volatilityPercentage);
                
                // Cache result (would be refreshed periodically in production)
                volatilityCache.put(asset, result);
                
                Log.d(TAG, String.format("Volatility for %s: %s (%.2f%%)", 
                        asset, result, volatilityPercentage));
                
            } catch (Exception e) {
                Log.e(TAG, "Error estimating volatility", e);
            }
        }
        
        return result;
    }
    
    /**
     * Calculate volatility percentage from ticker high/low range
     */
    private static double calculateTickerVolatility(Ticker ticker) {
        if (ticker.getHighPrice() > 0 && ticker.getLowPrice() > 0) {
            // High-Low range as percentage of low price
            return ((ticker.getHighPrice() - ticker.getLowPrice()) / ticker.getLowPrice()) * 100.0;
        } else {
            return 5.0; // Default 5% if no high/low data
        }
    }
    
    /**
     * Classify volatility level based on percentage
     */
    private static MarketVolatility classifyVolatility(double volatilityPercentage) {
        if (volatilityPercentage < VOLATILITY_THRESHOLDS[0]) {
            return MarketVolatility.VERY_LOW;
        } else if (volatilityPercentage < VOLATILITY_THRESHOLDS[1]) {
            return MarketVolatility.LOW;
        } else if (volatilityPercentage < VOLATILITY_THRESHOLDS[2]) {
            return MarketVolatility.MEDIUM;
        } else if (volatilityPercentage < VOLATILITY_THRESHOLDS[3]) {
            return MarketVolatility.HIGH;
        } else {
            return MarketVolatility.VERY_HIGH;
        }
    }
    
    /**
     * Analyze market liquidity based on order book and volume
     * 
     * @param asset Asset symbol
     * @param orderBook Order book data
     * @param volume Trading volume
     * @return Liquidity score (0-1, higher = more liquid)
     */
    public static double analyzeLiquidity(String asset, OrderBook orderBook, double volume) {
        // Start with base liquidity factor for the asset
        double baseFactor = getBaseLiquidityFactor(asset);
        
        // Calculate order book depth factor
        double depthFactor = calculateOrderBookDepthFactor(orderBook);
        
        // Calculate volume factor (higher volume = higher liquidity)
        double volumeFactor = calculateVolumeFactor(volume);
        
        // Calculate spread factor (tighter spread = higher liquidity)
        double spreadFactor = calculateSpreadFactor(orderBook);
        
        // Combine factors with weighted importance
        double combinedFactor = (baseFactor * 0.3) + 
                               (depthFactor * 0.3) + 
                               (volumeFactor * 0.2) + 
                               (spreadFactor * 0.2);
        
        // Ensure the result is between 0.1 and 1.0
        double liquidityScore = Math.max(0.1, Math.min(1.0, combinedFactor));
        
        // Cache result
        liquidityCache.put(asset, liquidityScore);
        
        Log.d(TAG, String.format("Liquidity for %s: %.2f (base: %.2f, depth: %.2f, volume: %.2f, spread: %.2f)",
                asset, liquidityScore, baseFactor, depthFactor, volumeFactor, spreadFactor));
        
        return liquidityScore;
    }
    
    /**
     * Get base liquidity factor for an asset
     */
    private static double getBaseLiquidityFactor(String asset) {
        String normalized = asset.toUpperCase();
        return BASE_LIQUIDITY_FACTORS.getOrDefault(normalized, 0.75);
    }
    
    /**
     * Calculate order book depth factor
     */
    private static double calculateOrderBookDepthFactor(OrderBook orderBook) {
        if (orderBook == null) {
            return 0.5; // Default if no data
        }
        
        double totalVolume = 0.0;
        int levelCount = 0;
        
        // Calculate total volume across bid and ask sides
        if (orderBook.getBids() != null) {
            for (OrderBookEntry entry : orderBook.getBids()) {
                totalVolume += entry.getVolume();
                levelCount++;
                if (levelCount >= 10) break; // Limit to 10 levels
            }
        }
        
        levelCount = 0;
        if (orderBook.getAsks() != null) {
            for (OrderBookEntry entry : orderBook.getAsks()) {
                totalVolume += entry.getVolume();
                levelCount++;
                if (levelCount >= 10) break;
            }
        }
        
        // Normalize with logarithmic scale
        if (totalVolume <= 0) {
            return 0.5;
        }
        
        return Math.min(1.0, Math.max(0.1, Math.log10(1 + totalVolume) / 5.0));
    }
    
    /**
     * Calculate volume factor
     */
    private static double calculateVolumeFactor(double volume) {
        if (volume <= 0) {
            return 0.5; // Default if no data
        }
        
        // Normalize using logarithmic scale
        return Math.min(1.0, Math.max(0.1, Math.log10(1 + volume) / 7.0));
    }
    
    /**
     * Calculate spread factor based on order book
     */
    private static double calculateSpreadFactor(OrderBook orderBook) {
        if (orderBook == null || orderBook.getBids() == null || orderBook.getAsks() == null || 
            orderBook.getBids().isEmpty() || orderBook.getAsks().isEmpty()) {
            return 0.5; // Default if no data
        }
        
        double bestBid = orderBook.getBids().get(0).getPrice();
        double bestAsk = orderBook.getAsks().get(0).getPrice();
        
        if (bestBid <= 0 || bestAsk <= 0) {
            return 0.5;
        }
        
        // Calculate relative spread
        double midPrice = (bestBid + bestAsk) / 2.0;
        double spreadPercentage = Math.abs(bestAsk - bestBid) / midPrice * 100;
        
        // Convert spread to factor (tighter spread = higher factor)
        if (spreadPercentage <= 0.1) {
            return 1.0; // Excellent liquidity
        } else if (spreadPercentage >= 2.0) {
            return 0.1; // Poor liquidity
        } else {
            // Linear interpolation
            return 1.0 - (spreadPercentage - 0.1) / 1.9 * 0.9;
        }
    }
    
    /**
     * Calculate market impact cost for a given trade size
     * 
     * @param orderBook Order book data
     * @param tradeAmount Amount to trade
     * @param isBuy True for buy orders, false for sell orders
     * @return Market impact as percentage of trade value
     */
    public static double calculateMarketImpact(OrderBook orderBook, double tradeAmount, boolean isBuy) {
        if (orderBook == null) {
            return 0.5; // Default if no data
        }
        
        List<OrderBookEntry> relevantSide = isBuy ? orderBook.getAsks() : orderBook.getBids();
        
        if (relevantSide == null || relevantSide.isEmpty()) {
            return 0.5;
        }
        
        double bestPrice = relevantSide.get(0).getPrice();
        double totalValue = 0;
        double totalAmount = 0;
        double remainingAmount = tradeAmount;
        
        // Simulate order execution through the order book
        for (OrderBookEntry level : relevantSide) {
            double levelPrice = level.getPrice();
            double levelAmount = level.getVolume();
            
            if (remainingAmount <= levelAmount) {
                totalValue += levelPrice * remainingAmount;
                totalAmount += remainingAmount;
                break;
            } else {
                totalValue += levelPrice * levelAmount;
                totalAmount += levelAmount;
                remainingAmount -= levelAmount;
            }
            
            // If we've gone through the entire order book
            if (remainingAmount > 0 && relevantSide.indexOf(level) == relevantSide.size() - 1) {
                // Extrapolate for the remainder using a penalty factor
                double lastPrice = levelPrice;
                double extrapolationFactor = 1.1; // 10% worse price for each level beyond book
                
                // Apply extrapolation with diminishing amounts
                while (remainingAmount > 0) {
                    double extrapolatedPrice = lastPrice * 
                            (isBuy ? extrapolationFactor : (1/extrapolationFactor));
                    
                    // Assume each extrapolated level has declining volume
                    double extraLevel = Math.min(remainingAmount, levelAmount * 0.5);
                    
                    totalValue += extrapolatedPrice * extraLevel;
                    totalAmount += extraLevel;
                    remainingAmount -= extraLevel;
                    
                    // Update for next iteration
                    lastPrice = extrapolatedPrice;
                    levelAmount *= 0.5; // Reduce available liquidity
                    
                    // Safety break
                    if (levelAmount < 0.001) break;
                }
            }
        }
        
        // If we couldn't fill the entire order
        if (totalAmount < tradeAmount) {
            // Return high impact to discourage large orders in illiquid markets
            return 5.0; // 5% impact
        }
        
        // Calculate effective average price
        double avgPrice = totalValue / totalAmount;
        
        // Calculate market impact as percentage
        double impact = Math.abs(avgPrice - bestPrice) / bestPrice * 100;
        
        Log.d(TAG, String.format("Market impact for %s %.4f: %.4f%%", 
                isBuy ? "buy" : "sell", tradeAmount, impact));
        
        return impact;
    }
    
    /**
     * Update market correlation between two assets
     * (Would be called periodically with price data in production)
     * 
     * @param asset1 First asset symbol
     * @param asset2 Second asset symbol
     * @param correlation Correlation coefficient (-1 to 1)
     */
    public static void updateCorrelation(String asset1, String asset2, double correlation) {
        String key = getCorrelationKey(asset1, asset2);
        correlationMatrix.put(key, correlation);
    }
    
    /**
     * Get correlation between two assets
     * 
     * @param asset1 First asset symbol
     * @param asset2 Second asset symbol
     * @return Correlation coefficient (-1 to 1) or 0 if unknown
     */
    public static double getCorrelation(String asset1, String asset2) {
        String key = getCorrelationKey(asset1, asset2);
        return correlationMatrix.getOrDefault(key, 0.0);
    }
    
    /**
     * Generate a consistent key for the correlation matrix
     * (ensures asset1:asset2 and asset2:asset1 map to same key)
     */
    private static String getCorrelationKey(String asset1, String asset2) {
        String[] assets = {asset1.toUpperCase(), asset2.toUpperCase()};
        Arrays.sort(assets);
        return assets[0] + ":" + assets[1];
    }
    
    /**
     * Calculate opportunity cost percentage for capital tied up in arbitrage
     * 
     * @param volatility Current market volatility
     * @return Hourly opportunity cost percentage
     */
    public static double calculateOpportunityCost(MarketVolatility volatility) {
        switch (volatility) {
            case VERY_LOW:
                return 0.0005; // 0.05% per hour
            case LOW:
                return 0.001;  // 0.1% per hour
            case MEDIUM:
                return 0.002;  // 0.2% per hour
            case HIGH:
                return 0.005;  // 0.5% per hour
            case VERY_HIGH:
                return 0.01;   // 1% per hour
            default:
                return 0.002;  // Default: 0.2% per hour
        }
    }
    
    /**
     * Reset cached data (for testing or when market conditions change significantly)
     */
    public static void resetCache() {
        volatilityCache.clear();
        liquidityCache.clear();
    }
} 