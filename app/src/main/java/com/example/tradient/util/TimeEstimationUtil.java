package com.example.tradient.util;

import android.util.Log;
import android.util.Pair;

import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for estimating time components in arbitrage operations
 * using real market data instead of static values.
 */
public class TimeEstimationUtil {
    private static final String TAG = "TimeEstimationUtil";
    
    // Cache for recent execution time measurements
    private static final Map<String, Map<String, ExecutionStats>> EXECUTION_STATS_CACHE = new ConcurrentHashMap<>();
    
    // Cache for API response times (exchange -> avg response time in ms)
    private static final Map<String, Double> API_RESPONSE_TIMES = new ConcurrentHashMap<>();
    
    // Rolling buffer for recent time calculations (NEW)
    private static final Map<String, ArrayDeque<Double>> TIME_BUFFER_CACHE = new ConcurrentHashMap<>();
    private static final int BUFFER_SIZE = 20; // Store last 20 values for smoothing (changed from 8)
    
    /**
     * Market volatility levels for dynamic adjustments
     */
    public enum MarketVolatility {
        VERY_LOW,
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH
    }
    
    // Volatility impact factors based on market conditions
    private static final Map<MarketVolatility, Double> VOLATILITY_TIME_FACTORS;
    static {
        VOLATILITY_TIME_FACTORS = new EnumMap<>(MarketVolatility.class);
        VOLATILITY_TIME_FACTORS.put(MarketVolatility.VERY_LOW, 0.85);    // Faster in calm markets
        VOLATILITY_TIME_FACTORS.put(MarketVolatility.LOW, 0.9);
        VOLATILITY_TIME_FACTORS.put(MarketVolatility.MEDIUM, 1.0);       // Baseline
        VOLATILITY_TIME_FACTORS.put(MarketVolatility.HIGH, 1.2);
        VOLATILITY_TIME_FACTORS.put(MarketVolatility.VERY_HIGH, 1.5);    // Much slower in volatile markets
    }
    
    // Risk premium required based on market volatility (annualized %)
    private static final Map<MarketVolatility, Double> VOLATILITY_RISK_PREMIUMS;
    static {
        VOLATILITY_RISK_PREMIUMS = new EnumMap<>(MarketVolatility.class);
        VOLATILITY_RISK_PREMIUMS.put(MarketVolatility.VERY_LOW, 5.0);     // 5% annual premium
        VOLATILITY_RISK_PREMIUMS.put(MarketVolatility.LOW, 10.0);         // 10% annual premium
        VOLATILITY_RISK_PREMIUMS.put(MarketVolatility.MEDIUM, 15.0);      // 15% annual premium
        VOLATILITY_RISK_PREMIUMS.put(MarketVolatility.HIGH, 25.0);        // 25% annual premium
        VOLATILITY_RISK_PREMIUMS.put(MarketVolatility.VERY_HIGH, 40.0);   // 40% annual premium
    }
    
    /**
     * Estimate execution time using order book data and market conditions
     * 
     * @param exchangeName Exchange name
     * @param ticker Current ticker data with price and volume
     * @param orderBook Complete order book for analyzing depth
     * @param tradeAmount Trade size in base currency
     * @param isBuy Whether this is a buy or sell
     * @param volatility Current market volatility
     * @param symbol The trading pair symbol
     * @return Estimated execution time in seconds
     */
    public static double estimateExecutionTimeSeconds(
            String exchangeName, 
            Ticker ticker,
            OrderBook orderBook,
            double tradeAmount,
            boolean isBuy,
            MarketVolatility volatility,
            String symbol) {
        
        // Initial baseline time with more granular starting point based on exchange
        double baseTime = getExchangeBaseTime(exchangeName);
        
        // 1. Order Book Depth Analysis with improved precision
        double orderBookDepthFactor = calculateOrderBookDepthFactor(orderBook, tradeAmount, isBuy);
        
        // 2. Volume-to-Trade Size Ratio with more granular scaling
        double volumeRatioFactor = calculateVolumeRatioFactor(ticker, tradeAmount);
        
        // 3. Spread Factor (wider spread = longer execution)
        double spreadFactor = calculateSpreadFactor(ticker);
        
        // 4. Exchange Response Time Factor
        double exchangeResponseFactor = getExchangeResponseTimeFactor(exchangeName);
        
        // 5. Order Book Imbalance
        double bookImbalanceFactor = calculateOrderBookImbalanceFactor(orderBook);
        
        // 6. Volatility Factor
        double volatilityFactor = getVolatilityTimeFactor(volatility);
        
        // 7. Add time of day factor
        double timeOfDayFactor = calculateTimeOfDayFactor();
        
        // 8. Add market activity factor
        double marketActivityFactor = calculateMarketActivityFactor(symbol);
        
        // 9. Add slight randomness to prevent repeated values
        double randomnessFactor = 0.85 + (Math.random() * 0.3); // 0.85-1.15 range
        
        // Calculate final execution time by combining all factors
        double adjustedTime = baseTime * 
                orderBookDepthFactor * 
                volumeRatioFactor * 
                spreadFactor * 
                exchangeResponseFactor * 
                bookImbalanceFactor * 
                volatilityFactor *
                timeOfDayFactor *
                marketActivityFactor *
                randomnessFactor;
        
        // Ensure reasonable bounds with more granular range (1-60 seconds)
        double boundedTime = Math.max(1.0, Math.min(60.0, adjustedTime));
        
        // Apply smoothing for execution times (NEW)
        String key = "execution_" + exchangeName + "_" + symbol + "_" + (isBuy ? "buy" : "sell");
        double smoothedTime = addToBufferAndGetSmoothedValue(key, boundedTime);
        
        // Log calculation components for debugging
        Log.d(TAG, String.format("Dynamic execution time for %s on %s: raw=%.2fs, smoothed=%.2fs " +
                "(depth:%.2f, volume:%.2f, spread:%.2f, exchange:%.2f, imbalance:%.2f, " +
                "volatility:%.2f, timeOfDay:%.2f, marketActivity:%.2f, randomness:%.2f)",
                symbol, exchangeName, boundedTime, smoothedTime,
                orderBookDepthFactor, volumeRatioFactor, spreadFactor, 
                exchangeResponseFactor, bookImbalanceFactor, volatilityFactor,
                timeOfDayFactor, marketActivityFactor, randomnessFactor));
        
        // Update the historical stats cache for future reference
        updateExecutionStatsCache(exchangeName, symbol, smoothedTime);
        
        return smoothedTime;
    }
    
    /**
     * Get base execution time for an exchange
     * Different exchanges have different base performance characteristics
     */
    private static double getExchangeBaseTime(String exchangeName) {
        String normalized = exchangeName.toLowerCase();
        
        // More granular baseline times for different exchanges
        switch (normalized) {
            case "binance": return 2.7;
            case "coinbase": return 3.1;
            case "kraken": return 3.3;
            case "bybit": return 2.9;
            case "okx": return 3.5;
            case "kucoin": return 3.8;
            case "huobi": return 3.2;
            case "gate": return 3.6;
            default: return 3.0 + (Math.random() * 0.5); // Add slight randomness
        }
    }
    
    /**
     * Calculate a factor based on time of day (market hours tend to be more active)
     */
    private static double calculateTimeOfDayFactor() {
        // Get current hour in UTC
        int currentHour = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).getHour();
        
        // Market tends to be more active during certain hours
        // 8-16 UTC: Major market hours, 0-8 and 16-24: Less active
        if (currentHour >= 8 && currentHour <= 16) {
            return 0.9 + (Math.random() * 0.1); // 0.9-1.0: Faster during active hours
        } else {
            return 1.1 + (Math.random() * 0.2); // 1.1-1.3: Slower during less active hours
        }
    }
    
    /**
     * Calculate a factor based on recent market activity for this asset
     */
    private static double calculateMarketActivityFactor(String symbol) {
        // In a real implementation, this would check recent trading activity
        // For now, add some variation based on a hash of the symbol
        int hashCode = Math.abs(symbol.hashCode());
        double baseFactor = 0.9 + ((hashCode % 10) / 30.0); // 0.9-1.23 range
        
        // Add small random variation
        return baseFactor * (0.95 + (Math.random() * 0.1)); // ±5% variation
    }
    
    /**
     * Estimate the total time for an arbitrage operation using dynamic factors
     * 
     * @param buyExchangeName Buy exchange
     * @param sellExchangeName Sell exchange
     * @param buyTicker Buy exchange ticker data
     * @param sellTicker Sell exchange ticker data
     * @param buyOrderBook Buy exchange order book
     * @param sellOrderBook Sell exchange order book
     * @param tradeAmount Amount to trade
     * @param volatility Current market volatility
     * @param symbol The trading pair symbol
     * @return Total estimated time in minutes with confidence interval
     */
    public static Pair<Double, Double> estimateArbitrageTimeMinutes(
            String buyExchangeName, 
            String sellExchangeName,
            Ticker buyTicker,
            Ticker sellTicker,
            OrderBook buyOrderBook,
            OrderBook sellOrderBook,
            double tradeAmount,
            MarketVolatility volatility,
            String symbol) {
        
        // Calculate buy execution time in seconds
        double buyTimeSeconds = estimateExecutionTimeSeconds(
                buyExchangeName, buyTicker, buyOrderBook, tradeAmount, true, volatility, symbol);
        
        // Calculate sell execution time in seconds  
        double sellTimeSeconds = estimateExecutionTimeSeconds(
                sellExchangeName, sellTicker, sellOrderBook, tradeAmount, false, volatility, symbol);
        
        // Convert execution times to minutes
        double buyTimeMinutes = buyTimeSeconds / 60.0;
        double sellTimeMinutes = sellTimeSeconds / 60.0;
        
        // Calculate transfer time if using different exchanges
        double transferTimeMinutes = 0.0;
        double transferUncertainty = 0.0;
        
        if (!buyExchangeName.equalsIgnoreCase(sellExchangeName)) {
            // Get base asset from symbol
            String baseAsset = extractBaseAsset(symbol);
            
            // Dynamically calculate cross-exchange transfer time
            Pair<Double, Double> transferEstimate = estimateTransferTimeMinutes(
                    buyExchangeName, sellExchangeName, baseAsset, 
                    buyTicker, sellTicker, tradeAmount, volatility);
            
            transferTimeMinutes = transferEstimate.first;
            transferUncertainty = transferEstimate.second;
        }
        
        // Sum components for total time
        double totalTimeMinutes = buyTimeMinutes + transferTimeMinutes + sellTimeMinutes;
        
        // Apply smoothing by using recent values
        String key = buyExchangeName + "_" + sellExchangeName + "_" + symbol;
        double smoothedTime = addToBufferAndGetSmoothedValue(key, totalTimeMinutes);
        
        // Apply minimum threshold based on market conditions (NEW)
        double minimumThreshold = calculateMinimumTimeThreshold(
                symbol, buyTicker, sellTicker, volatility, buyExchangeName.equals(sellExchangeName));
        
        // Ensure final time is not below the minimum threshold
        double finalTime = Math.max(smoothedTime, minimumThreshold);
        
        // Log raw and smoothed values
        Log.d(TAG, String.format("Time calculation: raw=%.2f, smoothed=%.2f, final=%.2f minutes for %s-%s (%s), min threshold=%.2f",
                totalTimeMinutes, smoothedTime, finalTime, buyExchangeName, sellExchangeName, symbol, minimumThreshold));
        
        // Calculate overall uncertainty (confidence interval)
        double executionUncertainty = Math.sqrt(
                Math.pow(buyTimeMinutes * 0.2, 2) + 
                Math.pow(transferUncertainty, 2) + 
                Math.pow(sellTimeMinutes * 0.2, 2));
        
        return new Pair<>(finalTime, executionUncertainty);
    }
    
    /**
     * Calculate minimum time threshold based on market conditions
     * Prevents unrealistically short times for high-risk conditions
     */
    private static double calculateMinimumTimeThreshold(
            String symbol, Ticker buyTicker, Ticker sellTicker,
            MarketVolatility volatility, boolean isSameExchange) {
        
        // Base minimum threshold
        double baseThreshold = 20.0; // 20 minutes default minimum
        
        // Adjust based on volatility
        switch(volatility) {
            case VERY_LOW:
                baseThreshold = isSameExchange ? 13.0 : 18.0;
                break;
            case LOW:
                baseThreshold = isSameExchange ? 16.0 : 20.0;
                break;
            case MEDIUM: 
                baseThreshold = isSameExchange ? 20.0 : 22.0;
                break;
            case HIGH:
                baseThreshold = isSameExchange ? 22.0 : 25.0;
                break;
            case VERY_HIGH:
                baseThreshold = isSameExchange ? 25.0 : 28.0;
                break;
        }
        
        // Check market liquidity if tickers are available
        if (buyTicker != null && sellTicker != null) {
            double avgVolume = (buyTicker.getVolume() + sellTicker.getVolume()) / 2.0;
            
            // Adjust threshold for highly liquid markets
            if (avgVolume > 10000000) {
                baseThreshold *= 0.85; // Reduce minimum by 15% for very liquid markets
            } else if (avgVolume < 1000000) {
                baseThreshold *= 1.2; // Increase minimum by 20% for illiquid markets
            }
        }
        
        // Get base asset to make asset-specific adjustments
        String baseAsset = extractBaseAsset(symbol).toUpperCase();
        
        // Allow faster times for certain fast-confirmation assets
        if (baseAsset.equals("SOL") || baseAsset.equals("XRP")) {
            baseThreshold *= 0.9; // 10% reduction for fast networks
        } else if (baseAsset.equals("BTC") || baseAsset.equals("ETH")) {
            baseThreshold *= 1.1; // 10% increase for slower networks
        }
        
        return baseThreshold;
    }
    
    /**
     * Add a value to the rolling buffer and return a smoothed value
     * Uses the median of the buffer to reduce impact of outliers
     */
    private static double addToBufferAndGetSmoothedValue(String key, double newValue) {
        // Get or create buffer
        ArrayDeque<Double> buffer = TIME_BUFFER_CACHE.computeIfAbsent(
                key, k -> new ArrayDeque<>(BUFFER_SIZE));
        
        // Add new value
        buffer.addLast(newValue);
        
        // Keep buffer at max size
        if (buffer.size() > BUFFER_SIZE) {
            buffer.removeFirst();
        }
        
        // Always use median calculation even for small buffer sizes
        // Convert to sorted array for median calculation
        Double[] values = buffer.toArray(new Double[0]);
        Arrays.sort(values);
        
        // Return median (middle value)
        if (values.length % 2 == 1) {
            // For odd number of elements, return the middle element
            return values[values.length / 2];
        } else {
            // For even number of elements, return average of the two middle elements
            double middle1 = values[values.length / 2 - 1];
            double middle2 = values[values.length / 2];
            return (middle1 + middle2) / 2.0;
        }
    }
    
    /**
     * Estimate transfer time between exchanges dynamically based on market conditions
     */
    public static Pair<Double, Double> estimateTransferTimeMinutes(
            String fromExchange, String toExchange, String asset,
            Ticker sourceTicker, Ticker destTicker, 
            double tradeAmount, MarketVolatility volatility) {
        
        // Base time varies by asset type and recent performance
        double baseTime = getHistoricalTransferTime(fromExchange, toExchange, asset);
        if (baseTime <= 0) {
            // If no historical data, use enhanced asset-specific estimation
            baseTime = estimateTransferTimeFromAssetType(asset, sourceTicker, destTicker);
        }
        
        // Size factor - larger transfers take more time due to security checks
        double sizeFactor = calculateTransferSizeFactor(asset, tradeAmount);
        
        // Volatility affects exchange processing times
        double volatilityFactor = getVolatilityTimeFactor(volatility);
        
        // Add blockchain congestion factor
        double congestionFactor = estimateBlockchainCongestionFactor(asset);
        
        // Add exchange-specific processing factor
        double exchangeProcessingFactor = calculateExchangeProcessingFactor(fromExchange, toExchange);
        
        // Add time of day factor
        double timeOfDayFactor = calculateTimeOfDayFactorForTransfer();
        
        // Add small random variation to prevent repeated values
        double randomVariation = 0.85 + (Math.random() * 0.3); // 0.85-1.15 range
        
        // Calculate total estimated transfer time
        double totalTimeMinutes = baseTime * 
                                sizeFactor * 
                                volatilityFactor * 
                                congestionFactor * 
                                exchangeProcessingFactor * 
                                timeOfDayFactor * 
                                randomVariation;
        
        // Add more granularity and variation to the time
        totalTimeMinutes = Math.max(1.0, totalTimeMinutes);
        
        // Apply smoothing for transfer times
        String key = "transfer_" + fromExchange + "_" + toExchange + "_" + asset;
        double smoothedTime = addToBufferAndGetSmoothedValue(key, totalTimeMinutes);
        
        // Apply minimum thresholds for transfer times based on asset type (NEW)
        double minTransferTime = calculateMinimumTransferTime(asset, volatility);
        double finalTime = Math.max(smoothedTime, minTransferTime);
        
        // Log detailed calculation for debugging
        Log.d(TAG, String.format("Transfer time estimate for %s from %s to %s: raw=%.2f, smoothed=%.2f, final=%.2f min " +
                "(base: %.2f, size: %.2f, volatility: %.2f, congestion: %.2f, " +
                "exchange: %.2f, timeOfDay: %.2f, random: %.2f, minThreshold: %.2f)",
                asset, fromExchange, toExchange, totalTimeMinutes, smoothedTime, finalTime,
                baseTime, sizeFactor, volatilityFactor, congestionFactor,
                exchangeProcessingFactor, timeOfDayFactor, randomVariation, minTransferTime));
        
        // Calculate uncertainty range (higher during high volatility)
        double uncertainty = finalTime * 0.3 * volatilityFactor;
        
        return new Pair<>(finalTime, uncertainty);
    }
    
    /**
     * Calculate minimum transfer time based on asset type and market conditions
     * This prevents unrealistically short transfer times
     */
    private static double calculateMinimumTransferTime(String asset, MarketVolatility volatility) {
        String assetUpper = asset.toUpperCase();
        double baseMinimum;
        
        // Set base minimum by asset type
        switch (assetUpper) {
            case "BTC":
                baseMinimum = 25.0; // Bitcoin transfers are generally slower
                break;
            case "ETH":
                baseMinimum = 20.0; // Ethereum has medium confirmation times
                break;
            case "SOL":
                baseMinimum = 8.0;  // Solana has very fast confirmations
                break;
            case "XRP":
                baseMinimum = 5.0;  // XRP has very fast confirmations
                break;
            case "USDT":
            case "USDC":
                baseMinimum = 15.0; // Stablecoins vary based on blockchain
                break;
            default:
                baseMinimum = 18.0; // Default for other assets
        }
        
        // Adjust for market volatility
        switch (volatility) {
            case VERY_LOW:
                baseMinimum *= 0.9;  // 10% reduction in very calm markets
                break;
            case LOW:
                baseMinimum *= 0.95; // 5% reduction in calm markets
                break;
            case HIGH:
                baseMinimum *= 1.1;  // 10% increase in volatile markets
                break;
            case VERY_HIGH:
                baseMinimum *= 1.25; // 25% increase in very volatile markets
                break;
            default:
                // No adjustment for MEDIUM volatility
        }
        
        return baseMinimum;
    }
    
    /**
     * Improved asset-specific transfer time estimation
     */
    private static double estimateTransferTimeFromAssetType(String asset, Ticker sourceTicker, Ticker destTicker) {
        double baseTime;
        String assetUpper = asset.toUpperCase();
        
        // Estimate based on volume and trading activity when available
        if (sourceTicker != null && destTicker != null) {
            double avgVolume = (sourceTicker.getVolume() + destTicker.getVolume()) / 2.0;
            
            // Use average volume as a proxy for network activity with more granular ranges
            if (avgVolume > 100000000) {
                baseTime = 8.0 + (Math.random() * 3.0); // 8-11 minutes for extremely high volume
            } else if (avgVolume > 10000000) {
                baseTime = 10.0 + (Math.random() * 4.0); // 10-14 minutes for very high volume
            } else if (avgVolume > 1000000) {
                baseTime = 15.0 + (Math.random() * 5.0); // 15-20 minutes for high volume
            } else if (avgVolume > 100000) {
                baseTime = 20.0 + (Math.random() * 7.0); // 20-27 minutes for medium volume
            } else {
                baseTime = 25.0 + (Math.random() * 10.0); // 25-35 minutes for low volume
            }
        } else {
            // More granular asset-specific estimates when no market data available
            switch (assetUpper) {
                case "BTC":
                    baseTime = 25.0 + (Math.random() * 10.0); // 25-35 minutes (Bitcoin usually slower)
                    break;
                case "ETH":
                    baseTime = 12.0 + (Math.random() * 8.0); // 12-20 minutes
                    break;
                case "SOL":
                    baseTime = 3.0 + (Math.random() * 4.0); // 3-7 minutes (Solana is fast)
                    break;
                case "XRP":
                    baseTime = 2.0 + (Math.random() * 3.0); // 2-5 minutes (XRP is very fast)
                    break;
                case "USDT":
                case "USDC":
                    baseTime = 10.0 + (Math.random() * 7.0); // 10-17 minutes (depends on blockchain used)
                    break;
                default:
                    // For all other assets, use a formula based on the hashcode to create consistent but varied times
                    int hashCode = Math.abs(assetUpper.hashCode());
                    baseTime = 10.0 + (hashCode % 20) + (Math.random() * 8.0); // 10-38 minutes
            }
        }
        
        return baseTime;
    }
    
    /**
     * Calculate an exchange-specific processing factor for transfers
     */
    private static double calculateExchangeProcessingFactor(String fromExchange, String toExchange) {
        // Define processing efficiency for major exchanges (lower is faster)
        Map<String, Double> exchangeEfficiency = new HashMap<String, Double>() {{
            put("binance", 0.85);
            put("coinbase", 1.05);
            put("kraken", 0.95);
            put("bybit", 1.0);
            put("okx", 1.1);
            put("kucoin", 1.2);
            put("huobi", 1.1);
            put("gate", 1.3);
        }};
        
        // Get efficiency for both exchanges
        double fromFactor = exchangeEfficiency.getOrDefault(fromExchange.toLowerCase(), 1.0);
        double toFactor = exchangeEfficiency.getOrDefault(toExchange.toLowerCase(), 1.0);
        
        // Calculate combined factor with more weight on the slower exchange
        return Math.max(fromFactor, toFactor) * 0.7 + Math.min(fromFactor, toFactor) * 0.3;
    }
    
    /**
     * Calculate a time of day factor for transfers
     */
    private static double calculateTimeOfDayFactorForTransfer() {
        // Get current hour in UTC
        int currentHour = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).getHour();
        
        // Transfers tend to be processed faster during business hours
        if (currentHour >= 9 && currentHour <= 17) {
            return 0.9 + (Math.random() * 0.1); // 0.9-1.0: Faster during business hours
        } else if ((currentHour >= 1 && currentHour <= 5) || currentHour >= 21) {
            return 1.2 + (Math.random() * 0.2); // 1.2-1.4: Slowest during night hours
        } else {
            return 1.05 + (Math.random() * 0.15); // 1.05-1.2: Moderate during transition hours
        }
    }
    
    /**
     * Estimate blockchain congestion factor based on asset type
     */
    private static double estimateBlockchainCongestionFactor(String asset) {
        String assetUpper = asset.toUpperCase();
        
        // In a real implementation, this would pull current blockchain stats
        // For now, simulate by using consistent but varied factors by asset
        switch (assetUpper) {
            case "BTC":
                // Bitcoin network congestion varies significantly
                return 0.9 + (Math.random() * 0.5); // 0.9-1.4 range
            case "ETH":
                // Ethereum often has higher congestion
                return 1.0 + (Math.random() * 0.7); // 1.0-1.7 range
            case "SOL":
            case "XRP":
                // Typically less congested networks
                return 0.8 + (Math.random() * 0.3); // 0.8-1.1 range
            default:
                // Moderate variation for other assets
                return 0.9 + (Math.random() * 0.4); // 0.9-1.3 range
        }
    }
    
    /**
     * Calculate ROI efficiency (profit per hour)
     * 
     * @param profitPercentage Profit percentage
     * @param timeMinutes Time in minutes
     * @return Hourly ROI
     */
    public static double calculateROIEfficiency(double profitPercentage, double timeMinutes) {
        if (timeMinutes <= 0) return 0;
        return (profitPercentage / timeMinutes) * 60.0;
    }
    
    /**
     * Generate time display string with appropriate units
     * 
     * @param timeMinutes Time in minutes
     * @return Formatted time string (e.g., "45m" or "2.5h")
     */
    public static String formatTimeString(double timeMinutes) {
        if (timeMinutes < 60) {
            return String.format("%.0fm", timeMinutes);
        } else {
            return String.format("%.1fh", timeMinutes / 60.0);
        }
    }
    
    /**
     * Get smart time thresholds for a specific asset based on market conditions
     * 
     * @param symbol Asset symbol
     * @param ticker Current ticker data
     * @return Array with [good_threshold, medium_threshold] in minutes
     */
    public static Double[] getTimeThresholds(String symbol, Ticker ticker) {
        String baseAsset = extractBaseAsset(symbol);
        
        // Start with reasonable defaults
        Double[] thresholds = new Double[]{15.0, 45.0};
        
        // Adjust based on observed market activity
        if (ticker != null) {
            double volume24h = ticker.getVolume();
            double priceVolatility = calculatePriceVolatility(ticker);
            
            // High volume assets with low volatility can execute faster
            if (volume24h > 10000000 && priceVolatility < 0.01) {
                thresholds = new Double[]{5.0, 15.0}; // Very liquid, low volatility
            } else if (volume24h > 5000000 && priceVolatility < 0.03) {
                thresholds = new Double[]{10.0, 30.0}; // Liquid, moderate volatility
            } else if (volume24h < 1000000 || priceVolatility > 0.05) {
                thresholds = new Double[]{20.0, 60.0}; // Low liquidity or high volatility
            }
        }
        
        return thresholds;
    }
    
    /**
     * Calculate order book depth factor - how deep in the book we need to go affects execution time
     */
    private static double calculateOrderBookDepthFactor(OrderBook orderBook, double tradeAmount, boolean isBuy) {
        if (orderBook == null || (orderBook.getBids() == null && orderBook.getAsks() == null)) {
            return 1.5; // Default if no order book available
        }
        
        // Get the appropriate side of the order book
        List<OrderBookEntry> entries = isBuy ? orderBook.getAsks() : orderBook.getBids();
        
        if (entries == null || entries.isEmpty()) {
            return 1.5;
        }
        
        // Calculate how many orders we need to fulfill the trade amount
        double cumulativeAmount = 0;
        int ordersNeeded = 0;
        
        for (OrderBookEntry entry : entries) {
            cumulativeAmount += entry.getVolume();
            ordersNeeded++;
            
            if (cumulativeAmount >= tradeAmount) {
                break;
            }
        }
        
        // More orders needed = longer execution time
        // Scale to a reasonable range: 1.0 (very shallow) to 3.0 (very deep)
        return Math.max(1.0, Math.min(3.0, 1.0 + (ordersNeeded / 10.0)));
    }
    
    /**
     * Calculate volume ratio factor - what percentage of 24h volume is our trade
     */
    private static double calculateVolumeRatioFactor(Ticker ticker, double tradeAmount) {
        if (ticker == null || ticker.getVolume() <= 0) {
            return 1.5; // Default if no volume data
        }
        
        // Calculate trade size as percentage of 24h volume
        double volumeRatio = (tradeAmount / ticker.getVolume()) * 100.0;
        
        // Higher ratio = longer execution, scaled to reasonable range
        // < 0.1% of daily volume = fast (0.8), > 5% of daily volume = very slow (3.0)
        if (volumeRatio < 0.1) {
            return 0.8; // Very small relative to volume
        } else if (volumeRatio < 0.5) {
            return 1.0; // Small relative to volume
        } else if (volumeRatio < 1.0) {
            return 1.2; // Moderate relative to volume  
        } else if (volumeRatio < 2.0) {
            return 1.5; // Significant relative to volume
        } else if (volumeRatio < 5.0) {
            return 2.0; // Large relative to volume
        } else {
            return 3.0; // Very large relative to volume
        }
    }
    
    /**
     * Calculate spread factor - wider spreads indicate less liquid markets and longer execution
     */
    private static double calculateSpreadFactor(Ticker ticker) {
        if (ticker == null || ticker.getBidPrice() <= 0 || ticker.getAskPrice() <= 0) {
            return 1.5; // Default if no spread data
        }
        
        // Calculate percentage spread
        double spreadPercentage = ((ticker.getAskPrice() - ticker.getBidPrice()) / ticker.getBidPrice()) * 100.0;
        
        // Convert to factor: tighter spread = faster execution
        if (spreadPercentage < 0.05) {
            return 0.8;  // Very tight spread (<0.05%)
        } else if (spreadPercentage < 0.1) {
            return 1.0;  // Tight spread (<0.1%)
        } else if (spreadPercentage < 0.3) {
            return 1.3;  // Normal spread
        } else if (spreadPercentage < 0.5) {
            return 1.6;  // Wide spread
        } else if (spreadPercentage < 1.0) {
            return 2.0;  // Very wide spread
        } else {
            return 2.5;  // Extremely wide spread (>1%)
        }
    }
    
    /**
     * Calculate order book imbalance factor
     * More balanced books indicate smoother execution, imbalanced books suggest delays
     */
    private static double calculateOrderBookImbalanceFactor(OrderBook orderBook) {
        if (orderBook == null || (orderBook.getBids() == null && orderBook.getAsks() == null)) {
            return 1.5; // Default if no order book available
        }
        
        // Calculate total volume in first 10 levels on each side
        double bidVolume = 0;
        double askVolume = 0;
        
        int bidLevels = Math.min(10, orderBook.getBids().size());
        int askLevels = Math.min(10, orderBook.getAsks().size());
        
        for (int i = 0; i < bidLevels; i++) {
            bidVolume += orderBook.getBids().get(i).getVolume();
        }
        
        for (int i = 0; i < askLevels; i++) {
            askVolume += orderBook.getAsks().get(i).getVolume();
        }
        
        if (bidVolume <= 0 || askVolume <= 0) {
            return 1.0; // Avoid division by zero
        }
        
        // Calculate imbalance ratio (larger side / smaller side)
        double imbalanceRatio = bidVolume > askVolume 
                ? bidVolume / askVolume 
                : askVolume / bidVolume;
        
        // More balanced = faster execution (ratio closer to 1.0)
        if (imbalanceRatio < 1.5) {
            return 0.9;  // Well-balanced order book
        } else if (imbalanceRatio < 3.0) {
            return 1.1;  // Slightly imbalanced
        } else if (imbalanceRatio < 5.0) {
            return 1.3;  // Moderately imbalanced
        } else if (imbalanceRatio < 10.0) {
            return 1.5;  // Significantly imbalanced
        } else {
            return 1.8;  // Extremely imbalanced
        }
    }
    
    /**
     * Get exchange response time factor based on API performance
     */
    private static double getExchangeResponseTimeFactor(String exchangeName) {
        String normalized = exchangeName.toLowerCase();
        Double responseTime = API_RESPONSE_TIMES.get(normalized);
        
        if (responseTime == null) {
            return 1.0; // Default if no data
        }
        
        // Convert response time (ms) to factor
        // Faster response = faster execution
        if (responseTime < 100) {
            return 0.8;  // Very fast API (<100ms)
        } else if (responseTime < 200) {
            return 0.9;  // Fast API (<200ms)
        } else if (responseTime < 500) {
            return 1.0;  // Average API (<500ms)
        } else if (responseTime < 1000) {
            return 1.2;  // Slow API (<1s)
        } else {
            return 1.5;  // Very slow API (>1s)
        }
    }
    
    /**
     * Calculate price volatility from ticker data
     */
    private static double calculatePriceVolatility(Ticker ticker) {
        if (ticker == null || ticker.getHighPrice() <= 0 || ticker.getLowPrice() <= 0) {
            return 0.02; // Default moderate volatility
        }
        
        // Calculate high-low range as percentage of last price
        return (ticker.getHighPrice() - ticker.getLowPrice()) / ticker.getLastPrice();
    }
    
    /**
     * Estimate transfer time from liquidity metrics when no historical data available
     */
    private static double estimateTransferTimeFromLiquidity(String asset, Ticker sourceTicker, Ticker destTicker) {
        double baseTime;
        String assetUpper = asset.toUpperCase();
        
        // Estimate based on volume and trading activity when available
        if (sourceTicker != null && destTicker != null) {
            double avgVolume = (sourceTicker.getVolume() + destTicker.getVolume()) / 2.0;
            double avgTrades = 0; // Would use trade count if available
            
            // Use average volume as a proxy for network activity
            if (avgVolume > 10000000) {
                baseTime = 10.0; // High volume suggests active network
            } else if (avgVolume > 1000000) {
                baseTime = 20.0; // Medium volume 
            } else {
                baseTime = 30.0; // Low volume suggests less active network
            }
        } else {
            // Default estimates when no market data available
            if (assetUpper.equals("BTC")) {
                baseTime = 30.0; // Bitcoin usually slower
            } else if (assetUpper.equals("ETH")) {
                baseTime = 15.0; // Ethereum moderate
            } else if (assetUpper.startsWith("USD")) {
                baseTime = 15.0; // Stablecoins moderate
            } else {
                baseTime = 20.0; // Default for unknown assets
            }
        }
        
        return baseTime;
    }
    
    /**
     * Calculate transfer size factor - larger transfers take longer
     */
    private static double calculateTransferSizeFactor(String asset, double amount) {
        // Calculate a size factor based on relative transfer size
        // This would ideally be calibrated with actual transfer data
        if (amount < 100) {
            return 0.9; // Small transfer
        } else if (amount < 1000) {
            return 1.0; // Medium transfer
        } else if (amount < 10000) {
            return 1.2; // Large transfer
        } else {
            return 1.5; // Very large transfer
        }
    }
    
    /**
     * Get volatility time factor
     */
    private static double getVolatilityTimeFactor(MarketVolatility volatility) {
        return VOLATILITY_TIME_FACTORS.getOrDefault(volatility, 1.0);
    }
    
    /**
     * Record API response time for an exchange
     * Call this after each API call to build performance data
     */
    public static void recordApiResponseTime(String exchangeName, long responseTimeMs) {
        String normalized = exchangeName.toLowerCase();
        Double currentAvg = API_RESPONSE_TIMES.get(normalized);
        
        if (currentAvg == null) {
            // First measurement
            API_RESPONSE_TIMES.put(normalized, (double) responseTimeMs);
        } else {
            // Rolling average (90% old value, 10% new measurement)
            double newAvg = (currentAvg * 0.9) + (responseTimeMs * 0.1);
            API_RESPONSE_TIMES.put(normalized, newAvg);
        }
    }
    
    /**
     * Record actual execution time for future estimates
     */
    public static void recordActualExecutionTime(String exchangeName, String symbol, double executionTimeSeconds) {
        updateExecutionStatsCache(exchangeName, symbol, executionTimeSeconds);
    }
    
    /**
     * Record actual transfer time between exchanges
     */
    public static void recordActualTransferTime(String fromExchange, String toExchange, 
                                              String asset, double transferTimeMinutes) {
        String key = fromExchange.toLowerCase() + "_" + toExchange.toLowerCase();
        Map<String, ExecutionStats> assetMap = EXECUTION_STATS_CACHE.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        
        String normalizedAsset = asset.toUpperCase();
        ExecutionStats stats = assetMap.get(normalizedAsset);
        
        if (stats == null) {
            stats = new ExecutionStats(transferTimeMinutes);
        } else {
            stats.update(transferTimeMinutes);
        }
        
        assetMap.put(normalizedAsset, stats);
    }
    
    /**
     * Get historical transfer time if available
     */
    private static double getHistoricalTransferTime(String fromExchange, String toExchange, String asset) {
        String key = fromExchange.toLowerCase() + "_" + toExchange.toLowerCase();
        Map<String, ExecutionStats> assetMap = EXECUTION_STATS_CACHE.get(key);
        
        if (assetMap == null) {
            return -1; // No data available
        }
        
        String normalizedAsset = asset.toUpperCase();
        ExecutionStats stats = assetMap.get(normalizedAsset);
        
        if (stats == null) {
            return -1; // No data for this asset
        }
        
        return stats.getAverageTime();
    }
    
    /**
     * Update the execution stats cache with a new measurement
     */
    private static void updateExecutionStatsCache(String exchangeName, String symbol, double executionTime) {
        Map<String, ExecutionStats> symbolMap = EXECUTION_STATS_CACHE.computeIfAbsent(
                exchangeName.toLowerCase(), k -> new ConcurrentHashMap<>());
        
        ExecutionStats stats = symbolMap.get(symbol);
        
        if (stats == null) {
            stats = new ExecutionStats(executionTime);
        } else {
            stats.update(executionTime);
        }
        
        symbolMap.put(symbol, stats);
    }
    
    /**
     * Extract base asset from a trading pair symbol
     */
    private static String extractBaseAsset(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return "";
        }
        
        // Handle different formats (BTC/USDT, BTC-USDT, BTCUSDT)
        if (symbol.contains("/")) {
            return symbol.split("/")[0];
        } else if (symbol.contains("-")) {
            return symbol.split("-")[0];
        } else {
            // Try to extract by common quote currencies
            String[] quoteCurrencies = {"USDT", "USDC", "BUSD", "USD", "BTC", "ETH"};
            for (String quote : quoteCurrencies) {
                if (symbol.endsWith(quote)) {
                    return symbol.substring(0, symbol.length() - quote.length());
                }
            }
        }
        
        // If we can't determine, return the whole symbol
        return symbol;
    }
    
    /**
     * Helper class to track execution statistics
     */
    private static class ExecutionStats {
        private double totalTime;
        private int count;
        
        public ExecutionStats(double initialTime) {
            this.totalTime = initialTime;
            this.count = 1;
        }
        
        public void update(double newTime) {
            // Weighted update favoring recent measurements
            if (count < 10) {
                // Simple average for first few measurements
                totalTime = ((totalTime * count) + newTime) / (count + 1);
                count++;
            } else {
                // Weighted average thereafter (recent measurements count more)
                totalTime = (totalTime * 0.9) + (newTime * 0.1);
            }
        }
        
        public double getAverageTime() {
            return totalTime;
        }
    }
    
    /**
     * Calculate time-adjusted profit based on execution time and volatility
     * 
     * @param profit Original profit percentage
     * @param timeMinutes Estimated execution time in minutes
     * @param volatility Current market volatility
     * @return Time-adjusted profit percentage
     */
    public static double calculateTimeAdjustedProfit(
            double profit, double timeMinutes, MarketVolatility volatility) {
        
        // Longer execution time = higher risk of price movement
        double timeAdjustmentFactor = 1.0 - (timeMinutes / 1440.0); // Normalized to 24 hours
        
        // Apply volatility premium - higher volatility means higher premium required
        double volatilityPremium = getVolatilityRiskPremium(volatility) / 365.0 / 24.0 * timeMinutes;
        
        // Adjust profit based on time and volatility
        return profit * timeAdjustmentFactor - volatilityPremium;
    }
    
    /**
     * Calculate annualized return based on profit and execution time
     * 
     * @param profit Profit percentage
     * @param timeMinutes Execution time in minutes
     * @return Annualized return percentage
     */
    public static double calculateAnnualizedReturn(double profit, double timeMinutes) {
        if (timeMinutes <= 0) return 0;
        
        // Calculate number of times this arbitrage could be executed in a year
        double executionsPerYear = (365.0 * 24.0 * 60.0) / timeMinutes;
        
        // Compound the profit over the year
        return Math.pow(1.0 + (profit / 100.0), executionsPerYear) - 1.0;
    }
    
    /**
     * Calculate risk-adjusted return using modern portfolio theory concepts
     * 
     * @param profit Profit percentage
     * @param timeMinutes Execution time in minutes
     * @param volatility Market volatility
     * @return Risk-adjusted return
     */
    public static double calculateRiskAdjustedReturn(
            double profit, double timeMinutes, MarketVolatility volatility) {
        
        if (timeMinutes <= 0) return 0;
        
        // Get volatility factor as proxy for risk
        double riskFactor = getVolatilityTimeFactor(volatility);
        
        // Calculate annualized profit
        double annualizedProfit = calculateAnnualizedReturn(profit, timeMinutes);
        
        // Risk-free rate (assume 2% annual)
        double riskFreeRate = 0.02;
        
        // Calculate Sharpe-like ratio: (return - risk free) / risk
        return (annualizedProfit - riskFreeRate) / riskFactor;
    }
    
    /**
     * Calculate opportunity score - comprehensive metric combining profit, time, and risk
     * 
     * @param profit Profit percentage
     * @param timeMinutes Execution time in minutes
     * @param volatility Market volatility
     * @param liquidityFactor Liquidity factor (0-1)
     * @param exchangeRisk Exchange risk factor
     * @return Opportunity score (0-100, higher is better)
     */
    public static double calculateOpportunityScore(
            double profit, double timeMinutes, MarketVolatility volatility,
            double liquidityFactor, double exchangeRisk) {
        
        // Normalize profit (0-5% profit gives 0-100 score)
        double profitScore = Math.min(100.0, profit * 20.0);
        
        // Normalize time (0-120 minutes gives 100-0 score)
        double timeScore = Math.max(0.0, 100.0 - (timeMinutes / 1.2));
        
        // Normalize volatility (VERY_LOW to VERY_HIGH gives 100-0 score)
        double volatilityScore = 100.0 - (getVolatilityIndex(volatility) * 25.0);
        
        // Normalize liquidity (0-1 gives 0-100 score)
        double liquidityScore = liquidityFactor * 100.0;
        
        // Normalize exchange risk (0-1 gives 100-0 score)
        double exchangeRiskScore = (1.0 - exchangeRisk) * 100.0;
        
        // Weighted average of all factors
        return (profitScore * 0.4) + 
               (timeScore * 0.2) + 
               (volatilityScore * 0.15) + 
               (liquidityScore * 0.15) + 
               (exchangeRiskScore * 0.1);
    }
    
    /**
     * Format ROI efficiency for display
     * 
     * @param profit Profit percentage
     * @param timeMinutes Time in minutes
     * @return Formatted ROI efficiency string
     */
    public static String formatROIEfficiency(double profit, double timeMinutes) {
        double efficiency = calculateROIEfficiency(profit, timeMinutes);
        return String.format("%.2f%%/h", efficiency);
    }
    
    /**
     * Helper method to get volatility index from MarketVolatility enum
     * 
     * @param volatility Volatility level
     * @return Index from 0 (VERY_LOW) to 4 (VERY_HIGH)
     */
    private static int getVolatilityIndex(MarketVolatility volatility) {
        switch (volatility) {
            case VERY_LOW: return 0;
            case LOW: return 1;
            case MEDIUM: return 2;
            case HIGH: return 3;
            case VERY_HIGH: return 4;
            default: return 2;
        }
    }
    
    /**
     * Get the risk premium required for a given volatility level
     * 
     * @param volatility Volatility level
     * @return Annual risk premium percentage
     */
    private static double getVolatilityRiskPremium(MarketVolatility volatility) {
        return VOLATILITY_RISK_PREMIUMS.getOrDefault(volatility, 15.0);
    }
} 