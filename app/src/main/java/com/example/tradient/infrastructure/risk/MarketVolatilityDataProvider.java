package com.example.tradient.infrastructure.risk;

import com.example.tradient.data.interfaces.VolatilityDataProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides market volatility data for risk calculation.
 * Collects and processes volatility metrics from market data.
 */
public class MarketVolatilityDataProvider implements VolatilityDataProvider {
    
    private final Map<String, Double> volatilityCache;
    private final Map<String, Long> lastUpdateTimestamps;
    private final long cacheExpiryMs;
    
    /**
     * Initializes the volatility data provider with a default cache expiry time.
     */
    public MarketVolatilityDataProvider() {
        this.volatilityCache = new ConcurrentHashMap<>();
        this.lastUpdateTimestamps = new ConcurrentHashMap<>();
        // Default cache expiry of 5 minutes
        this.cacheExpiryMs = 5 * 60 * 1000;
    }
    
    /**
     * Gets the volatility score for a specific symbol.
     * Lower scores indicate higher volatility.
     *
     * @param symbol The trading symbol
     * @return Volatility score normalized between 0.0 and 1.0
     */
    @Override
    public double getVolatilityScore(String symbol) {
        // Check cache first
        if (isCacheValid(symbol)) {
            return volatilityCache.get(symbol);
        }
        
        // Calculate or fetch new volatility score
        double volatilityScore = calculateVolatilityScore(symbol);
        
        // Update cache
        volatilityCache.put(symbol, volatilityScore);
        lastUpdateTimestamps.put(symbol, System.currentTimeMillis());
        
        return volatilityScore;
    }
    
    /**
     * Checks if the cached volatility data is still valid.
     *
     * @param symbol The trading symbol
     * @return true if cache is valid, false if expired or not in cache
     */
    private boolean isCacheValid(String symbol) {
        if (!volatilityCache.containsKey(symbol) || !lastUpdateTimestamps.containsKey(symbol)) {
            return false;
        }
        
        long lastUpdate = lastUpdateTimestamps.get(symbol);
        long currentTime = System.currentTimeMillis();
        
        return (currentTime - lastUpdate) < cacheExpiryMs;
    }
    
    /**
     * Calculates volatility score based on market data.
     * This implementation would typically use historical price data and standard deviation.
     *
     * @param symbol The trading symbol
     * @return Volatility score between 0.0 (high volatility) and 1.0 (low volatility)
     */
    private double calculateVolatilityScore(String symbol) {
        // This is a simplified implementation
        // In a real system, this would use historical data analysis,
        // price movement patterns, and statistical methods
        
        // For now, return a reasonable default or mock value
        // In production, this would be replaced with actual calculation
        return getDefaultVolatilityMap().getOrDefault(symbol, 0.8);
    }
    
    /**
     * Provides default volatility values for common symbols.
     * This is used as a fallback when actual data is not available.
     *
     * @return Map of symbols to their default volatility scores
     */
    private Map<String, Double> getDefaultVolatilityMap() {
        Map<String, Double> defaults = new HashMap<>();
        
        // Common stablecoins have low volatility (high scores)
        defaults.put("USDTUSDC", 0.98);
        defaults.put("USDTBUSD", 0.97);
        
        // Major cryptocurrencies have moderate volatility
        defaults.put("BTCUSDT", 0.85);
        defaults.put("ETHUSDT", 0.82);
        defaults.put("BNBUSDT", 0.80);
        
        // Altcoins typically have higher volatility (lower scores)
        defaults.put("DOGEUSDT", 0.70);
        defaults.put("SHIBUSDT", 0.65);
        
        return defaults;
    }
    
    /**
     * Invalidates the cache for a specific symbol or all symbols.
     *
     * @param symbol The symbol to invalidate, or null for all symbols
     */
    public void invalidateCache(String symbol) {
        if (symbol == null) {
            volatilityCache.clear();
            lastUpdateTimestamps.clear();
        } else {
            volatilityCache.remove(symbol);
            lastUpdateTimestamps.remove(symbol);
        }
    }
} 