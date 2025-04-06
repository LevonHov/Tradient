package com.example.tradient.domain.risk;

import android.util.Log;
import android.util.Pair;

import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.data.model.Ticker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Specialized calculator for asset-specific risk assessment.
 * This class builds on the core RiskCalculator to provide risk scores
 * specifically for trading pairs/assets rather than for arbitrage opportunities.
 */
public class AssetRiskCalculator {
    
    private static final String TAG = "AssetRiskCalculator";
    
    // Main risk calculator instance
    private final RiskCalculator riskCalculator;
    
    // Cache for asset risk scores to avoid recalculating frequently
    private final Map<String, RiskAssessment> assetRiskCache;
    
    // Cache for asset volatility data
    private final Map<String, Double> assetVolatilityCache;
    
    // Default values for calculating asset risk
    private static final double DEFAULT_BUY_FEE = 0.001; // 0.1%
    private static final double DEFAULT_SELL_FEE = 0.001; // 0.1%
    
    /**
     * Creates a new AssetRiskCalculator with default parameters.
     */
    public AssetRiskCalculator() {
        this.riskCalculator = new RiskCalculator();
        this.assetRiskCache = new ConcurrentHashMap<>();
        this.assetVolatilityCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Calculate risk for a specific asset using ticker data from a single exchange.
     * This is a simplified assessment when only one exchange's data is available.
     *
     * @param symbol The trading symbol (e.g., "BTC/USDT")
     * @param ticker The ticker data for the asset
     * @return A risk assessment for the asset
     */
    public RiskAssessment calculateAssetRisk(String symbol, Ticker ticker) {
        // If we have cached data and it's recent, use it
        if (assetRiskCache.containsKey(symbol) && !isCacheStale(symbol)) {
            return assetRiskCache.get(symbol);
        }
        
        try {
            // Since we only have one ticker, we'll use it for both buy and sell sides
            RiskAssessment assessment = riskCalculator.calculateRisk(
                    ticker, ticker, DEFAULT_BUY_FEE, DEFAULT_SELL_FEE);
            
            // Cache the result
            assetRiskCache.put(symbol, assessment);
            
            // Update volatility cache
            double volatility = assessment.getVolatilityScore();
            assetVolatilityCache.put(symbol, volatility);
            
            return assessment;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating asset risk for " + symbol + ": " + e.getMessage());
            return createDefaultRiskAssessment();
        }
    }
    
    /**
     * Calculate comprehensive risk for an asset using data from two exchanges.
     * This provides a more accurate risk assessment when cross-exchange data is available.
     *
     * @param symbol The trading symbol (e.g., "BTC/USDT")
     * @param buyExchangeTicker Ticker data from the buy exchange
     * @param sellExchangeTicker Ticker data from the sell exchange
     * @param buyFee Fee percentage for the buy exchange (decimal format, e.g., 0.001 for 0.1%)
     * @param sellFee Fee percentage for the sell exchange (decimal format)
     * @return A risk assessment for the asset
     */
    public RiskAssessment calculateAssetRisk(String symbol, 
                                            Ticker buyExchangeTicker, 
                                            Ticker sellExchangeTicker,
                                            double buyFee,
                                            double sellFee) {
        // If we have cached data and it's recent, use it
        if (assetRiskCache.containsKey(symbol) && !isCacheStale(symbol)) {
            return assetRiskCache.get(symbol);
        }
        
        try {
            // Use the full risk calculation with data from both exchanges
            RiskAssessment assessment = riskCalculator.calculateRisk(
                    buyExchangeTicker, sellExchangeTicker, buyFee, sellFee);
            
            // Cache the result
            assetRiskCache.put(symbol, assessment);
            
            // Update volatility cache (average of both exchanges)
            double volatility = assessment.getVolatilityScore();
            assetVolatilityCache.put(symbol, volatility);
            
            return assessment;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating asset risk for " + symbol + ": " + e.getMessage());
            return createDefaultRiskAssessment();
        }
    }
    
    /**
     * Get the normalized risk score for an asset.
     * 
     * @param symbol The trading symbol
     * @return Risk score as a percentage (0-100, where higher values mean higher risk)
     */
    public int getAssetRiskPercentage(String symbol) {
        if (!assetRiskCache.containsKey(symbol)) {
            return 50; // Default medium risk if no data
        }
        
        RiskAssessment assessment = assetRiskCache.get(symbol);
        
        // Convert the risk score to a percentage where higher numbers mean higher risk
        // Note: In our assessment, lower numbers in overallRiskScore actually mean higher risk
        double riskScore = assessment.getOverallRiskScore();
        
        // Invert the scale since our UI displays higher numbers as higher risk
        return 100 - (int)(riskScore * 100);
    }
    
    /**
     * Get the volatility level for an asset.
     * 
     * @param symbol The trading symbol
     * @return Volatility as a percentage (0-100, where higher values mean higher volatility)
     */
    public int getAssetVolatilityPercentage(String symbol) {
        if (!assetVolatilityCache.containsKey(symbol)) {
            return 50; // Default medium volatility if no data
        }
        
        // Convert volatility score to percentage (invert if needed)
        // For volatilityScore, lower scores mean higher volatility
        double volatilityScore = assetVolatilityCache.get(symbol);
        return 100 - (int)(volatilityScore * 100);
    }
    
    /**
     * Get a risk classification for an asset (Low, Medium-Low, Medium, Medium-High, High).
     * 
     * @param symbol The trading symbol
     * @return A string classification of the risk level
     */
    public String getAssetRiskClassification(String symbol) {
        int riskPercentage = getAssetRiskPercentage(symbol);
        
        if (riskPercentage < 25) {
            return "Low";
        } else if (riskPercentage < 50) {
            return "Medium-Low";
        } else if (riskPercentage < 70) {
            return "Medium";
        } else if (riskPercentage < 85) {
            return "Medium-High";
        } else {
            return "High";
        }
    }
    
    /**
     * Get the detailed risk factors for an asset.
     * 
     * @param symbol The trading symbol
     * @return A map of risk factor names to their percentage values (0-100)
     */
    public Map<String, Integer> getDetailedRiskFactors(String symbol) {
        Map<String, Integer> factors = new HashMap<>();
        
        if (!assetRiskCache.containsKey(symbol)) {
            // Default values if no data
            factors.put("Liquidity", 50);
            factors.put("Volatility", 50);
            factors.put("Market Depth", 50);
            factors.put("Slippage", 50);
            return factors;
        }
        
        RiskAssessment assessment = assetRiskCache.get(symbol);
        
        // Convert all risk factors to percentages (0-100)
        // Note: For most risk factors, lower scores mean higher risk, so we invert for UI display
        factors.put("Liquidity", 100 - (int)(assessment.getLiquidityScore() * 100));
        factors.put("Volatility", 100 - (int)(assessment.getVolatilityScore() * 100));
        factors.put("Market Depth", 100 - (int)(assessment.getDepthScore() * 100));
        factors.put("Slippage", (int)(assessment.getSlippageRisk() * 100));
        
        return factors;
    }
    
    /**
     * Clear all cached risk assessments to force recalculation.
     */
    public void clearCache() {
        assetRiskCache.clear();
        assetVolatilityCache.clear();
    }
    
    /**
     * Check if the cached risk assessment for an asset is stale.
     * 
     * @param symbol The trading symbol
     * @return true if the cache is stale and should be refreshed
     */
    private boolean isCacheStale(String symbol) {
        // For the sample app, we'll use a simple implementation
        // In a production app, you'd want to track cache timestamps
        return false;
    }
    
    /**
     * Create a default risk assessment with moderate values.
     * 
     * @return A default risk assessment
     */
    private RiskAssessment createDefaultRiskAssessment() {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setLiquidityScore(0.5);
        assessment.setVolatilityScore(0.5);
        assessment.setMarketDepthScore(0.5);
        assessment.setSlippageRisk(0.5);
        assessment.setOverallRiskScore(0.5);
        return assessment;
    }
} 