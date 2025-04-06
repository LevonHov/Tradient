package com.example.tradient.domain.slippage;

/**
 * Value object that represents the result of a slippage calculation.
 * Includes slippage percentage and detailed information about the calculation.
 */
public class SlippageResult {
    private final double slippagePercentage;
    private final double effectiveExecutionPrice;
    private final double basePrice;
    private final double orderSize;
    private final SlippageConfidence confidence;
    private final String calculationMethod;
    
    /**
     * Creates a new slippage result.
     * 
     * @param slippagePercentage Percentage of slippage (positive value)
     * @param effectiveExecutionPrice The effective execution price after slippage
     * @param basePrice The base price before slippage
     * @param orderSize The size of the order
     * @param confidence Confidence level in the slippage estimate
     * @param calculationMethod The method used to calculate slippage
     */
    public SlippageResult(
            double slippagePercentage,
            double effectiveExecutionPrice,
            double basePrice,
            double orderSize,
            SlippageConfidence confidence,
            String calculationMethod) {
        this.slippagePercentage = slippagePercentage;
        this.effectiveExecutionPrice = effectiveExecutionPrice;
        this.basePrice = basePrice;
        this.orderSize = orderSize;
        this.confidence = confidence;
        this.calculationMethod = calculationMethod;
    }
    
    public double getSlippagePercentage() {
        return slippagePercentage;
    }
    
    public double getEffectiveExecutionPrice() {
        return effectiveExecutionPrice;
    }
    
    public double getBasePrice() {
        return basePrice;
    }
    
    public double getOrderSize() {
        return orderSize;
    }
    
    public SlippageConfidence getConfidence() {
        return confidence;
    }
    
    public String getCalculationMethod() {
        return calculationMethod;
    }
    
    /**
     * Combines this slippage result with another (e.g., buy side with sell side).
     * 
     * @param other Another slippage result to combine with
     * @return A new combined slippage result
     */
    public SlippageResult combine(SlippageResult other) {
        double combinedSlippage = this.slippagePercentage + other.slippagePercentage;
        SlippageConfidence combinedConfidence = this.confidence.compareTo(other.confidence) <= 0 ?
                this.confidence : other.confidence;
        
        return new SlippageResult(
                combinedSlippage,
                this.effectiveExecutionPrice,
                this.basePrice,
                this.orderSize,
                combinedConfidence,
                this.calculationMethod);
    }
} 