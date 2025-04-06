package com.example.tradient.domain.slippage;

import com.example.tradient.data.interfaces.HistoricalDataProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Advanced slippage calculator that incorporates historical time series data
 * to improve slippage predictions based on recent market behavior.
 */
public class TimeSeriesSlippageCalculator implements SlippageCalculator {
    
    private final SlippageCalculator baseCalculator;
    private final HistoricalDataProvider historicalDataProvider;
    private final Duration lookbackPeriod;
    
    public TimeSeriesSlippageCalculator(
            SlippageCalculator baseCalculator,
            HistoricalDataProvider historicalDataProvider,
            Duration lookbackPeriod) {
        this.baseCalculator = baseCalculator;
        this.historicalDataProvider = historicalDataProvider;
        this.lookbackPeriod = lookbackPeriod;
    }
    
    @Override
    public SlippageResult calculateBuySlippage(OrderBookSnapshot orderBookSnapshot, double orderSize) {
        // First get the base calculation
        SlippageResult baseResult = baseCalculator.calculateBuySlippage(orderBookSnapshot, orderSize);
        
        // Get historical volatility adjustment
        double volatilityFactor = calculateVolatilityFactor(orderBookSnapshot.getSymbol(), orderBookSnapshot.getTimestamp());
        
        // Adjust slippage based on historical data
        double adjustedSlippage = baseResult.getSlippagePercentage() * volatilityFactor;
        
        return new SlippageResult(
                adjustedSlippage,
                baseResult.getBasePrice() * (1 + adjustedSlippage / 100),
                baseResult.getBasePrice(),
                orderSize,
                baseResult.getConfidence(),
                getName()
        );
    }
    
    @Override
    public SlippageResult calculateSellSlippage(OrderBookSnapshot orderBookSnapshot, double orderSize) {
        // First get the base calculation
        SlippageResult baseResult = baseCalculator.calculateSellSlippage(orderBookSnapshot, orderSize);
        
        // Get historical volatility adjustment
        double volatilityFactor = calculateVolatilityFactor(orderBookSnapshot.getSymbol(), orderBookSnapshot.getTimestamp());
        
        // Adjust slippage based on historical data
        double adjustedSlippage = baseResult.getSlippagePercentage() * volatilityFactor;
        
        return new SlippageResult(
                adjustedSlippage,
                baseResult.getBasePrice() * (1 - adjustedSlippage / 100),
                baseResult.getBasePrice(),
                orderSize,
                baseResult.getConfidence(),
                getName()
        );
    }
    
    @Override
    public String getName() {
        return "TimeSeriesSlippage";
    }
    
    /**
     * Calculates a volatility factor to adjust slippage based on recent historical data.
     * 
     * @param symbol The trading symbol
     * @param currentTime The current timestamp
     * @return A factor to adjust base slippage (greater than 1 means higher slippage)
     */
    private double calculateVolatilityFactor(String symbol, Instant currentTime) {
        Instant startTime = currentTime.minus(lookbackPeriod);
        
        // Get recent volatility data
        double recentVolatility = historicalDataProvider.getVolatility(symbol, startTime, currentTime);
        double averageVolatility = historicalDataProvider.getAverageVolatility(symbol, Duration.ofDays(30));
        
        if (averageVolatility == 0) {
            return 1.0;
        }
        
        // Calculate adjustment factor (normalized to a reasonable range)
        double rawFactor = recentVolatility / averageVolatility;
        
        // Apply sigmoid function to keep the factor in a reasonable range (0.5 to 2.0)
        return 0.5 + 1.5 / (1 + Math.exp(-2 * (rawFactor - 1)));
    }
} 