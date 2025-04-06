package com.example.tradient.domain.slippage;

/**
 * Interface for slippage calculation strategies.
 * Implementations provide different algorithms for estimating slippage.
 */
public interface SlippageCalculator {
    
    /**
     * Calculates the expected slippage for a buy order.
     * 
     * @param orderBookSnapshot The order book snapshot to use for calculation
     * @param orderSize The size of the order in base units
     * @return The calculated slippage result
     */
    SlippageResult calculateBuySlippage(OrderBookSnapshot orderBookSnapshot, double orderSize);
    
    /**
     * Calculates the expected slippage for a sell order.
     * 
     * @param orderBookSnapshot The order book snapshot to use for calculation
     * @param orderSize The size of the order in base units
     * @return The calculated slippage result
     */
    SlippageResult calculateSellSlippage(OrderBookSnapshot orderBookSnapshot, double orderSize);
    
    /**
     * Gets the name of this slippage calculator.
     * 
     * @return The calculator name
     */
    String getName();
} 