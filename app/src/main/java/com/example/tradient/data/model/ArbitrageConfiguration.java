package com.example.tradient.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration model for arbitrage parameters.
 * Contains settings related to profit thresholds, success rate requirements,
 * and other arbitrage-specific properties.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArbitrageConfiguration {
    
    /**
     * Minimum profit percentage required to execute an arbitrage opportunity
     */
    private double minProfitThreshold = 0.5;
    
    /**
     * Minimum risk-adjusted profit required for an arbitrage opportunity to be considered viable
     */
    private double minRiskAdjustedProfit = 0.05;
    
    /**
     * Base order size in base currency
     */
    private double baseOrderSize = 1000.0;
    
    /**
     * Base slippage percentage
     */
    private double baseSlippagePercentage = 0.05;
    
    /**
     * Minimum success rate required for an arbitrage opportunity to be considered viable
     */
    private int minimumSuccessRate = 70;
    
    /**
     * Maximum percentage of capital to use in a single arbitrage trade
     */
    private double maxPositionPercent = 0.25;
    
    /**
     * Base capital available for trading
     */
    private double availableCapital = 100000.0;
    
    /**
     * Minimum trade size in base currency
     */
    private double minimumTradeSize = 10.0;
    
    /**
     * Enable detailed logging of arbitrage calculations
     */
    private boolean detailedLogging = false;
    
    /**
     * Minimum profit percentage.
     * This is an alias for getMinProfitThreshold() for backward compatibility.
     */
    private double minProfitPercent = 0.5;
    
    /**
     * Default constructor
     */
    public ArbitrageConfiguration() {
    }
    
    /**
     * Constructor with parameters
     */
    public ArbitrageConfiguration(double minProfitThreshold, double minRiskAdjustedProfit,
                                double baseOrderSize, double baseSlippagePercentage,
                                int minimumSuccessRate, double maxPositionPercent,
                                double availableCapital, double minimumTradeSize,
                                boolean detailedLogging) {
        this.minProfitThreshold = minProfitThreshold;
        this.minRiskAdjustedProfit = minRiskAdjustedProfit;
        this.baseOrderSize = baseOrderSize;
        this.baseSlippagePercentage = baseSlippagePercentage;
        this.minimumSuccessRate = minimumSuccessRate;
        this.maxPositionPercent = maxPositionPercent;
        this.availableCapital = availableCapital;
        this.minimumTradeSize = minimumTradeSize;
        this.detailedLogging = detailedLogging;
        this.minProfitPercent = minProfitThreshold;
    }

    /**
     * Gets the minimum profit threshold in percentage points.
     * 
     * @return The minimum profit threshold
     */
    public double getMinProfitThreshold() {
        return minProfitThreshold;
    }

    public void setMinProfitThreshold(double minProfitThreshold) {
        this.minProfitThreshold = minProfitThreshold;
        this.minProfitPercent = minProfitThreshold;
    }

    /**
     * Gets the minimum profit percentage.
     * This is an alias for getMinProfitThreshold() for backward compatibility.
     * 
     * @return The minimum profit percentage
     */
    public double getMinProfitPercent() {
        return minProfitPercent;
    }

    public void setMinProfitPercent(double minProfitPercent) {
        this.minProfitPercent = minProfitPercent;
        this.minProfitThreshold = minProfitPercent;
    }

    public double getMinRiskAdjustedProfit() {
        return minRiskAdjustedProfit;
    }

    public void setMinRiskAdjustedProfit(double minRiskAdjustedProfit) {
        this.minRiskAdjustedProfit = minRiskAdjustedProfit;
    }

    public double getBaseOrderSize() {
        return baseOrderSize;
    }

    public void setBaseOrderSize(double baseOrderSize) {
        this.baseOrderSize = baseOrderSize;
    }

    public double getBaseSlippagePercentage() {
        return baseSlippagePercentage;
    }

    public void setBaseSlippagePercentage(double baseSlippagePercentage) {
        this.baseSlippagePercentage = baseSlippagePercentage;
    }

    public int getMinimumSuccessRate() {
        return minimumSuccessRate;
    }

    public void setMinimumSuccessRate(int minimumSuccessRate) {
        this.minimumSuccessRate = minimumSuccessRate;
    }

    public double getMaxPositionPercent() {
        return maxPositionPercent;
    }

    public void setMaxPositionPercent(double maxPositionPercent) {
        this.maxPositionPercent = maxPositionPercent;
    }

    public double getAvailableCapital() {
        return availableCapital;
    }

    public void setAvailableCapital(double availableCapital) {
        this.availableCapital = availableCapital;
    }

    public double getMinimumTradeSize() {
        return minimumTradeSize;
    }

    public void setMinimumTradeSize(double minimumTradeSize) {
        this.minimumTradeSize = minimumTradeSize;
    }

    public boolean isDetailedLogging() {
        return detailedLogging;
    }

    public void setDetailedLogging(boolean detailedLogging) {
        this.detailedLogging = detailedLogging;
    }
} 