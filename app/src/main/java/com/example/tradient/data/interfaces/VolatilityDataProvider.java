package com.example.tradient.data.interfaces;

/**
 * Interface for accessing market volatility data.
 * Implementations provide volatility metrics for risk calculation.
 */
public interface VolatilityDataProvider {
    
    /**
     * Gets the volatility score for a specific symbol.
     * Lower scores indicate higher volatility.
     *
     * @param symbol The trading symbol
     * @return Volatility score normalized between 0.0 and 1.0
     */
    double getVolatilityScore(String symbol);
} 