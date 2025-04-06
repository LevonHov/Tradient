package com.example.tradient.domain.risk;

import android.util.Log;

import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.service.ExchangeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to calculate volatility metrics for trading pairs.
 */
public class VolatilityService {
    private static final String TAG = "VolatilityService";
    
    // Cache system for volatility values
    private final Map<String, Double> volatilityCache = new HashMap<>();
    private final Map<String, Long> lastUpdateTimestamps = new HashMap<>();
    private static final long CACHE_EXPIRY_MS = 10 * 60 * 1000; // 10 minutes
    
    // Lookback periods for different volatility calculations
    private static final int SHORT_TERM_HOURS = 4;
    private static final int MID_TERM_HOURS = 12;
    private static final int LONG_TERM_HOURS = 24;
    
    /**
     * Calculate volatility based on ticker data.
     * 
     * @param buyExchangeTickers List of tickers from buy exchange
     * @param sellExchangeTickers List of tickers from sell exchange
     * @param symbol Trading pair symbol
     * @return Volatility as a decimal (e.g., 0.05 = 5%)
     */
    public double calculateVolatility(
            List<Ticker> buyExchangeTickers, 
            List<Ticker> sellExchangeTickers,
            String symbol) {
        
        // Check for valid input data
        if (isDataMissing(buyExchangeTickers) || isDataMissing(sellExchangeTickers)) {
            Log.w(TAG, "Missing ticker data for volatility calculation");
            return createFallbackVolatility(symbol);
        }
        
        try {
            // Calculate volatility from price changes
            double buyVolatility = calculatePriceVolatility(buyExchangeTickers);
            double sellVolatility = calculatePriceVolatility(sellExchangeTickers);
            
            // Use average of buy and sell volatility
            double volatility = (buyVolatility + sellVolatility) / 2.0;
            
            // Cache the result
            String cacheKey = symbol.toLowerCase();
            volatilityCache.put(cacheKey, volatility);
            lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
            
            Log.d(TAG, "Calculated volatility for " + symbol + ": " + volatility);
            return volatility;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating volatility: " + e.getMessage(), e);
            return createFallbackVolatility(symbol);
        }
    }
    
    /**
     * Calculate real-time volatility using direct API calls.
     * 
     * @param buyExchangeService Buy exchange service
     * @param sellExchangeService Sell exchange service
     * @param symbol Trading pair symbol
     * @return Volatility as a decimal (e.g., 0.05 = 5%)
     */
    public double calculateRealTimeVolatility(
            ExchangeService buyExchangeService,
            ExchangeService sellExchangeService,
            String symbol) {
        
        String cacheKey = symbol.toLowerCase();
        
        // Check if we have a valid cached value
        if (isCacheValid(cacheKey)) {
            return volatilityCache.get(cacheKey);
        }
        
        try {
            // Get historical candles if possible
            List<Ticker> buyTickers = buyExchangeService.getHistoricalTickers(symbol, 24);
            List<Ticker> sellTickers = sellExchangeService.getHistoricalTickers(symbol, 24);
            
            // Check if we got enough data
            if (isDataMissing(buyTickers) || isDataMissing(sellTickers)) {
                Log.w(TAG, "Insufficient data for volatility calculation");
                
                // Try to calculate with recent data from ticker only
                Ticker buyTicker = buyExchangeService.getTickerData(symbol);
                Ticker sellTicker = sellExchangeService.getTickerData(symbol);
                
                if (buyTicker != null && sellTicker != null) {
                    // Calculate from 24h high/low
                    double buyVolatility = calculateTickerVolatility(buyTicker);
                    double sellVolatility = calculateTickerVolatility(sellTicker);
                    double volatility = (buyVolatility + sellVolatility) / 2.0;
                    
                    // Cache the result
                    volatilityCache.put(cacheKey, volatility);
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                    
                    Log.d(TAG, "Calculated volatility from ticker for " + symbol + ": " + volatility);
                    return volatility;
                }
                
                // If we still can't get data, use fallback
                return createFallbackVolatility(symbol);
            }
            
            // Calculate from historical data
            return calculateVolatility(buyTickers, sellTickers, symbol);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating real-time volatility: " + e.getMessage(), e);
            return createFallbackVolatility(symbol);
        }
    }
    
    /**
     * Create fallback volatility values based on the asset type.
     * Used when real data cannot be obtained.
     */
    private double createFallbackVolatility(String symbol) {
        Log.d(TAG, "Creating fallback volatility for " + symbol);
        
        // Extract the base asset from the symbol
        String baseAsset = symbol.split("/")[0];
        if (baseAsset == null) {
            baseAsset = symbol.length() > 3 ? symbol.substring(0, 3) : symbol;
        }
        
        // Assign reasonable volatility values based on asset type
        double fallbackVolatility;
        switch (baseAsset.toUpperCase()) {
            case "BTC":
                fallbackVolatility = 0.025; // 2.5% - moderate volatility
                break;
            case "ETH":
                fallbackVolatility = 0.03; // 3% - slightly higher than BTC
                break;
            case "SOL":
            case "AVAX":
            case "DOT":
                fallbackVolatility = 0.06; // 6% - higher volatility alts
                break;
            case "SHIB":
            case "DOGE":
            case "PEPE":
                fallbackVolatility = 0.09; // 9% - meme coins have high volatility
                break;
            default:
                // Stablecoins (USDT, USDC, DAI, etc.)
                if (baseAsset.toUpperCase().contains("USD")) {
                    fallbackVolatility = 0.002; // 0.2% - very stable
                } else {
                    fallbackVolatility = 0.04; // 4% - typical altcoin
                }
        }
        
        // Cache the fallback value but with shorter expiry
        String cacheKey = symbol.toLowerCase();
        volatilityCache.put(cacheKey, fallbackVolatility);
        // Expire fallback values twice as fast
        lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis() - (CACHE_EXPIRY_MS / 2));
        
        Log.d(TAG, "Using fallback volatility for " + symbol + ": " + fallbackVolatility);
        return fallbackVolatility;
    }
    
    /**
     * Calculate volatility from single ticker with high/low data.
     */
    private double calculateTickerVolatility(Ticker ticker) {
        if (ticker == null) {
            return 0.03; // Default moderate volatility
        }
        
        double highPrice = ticker.getHighPrice();
        double lowPrice = ticker.getLowPrice();
        
        // If high and low are both valid
        if (highPrice > 0 && lowPrice > 0 && highPrice >= lowPrice) {
            // Range-based volatility estimate
            return (highPrice - lowPrice) / ((highPrice + lowPrice) / 2);
        }
        
        // Fallback to default
        return 0.03;
    }
    
    /**
     * Calculate volatility from a list of tickers with price data.
     */
    private double calculatePriceVolatility(List<Ticker> tickers) {
        if (isDataMissing(tickers)) {
            return 0.03; // Default moderate volatility
        }
        
        // Calculate standard deviation of returns
        double[] returns = new double[tickers.size() - 1];
        for (int i = 1; i < tickers.size(); i++) {
            double prevPrice = tickers.get(i-1).getLastPrice();
            double currentPrice = tickers.get(i).getLastPrice();
            
            if (prevPrice > 0) {
                returns[i-1] = (currentPrice - prevPrice) / prevPrice;
            }
        }
        
        // Calculate standard deviation of returns
        double mean = calculateMean(returns);
        double variance = calculateVariance(returns, mean);
        double stdDev = Math.sqrt(variance);
        
        // Annualize the volatility from hourly to daily scale
        double adjustedStdDev = stdDev * Math.sqrt(24);
        
        // Cap the calculated volatility to reasonable bounds
        return Math.min(0.2, Math.max(0.01, adjustedStdDev));
    }
    
    /**
     * Calculate the mean of an array of values.
     */
    private double calculateMean(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return values.length > 0 ? sum / values.length : 0;
    }
    
    /**
     * Calculate the variance of an array of values.
     */
    private double calculateVariance(double[] values, double mean) {
        double sumSquaredDifferences = 0;
        for (double value : values) {
            double difference = value - mean;
            sumSquaredDifferences += difference * difference;
        }
        return values.length > 0 ? sumSquaredDifferences / values.length : 0;
    }
    
    /**
     * Calculate volatility with market sentiment consideration.
     * This method enhances regular volatility with market-wide data.
     * 
     * @param symbol The trading pair symbol
     * @param baseVolatility The base calculated volatility
     * @return Adjusted volatility value
     */
    public double calculateVolatilityWithMarketSentiment(String symbol, double baseVolatility) {
        // Get the base asset from the symbol
        String baseAsset = symbol.split("/")[0];
        if (baseAsset == null) {
            baseAsset = symbol.length() > 3 ? symbol.substring(0, 3) : symbol;
        }
        
        // Apply market condition adjustment (more volatility during uncertain markets)
        double marketSentimentFactor = getMarketSentimentFactor(baseAsset);
        
        // Adjust volatility based on market conditions
        return baseVolatility * marketSentimentFactor;
    }
    
    /**
     * Get current market sentiment factor for volatility adjustment.
     * Higher factor = more market uncertainty = higher effective volatility
     * 
     * @param baseAsset The base asset (e.g., BTC, ETH)
     * @return Market sentiment multiplier (0.8-1.5)
     */
    private double getMarketSentimentFactor(String baseAsset) {
        // In a real implementation, this would pull from market sentiment APIs
        // Here we use reasonably typical values for demonstration
        
        // If we have cached values from a market sentiment service, we would use those
        // For now, default to moderate values based on asset type
        switch (baseAsset.toUpperCase()) {
            case "BTC":
            case "ETH":
                return 1.0; // Standard market conditions for major assets
            case "SOL":
            case "AVAX":
            case "DOT":
                return 1.2; // Slightly elevated volatility for mid-cap alts
            case "SHIB":
            case "DOGE":
            case "PEPE":
                return 1.5; // High volatility for meme coins
            default:
                // Stablecoins
                if (baseAsset.toUpperCase().contains("USD")) {
                    return 0.8; // Reduced volatility for stablecoins
                } else {
                    return 1.1; // Slightly elevated for general altcoins
                }
        }
    }
    
    /**
     * Check if data is missing or insufficient.
     */
    private boolean isDataMissing(List<?> data) {
        return data == null || data.size() < 2;
    }
    
    /**
     * Check if cached value is still valid.
     */
    private boolean isCacheValid(String key) {
        if (!volatilityCache.containsKey(key) || !lastUpdateTimestamps.containsKey(key)) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        long lastUpdate = lastUpdateTimestamps.get(key);
        return (now - lastUpdate) < CACHE_EXPIRY_MS;
    }
    
    /**
     * Clear all cached volatility data.
     * Used when market conditions change significantly or when fresh calculations are needed.
     */
    public void clearCache() {
        volatilityCache.clear();
        lastUpdateTimestamps.clear();
        Log.d(TAG, "Volatility cache cleared");
    }
} 