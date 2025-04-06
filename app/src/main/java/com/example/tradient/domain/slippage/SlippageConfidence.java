package com.example.tradient.domain.slippage;

/**
 * Enum representing the confidence level in a slippage estimate.
 * Higher confidence levels indicate more reliable slippage calculations.
 */
public enum SlippageConfidence {
    VERY_LOW(0.2),
    LOW(0.4),
    MEDIUM(0.6),
    HIGH(0.8),
    VERY_HIGH(0.95);
    
    private final double confidenceValue;
    
    SlippageConfidence(double confidenceValue) {
        this.confidenceValue = confidenceValue;
    }
    
    public double getValue() {
        return confidenceValue;
    }
    
    /**
     * Gets the appropriate confidence level based on available data quality.
     * 
     * @param orderBookDepth Depth of the order book (number of levels)
     * @param volumeCoverage Percentage of order size covered in the order book
     * @param isNormalMarket Whether market conditions are normal
     * @return The appropriate confidence level
     */
    public static SlippageConfidence determineConfidence(
            int orderBookDepth,
            double volumeCoverage,
            boolean isNormalMarket) {
        
        if (!isNormalMarket) {
            return VERY_LOW;
        }
        
        if (orderBookDepth < 5 || volumeCoverage < 0.5) {
            return LOW;
        }
        
        if (orderBookDepth < 15 || volumeCoverage < 0.8) {
            return MEDIUM;
        }
        
        if (orderBookDepth < 25 || volumeCoverage < 1.0) {
            return HIGH;
        }
        
        return VERY_HIGH;
    }
} 