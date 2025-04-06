package com.example.tradient.data.interfaces;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.example.tradient.domain.slippage.OrderBookSnapshot;

/**
 * Interface for accessing historical market data needed for slippage calculation.
 */
public interface HistoricalDataProvider {
    
    /**
     * Gets the historical volatility for a symbol over a specific time period.
     * 
     * @param symbol The trading symbol
     * @param startTime Start of the period
     * @param endTime End of the period
     * @return Volatility value (standard deviation of returns)
     */
    double getVolatility(String symbol, Instant startTime, Instant endTime);
    
    /**
     * Gets the average volatility for a symbol over a duration.
     * 
     * @param symbol The trading symbol
     * @param duration The duration to average over
     * @return Average volatility
     */
    double getAverageVolatility(String symbol, Duration duration);
    
    /**
     * Gets historical order book snapshots for a symbol and exchange.
     * 
     * @param symbol The trading symbol
     * @param exchange The exchange name
     * @param startTime Start of the period
     * @param endTime End of the period
     * @param maxResults Maximum number of snapshots to return
     * @return List of order book snapshots
     */
    List<OrderBookSnapshot> getOrderBookHistory(
            String symbol,
            String exchange,
            Instant startTime,
            Instant endTime,
            int maxResults);
    
    /**
     * Gets the average slippage observed over a period for orders of a specific size.
     * 
     * @param symbol The trading symbol
     * @param exchange The exchange name
     * @param orderSize The approximate order size
     * @param duration The duration to analyze
     * @param isBuy Whether to get buy or sell slippage
     * @return The average observed slippage percentage
     */
    double getAverageObservedSlippage(
            String symbol,
            String exchange,
            double orderSize,
            Duration duration,
            boolean isBuy);
} 