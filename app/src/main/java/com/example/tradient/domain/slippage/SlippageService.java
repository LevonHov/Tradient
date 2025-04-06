package com.example.tradient.domain.slippage;

import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.data.interfaces.HistoricalDataProvider;
import com.example.tradient.data.interfaces.OrderBookProvider;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for calculating slippage using the most appropriate calculator
 * based on market conditions and configuration.
 */
public class SlippageService {
    
    private final Map<String, SlippageCalculator> calculators = new HashMap<>();
    private final OrderBookProvider orderBookProvider;
    private final String defaultCalculatorName;
    
    /**
     * Creates a new slippage service with the necessary dependencies.
     * 
     * @param orderBookProvider Provider for current order book data
     * @param historicalDataProvider Provider for historical market data
     */
    public SlippageService(OrderBookProvider orderBookProvider, HistoricalDataProvider historicalDataProvider) {
        this.orderBookProvider = orderBookProvider;
        this.defaultCalculatorName = ConfigurationFactory.getString(
                "slippage.defaultCalculator", "LinearVolumeSlippage");
        
        // Initialize calculators
        SlippageCalculator linearCalculator = new LinearVolumeSlippageCalculator();
        calculators.put(linearCalculator.getName(), linearCalculator);
        
        // Only add time series calculator if historical data is available
        if (historicalDataProvider != null) {
            SlippageCalculator timeSeriesCalculator = new TimeSeriesSlippageCalculator(
                    linearCalculator,
                    historicalDataProvider,
                    Duration.ofHours(24)
            );
            calculators.put(timeSeriesCalculator.getName(), timeSeriesCalculator);
        }
        
        // Additional calculators can be registered here
    }
    
    /**
     * Calculates the expected buy slippage for a specific symbol and exchange.
     * 
     * @param symbol The trading symbol
     * @param exchange The exchange name
     * @param orderSize The size of the order
     * @return The calculated slippage result
     */
    public SlippageResult calculateBuySlippage(String symbol, String exchange, double orderSize) {
        // Get current order book
        OrderBookSnapshot orderBookSnapshot = orderBookProvider.getOrderBook(symbol, exchange);
        
        // Use appropriate calculator
        SlippageCalculator calculator = getCalculator(symbol, exchange);
        return calculator.calculateBuySlippage(orderBookSnapshot, orderSize);
    }
    
    /**
     * Calculates the expected sell slippage for a specific symbol and exchange.
     * 
     * @param symbol The trading symbol
     * @param exchange The exchange name
     * @param orderSize The size of the order
     * @return The calculated slippage result
     */
    public SlippageResult calculateSellSlippage(String symbol, String exchange, double orderSize) {
        // Get current order book
        OrderBookSnapshot orderBookSnapshot = orderBookProvider.getOrderBook(symbol, exchange);
        
        // Use appropriate calculator
        SlippageCalculator calculator = getCalculator(symbol, exchange);
        return calculator.calculateSellSlippage(orderBookSnapshot, orderSize);
    }
    
    /**
     * Calculates total slippage for an arbitrage opportunity (buy + sell).
     * 
     * @param symbol The trading symbol
     * @param buyExchange The exchange to buy on
     * @param sellExchange The exchange to sell on
     * @param orderSize The size of the order
     * @return The combined slippage result
     */
    public SlippageResult calculateArbitrageSlippage(
            String symbol,
            String buyExchange,
            String sellExchange,
            double orderSize) {
        
        SlippageResult buySlippage = calculateBuySlippage(symbol, buyExchange, orderSize);
        SlippageResult sellSlippage = calculateSellSlippage(symbol, sellExchange, orderSize);
        
        return buySlippage.combine(sellSlippage);
    }
    
    /**
     * Gets the appropriate calculator based on symbol, exchange, and current conditions.
     * 
     * @param symbol The trading symbol
     * @param exchange The exchange name
     * @return The selected slippage calculator
     */
    private SlippageCalculator getCalculator(String symbol, String exchange) {
        // Check if there's a specific calculator configured for this symbol/exchange
        String configKey = "slippage.calculator." + symbol + "." + exchange;
        String calculatorName = ConfigurationFactory.getString(configKey, defaultCalculatorName);
        
        return calculators.getOrDefault(calculatorName, 
                calculators.get(defaultCalculatorName));
    }
    
    /**
     * Registers a custom slippage calculator.
     * 
     * @param calculator The calculator to register
     */
    public void registerCalculator(SlippageCalculator calculator) {
        calculators.put(calculator.getName(), calculator);
    }
} 