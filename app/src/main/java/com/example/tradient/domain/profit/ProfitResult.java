package com.example.tradient.domain.profit;

/**
 * A class that holds the results of profit calculations
 */
public class ProfitResult {
    private final double absoluteProfit;
    private final double percentageProfit;
    private final double profitPerUnit;
    
    /**
     * Constructor for profit calculation results
     * 
     * @param absoluteProfit The total profit in currency units
     * @param percentageProfit The profit as a percentage
     * @param profitPerUnit The profit per unit of the traded asset
     */
    public ProfitResult(double absoluteProfit, double percentageProfit, double profitPerUnit) {
        this.absoluteProfit = absoluteProfit;
        this.percentageProfit = percentageProfit;
        this.profitPerUnit = profitPerUnit;
    }
    
    /**
     * Get the total profit in currency units
     * 
     * @return The absolute profit
     */
    public double getAbsoluteProfit() {
        return absoluteProfit;
    }
    
    /**
     * Get the profit as a percentage of the investment
     * 
     * @return The percentage profit
     */
    public double getPercentageProfit() {
        return percentageProfit;
    }
    
    /**
     * Get the profit per unit of the traded asset
     * 
     * @return The profit per unit
     */
    public double getProfitPerUnit() {
        return profitPerUnit;
    }
    
    /**
     * Check if the trade is profitable
     * 
     * @return true if profit is positive, false otherwise
     */
    public boolean isProfitable() {
        return percentageProfit > 0;
    }
    
    /**
     * Check if the trade meets the minimum profitability threshold
     * 
     * @param threshold The minimum percentage profit required
     * @return true if profit exceeds threshold, false otherwise
     */
    public boolean isViable(double threshold) {
        return percentageProfit >= threshold;
    }
    
    @Override
    public String toString() {
        return String.format("Profit: %.2f (%.4f%%)", absoluteProfit, percentageProfit);
    }
} 