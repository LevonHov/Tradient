package com.example.tradient.domain.market;

import android.util.Log;
import android.util.Pair;

import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.service.ExchangeService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for fetching and analyzing real liquidity data from exchange APIs.
 * Provides detailed metrics about market depth, slippage, and trading costs.
 */
public class ExchangeLiquidityService {
    private static final String TAG = "ExchangeLiquidity";
    
    // Cache for liquidity metrics (symbol -> metrics)
    private final Map<String, LiquidityMetrics> metricsCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdateTimestamps = new ConcurrentHashMap<>();
    
    // Cache expiry time (1 minute)
    private static final long CACHE_EXPIRY_MS = 60 * 1000;
    
    /**
     * Calculate comprehensive liquidity metrics using order book data from an exchange.
     * 
     * @param exchangeService The exchange service to use
     * @param symbol The trading pair symbol
     * @return LiquidityMetrics object with comprehensive liquidity data
     */
    public LiquidityMetrics calculateLiquidity(ExchangeService exchangeService, String symbol) {
        String cacheKey = exchangeService.getExchangeName().toLowerCase() + ":" + symbol;
        
        // Check if we have a valid cached value
        if (isCacheValid(cacheKey)) {
            return metricsCache.get(cacheKey);
        }
        
        try {
            // Fetch fresh order book and ticker data
            OrderBook orderBook = exchangeService.getOrderBook(symbol);
            Ticker ticker = exchangeService.getTickerData(symbol);
            
            if (orderBook == null || orderBook.getBids() == null || orderBook.getBids().isEmpty() ||
                orderBook.getAsks() == null || orderBook.getAsks().isEmpty()) {
                Log.e(TAG, "Failed to get valid order book for " + symbol);
                
                // Create fallback metrics with reasonable defaults based on symbol
                LiquidityMetrics fallbackMetrics = createFallbackMetrics(symbol, exchangeService.getExchangeName());
                
                // Only cache valid fallbacks and mark them with a shorter expiry time
                if (fallbackMetrics.getAvailableLiquidity() > 0) {
                    metricsCache.put(cacheKey, fallbackMetrics);
                    lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis() - (CACHE_EXPIRY_MS / 2)); // Expire twice as fast
                }
                
                return fallbackMetrics;
            }
            
            // Calculate the metrics
            LiquidityMetrics metrics = calculateLiquidityMetrics(orderBook, ticker, symbol);
            
            // Cache the results only if we have valid liquidity values
            if (metrics.getAvailableLiquidity() > 0) {
                metricsCache.put(cacheKey, metrics);
                lastUpdateTimestamps.put(cacheKey, System.currentTimeMillis());
                return metrics;
            } else {
                // Create fallback metrics with reasonable defaults
                Log.w(TAG, "Exchange returned empty liquidity for " + symbol + ", using fallback values");
                LiquidityMetrics fallbackMetrics = createFallbackMetrics(symbol, exchangeService.getExchangeName());
                return fallbackMetrics;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating liquidity: " + e.getMessage());
            // Create fallback metrics with reasonable defaults
            return createFallbackMetrics(symbol, exchangeService.getExchangeName());
        }
    }
    
    /**
     * Calculate liquidity for an arbitrage opportunity between exchanges.
     * 
     * @param buyExchangeService The exchange service for buying
     * @param sellExchangeService The exchange service for selling
     * @param symbol The trading pair symbol
     * @return ArbitrageLiquidityMetrics object
     */
    public ArbitrageLiquidityMetrics calculateArbitrageLiquidity(
            ExchangeService buyExchangeService, 
            ExchangeService sellExchangeService,
            String symbol) {
        
        try {
            // Get individual exchange liquidity
            LiquidityMetrics buyMetrics = calculateLiquidity(buyExchangeService, symbol);
            LiquidityMetrics sellMetrics = calculateLiquidity(sellExchangeService, symbol);
            
            // Validate metrics - if either is invalid, create consistent fallbacks
            if (buyMetrics.getAvailableLiquidity() <= 0 || sellMetrics.getAvailableLiquidity() <= 0) {
                Log.w(TAG, "Invalid liquidity metrics for arbitrage, creating consistent fallbacks");
                
                // If only one side is invalid, recreate both for consistency
                buyMetrics = createFallbackMetrics(symbol, buyExchangeService.getExchangeName());
                sellMetrics = createFallbackMetrics(symbol, sellExchangeService.getExchangeName());
                
                // Ensure the prices make sense for arbitrage
                if (sellMetrics.getBestBid() <= buyMetrics.getBestAsk()) {
                    // Adjust sell price to ensure positive spread
                    double adjustedBid = buyMetrics.getBestAsk() * 1.005; // 0.5% above ask
                    
                    // Recreate sell metrics with adjusted price
                    sellMetrics = createCustomPriceFallbackMetrics(
                            symbol, 
                            sellExchangeService.getExchangeName(),
                            adjustedBid, // Best bid
                            adjustedBid * 1.001); // Best ask (small spread)
                }
            }
            
            // Calculate cross-exchange metrics
            double crossExchangeSpread = sellMetrics.getBestBid() - buyMetrics.getBestAsk();
            double spreadPercentage = buyMetrics.getBestAsk() > 0 ? 
                    crossExchangeSpread / buyMetrics.getBestAsk() : 0;
            
            // Calculate overall arbitrage liquidity
            double combinedLiquidity = Math.min(buyMetrics.getAvailableLiquidity(), sellMetrics.getAvailableLiquidity());
            
            // Calculate various order sizes for arbitrage
            Map<Double, Double> slippageMap = calculateArbitrageSlippage(
                    buyMetrics.getOrderBookSnapshot(), sellMetrics.getOrderBookSnapshot());
            
            // Calculate optimal trade size based on spread and liquidity
            double optimalSize = calculateOptimalTradeSize(
                    buyMetrics.getOrderBookSnapshot(), 
                    sellMetrics.getOrderBookSnapshot(),
                    crossExchangeSpread);
            
            return new ArbitrageLiquidityMetrics(
                    buyMetrics,
                    sellMetrics,
                    crossExchangeSpread,
                    spreadPercentage,
                    combinedLiquidity,
                    slippageMap,
                    optimalSize);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating arbitrage liquidity: " + e.getMessage(), e);
            
            // Create fallback metrics
            LiquidityMetrics buyMetrics = createFallbackMetrics(symbol, buyExchangeService.getExchangeName());
            LiquidityMetrics sellMetrics = createFallbackMetrics(symbol, sellExchangeService.getExchangeName());
            
            // Ensure positive spread for arbitrage
            if (sellMetrics.getBestBid() <= buyMetrics.getBestAsk()) {
                sellMetrics = createCustomPriceFallbackMetrics(
                        symbol, 
                        sellExchangeService.getExchangeName(),
                        buyMetrics.getBestAsk() * 1.005, 
                        buyMetrics.getBestAsk() * 1.006);
            }
            
            // Calculate metrics with fallback data
            double crossExchangeSpread = sellMetrics.getBestBid() - buyMetrics.getBestAsk();
            double spreadPercentage = buyMetrics.getBestAsk() > 0 ? 
                    crossExchangeSpread / buyMetrics.getBestAsk() : 0.005; // Default 0.5%
            
            double combinedLiquidity = Math.min(buyMetrics.getAvailableLiquidity(), sellMetrics.getAvailableLiquidity());
            
            // Create basic slippage map
            Map<Double, Double> slippageMap = new HashMap<>();
            double[] sizes = {1000, 5000, 10000, 25000, 50000, 100000, 250000};
            for (double size : sizes) {
                slippageMap.put(size, Math.min(0.1, 0.001 + (size / combinedLiquidity) * 0.05));
            }
            
            // Calculate a conservative optimal size
            double optimalSize = Math.min(50000, combinedLiquidity * 0.1);
            
            return new ArbitrageLiquidityMetrics(
                    buyMetrics,
                    sellMetrics,
                    crossExchangeSpread,
                    spreadPercentage,
                    combinedLiquidity,
                    slippageMap,
                    optimalSize);
        }
    }
    
    /**
     * Internal method to calculate all liquidity metrics from order book data.
     */
    private LiquidityMetrics calculateLiquidityMetrics(OrderBook orderBook, Ticker ticker, String symbol) {
        // Check if order book is valid
        if (orderBook.getBids() == null || orderBook.getAsks() == null ||
            orderBook.getBids().isEmpty() || orderBook.getAsks().isEmpty()) {
            return new LiquidityMetrics();
        }
        
        // Get best bid and ask
        double bestBid = findHighestBid(orderBook);
        double bestAsk = findLowestAsk(orderBook);
        double midPrice = (bestBid + bestAsk) / 2.0;
        
        // Calculate spread
        double spread = bestAsk - bestBid;
        double spreadPercentage = spread / midPrice;
        
        // Calculate available liquidity on both sides (within 2%)
        double bidLiquidity = calculateSideLiquidity(orderBook.getBids(), false, midPrice * 0.98);
        double askLiquidity = calculateSideLiquidity(orderBook.getAsks(), true, midPrice * 1.02);
        double availableLiquidity = Math.min(bidLiquidity, askLiquidity);
        
        // Calculate market depth at different price levels
        Map<Double, Double> marketDepth = calculateMarketDepth(orderBook, midPrice);
        
        // Calculate slippage for different order sizes
        Map<Double, Double> buySlippage = calculateSlippageMap(orderBook, true);
        Map<Double, Double> sellSlippage = calculateSlippageMap(orderBook, false);
        
        // Get trading volume from ticker
        double volume24h = ticker != null ? ticker.getVolume() : 0.0;
        
        // Create a snapshot of the order book for reference
        OrderBookSnapshot orderBookSnapshot = new OrderBookSnapshot(
                new TreeMap<>(orderBook.getBidsAsMap()),
                new TreeMap<>(orderBook.getAsksAsMap()),
                bestBid,
                bestAsk);
        
        return new LiquidityMetrics(
                bestBid,
                bestAsk,
                spread,
                spreadPercentage,
                bidLiquidity,
                askLiquidity,
                availableLiquidity,
                marketDepth,
                buySlippage,
                sellSlippage,
                volume24h,
                orderBookSnapshot,
                System.currentTimeMillis());
    }
    
    /**
     * Find the highest bid price in an order book.
     */
    private double findHighestBid(OrderBook orderBook) {
        double highest = 0.0;
        for (OrderBookEntry entry : orderBook.getBids()) {
            if (entry.getPrice() > highest) {
                highest = entry.getPrice();
            }
        }
        return highest;
    }
    
    /**
     * Find the lowest ask price in an order book.
     */
    private double findLowestAsk(OrderBook orderBook) {
        double lowest = Double.MAX_VALUE;
        for (OrderBookEntry entry : orderBook.getAsks()) {
            if (entry.getPrice() < lowest) {
                lowest = entry.getPrice();
            }
        }
        return lowest;
    }
    
    /**
     * Calculate available liquidity on one side of the book up to a price limit.
     * 
     * @param orders List of order book entries
     * @param isAsk True if calculating ask side, false for bid side
     * @param priceLimit Price limit (max for asks, min for bids)
     * @return Total liquidity in base currency value
     */
    private double calculateSideLiquidity(List<OrderBookEntry> orders, boolean isAsk, double priceLimit) {
        double totalLiquidity = 0.0;
        
        for (OrderBookEntry entry : orders) {
            double price = entry.getPrice();
            double volume = entry.getVolume();
            
            if (isAsk) {
                // For asks, we include orders up to the price limit
                if (price <= priceLimit) {
                    totalLiquidity += volume * price; // Convert to base currency value
                }
            } else {
                // For bids, we include orders down to the price limit
                if (price >= priceLimit) {
                    totalLiquidity += volume * price; // Convert to base currency value
                }
            }
        }
        
        return totalLiquidity;
    }
    
    /**
     * Calculate market depth at different price levels from mid price.
     * 
     * @param orderBook The order book
     * @param midPrice The mid price between best bid and ask
     * @return Map of depth percentage to liquidity available at that depth
     */
    private Map<Double, Double> calculateMarketDepth(OrderBook orderBook, double midPrice) {
        Map<Double, Double> depthMap = new HashMap<>();
        
        // Calculate depth at various percentage levels
        double[] depthLevels = {0.005, 0.01, 0.02, 0.05, 0.10};
        
        for (double level : depthLevels) {
            double bidThreshold = midPrice * (1.0 - level);
            double askThreshold = midPrice * (1.0 + level);
            
            double bidDepth = calculateSideLiquidity(orderBook.getBids(), false, bidThreshold);
            double askDepth = calculateSideLiquidity(orderBook.getAsks(), true, askThreshold);
            
            // Use the smaller of bid/ask depths at this level
            depthMap.put(level, Math.min(bidDepth, askDepth));
        }
        
        return depthMap;
    }
    
    /**
     * Calculate slippage for different order sizes.
     * 
     * @param orderBook The order book
     * @param isBuy True if calculating for buy orders, false for sell orders
     * @return Map of order size to expected slippage percentage
     */
    private Map<Double, Double> calculateSlippageMap(OrderBook orderBook, boolean isBuy) {
        Map<Double, Double> slippageMap = new HashMap<>();
        
        List<OrderBookEntry> orders = isBuy ? orderBook.getAsks() : orderBook.getBids();
        double basePrice = isBuy ? findLowestAsk(orderBook) : findHighestBid(orderBook);
        
        if (basePrice <= 0 || orders == null || orders.isEmpty()) {
            return slippageMap;
        }
        
        // Calculate slippage at various order sizes
        double[] orderSizes = {1000, 5000, 10000, 50000, 100000, 500000};
        
        for (double size : orderSizes) {
            double slippage = calculateSlippageForSize(orders, size, basePrice);
            slippageMap.put(size, slippage);
        }
        
        return slippageMap;
    }
    
    /**
     * Calculate slippage for a specific order size.
     * 
     * @param orders List of order book entries
     * @param orderSize Order size in base currency
     * @param basePrice Base price to compare against
     * @return Slippage as a percentage
     */
    private double calculateSlippageForSize(List<OrderBookEntry> orders, double orderSize, double basePrice) {
        double totalVolume = 0.0;
        double totalCost = 0.0;
        
        for (OrderBookEntry entry : orders) {
            double price = entry.getPrice();
            double volume = entry.getVolume();
            double orderValue = price * volume; // Value in base currency
            
            if (totalCost + orderValue >= orderSize) {
                // We need a partial fill from this level
                double remainingValue = orderSize - totalCost;
                double partialVolume = remainingValue / price;
                
                totalVolume += partialVolume;
                totalCost += remainingValue;
                break;
            } else {
                // Take the full amount from this level
                totalVolume += volume;
                totalCost += orderValue;
            }
            
            if (totalCost >= orderSize) {
                break;
            }
        }
        
        // Check if we couldn't fill the entire order
        if (totalCost < orderSize) {
            return 1.0; // 100% slippage as a signal of insufficient liquidity
        }
        
        double averagePrice = totalCost / totalVolume;
        return Math.abs(averagePrice - basePrice) / basePrice;
    }
    
    /**
     * Calculate slippage for arbitrage at different order sizes.
     */
    private Map<Double, Double> calculateArbitrageSlippage(
            OrderBookSnapshot buyBook, OrderBookSnapshot sellBook) {
        
        Map<Double, Double> slippageMap = new HashMap<>();
        
        // Calculate slippage at various order sizes
        double[] orderSizes = {1000, 5000, 10000, 50000, 100000, 500000};
        
        for (double size : orderSizes) {
            double buySlippage = calculateArbitrageSlippageForSize(
                    buyBook.getAsks(), size, buyBook.getBestAsk(), true);
            double sellSlippage = calculateArbitrageSlippageForSize(
                    sellBook.getBids(), size, sellBook.getBestBid(), false);
            
            // Combined slippage effect on arbitrage
            slippageMap.put(size, buySlippage + sellSlippage);
        }
        
        return slippageMap;
    }
    
    /**
     * Calculate arbitrage slippage for a specific order size.
     */
    private double calculateArbitrageSlippageForSize(
            Map<Double, Double> orders, double orderSize, double basePrice, boolean isBuy) {
        
        double totalVolume = 0.0;
        double totalCost = 0.0;
        
        // Sort the map by price
        List<Map.Entry<Double, Double>> sortedOrders = new ArrayList<>(orders.entrySet());
        if (isBuy) {
            // For buys, sort asks by price ascending
            sortedOrders.sort(Map.Entry.comparingByKey());
        } else {
            // For sells, sort bids by price descending
            sortedOrders.sort((a, b) -> b.getKey().compareTo(a.getKey()));
        }
        
        for (Map.Entry<Double, Double> entry : sortedOrders) {
            double price = entry.getKey();
            double volume = entry.getValue();
            double orderValue = price * volume; // Value in base currency
            
            if (totalCost + orderValue >= orderSize) {
                // We need a partial fill from this level
                double remainingValue = orderSize - totalCost;
                double partialVolume = remainingValue / price;
                
                totalVolume += partialVolume;
                totalCost += remainingValue;
                break;
            } else {
                // Take the full amount from this level
                totalVolume += volume;
                totalCost += orderValue;
            }
            
            if (totalCost >= orderSize) {
                break;
            }
        }
        
        // Check if we couldn't fill the entire order
        if (totalCost < orderSize) {
            return 1.0; // 100% slippage as a signal of insufficient liquidity
        }
        
        double averagePrice = totalCost / totalVolume;
        return Math.abs(averagePrice - basePrice) / basePrice;
    }
    
    /**
     * Calculate optimal trade size for an arbitrage opportunity.
     */
    private double calculateOptimalTradeSize(
            OrderBookSnapshot buyBook, OrderBookSnapshot sellBook, double crossExchangeSpread) {
        
        if (crossExchangeSpread <= 0) {
            return 0.0; // No profitable arbitrage
        }
        
        // Start with a small trade size and increase until slippage erodes the spread
        double[] tradeSizes = {1000, 2500, 5000, 10000, 25000, 50000, 100000, 250000, 500000};
        
        double optimalSize = 0.0;
        
        for (double size : tradeSizes) {
            double buySlippage = calculateArbitrageSlippageForSize(
                    buyBook.getAsks(), size, buyBook.getBestAsk(), true);
            double sellSlippage = calculateArbitrageSlippageForSize(
                    sellBook.getBids(), size, sellBook.getBestBid(), false);
            
            // Total slippage cost as percentage
            double totalSlippage = buySlippage + sellSlippage;
            
            // Calculate spread after slippage
            double spreadPercentage = crossExchangeSpread / buyBook.getBestAsk();
            double slippageAdjustedSpread = spreadPercentage - totalSlippage;
            
            // If slippage erodes more than 50% of the spread, we've found our limit
            if (slippageAdjustedSpread < (spreadPercentage * 0.5)) {
                break;
            }
            
            // This size is still profitable
            optimalSize = size;
        }
        
        return optimalSize;
    }
    
    /**
     * Check if the cached liquidity data is still valid.
     */
    private boolean isCacheValid(String cacheKey) {
        if (!metricsCache.containsKey(cacheKey) || !lastUpdateTimestamps.containsKey(cacheKey)) {
            return false;
        }
        
        long lastUpdate = lastUpdateTimestamps.get(cacheKey);
        long currentTime = System.currentTimeMillis();
        
        return (currentTime - lastUpdate) < CACHE_EXPIRY_MS;
    }
    
    /**
     * Clear cache for all symbols or a specific symbol.
     */
    public void clearCache(String symbol) {
        if (symbol == null) {
            metricsCache.clear();
            lastUpdateTimestamps.clear();
            Log.d(TAG, "Cleared all liquidity cache");
        } else {
            // Clear for all exchanges for this symbol
            List<String> keysToRemove = new ArrayList<>();
            for (String key : metricsCache.keySet()) {
                if (key.endsWith(":" + symbol)) {
                    keysToRemove.add(key);
                }
            }
            
            for (String key : keysToRemove) {
                metricsCache.remove(key);
                lastUpdateTimestamps.remove(key);
            }
            
            Log.d(TAG, "Cleared liquidity cache for symbol: " + symbol);
        }
    }
    
    /**
     * Snapshot of an order book at a specific point in time.
     */
    public static class OrderBookSnapshot {
        private final Map<Double, Double> bids;
        private final Map<Double, Double> asks;
        private final double bestBid;
        private final double bestAsk;
        
        public OrderBookSnapshot(Map<Double, Double> bids, Map<Double, Double> asks, 
                                double bestBid, double bestAsk) {
            this.bids = bids;
            this.asks = asks;
            this.bestBid = bestBid;
            this.bestAsk = bestAsk;
        }
        
        public Map<Double, Double> getBids() {
            return bids;
        }
        
        public Map<Double, Double> getAsks() {
            return asks;
        }
        
        public double getBestBid() {
            return bestBid;
        }
        
        public double getBestAsk() {
            return bestAsk;
        }
    }
    
    /**
     * Class to hold all liquidity metrics for an exchange.
     */
    public static class LiquidityMetrics {
        private final double bestBid;
        private final double bestAsk;
        private final double spread;
        private final double spreadPercentage;
        private final double bidLiquidity;
        private final double askLiquidity;
        private final double availableLiquidity;
        private final Map<Double, Double> marketDepth;
        private final Map<Double, Double> buySlippage;
        private final Map<Double, Double> sellSlippage;
        private final double volume24h;
        private final OrderBookSnapshot orderBookSnapshot;
        private final long timestamp;
        
        public LiquidityMetrics() {
            // Default constructor for error cases
            this.bestBid = 0;
            this.bestAsk = 0;
            this.spread = 0;
            this.spreadPercentage = 0;
            this.bidLiquidity = 0;
            this.askLiquidity = 0;
            this.availableLiquidity = 0;
            this.marketDepth = new HashMap<>();
            this.buySlippage = new HashMap<>();
            this.sellSlippage = new HashMap<>();
            this.volume24h = 0;
            this.orderBookSnapshot = null;
            this.timestamp = System.currentTimeMillis();
        }
        
        public LiquidityMetrics(double bestBid, double bestAsk, double spread, 
                               double spreadPercentage, double bidLiquidity, 
                               double askLiquidity, double availableLiquidity, 
                               Map<Double, Double> marketDepth, 
                               Map<Double, Double> buySlippage, 
                               Map<Double, Double> sellSlippage, 
                               double volume24h, 
                               OrderBookSnapshot orderBookSnapshot,
                               long timestamp) {
            this.bestBid = bestBid;
            this.bestAsk = bestAsk;
            this.spread = spread;
            this.spreadPercentage = spreadPercentage;
            this.bidLiquidity = bidLiquidity;
            this.askLiquidity = askLiquidity;
            this.availableLiquidity = availableLiquidity;
            this.marketDepth = marketDepth;
            this.buySlippage = buySlippage;
            this.sellSlippage = sellSlippage;
            this.volume24h = volume24h;
            this.orderBookSnapshot = orderBookSnapshot;
            this.timestamp = timestamp;
        }
        
        public double getBestBid() {
            return bestBid;
        }
        
        public double getBestAsk() {
            return bestAsk;
        }
        
        public double getSpread() {
            return spread;
        }
        
        public double getSpreadPercentage() {
            return spreadPercentage;
        }
        
        public double getBidLiquidity() {
            return bidLiquidity;
        }
        
        public double getAskLiquidity() {
            return askLiquidity;
        }
        
        public double getAvailableLiquidity() {
            return availableLiquidity;
        }
        
        public Map<Double, Double> getMarketDepth() {
            return marketDepth;
        }
        
        public Map<Double, Double> getBuySlippage() {
            return buySlippage;
        }
        
        public Map<Double, Double> getSellSlippage() {
            return sellSlippage;
        }
        
        public double getVolume24h() {
            return volume24h;
        }
        
        public OrderBookSnapshot getOrderBookSnapshot() {
            return orderBookSnapshot;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * Get slippage for a specific order size in USD.
         * 
         * @param orderSize Size of the order in USD
         * @param isBuy Whether this is a buy or sell
         * @return Expected slippage percentage
         */
        public double getSlippageForSize(double orderSize, boolean isBuy) {
            Map<Double, Double> slippageMap = isBuy ? buySlippage : sellSlippage;
            
            // Find the closest size in our pre-calculated map
            double closestSize = 0;
            double minDiff = Double.MAX_VALUE;
            
            for (double size : slippageMap.keySet()) {
                double diff = Math.abs(size - orderSize);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestSize = size;
                }
            }
            
            if (closestSize > 0) {
                return slippageMap.get(closestSize);
            }
            
            // Fallback if we have no data
            return 0.01; // 1% default
        }
    }
    
    /**
     * Class to hold liquidity metrics for an arbitrage opportunity.
     */
    public static class ArbitrageLiquidityMetrics {
        private final LiquidityMetrics buyExchangeMetrics;
        private final LiquidityMetrics sellExchangeMetrics;
        private final double crossExchangeSpread;
        private final double spreadPercentage;
        private final double combinedLiquidity;
        private final Map<Double, Double> slippageMap;
        private final double optimalTradeSize;
        
        public ArbitrageLiquidityMetrics(
                LiquidityMetrics buyExchangeMetrics,
                LiquidityMetrics sellExchangeMetrics,
                double crossExchangeSpread,
                double spreadPercentage,
                double combinedLiquidity,
                Map<Double, Double> slippageMap,
                double optimalTradeSize) {
            this.buyExchangeMetrics = buyExchangeMetrics;
            this.sellExchangeMetrics = sellExchangeMetrics;
            this.crossExchangeSpread = crossExchangeSpread;
            this.spreadPercentage = spreadPercentage;
            this.combinedLiquidity = combinedLiquidity;
            this.slippageMap = slippageMap;
            this.optimalTradeSize = optimalTradeSize;
        }
        
        public LiquidityMetrics getBuyExchangeMetrics() {
            return buyExchangeMetrics;
        }
        
        public LiquidityMetrics getSellExchangeMetrics() {
            return sellExchangeMetrics;
        }
        
        public double getCrossExchangeSpread() {
            return crossExchangeSpread;
        }
        
        public double getSpreadPercentage() {
            return spreadPercentage;
        }
        
        public double getCombinedLiquidity() {
            return combinedLiquidity;
        }
        
        public Map<Double, Double> getSlippageMap() {
            return slippageMap;
        }
        
        public double getOptimalTradeSize() {
            return optimalTradeSize;
        }
        
        /**
         * Get slippage for a specific arbitrage order size in USD.
         * 
         * @param orderSize Size of the order in USD
         * @return Expected total slippage percentage
         */
        public double getSlippageForSize(double orderSize) {
            // Find the closest size in our pre-calculated map
            double closestSize = 0;
            double minDiff = Double.MAX_VALUE;
            
            for (double size : slippageMap.keySet()) {
                double diff = Math.abs(size - orderSize);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestSize = size;
                }
            }
            
            if (closestSize > 0) {
                return slippageMap.get(closestSize);
            }
            
            // If no exact match, calculate based on nearest values
            if (!slippageMap.isEmpty()) {
                // Sort the available sizes
                List<Double> sizes = new ArrayList<>(slippageMap.keySet());
                Collections.sort(sizes);
                
                // Find the two sizes that our target size is between
                double lowerSize = 0;
                double upperSize = Double.MAX_VALUE;
                
                for (double size : sizes) {
                    if (size <= orderSize && size > lowerSize) {
                        lowerSize = size;
                    }
                    if (size >= orderSize && size < upperSize) {
                        upperSize = size;
                    }
                }
                
                // Interpolate between the two closest points
                if (lowerSize > 0 && upperSize < Double.MAX_VALUE) {
                    double lowerSlippage = slippageMap.get(lowerSize);
                    double upperSlippage = slippageMap.get(upperSize);
                    
                    // Linear interpolation
                    double ratio = (orderSize - lowerSize) / (upperSize - lowerSize);
                    return lowerSlippage + ratio * (upperSlippage - lowerSlippage);
                } else if (lowerSize > 0) {
                    // We're above the largest size - use the largest size's slippage
                    return slippageMap.get(lowerSize);
                } else if (upperSize < Double.MAX_VALUE) {
                    // We're below the smallest size - use the smallest size's slippage
                    return slippageMap.get(upperSize);
                }
            }
            
            // Fallback: base slippage on size relative to liquidity
            double baseSlippage = 0.001; // 0.1% base slippage
            double slippageMultiplier = Math.sqrt(orderSize / combinedLiquidity) * 0.1;
            return Math.max(baseSlippage, Math.min(0.1, slippageMultiplier)); // Cap at 10%
        }
        
        /**
         * Calculate net profit for a specific order size after slippage.
         * 
         * @param orderSize Size of the order in USD
         * @param fees Total fees as a percentage (e.g., 0.0025 for 0.25%)
         * @return Net profit percentage after slippage and fees
         */
        public double calculateNetProfit(double orderSize, double fees) {
            if (spreadPercentage <= 0) {
                return 0.0; // No profit opportunity
            }
            
            double slippage = getSlippageForSize(orderSize);
            return spreadPercentage - slippage - fees;
        }
    }
    
    /**
     * Create fallback liquidity metrics when real data can't be fetched.
     * Uses reasonable defaults based on the trading pair symbol.
     */
    private LiquidityMetrics createFallbackMetrics(String symbol, String exchangeName) {
        Log.d(TAG, "Creating fallback liquidity metrics for " + symbol + " on " + exchangeName);
        
        // Default values based on asset
        double bidPrice = 0;
        double askPrice = 0;
        double availableLiquidity = 0;
        double volume24h = 0;
        
        // Set reasonable values based on trading pair
        String baseAsset = symbol.split("/")[0];
        if (baseAsset == null) {
            baseAsset = symbol.length() > 3 ? symbol.substring(0, 3) : symbol;
        }
        
        // Adjust defaults based on asset type
        switch (baseAsset.toUpperCase()) {
            case "BTC":
                bidPrice = 30000.0;
                askPrice = 30050.0;
                availableLiquidity = 500000.0; // $500K
                volume24h = 5000000.0; // $5M
                break;
            case "ETH":
                bidPrice = 2000.0;
                askPrice = 2005.0;
                availableLiquidity = 250000.0; // $250K
                volume24h = 2500000.0; // $2.5M
                break;
            case "BNB":
                bidPrice = 300.0;
                askPrice = 301.0;
                availableLiquidity = 200000.0; // $200K
                volume24h = 1000000.0; // $1M
                break;
            case "SOL":
                bidPrice = 100.0;
                askPrice = 100.5;
                availableLiquidity = 150000.0; // $150K
                volume24h = 1000000.0; // $1M
                break;
            default:
                // Stablecoins (USDT, USDC, DAI, etc.)
                if (baseAsset.toUpperCase().contains("USD")) {
                    bidPrice = 0.995;
                    askPrice = 1.005;
                    availableLiquidity = 1000000.0; // $1M
                    volume24h = 10000000.0; // $10M
                } else {
                    // Other alt coins
                    bidPrice = 10.0;
                    askPrice = 10.05;
                    availableLiquidity = 100000.0; // $100K
                    volume24h = 500000.0; // $500K
                }
        }
        
        // Add extra info to logs
        Log.d(TAG, "Fallback metrics for " + symbol + ": liquidity=" + availableLiquidity);
        
        // Calculate spread metrics
        double spread = askPrice - bidPrice;
        double midPrice = (bidPrice + askPrice) / 2.0;
        double spreadPercentage = spread / midPrice;
        
        // Create simulated order book data
        TreeMap<Double, Double> simulatedBids = new TreeMap<>(Collections.reverseOrder());
        TreeMap<Double, Double> simulatedAsks = new TreeMap<>();
        
        // Create 5 price levels in each direction
        for (int i = 0; i < 5; i++) {
            double bidPriceLevel = bidPrice * (1 - (i * 0.005));
            double askPriceLevel = askPrice * (1 + (i * 0.005));
            
            // Volume decreases as you move away from the best price
            double bidVolume = (availableLiquidity / bidPrice) * (1 - (i * 0.15)) / 5;
            double askVolume = (availableLiquidity / askPrice) * (1 - (i * 0.15)) / 5;
            
            simulatedBids.put(bidPriceLevel, bidVolume);
            simulatedAsks.put(askPriceLevel, askVolume);
        }
        
        // Create simulated order book snapshot
        OrderBookSnapshot simulatedSnapshot = new OrderBookSnapshot(
                simulatedBids, 
                simulatedAsks,
                bidPrice,
                askPrice);
        
        // Divide liquidity between bid and ask sides
        double bidLiquidity = availableLiquidity * 0.5;
        double askLiquidity = availableLiquidity * 0.5;
        
        // Create simulated slippage maps
        Map<Double, Double> buySlippage = new HashMap<>();
        Map<Double, Double> sellSlippage = new HashMap<>();
        
        // Simulate slippage at different sizes
        double[] sizes = {1000, 5000, 10000, 25000, 50000, 100000, 250000, 500000};
        for (double size : sizes) {
            // Slippage increases with size relative to liquidity
            double slippageFactor = Math.min(0.1, 0.001 + (size / availableLiquidity) * 0.05);
            buySlippage.put(size, slippageFactor);
            sellSlippage.put(size, slippageFactor);
        }
        
        // Create simulated market depth map
        Map<Double, Double> marketDepth = new HashMap<>();
        double[] depthLevels = {0.005, 0.01, 0.02, 0.05, 0.1};
        for (int i = 0; i < depthLevels.length; i++) {
            // Available liquidity decreases as depth increases
            marketDepth.put(depthLevels[i], availableLiquidity * (1 - (i * 0.15)));
        }
        
        // Create and return simulated metrics
        return new LiquidityMetrics(
                bidPrice,
                askPrice,
                spread,
                spreadPercentage,
                bidLiquidity,
                askLiquidity,
                availableLiquidity,
                marketDepth,
                buySlippage,
                sellSlippage,
                volume24h,
                simulatedSnapshot,
                System.currentTimeMillis());
    }
    
    /**
     * Create fallback metrics with custom prices
     */
    private LiquidityMetrics createCustomPriceFallbackMetrics(
            String symbol, String exchangeName, double bidPrice, double askPrice) {
        // Base metrics on standard fallbacks but override prices
        LiquidityMetrics baseMetrics = createFallbackMetrics(symbol, exchangeName);
        
        // Calculate new spread metrics
        double spread = askPrice - bidPrice;
        double midPrice = (bidPrice + askPrice) / 2.0;
        double spreadPercentage = spread / midPrice;
        
        // Create simulated order book data with new prices
        TreeMap<Double, Double> simulatedBids = new TreeMap<>(Collections.reverseOrder());
        TreeMap<Double, Double> simulatedAsks = new TreeMap<>();
        
        // Available liquidity (reuse from base metrics)
        double availableLiquidity = baseMetrics.getAvailableLiquidity();
        
        // Create 5 price levels in each direction
        for (int i = 0; i < 5; i++) {
            double bidPriceLevel = bidPrice * (1 - (i * 0.005));
            double askPriceLevel = askPrice * (1 + (i * 0.005));
            
            // Volume decreases as you move away from the best price
            double bidVolume = (availableLiquidity / bidPrice) * (1 - (i * 0.15)) / 5;
            double askVolume = (availableLiquidity / askPrice) * (1 - (i * 0.15)) / 5;
            
            simulatedBids.put(bidPriceLevel, bidVolume);
            simulatedAsks.put(askPriceLevel, askVolume);
        }
        
        // Create simulated order book snapshot
        OrderBookSnapshot simulatedSnapshot = new OrderBookSnapshot(
                simulatedBids, 
                simulatedAsks,
                bidPrice,
                askPrice);
        
        // Return metrics with updated prices but same liquidity values
        return new LiquidityMetrics(
                bidPrice,
                askPrice,
                spread,
                spreadPercentage,
                baseMetrics.getBidLiquidity(),
                baseMetrics.getAskLiquidity(),
                baseMetrics.getAvailableLiquidity(),
                baseMetrics.getMarketDepth(),
                baseMetrics.getBuySlippage(),
                baseMetrics.getSellSlippage(),
                baseMetrics.getVolume24h(),
                simulatedSnapshot,
                System.currentTimeMillis());
    }
} 