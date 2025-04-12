package com.example.tradient.domain.analytics;

import android.util.Log;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.domain.risk.SlippageManagerService;
import com.example.tradient.domain.risk.SlippageStressTester;
import java.time.Instant;

/**
 * SlippageAnalyticsBuilder provides a convenient builder pattern for creating and accessing
 * all slippage-related components, making it easier to integrate slippage analytics into the application.
 * This class serves as a central point of access to slippage calculation, volatility tracking,
 * and stress testing functionality.
 */
public class SlippageAnalyticsBuilder {
    
    private static final String TAG = "SlippageAnalytics";
    private SlippageManagerService slippageManager;
    private SlippageStressTester stressTester;
    
    /**
     * Creates a new SlippageAnalyticsBuilder with default components.
     */
    public SlippageAnalyticsBuilder() {
        this.slippageManager = new SlippageManagerService();
        this.stressTester = new SlippageStressTester(slippageManager);
    }
    
    /**
     * Gets the slippage manager service.
     *
     * @return The slippage manager service
     */
    public SlippageManagerService getSlippageManager() {
        return slippageManager;
    }
    
    /**
     * Gets the slippage stress tester.
     *
     * @return The slippage stress tester
     */
    public SlippageStressTester getStressTester() {
        return stressTester;
    }
    
    /**
     * Calculates slippage for a trade.
     * 
     * @param ticker The ticker data
     * @param tradeSize The size of the trade
     * @param isBuy Whether this is a buy (true) or sell (false) operation
     * @param symbol The trading symbol
     * @return Expected slippage as a decimal
     */
    public double calculateSlippage(Ticker ticker, double tradeSize, boolean isBuy, String symbol) {
        return slippageManager.calculateSlippage(ticker, tradeSize, isBuy, symbol);
    }
    
    /**
     * Calculates slippage for a trade with order book data.
     * 
     * @param ticker The ticker data
     * @param orderBook The order book data
     * @param tradeSize The size of the trade
     * @param isBuy Whether this is a buy (true) or sell (false) operation
     * @param symbol The trading symbol
     * @return Expected slippage as a decimal
     */
    public double calculateSlippage(Ticker ticker, OrderBook orderBook, double tradeSize, boolean isBuy, String symbol) {
        // Order book is now ignored in the updated implementation
        Log.w(TAG, "Order book parameter is ignored in the updated slippage calculation");
        return slippageManager.calculateSlippage(ticker, tradeSize, isBuy, symbol);
    }
    
    /**
     * Creates a fully configured SlippageAnalyticsBuilder.
     *
     * @return A new SlippageAnalyticsBuilder instance
     */
    public static SlippageAnalyticsBuilder create() {
        return new SlippageAnalyticsBuilder();
    }
} 