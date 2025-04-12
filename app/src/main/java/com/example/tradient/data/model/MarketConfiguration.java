package com.example.tradient.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration model for market-specific parameters.
 * Contains settings for market regimes, sentiment analysis, and correlation data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketConfiguration {
    
    private Map<String, Double> regimeScores = new HashMap<>();
    private Map<String, Double> sentimentScores = new HashMap<>();
    private Map<String, Double> correlationScores = new HashMap<>();
    
    public MarketConfiguration() {
        // Initialize with default values for major assets
        initializeDefaultScores();
    }
    
    private void initializeDefaultScores() {
        // Market regime scores (higher is better)
        regimeScores.put("btc", 0.8);
        regimeScores.put("eth", 0.75);
        regimeScores.put("usdt", 0.9);
        
        // Sentiment scores (higher is better)
        sentimentScores.put("btc", 0.7);
        sentimentScores.put("eth", 0.65);
        sentimentScores.put("usdt", 0.8);
        
        // Correlation scores (higher means more correlated)
        correlationScores.put("btc", 0.9);
        correlationScores.put("eth", 0.85);
        correlationScores.put("usdt", 0.95);
    }
    
    /**
     * Get the market regime score for a symbol
     * 
     * @param symbol The trading pair symbol
     * @param defaultScore Default score if not configured
     * @return The regime score (0.0-1.0)
     */
    public double getRegimeScore(String symbol, double defaultScore) {
        String baseAsset = extractBaseAsset(symbol);
        return regimeScores.getOrDefault(baseAsset.toLowerCase(), defaultScore);
    }
    
    /**
     * Get the sentiment score for a symbol
     * 
     * @param symbol The trading pair symbol
     * @param defaultScore Default score if not configured
     * @return The sentiment score (0.0-1.0)
     */
    public double getSentimentScore(String symbol, double defaultScore) {
        String baseAsset = extractBaseAsset(symbol);
        return sentimentScores.getOrDefault(baseAsset.toLowerCase(), defaultScore);
    }
    
    /**
     * Get the correlation score for a symbol
     * 
     * @param symbol The trading pair symbol
     * @param defaultScore Default score if not configured
     * @return The correlation score (0.0-1.0)
     */
    public double getCorrelationScore(String symbol, double defaultScore) {
        String baseAsset = extractBaseAsset(symbol);
        return correlationScores.getOrDefault(baseAsset.toLowerCase(), defaultScore);
    }
    
    /**
     * Extract the base asset from a trading pair symbol
     * 
     * @param symbol The trading pair symbol
     * @return The base asset code
     */
    private String extractBaseAsset(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return "";
        }
        
        // Common quote assets to look for
        String[] quoteAssets = {"USDT", "USD", "BTC", "ETH", "EUR", "DAI", "GBP", "JPY"};
        
        for (String quote : quoteAssets) {
            if (symbol.endsWith(quote)) {
                return symbol.substring(0, symbol.length() - quote.length());
            }
        }
        
        // Default to first 3-4 characters if no known quote asset
        return symbol.length() > 3 ? symbol.substring(0, 3) : symbol;
    }
    
    // Getters and setters for configuration maps
    public Map<String, Double> getRegimeScores() {
        return regimeScores;
    }
    
    public void setRegimeScores(Map<String, Double> regimeScores) {
        this.regimeScores = regimeScores;
    }
    
    public Map<String, Double> getSentimentScores() {
        return sentimentScores;
    }
    
    public void setSentimentScores(Map<String, Double> sentimentScores) {
        this.sentimentScores = sentimentScores;
    }
    
    public Map<String, Double> getCorrelationScores() {
        return correlationScores;
    }
    
    public void setCorrelationScores(Map<String, Double> correlationScores) {
        this.correlationScores = correlationScores;
    }
} 