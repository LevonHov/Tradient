package com.example.tradient.domain.profit;

import android.util.Log;
import android.util.Pair;

import com.example.tradient.data.interfaces.IExchangeService;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.util.TimeEstimationUtil;
import com.example.tradient.util.TimeEstimationUtil.MarketVolatility;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;

/**
 * Enhanced profit calculator with advanced market analysis capabilities.
 * Incorporates time-based metrics, risk assessment, and dynamic market factors.
 */
public class ProfitCalculator {
    private static final String TAG = "ProfitCalculator";

    // Constants for calculation
    private static final double DEFAULT_SLIPPAGE_FACTOR = 0.001; // 0.1% default slippage
    private static final double MAX_ORDER_BOOK_DEPTH = 10; // How many order book levels to consider
    private static final double MIN_VIABLE_PROFIT = 0.05; // 0.05% minimum viable profit
    
    // Cache for market volatility estimates (asset -> volatility level)
    private static final Map<String, MarketVolatility> volatilityCache = new ConcurrentHashMap<>();
    
    // Cache for exchange risk scores (exchange -> risk score 0-1)
    private static final Map<String, Double> exchangeRiskScores = new HashMap<String, Double>() {{
        put("binance", 0.15);  // 15% risk (lower is better)
        put("coinbase", 0.18);
        put("kraken", 0.17);
        put("bybit", 0.25);
        put("okx", 0.28);
        put("kucoin", 0.30);
        put("huobi", 0.32);
        put("gate", 0.35);
        // Default handled in getExchangeRiskScore method
    }};
    
    // NEW: Exchange-specific slippage factors (lower = better execution quality)
    private static final Map<String, Double> EXCHANGE_SLIPPAGE_FACTORS = new HashMap<String, Double>() {{
        put("binance", 0.85);   // Binance has good execution quality
        put("coinbase", 0.90);
        put("kraken", 0.88);
        put("bybit", 0.95);
        put("okx", 1.0);       // Baseline
        put("kucoin", 1.05);
        put("huobi", 1.05);
        put("gate", 1.15);
    }};
    
    // NEW: Recent slippage history cache for adaptive slippage estimation
    private static final Map<String, SlippageHistory> SLIPPAGE_HISTORY_CACHE = new ConcurrentHashMap<>();
    
    // NEW: Market hours impact on slippage (UTC hours)
    private static final Map<Integer, Double> MARKET_HOURS_SLIPPAGE_FACTORS = new HashMap<Integer, Double>() {{
        // High volume hours generally have lower slippage
        put(0, 1.2);   // 00:00 UTC - lower volume, higher slippage
        put(1, 1.25);  // 01:00 UTC
        put(2, 1.3);   // 02:00 UTC
        put(3, 1.3);   // 03:00 UTC
        put(4, 1.2);   // 04:00 UTC
        put(5, 1.1);   // 05:00 UTC
        put(6, 1.05);  // 06:00 UTC
        put(7, 1.0);   // 07:00 UTC - Asian markets active
        put(8, 0.95);  // 08:00 UTC
        put(9, 0.90);  // 09:00 UTC - European trading begins
        put(10, 0.85); // 10:00 UTC - High liquidity
        put(11, 0.80); // 11:00 UTC - European and Asian overlap
        put(12, 0.85); // 12:00 UTC
        put(13, 0.90); // 13:00 UTC
        put(14, 0.85); // 14:00 UTC - US pre-market
        put(15, 0.80); // 15:00 UTC - US market open
        put(16, 0.80); // 16:00 UTC - US and European overlap (highest liquidity)
        put(17, 0.85); // 17:00 UTC
        put(18, 0.90); // 18:00 UTC - European close
        put(19, 0.95); // 19:00 UTC
        put(20, 1.0);  // 20:00 UTC
        put(21, 1.05); // 21:00 UTC
        put(22, 1.1);  // 22:00 UTC
        put(23, 1.15); // 23:00 UTC
    }};
    
    // Asset liquidity classifications (higher = more liquid)
    private static final Map<String, Double> ASSET_LIQUIDITY_FACTORS = new HashMap<String, Double>() {{
        put("BTC", 1.0);   // Most liquid
        put("ETH", 0.95);
        put("SOL", 0.9);
        put("BNB", 0.9);
        put("XRP", 0.85);
        put("ADA", 0.8);
        put("USDT", 1.0);
        put("USDC", 0.98);
        // Default for other assets is handled in the code
    }};
    
    // Volatility classification thresholds (24h price change %)
    private static final double[] VOLATILITY_THRESHOLDS = {
        1.0,   // < 1% = VERY_LOW
        2.5,   // < 2.5% = LOW
        5.0,   // < 5% = MEDIUM
        10.0   // < 10% = HIGH, >= 10% = VERY_HIGH
    };
    
    /**
     * Calculate the basic profit percentage without considering time and risk factors
     * 
     * @param buyPrice Buy price
     * @param sellPrice Sell price
     * @param buyFeePercentage Buy fee as decimal (e.g., 0.001 for 0.1%)
     * @param sellFeePercentage Sell fee as decimal (e.g., 0.001 for 0.1%)
     * @return Basic profit percentage
     */
    public static double calculateBasicProfitPercentage(
            double buyPrice, double sellPrice, double buyFeePercentage, double sellFeePercentage) {
        
        // Calculate effective costs with fees (fees are in decimal format, e.g., 0.001 for 0.1%)
        double effectiveBuyCost = buyPrice * (1 + buyFeePercentage);
        double effectiveSellRevenue = sellPrice * (1 - sellFeePercentage);
        
        // Calculate profit percentage
        double netProfitPerUnit = effectiveSellRevenue - effectiveBuyCost;
        double profitPercentage = (netProfitPerUnit / effectiveBuyCost) * 100;
        
        Log.d(TAG, String.format("Basic profit: %.4f%% (buy: %.8f, sell: %.8f, fees: %.4f/%.4f%%)",
                profitPercentage, buyPrice, sellPrice, buyFeePercentage*100, sellFeePercentage*100));
        
        return profitPercentage;
    }
    
    /**
     * Calculate the slippage-adjusted profit with advanced order book analysis
     */
    public static double calculateSlippageAdjustedProfitPercentage(
            double buyPrice, double sellPrice, 
            double buyFeePercentage, double sellFeePercentage,
            double amount, OrderBook buyOrderBook, OrderBook sellOrderBook,
            String baseAsset) {
        
        // Get market volatility
        MarketVolatility volatility = estimateMarketVolatility(
                baseAsset, 
                buyOrderBook != null ? buyOrderBook.getTicker() : null, 
                sellOrderBook != null ? sellOrderBook.getTicker() : null);
        
        // Perform a more sophisticated slippage analysis using a simulated order
        // that accounts for market depth and recent trade activity
        Pair<Double, Double> buySlippageInfo = simulateMarketBuySlippage(
                buyOrderBook, 
                amount, 
                baseAsset,
                volatility,
                buyOrderBook != null ? buyOrderBook.getExchangeName() : "unknown",
                buyOrderBook != null ? buyOrderBook.getTicker() : null);
                
        Pair<Double, Double> sellSlippageInfo = simulateMarketSellSlippage(
                sellOrderBook, 
                amount, 
                baseAsset,
                volatility,
                sellOrderBook != null ? sellOrderBook.getExchangeName() : "unknown",
                sellOrderBook != null ? sellOrderBook.getTicker() : null);
        
        double buySlippage = buySlippageInfo.first;
        double sellSlippage = sellSlippageInfo.first;
        double buyFillRate = buySlippageInfo.second;
        double sellFillRate = sellSlippageInfo.second;
        
        // Adjust prices for slippage
        double adjustedBuyPrice = buyPrice * (1 + buySlippage);
        double adjustedSellPrice = sellPrice * (1 - sellSlippage);
        
        // Calculate adjusted profit
        double slippageAdjustedProfit = calculateBasicProfitPercentage(
                adjustedBuyPrice, adjustedSellPrice, buyFeePercentage, sellFeePercentage);
        
        // Apply a liquidity risk factor if the fill rate is low
        double fillRiskFactor = Math.min(buyFillRate, sellFillRate);
        if (fillRiskFactor < 0.95) {
            // Penalize opportunities with <95% expected fill
            double confidenceAdjustment = 1.0 - ((1.0 - fillRiskFactor) * 2.0);
            slippageAdjustedProfit *= Math.max(0.5, confidenceAdjustment);
            
            Log.d(TAG, String.format("Fill risk adjustment: %.2f%% fill rate, factor: %.2f",
                    fillRiskFactor*100, confidenceAdjustment));
        }
        
        Log.d(TAG, String.format("Slippage-adjusted profit: %.4f%% (buy slip: %.4f%%, sell slip: %.4f%%)",
                slippageAdjustedProfit, buySlippage*100, sellSlippage*100));
        
        return slippageAdjustedProfit;
    }
    
    /**
     * Simulate actual market buy order execution to estimate slippage
     * Enhanced with historical data, market volatility, and temporal factors
     * 
     * @return Pair of (slippage percentage, expected fill rate)
     */
    private static Pair<Double, Double> simulateMarketBuySlippage(
            OrderBook orderBook, double amount, String baseAsset,
            MarketVolatility volatility, String exchangeName, Ticker ticker) {
        
        // Handle null or empty order book case
        if (orderBook == null || orderBook.getAsks() == null || orderBook.getAsks().isEmpty()) {
            return new Pair<>(DEFAULT_SLIPPAGE_FACTOR, 0.5); // Default with low confidence
        }
        
        // Get historical slippage data if available
        String slippageKey = exchangeName.toLowerCase() + "_" + baseAsset.toUpperCase() + "_buy";
        SlippageHistory history = SLIPPAGE_HISTORY_CACHE.get(slippageKey);
        
        // Start with base slippage from multiple factors
        double baseSlippage = DEFAULT_SLIPPAGE_FACTOR;
        double liquidityFactor = ASSET_LIQUIDITY_FACTORS.getOrDefault(baseAsset.toUpperCase(), 0.75);
        double exchangeFactor = EXCHANGE_SLIPPAGE_FACTORS.getOrDefault(exchangeName.toLowerCase(), 1.0);
        
        // Apply historical data if available
        double historyConfidence = 0.0;
        if (history != null) {
            double historicalSlippage = history.getPredictedSlippage();
            historyConfidence = history.getConfidence();
            
            // Blend historical data with base slippage based on confidence
            baseSlippage = (historicalSlippage * historyConfidence) + 
                           (baseSlippage * (1.0 - historyConfidence));
        }
        
        // Apply time-of-day factor
        int currentHour = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).getHour();
        double timeOfDayFactor = MARKET_HOURS_SLIPPAGE_FACTORS.getOrDefault(currentHour, 1.0);
        
        // Apply volatility factor (higher volatility = higher slippage)
        double volatilityFactor;
        switch (volatility) {
            case VERY_LOW: volatilityFactor = 0.8; break;
            case LOW: volatilityFactor = 0.9; break;
            case MEDIUM: volatilityFactor = 1.0; break;
            case HIGH: volatilityFactor = 1.3; break;
            case VERY_HIGH: volatilityFactor = 1.8; break;
            default: volatilityFactor = 1.0;
        }
        
        // Apply recent price movement factor if ticker available
        double momentumFactor = 1.0;
        if (ticker != null && ticker.getLastPrice() > 0 && ticker.getOpenPrice() > 0) {
            // Calculate price change percentage
            double priceChangePercent = (ticker.getLastPrice() - ticker.getOpenPrice()) / ticker.getOpenPrice() * 100.0;
            // Rapid price movement in either direction increases slippage
            double absChange = Math.abs(priceChangePercent);
            if (absChange > 5.0) {
                momentumFactor = 1.5; // Significant movement
            } else if (absChange > 2.0) {
                momentumFactor = 1.2; // Moderate movement
            } else if (absChange > 1.0) {
                momentumFactor = 1.1; // Slight movement
            }
        }
        
        // Apply all factors to base slippage
        double adjustedBaseSlippage = baseSlippage * 
                                     exchangeFactor * 
                                     timeOfDayFactor * 
                                     volatilityFactor * 
                                     momentumFactor;
        
        // Calculate weighted average price with actual order simulation
        double totalCost = 0;
        double totalAmount = 0;
        double remainingAmount = amount;
        double bestPrice = orderBook.getAsks().get(0).getPrice();
        
        // Enhanced simulation with more precise order book analysis
        for (int i = 0; i < Math.min(MAX_ORDER_BOOK_DEPTH, orderBook.getAsks().size()); i++) {
            OrderBookEntry entry = orderBook.getAsks().get(i);
            double levelPrice = entry.getPrice();
            double levelQuantity = entry.getVolume();
            
            if (remainingAmount <= levelQuantity) {
                totalCost += levelPrice * remainingAmount;
                totalAmount += remainingAmount;
                remainingAmount = 0;
                break;
            } else {
                totalCost += levelPrice * levelQuantity;
                totalAmount += levelQuantity;
                remainingAmount -= levelQuantity;
            }
        }
        
        // Calculate fill rate (how much of order would be filled)
        double fillRate = 1.0;
        if (remainingAmount > 0) {
            fillRate = totalAmount / amount;
            
            // Add penalty for large orders with low fill rates
            if (fillRate < 0.9) {
                // Large unfilled portions lead to higher slippage estimates
                double unfillPenalty = (1.0 - fillRate) * 2.0;
                adjustedBaseSlippage *= (1.0 + unfillPenalty);
            }
        }
        
        // Calculate slippage based on average execution price
        double slippage;
        if (totalAmount == 0) {
            slippage = adjustedBaseSlippage / liquidityFactor; // Higher slippage for lower liquidity
        } else {
            double avgPrice = totalCost / totalAmount;
            slippage = (avgPrice - bestPrice) / bestPrice;
            
            // Apply asset liquidity factor and ensure reasonable bounds
            slippage = slippage / liquidityFactor;
            
            // Blend simulated slippage with adjusted base slippage
            slippage = slippage * 0.7 + adjustedBaseSlippage * 0.3;
        }
        
        // Ensure reasonable bounds for slippage (more flexible upper bound for extreme conditions)
        double maxSlippage = (volatility == MarketVolatility.VERY_HIGH) ? 0.08 : 0.05;
        slippage = Math.max(adjustedBaseSlippage, Math.min(maxSlippage, slippage));
        
        // Record this observation in history for future estimates
        if (history == null) {
            history = new SlippageHistory();
            SLIPPAGE_HISTORY_CACHE.put(slippageKey, history);
        }
        history.recordSlippage(slippage, amount, fillRate);
        
        // Log detailed slippage calculation
        Log.d(TAG, String.format("Buy slippage for %s on %s: %.4f%% (fill rate: %.2f%%, base: %.4f%%, " +
                "exchange: %.2f, time: %.2f, volatility: %.2f, momentum: %.2f, history confidence: %.2f)",
                baseAsset, exchangeName, slippage * 100, fillRate * 100, baseSlippage * 100,
                exchangeFactor, timeOfDayFactor, volatilityFactor, momentumFactor, historyConfidence));
        
        return new Pair<>(slippage, fillRate);
    }
    
    /**
     * Simulate actual market sell order execution to estimate slippage
     * Enhanced with historical data, market volatility, and temporal factors
     * 
     * @return Pair of (slippage percentage, expected fill rate)
     */
    private static Pair<Double, Double> simulateMarketSellSlippage(
            OrderBook orderBook, double amount, String baseAsset,
            MarketVolatility volatility, String exchangeName, Ticker ticker) {
        
        // Handle null or empty order book case
        if (orderBook == null || orderBook.getBids() == null || orderBook.getBids().isEmpty()) {
            return new Pair<>(DEFAULT_SLIPPAGE_FACTOR, 0.5); // Default with low confidence
        }
        
        // Get historical slippage data if available
        String slippageKey = exchangeName.toLowerCase() + "_" + baseAsset.toUpperCase() + "_sell";
        SlippageHistory history = SLIPPAGE_HISTORY_CACHE.get(slippageKey);
        
        // Start with base slippage from multiple factors
        double baseSlippage = DEFAULT_SLIPPAGE_FACTOR;
        double liquidityFactor = ASSET_LIQUIDITY_FACTORS.getOrDefault(baseAsset.toUpperCase(), 0.75);
        double exchangeFactor = EXCHANGE_SLIPPAGE_FACTORS.getOrDefault(exchangeName.toLowerCase(), 1.0);
        
        // Apply historical data if available
        double historyConfidence = 0.0;
        if (history != null) {
            double historicalSlippage = history.getPredictedSlippage();
            historyConfidence = history.getConfidence();
            
            // Blend historical data with base slippage based on confidence
            baseSlippage = (historicalSlippage * historyConfidence) + 
                          (baseSlippage * (1.0 - historyConfidence));
        }
        
        // Apply time-of-day factor
        int currentHour = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).getHour();
        double timeOfDayFactor = MARKET_HOURS_SLIPPAGE_FACTORS.getOrDefault(currentHour, 1.0);
        
        // Apply volatility factor (higher volatility = higher slippage)
        double volatilityFactor;
        switch (volatility) {
            case VERY_LOW: volatilityFactor = 0.8; break;
            case LOW: volatilityFactor = 0.9; break;
            case MEDIUM: volatilityFactor = 1.0; break;
            case HIGH: volatilityFactor = 1.3; break;
            case VERY_HIGH: volatilityFactor = 1.8; break;
            default: volatilityFactor = 1.0;
        }
        
        // Apply recent price movement factor if ticker available
        double momentumFactor = 1.0;
        if (ticker != null && ticker.getLastPrice() > 0 && ticker.getOpenPrice() > 0) {
            // Calculate price change percentage
            double priceChangePercent = (ticker.getLastPrice() - ticker.getOpenPrice()) / ticker.getOpenPrice() * 100.0;
            // Rapid price movement in either direction increases slippage
            double absChange = Math.abs(priceChangePercent);
            if (absChange > 5.0) {
                momentumFactor = 1.5; // Significant movement
            } else if (absChange > 2.0) {
                momentumFactor = 1.2; // Moderate movement
            } else if (absChange > 1.0) {
                momentumFactor = 1.1; // Slight movement
            }
        }
        
        // Apply all factors to base slippage
        double adjustedBaseSlippage = baseSlippage * 
                                     exchangeFactor * 
                                     timeOfDayFactor * 
                                     volatilityFactor * 
                                     momentumFactor;
        
        // Calculate weighted average price with actual order simulation
        double totalRevenue = 0;
        double totalAmount = 0;
        double remainingAmount = amount;
        double bestPrice = orderBook.getBids().get(0).getPrice();
        
        // Enhanced simulation with more precise order book analysis
        for (int i = 0; i < Math.min(MAX_ORDER_BOOK_DEPTH, orderBook.getBids().size()); i++) {
            OrderBookEntry entry = orderBook.getBids().get(i);
            double levelPrice = entry.getPrice();
            double levelQuantity = entry.getVolume();
            
            if (remainingAmount <= levelQuantity) {
                totalRevenue += levelPrice * remainingAmount;
                totalAmount += remainingAmount;
                remainingAmount = 0;
                break;
            } else {
                totalRevenue += levelPrice * levelQuantity;
                totalAmount += levelQuantity;
                remainingAmount -= levelQuantity;
            }
        }
        
        // Calculate fill rate (how much of order would be filled)
        double fillRate = 1.0;
        if (remainingAmount > 0) {
            fillRate = totalAmount / amount;
            
            // Add penalty for large orders with low fill rates
            if (fillRate < 0.9) {
                // Large unfilled portions lead to higher slippage estimates
                double unfillPenalty = (1.0 - fillRate) * 2.0;
                adjustedBaseSlippage *= (1.0 + unfillPenalty);
            }
        }
        
        // Calculate slippage based on average execution price
        double slippage;
        if (totalAmount == 0) {
            slippage = adjustedBaseSlippage / liquidityFactor;
        } else {
            double avgPrice = totalRevenue / totalAmount;
            slippage = (bestPrice - avgPrice) / bestPrice;
            // Apply asset liquidity factor and ensure reasonable bounds
            slippage = slippage / liquidityFactor;
            
            // Blend simulated slippage with adjusted base slippage
            slippage = slippage * 0.7 + adjustedBaseSlippage * 0.3;
        }
        
        // Ensure reasonable bounds for slippage (more flexible upper bound for extreme conditions)
        double maxSlippage = (volatility == MarketVolatility.VERY_HIGH) ? 0.08 : 0.05;
        slippage = Math.max(adjustedBaseSlippage, Math.min(maxSlippage, slippage));
        
        // Record this observation in history for future estimates
        if (history == null) {
            history = new SlippageHistory();
            SLIPPAGE_HISTORY_CACHE.put(slippageKey, history);
        }
        history.recordSlippage(slippage, amount, fillRate);
        
        // Log detailed slippage calculation
        Log.d(TAG, String.format("Sell slippage for %s on %s: %.4f%% (fill rate: %.2f%%, base: %.4f%%, " +
                "exchange: %.2f, time: %.2f, volatility: %.2f, momentum: %.2f, history confidence: %.2f)",
                baseAsset, exchangeName, slippage * 100, fillRate * 100, baseSlippage * 100,
                exchangeFactor, timeOfDayFactor, volatilityFactor, momentumFactor, historyConfidence));
        
        return new Pair<>(slippage, fillRate);
    }
    
    /**
     * Estimate market volatility based on ticker data
     */
    private static MarketVolatility estimateMarketVolatility(
            String baseAsset, Ticker buyTicker, Ticker sellTicker) {
        
        // Check cache first
        if (volatilityCache.containsKey(baseAsset)) {
            return volatilityCache.get(baseAsset);
        }
        
        // If tickers are available, calculate volatility based on high/low ranges
        MarketVolatility result = MarketVolatility.MEDIUM; // Default
        
        if (buyTicker != null && sellTicker != null) {
            try {
                // Calculate average volatility from both tickers
                double buyVolatility = calculateTickerVolatility(buyTicker);
                double sellVolatility = calculateTickerVolatility(sellTicker);
                double avgVolatility = (buyVolatility + sellVolatility) / 2.0;
                
                // Classify volatility
                result = classifyVolatility(avgVolatility);
                
                // Cache the result (would be periodically refreshed in production)
                volatilityCache.put(baseAsset, result);
                
                Log.d(TAG, String.format("Estimated volatility for %s: %s (%.2f%%)", 
                        baseAsset, result, avgVolatility));
                
            } catch (Exception e) {
                Log.e(TAG, "Error calculating volatility", e);
            }
        }
        
        return result;
    }
    
    /**
     * Calculate volatility percentage from ticker data
     */
    private static double calculateTickerVolatility(Ticker ticker) {
        if (ticker.getHighPrice() > 0 && ticker.getLowPrice() > 0) {
            // Calculate high/low range as percentage
            return ((ticker.getHighPrice() - ticker.getLowPrice()) / ticker.getLowPrice()) * 100.0;
        } else {
            return 5.0; // Default 5% if no high/low data
        }
    }
    
    /**
     * Classify volatility level based on percentage
     */
    private static MarketVolatility classifyVolatility(double volatilityPercentage) {
        if (volatilityPercentage < VOLATILITY_THRESHOLDS[0]) {
            return MarketVolatility.VERY_LOW;
        } else if (volatilityPercentage < VOLATILITY_THRESHOLDS[1]) {
            return MarketVolatility.LOW;
        } else if (volatilityPercentage < VOLATILITY_THRESHOLDS[2]) {
            return MarketVolatility.MEDIUM;
        } else if (volatilityPercentage < VOLATILITY_THRESHOLDS[3]) {
            return MarketVolatility.HIGH;
        } else {
            return MarketVolatility.VERY_HIGH;
        }
    }
    
    /**
     * Estimate liquidity factor based on order books and asset type
     */
    private static double estimateLiquidityFactor(String baseAsset, OrderBook buyOrderBook, OrderBook sellOrderBook) {
        // Start with the base asset liquidity factor
        double baseLiquidityFactor = ASSET_LIQUIDITY_FACTORS.getOrDefault(baseAsset.toUpperCase(), 0.75);
        
        // Calculate order book depth factors
        double buyDepthFactor = calculateOrderBookDepthFactor(buyOrderBook);
        double sellDepthFactor = calculateOrderBookDepthFactor(sellOrderBook);
        
        // Calculate spread factor (tighter spread = better liquidity)
        double spreadFactor = calculateSpreadFactor(buyOrderBook, sellOrderBook);
        
        // Combine factors with weighted importance
        double combinedLiquidityFactor = (baseLiquidityFactor * 0.4) + 
                                        (buyDepthFactor * 0.25) + 
                                        (sellDepthFactor * 0.25) +
                                        (spreadFactor * 0.1);
        
        // Ensure the factor is between 0.1 and 1.0
        double finalFactor = Math.max(0.1, Math.min(1.0, combinedLiquidityFactor));
        
        Log.d(TAG, String.format("Liquidity factor for %s: %.2f (base: %.2f, depth: %.2f/%.2f, spread: %.2f)",
                baseAsset, finalFactor, baseLiquidityFactor, buyDepthFactor, sellDepthFactor, spreadFactor));
        
        return finalFactor;
    }
    
    /**
     * Calculate a factor representing the depth of an order book
     */
    private static double calculateOrderBookDepthFactor(OrderBook orderBook) {
        if (orderBook == null) {
            return 0.5; // Neutral value if no order book
        }
        
        // Calculate total volume in the order book
        double totalVolume = 0;
        
        if (orderBook.getBids() != null) {
            for (int i = 0; i < Math.min(MAX_ORDER_BOOK_DEPTH, orderBook.getBids().size()); i++) {
                totalVolume += orderBook.getBids().get(i).getVolume();
            }
        }
        
        if (orderBook.getAsks() != null) {
            for (int i = 0; i < Math.min(MAX_ORDER_BOOK_DEPTH, orderBook.getAsks().size()); i++) {
                totalVolume += orderBook.getAsks().get(i).getVolume();
            }
        }
        
        // Normalize with log scale to handle wide range of volumes
        if (totalVolume <= 0) {
            return 0.5;
        }
        
        return Math.min(1.0, Math.max(0.1, Math.log10(1 + totalVolume) / 5.0));
    }
    
    /**
     * Calculate spread factor (tighter spread = better liquidity)
     */
    private static double calculateSpreadFactor(OrderBook buyOrderBook, OrderBook sellOrderBook) {
        if (buyOrderBook == null || sellOrderBook == null || 
            buyOrderBook.getBids().isEmpty() || sellOrderBook.getAsks().isEmpty()) {
            return 0.5; // Neutral value if data is missing
        }
        
        try {
            double buyBestBid = buyOrderBook.getBids().get(0).getPrice();
            double sellBestAsk = sellOrderBook.getAsks().get(0).getPrice();
            
            if (buyBestBid <= 0 || sellBestAsk <= 0) {
                return 0.5;
            }
            
            // Use mid price as reference
            double midPrice = (buyBestBid + sellBestAsk) / 2.0;
            
            // Calculate relative spread in percentage
            double spreadPercentage = Math.abs(sellBestAsk - buyBestBid) / midPrice * 100.0;
            
            // Tighter spread = higher factor (inverse relationship)
            // Spread of 0.1% or less is excellent (factor = 1.0)
            // Spread of 2% or more is poor (factor = 0.1)
            if (spreadPercentage <= 0.1) {
                return 1.0;
            } else if (spreadPercentage >= 2.0) {
                return 0.1;
            } else {
                // Linear interpolation between 0.1% and 2%
                return 1.0 - (spreadPercentage - 0.1) / 1.9 * 0.9;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating spread factor", e);
            return 0.5;
        }
    }
    
    /**
     * Estimate the transfer cost as a percentage of the trade amount
     */
    private static double estimateTransferCostPercentage(String asset, double amount) {
        // Default cost estimates for different asset types
        Map<String, Double> fixedCosts = new HashMap<String, Double>() {{
            put("BTC", 0.0005);  // BTC has relatively higher withdrawal fees
            put("ETH", 0.005);   // ETH has high gas costs
            put("SOL", 0.01);    // SOL has low fees
            put("XRP", 0.02);    // XRP has very low fees
            put("USDT", 10.0);   // USDT often has flat fees around $10
            put("USDC", 10.0);   // USDC similar to USDT
        }};
        
        String normalizedAsset = asset.toUpperCase();
        double fixedCost = fixedCosts.getOrDefault(normalizedAsset, 0.01);
        
        // Calculate percentage cost
        return (fixedCost / amount) * 100;
    }
    
    /**
     * Get exchange risk score (lower is better)
     */
    private static double getExchangeRiskScore(String exchangeName) {
        String normalized = exchangeName.toLowerCase();
        return exchangeRiskScores.getOrDefault(normalized, 0.40);
    }
    
    /**
     * Combine risk scores from two exchanges
     */
    private static double combineExchangeRisk(double buyRisk, double sellRisk) {
        // Higher weight (0.6) to higher risk exchange
        return Math.max(buyRisk, sellRisk) * 0.6 + Math.min(buyRisk, sellRisk) * 0.4;
    }
    
    /**
     * Calculate arbitrage profit using the correct formula that accounts for fees
     * properly on both buy and sell sides.
     * 
     * @param buyPrice The price at which the asset is bought
     * @param sellPrice The price at which the asset is sold
     * @param buyFeePercentage Buy fee as a decimal (e.g., 0.001 for 0.1%)
     * @param sellFeePercentage Sell fee as a decimal (e.g., 0.001 for 0.1%)
     * @param amount Amount of the asset being traded
     * @return A profit calculation result containing absolute and percentage profits
     */
    public static com.example.tradient.domain.profit.ProfitResult calculateArbitrageProfit(
            double buyPrice, 
            double sellPrice, 
            double buyFeePercentage, 
            double sellFeePercentage, 
            double amount) {
        
        // Fees are expected to be in decimal format (e.g., 0.001 for 0.1%)
        // No conversion needed as IExchangeService.getFeePercentage() already returns fees in decimal format
        
        // Calculate effective buy cost (including fees)
        double effectiveBuyCost = buyPrice * (1 + buyFeePercentage);
        
        // Calculate effective sell revenue (after fees)
        double effectiveSellRevenue = sellPrice * (1 - sellFeePercentage);
        
        // Calculate net profit per unit
        double netProfitPerUnit = effectiveSellRevenue - effectiveBuyCost;
        
        // Calculate absolute profit
        double absoluteProfit = netProfitPerUnit * amount;
        
        // Calculate percentage profit (relative to investment)
        double percentageProfit = (netProfitPerUnit / effectiveBuyCost) * 100;
        
        // Log the calculation details for debugging
        Log.d(TAG, String.format(
                "Profit calculation: Buy=%f(+%f%%), Sell=%f(-%f%%), EffBuy=%f, EffSell=%f, Net=%f, Profit=%f(%f%%)",
                buyPrice, buyFeePercentage*100, sellPrice, sellFeePercentage*100,
                effectiveBuyCost, effectiveSellRevenue, netProfitPerUnit,
                absoluteProfit, percentageProfit));
        
        return new com.example.tradient.domain.profit.ProfitResult(absoluteProfit, percentageProfit, netProfitPerUnit);
    }
    
    /**
     * Calculate a comprehensive arbitrage opportunity evaluation
     * with all factors considered: time, slippage, market conditions
     */
    public static ArbitrageMetrics calculateFullyAdjustedMetrics(
            IExchangeService buyExchange, IExchangeService sellExchange,
            String tradingPair, double amount, double buyPrice, double sellPrice) {
        
        // Get exchange information
        String buyExchangeName = buyExchange.getExchangeName();
        String sellExchangeName = sellExchange.getExchangeName();
        double buyFeePercentage = buyExchange.getFeePercentage(tradingPair, true);
        double sellFeePercentage = sellExchange.getFeePercentage(tradingPair, false);
        
        // Get order books
        OrderBook buyOrderBook = buyExchange.getOrderBook(tradingPair);
        OrderBook sellOrderBook = sellExchange.getOrderBook(tradingPair);
        
        // Extract base asset from trading pair
        String baseAsset = tradingPair.split("/")[0];
        
        // Gather tickers for volatility assessment
        Ticker buyTicker = buyExchange.getTicker(tradingPair);
        Ticker sellTicker = sellExchange.getTicker(tradingPair);
        
        // Estimate current market volatility
        MarketVolatility volatility = estimateMarketVolatility(baseAsset, buyTicker, sellTicker);
        
        // Calculate profit adjusted for slippage
        double slippageAdjustedProfit = calculateSlippageAdjustedProfitPercentage(
                buyPrice, sellPrice, buyFeePercentage, sellFeePercentage,
                amount, buyOrderBook, sellOrderBook, baseAsset);
        
        // Estimate liquidity for time calculations
        double liquidityFactor = estimateLiquidityFactor(baseAsset, buyOrderBook, sellOrderBook);
        
        // Estimate time for the complete arbitrage operation
        Pair<Double, Double> timeEstimate = TimeEstimationUtil.estimateArbitrageTimeMinutes(
                buyExchangeName, 
                sellExchangeName,
                buyTicker,
                sellTicker,
                buyOrderBook,
                sellOrderBook,
                amount,
                volatility,
                baseAsset);
        
        double estimatedTimeMinutes = timeEstimate.first;
        double timeUncertainty = timeEstimate.second;
        
        // Calculate time-adjusted profit
        double timeAdjustedProfit = TimeEstimationUtil.calculateTimeAdjustedProfit(
                slippageAdjustedProfit, estimatedTimeMinutes, volatility);
        
        // Calculate transfer costs if cross-exchange (for crypto-to-crypto arbitrage)
        double transferCostPercentage = 0.0;
        if (!buyExchangeName.equals(sellExchangeName)) {
            transferCostPercentage = estimateTransferCostPercentage(baseAsset, amount);
        }
        
        // Apply transfer costs
        double finalProfitPercentage = timeAdjustedProfit - transferCostPercentage;
        
        // Calculate additional metrics
        double annualizedReturn = TimeEstimationUtil.calculateAnnualizedReturn(
                finalProfitPercentage, estimatedTimeMinutes);
        
        double roiEfficiency = TimeEstimationUtil.calculateROIEfficiency(
                finalProfitPercentage, estimatedTimeMinutes);
        
        double riskAdjustedReturn = TimeEstimationUtil.calculateRiskAdjustedReturn(
                finalProfitPercentage, estimatedTimeMinutes, volatility);
        
        // Calculate combined exchange risk
        double exchangeRisk = combineExchangeRisk(
                getExchangeRiskScore(buyExchangeName), 
                getExchangeRiskScore(sellExchangeName));
        
        // Calculate opportunity score
        double opportunityScore = TimeEstimationUtil.calculateOpportunityScore(
                finalProfitPercentage, estimatedTimeMinutes, volatility, liquidityFactor, exchangeRisk);
        
        // Check if opportunity is viable
        boolean isViable = finalProfitPercentage > MIN_VIABLE_PROFIT;
        
        // Create metrics object with all calculations
        return new ArbitrageMetrics.Builder()
                .setRawProfit(calculateBasicProfitPercentage(
                        buyPrice, sellPrice, buyFeePercentage, sellFeePercentage))
                .setSlippageAdjustedProfit(slippageAdjustedProfit)
                .setTimeAdjustedProfit(timeAdjustedProfit)
                .setFinalProfit(finalProfitPercentage)
                .setEstimatedTimeMinutes(estimatedTimeMinutes)
                .setTimeUncertainty(timeUncertainty)
                .setAnnualizedReturn(annualizedReturn)
                .setRoiEfficiency(roiEfficiency)
                .setRiskAdjustedReturn(riskAdjustedReturn)
                .setOpportunityScore(opportunityScore)
                .setLiquidityFactor(liquidityFactor)
                .setVolatility(volatility)
                .setExchangeRisk(exchangeRisk)
                .setIsViable(isViable)
                .build();
    }
    
    /**
     * Helper class to track historical slippage data for adaptive slippage estimation
     */
    private static class SlippageHistory {
        private static final int MAX_HISTORY_SIZE = 50;
        private static final double RECENT_WEIGHT = 0.7; // Weight given to recent data vs historical average
        
        private final LinkedList<SlippageRecord> history = new LinkedList<>();
        private double averageSlippage = DEFAULT_SLIPPAGE_FACTOR;
        private double shortTermSlippage = DEFAULT_SLIPPAGE_FACTOR;
        private long lastUpdateTimestamp = 0;
        
        /**
         * Record a new slippage observation
         */
        public void recordSlippage(double slippage, double amount, double fillRate) {
            // Add new record
            SlippageRecord record = new SlippageRecord(
                    System.currentTimeMillis(), slippage, amount, fillRate);
            history.addLast(record);
            
            // Trim history if needed
            if (history.size() > MAX_HISTORY_SIZE) {
                history.removeFirst();
            }
            
            // Update averages
            updateAverages();
        }
        
        /**
         * Get the predicted slippage based on historical data
         */
        public double getPredictedSlippage() {
            // Check if data is stale (older than 1 hour)
            if (System.currentTimeMillis() - lastUpdateTimestamp > 3600000) {
                return DEFAULT_SLIPPAGE_FACTOR; // Use default if stale
            }
            
            // Blend long-term and short-term averages
            return (shortTermSlippage * RECENT_WEIGHT) + 
                   (averageSlippage * (1 - RECENT_WEIGHT));
        }
        
        /**
         * Get the confidence level in the prediction (0-1)
         */
        public double getConfidence() {
            // More data points = higher confidence, up to a maximum
            return Math.min(0.9, history.size() / (double)MAX_HISTORY_SIZE);
        }
        
        /**
         * Update average slippage values
         */
        private void updateAverages() {
            if (history.isEmpty()) {
                return;
            }
            
            // Calculate overall average
            double total = 0;
            for (SlippageRecord record : history) {
                total += record.slippage;
            }
            averageSlippage = total / history.size();
            
            // Calculate short-term average (last 10 records)
            int recentCount = Math.min(10, history.size());
            double recentTotal = 0;
            for (int i = history.size() - 1; i >= history.size() - recentCount; i--) {
                recentTotal += history.get(i).slippage;
            }
            shortTermSlippage = recentTotal / recentCount;
            
            // Update timestamp
            lastUpdateTimestamp = System.currentTimeMillis();
        }
        
        /**
         * Inner class to hold individual slippage records
         */
        private static class SlippageRecord {
            public final long timestamp;
            public final double slippage;
            public final double amount;
            public final double fillRate;
            
            public SlippageRecord(long timestamp, double slippage, double amount, double fillRate) {
                this.timestamp = timestamp;
                this.slippage = slippage;
                this.amount = amount;
                this.fillRate = fillRate;
            }
        }
    }
    
    /**
     * Class to hold comprehensive arbitrage metrics calculation results
     */
    public static class ArbitrageMetrics {
        private final double rawProfit;
        private final double slippageAdjustedProfit;
        private final double timeAdjustedProfit;
        private final double finalProfit;
        private final double estimatedTimeMinutes;
        private final double timeUncertainty;
        private final double annualizedReturn;
        private final double roiEfficiency;
        private final double riskAdjustedReturn;
        private final double opportunityScore;
        private final double liquidityFactor;
        private final MarketVolatility volatility;
        private final double exchangeRisk;
        private final boolean isViable;
        
        private ArbitrageMetrics(Builder builder) {
            this.rawProfit = builder.rawProfit;
            this.slippageAdjustedProfit = builder.slippageAdjustedProfit;
            this.timeAdjustedProfit = builder.timeAdjustedProfit;
            this.finalProfit = builder.finalProfit;
            this.estimatedTimeMinutes = builder.estimatedTimeMinutes;
            this.timeUncertainty = builder.timeUncertainty;
            this.annualizedReturn = builder.annualizedReturn;
            this.roiEfficiency = builder.roiEfficiency;
            this.riskAdjustedReturn = builder.riskAdjustedReturn;
            this.opportunityScore = builder.opportunityScore;
            this.liquidityFactor = builder.liquidityFactor;
            this.volatility = builder.volatility;
            this.exchangeRisk = builder.exchangeRisk;
            this.isViable = builder.isViable;
        }
        
        public double getRawProfit() {
            return rawProfit;
        }
        
        public double getSlippageAdjustedProfit() {
            return slippageAdjustedProfit;
        }
        
        public double getTimeAdjustedProfit() {
            return timeAdjustedProfit;
        }
        
        public double getFinalProfit() {
            return finalProfit;
        }
        
        public double getEstimatedTimeMinutes() {
            return estimatedTimeMinutes;
        }
        
        public double getTimeUncertainty() {
            return timeUncertainty;
        }
        
        public double getAnnualizedReturn() {
            return annualizedReturn;
        }
        
        public double getRoiEfficiency() {
            return roiEfficiency;
        }
        
        public double getRiskAdjustedReturn() {
            return riskAdjustedReturn;
        }
        
        public double getOpportunityScore() {
            return opportunityScore;
        }
        
        public double getLiquidityFactor() {
            return liquidityFactor;
        }
        
        public MarketVolatility getVolatility() {
            return volatility;
        }
        
        public double getExchangeRisk() {
            return exchangeRisk;
        }
        
        public boolean isViable() {
            return isViable;
        }
        
        public String getFormattedTimeEstimate() {
            return TimeEstimationUtil.formatTimeString(estimatedTimeMinutes);
        }
        
        public String getFormattedROIEfficiency() {
            return TimeEstimationUtil.formatROIEfficiency(finalProfit, estimatedTimeMinutes);
        }
        
        public static class Builder {
            private double rawProfit = 0.0;
            private double slippageAdjustedProfit = 0.0;
            private double timeAdjustedProfit = 0.0;
            private double finalProfit = 0.0;
            private double estimatedTimeMinutes = 0.0;
            private double timeUncertainty = 0.0;
            private double annualizedReturn = 0.0;
            private double roiEfficiency = 0.0;
            private double riskAdjustedReturn = 0.0;
            private double opportunityScore = 0.0;
            private double liquidityFactor = 0.0;
            private MarketVolatility volatility = MarketVolatility.MEDIUM;
            private double exchangeRisk = 0.0;
            private boolean isViable = false;
            
            public Builder setRawProfit(double rawProfit) {
                this.rawProfit = rawProfit;
                return this;
            }
            
            public Builder setSlippageAdjustedProfit(double slippageAdjustedProfit) {
                this.slippageAdjustedProfit = slippageAdjustedProfit;
                return this;
            }
            
            public Builder setTimeAdjustedProfit(double timeAdjustedProfit) {
                this.timeAdjustedProfit = timeAdjustedProfit;
                return this;
            }
            
            public Builder setFinalProfit(double finalProfit) {
                this.finalProfit = finalProfit;
                return this;
            }
            
            public Builder setEstimatedTimeMinutes(double estimatedTimeMinutes) {
                this.estimatedTimeMinutes = estimatedTimeMinutes;
                return this;
            }
            
            public Builder setTimeUncertainty(double timeUncertainty) {
                this.timeUncertainty = timeUncertainty;
                return this;
            }
            
            public Builder setAnnualizedReturn(double annualizedReturn) {
                this.annualizedReturn = annualizedReturn;
                return this;
            }
            
            public Builder setRoiEfficiency(double roiEfficiency) {
                this.roiEfficiency = roiEfficiency;
                return this;
            }
            
            public Builder setRiskAdjustedReturn(double riskAdjustedReturn) {
                this.riskAdjustedReturn = riskAdjustedReturn;
                return this;
            }
            
            public Builder setOpportunityScore(double opportunityScore) {
                this.opportunityScore = opportunityScore;
                return this;
            }
            
            public Builder setLiquidityFactor(double liquidityFactor) {
                this.liquidityFactor = liquidityFactor;
                return this;
            }
            
            public Builder setVolatility(MarketVolatility volatility) {
                this.volatility = volatility;
                return this;
            }
            
            public Builder setExchangeRisk(double exchangeRisk) {
                this.exchangeRisk = exchangeRisk;
                return this;
            }
            
            public Builder setIsViable(boolean isViable) {
                this.isViable = isViable;
                return this;
            }
            
            public ArbitrageMetrics build() {
                return new ArbitrageMetrics(this);
            }
        }
    }
    
    /**
     * Represents the result of a profit calculation
     * INTERNAL USE ONLY - For external use, see the standalone ProfitResult class
     */
    public static class InternalProfitResult {
        private final double absoluteProfit;
        private final double percentageProfit;
        private final double netProfitPerUnit;
        
        public InternalProfitResult(double absoluteProfit, double percentageProfit, double netProfitPerUnit) {
            this.absoluteProfit = absoluteProfit;
            this.percentageProfit = percentageProfit;
            this.netProfitPerUnit = netProfitPerUnit;
        }
        
        public double getAbsoluteProfit() {
            return absoluteProfit;
        }
        
        public double getPercentageProfit() {
            return percentageProfit;
        }
        
        public double getNetProfitPerUnit() {
            return netProfitPerUnit;
        }
    }

    /**
     * Calculate profit adjusted for slippage
     * 
     * @param buyPrice The base buy price
     * @param sellPrice The base sell price
     * @param buySlippage The buy slippage as a decimal (e.g., 0.005 for 0.5%)
     * @param sellSlippage The sell slippage as a decimal (e.g., 0.005 for 0.5%)
     * @param buyFee The buy fee as a decimal (e.g., 0.001 for 0.1%)
     * @param sellFee The sell fee as a decimal (e.g., 0.001 for 0.1%)
     * @return The adjusted profit percentage
     */
    public static double calculateSlippageAdjustedProfit(
            double buyPrice, double sellPrice, 
            double buySlippage, double sellSlippage,
            double buyFee, double sellFee) {
        
        // Adjust prices for slippage
        double adjustedBuyPrice = buyPrice * (1 + buySlippage);
        double adjustedSellPrice = sellPrice * (1 - sellSlippage);
        
        // Calculate profit with slippage-adjusted prices
        double amount = 1.0; // Use 1.0 as a reference amount
        
        // Calculate buy cost with fee
        double buyCost = adjustedBuyPrice * amount * (1 + buyFee);
        
        // Calculate sell revenue with fee
        double sellRevenue = adjustedSellPrice * amount * (1 - sellFee);
        
        // Calculate absolute profit
        double absoluteProfit = sellRevenue - buyCost;
        
        // Calculate percentage profit relative to buy cost
        double percentageProfit = (absoluteProfit / buyCost) * 100;
        
        return percentageProfit;
    }

    /**
     * Calculate arbitrage profit with comprehensive fee consideration.
     * This implements the step-by-step formula approach for arbitrage profit calculation
     * that explicitly accounts for all types of fees in the arbitrage process.
     * 
     * @param initialAmount The starting amount in the base currency
     * @param buyPrice The price to buy at on the first exchange
     * @param sellPrice The price to sell at on the second exchange 
     * @param buyTradingFee Trading fee for buy transaction as a decimal (e.g., 0.001 for 0.1%)
     * @param sellTradingFee Trading fee for sell transaction as a decimal (e.g., 0.001 for 0.1%)
     * @param withdrawalFee Fixed fee for withdrawing from buy exchange (in base currency)
     * @param networkFee Fixed network/blockchain fee (in base currency)
     * @param depositFee Fixed or percentage fee for depositing to sell exchange (in base currency)
     * @return A ProfitResult containing absolute profit, percentage profit, and profit per unit
     */
    public static ProfitResult calculateComprehensiveArbitrageProfit(
            double initialAmount,
            double buyPrice,
            double sellPrice,
            double buyTradingFee,
            double sellTradingFee,
            double withdrawalFee,
            double networkFee,
            double depositFee) {
            
        Log.d(TAG, String.format(
                "Starting comprehensive profit calculation with initial amount: %.8f, buy: %.8f, sell: %.8f",
                initialAmount, buyPrice, sellPrice));
                
        // Step 1: Calculate amount after buy trade including trading fee
        double buyTradeAmount = (initialAmount / buyPrice) * (1 - buyTradingFee);
        Log.d(TAG, String.format("Amount after buy trade (including %.4f%% fee): %.8f", 
                buyTradingFee * 100, buyTradeAmount));
        
        // Step 2: Subtract withdrawal fee (fixed amount in the purchased asset)
        double amountAfterWithdrawal = buyTradeAmount - withdrawalFee;
        Log.d(TAG, String.format("Amount after withdrawal (fee: %.8f): %.8f", 
                withdrawalFee, amountAfterWithdrawal));
        
        // Step 3: Subtract network fee if applicable
        double amountAfterNetwork = amountAfterWithdrawal - networkFee;
        Log.d(TAG, String.format("Amount after network fee (fee: %.8f): %.8f", 
                networkFee, amountAfterNetwork));
        
        // Step 4: Calculate deposit to second exchange (if percentage fee)
        double amountAfterDeposit = amountAfterNetwork - depositFee;
        Log.d(TAG, String.format("Amount after deposit fee (fee: %.8f): %.8f", 
                depositFee, amountAfterDeposit));
        
        // Step 5: Calculate final amount after selling, including sell trading fee
        double finalAmount = (amountAfterDeposit * sellPrice) * (1 - sellTradingFee);
        Log.d(TAG, String.format("Final amount after sell (including %.4f%% fee): %.8f", 
                sellTradingFee * 100, finalAmount));
        
        // Calculate absolute profit
        double absoluteProfit = finalAmount - initialAmount;
        
        // Calculate profit percentage
        double percentageProfit = ((finalAmount / initialAmount) - 1) * 100;
        
        // Calculate profit per unit (rarely used, but maintaining for compatibility)
        double profitPerUnit = absoluteProfit / initialAmount;
        
        Log.d(TAG, String.format(
                "Comprehensive profit calculation result: Initial=%.8f, Final=%.8f, Profit=%.8f (%.4f%%)",
                initialAmount, finalAmount, absoluteProfit, percentageProfit));
        
        return new ProfitResult(absoluteProfit, percentageProfit, profitPerUnit);
    }
    
    /**
     * Estimate withdrawal fee for a given asset
     * 
     * @param asset The asset/currency symbol
     * @param exchange The exchange name
     * @return Estimated withdrawal fee in asset units
     */
    public static double estimateWithdrawalFee(String asset, String exchange) {
        // Default withdrawal fees for common assets on various exchanges
        Map<String, Map<String, Double>> withdrawalFees = new HashMap<String, Map<String, Double>>() {{
            put("BTC", new HashMap<String, Double>() {{
                put("binance", 0.0005);
                put("coinbase", 0.0003);
                put("kraken", 0.0002);
                put("kucoin", 0.0005);
                put("bybit", 0.0006);
                put("default", 0.0005);
            }});
            put("ETH", new HashMap<String, Double>() {{
                put("binance", 0.005);
                put("coinbase", 0.003);
                put("kraken", 0.004);
                put("kucoin", 0.006);
                put("bybit", 0.004);
                put("default", 0.005);
            }});
            put("USDT", new HashMap<String, Double>() {{
                put("binance", 20.0);  // Fixed fee
                put("coinbase", 25.0); // Fixed fee
                put("kraken", 5.0);    // Fixed fee
                put("kucoin", 10.0);   // Fixed fee
                put("bybit", 15.0);    // Fixed fee
                put("default", 20.0);  // Fixed fee
            }});
            // Default for other assets
            put("default", new HashMap<String, Double>() {{
                put("default", 0.001); // 0.1% of asset value as default
            }});
        }};
        
        // Normalize inputs
        String normalizedAsset = asset.toUpperCase();
        String normalizedExchange = exchange.toLowerCase();
        
        // Get asset-specific fee map
        Map<String, Double> assetFees = withdrawalFees.getOrDefault(normalizedAsset, 
                                                                   withdrawalFees.get("default"));
        
        // Get exchange-specific fee or default for this asset
        return assetFees.getOrDefault(normalizedExchange, assetFees.get("default"));
    }
    
    /**
     * Estimate network fee for blockchain transfers
     * 
     * @param asset The asset/currency symbol
     * @return Estimated network fee in asset units
     */
    public static double estimateNetworkFee(String asset) {
        // Default network fees for common assets
        Map<String, Double> networkFees = new HashMap<String, Double>() {{
            put("BTC", 0.0001);  // Bitcoin network fee
            put("ETH", 0.003);   // Ethereum gas cost converted to ETH
            put("SOL", 0.0001);  // Solana network fee
            put("XRP", 0.0001);  // XRP network fee
            put("USDT", 5.0);    // USDT-ERC20 gas cost in USDT
            put("USDC", 5.0);    // USDC-ERC20 gas cost in USDC
        }};
        
        String normalizedAsset = asset.toUpperCase();
        return networkFees.getOrDefault(normalizedAsset, 0.001); // Default 0.1% for unknown assets
    }
} 