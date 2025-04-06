package com.example.tradient.data.service;

import com.example.tradient.data.fee.ExchangeFeeFactory;
import com.example.tradient.data.fee.Fee;
import com.example.tradient.data.fee.FeeTracker;
import com.example.tradient.data.fee.FixedFee;
import com.example.tradient.data.fee.TieredFee;
import com.example.tradient.data.fee.TransactionFee;
import com.example.tradient.data.interfaces.IExchangeService;
import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.interfaces.IWebSocketUpdateListener;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;
import com.example.tradient.data.service.websocket.BaseWebSocketProvider;
import com.example.tradient.data.fee.PercentageFee;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base implementation of the exchange service interfaces.
 * Provides common functionality for all exchange services, following the Template Method pattern.
 */
public abstract class BaseExchangeService implements IExchangeService {
    
    // Unique exchange identifier
    private final String exchangeName;
    
    // Exchange logo reference for UI display
    protected String logoResource;
    
    // Property to cache trading pairs
    private List<TradingPair> tradingPairs;
    
    // Cache for real-time ticker data
    protected final ConcurrentHashMap<String, Ticker> tickerCache;
    
    // Cache for real-time order book data
    protected final ConcurrentHashMap<String, OrderBook> orderBookCache;
    
    // Fee structures
    private Fee makerFee;
    private Fee takerFee;
    
    // Fee tracker for this exchange
    private final FeeTracker feeTracker;
    
    // The 30-day trading volume used for fee tier calculations
    protected double thirtyDayTradingVolume;
    
    // Map of discount types to their enabled status
    private final Map<String, Boolean> feeDiscounts;
    
    // Optional notification service
    private INotificationService notificationService;
    
    // WebSocket provider
    private BaseWebSocketProvider webSocketProvider;
    
    /**
     * Constructor to initialize the exchange service.
     *
     * @param exchangeName The name of the exchange
     * @param fees Default fee rate (for backward compatibility)
     */
    public BaseExchangeService(String exchangeName, double fees) {
        this.exchangeName = exchangeName;
        this.tradingPairs = new ArrayList<>();
        this.tickerCache = new ConcurrentHashMap<>();
        this.orderBookCache = new ConcurrentHashMap<>();
        this.feeTracker = new FeeTracker();
        this.thirtyDayTradingVolume = 0.0;
        this.feeDiscounts = new HashMap<>();
        
        // Initialize with simple percentage fees to maintain backward compatibility
        this.makerFee = ExchangeFeeFactory.getInstance().getDefaultMakerFee(exchangeName);
        this.takerFee = ExchangeFeeFactory.getInstance().getDefaultTakerFee(exchangeName);
        
        // Initialize the WebSocket provider
        this.webSocketProvider = createWebSocketProvider();
    }
    
    /**
     * Factory method for creating the WebSocket provider.
     * To be implemented by concrete exchange service classes.
     *
     * @return A WebSocket provider for this exchange
     */
    protected abstract BaseWebSocketProvider createWebSocketProvider();
    
    @Override
    public String getExchangeName() {
        return exchangeName;
    }
    
    @Override
    public List<TradingPair> getTradingPairs() {
        if (tradingPairs == null || tradingPairs.isEmpty()) {
            tradingPairs = fetchTradingPairs();
        }
        return tradingPairs;
    }
    
    @Override
    public abstract List<TradingPair> fetchTradingPairs();
    
    @Override
    public Ticker getTickerData(String symbol) {
        Ticker ticker = tickerCache.get(symbol);
        
        if (ticker == null || isTickerStale(ticker)) {
            ticker = fetchTickerDataREST(symbol);
            if (ticker != null) {
                tickerCache.put(symbol, ticker);
            }
        }
        return ticker;
    }
    
    @Override
    public abstract Ticker fetchTickerDataREST(String symbol);
    
    @Override
    public OrderBook getOrderBook(String symbol) {
        OrderBook orderBook = orderBookCache.get(symbol);
        
        if (orderBook == null || isOrderBookStale(orderBook)) {
            orderBook = fetchOrderBookREST(symbol);
            if (orderBook != null) {
                orderBookCache.put(symbol, orderBook);
            }
        }
        
        // Set exchange name and ticker in the OrderBook
        if (orderBook != null) {
            orderBook.setExchangeName(exchangeName);
            Ticker ticker = getTickerData(symbol);
            orderBook.setTicker(ticker);
        }
        
        return orderBook;
    }
    
    @Override
    public abstract OrderBook fetchOrderBookREST(String symbol);
    
    @Override
    public boolean isTickerStale(Ticker ticker) {
        // Default implementation: consider a ticker stale if it's older than 5 seconds
        return System.currentTimeMillis() - ticker.getTimestamp().getTime() > 5000;
    }
    
    /**
     * Check if an order book is stale and needs to be refreshed.
     * Default implementation considers an order book stale if it is older than 5 seconds.
     *
     * @param orderBook The order book to check
     * @return true if the order book is stale, false otherwise
     */
    public boolean isOrderBookStale(OrderBook orderBook) {
        if (orderBook == null) {
            return true;
        }
        return System.currentTimeMillis() - orderBook.getTimestamp() > 5000;
    }
    
    @Override
    public boolean isMakerOrder(String symbol, double price, boolean isBuy) {
        // This is a simplified determination. In reality, this depends on:
        // 1. Limit vs. Market orders (market orders are always takers)
        // 2. The current order book and whether your order would be matched immediately
        
        // Get the current order book
        OrderBook orderBook = getOrderBook(symbol);
        if (orderBook == null) {
            return false; // Default to taker if we can't determine
        }
        
        if (isBuy) {
            // For a buy order to be a maker, it must be below the current lowest ask
            double lowestAsk = orderBook.getBestAsk() != null ? orderBook.getBestAsk().getPrice() : 0;
            return lowestAsk > 0 && price < lowestAsk;
        } else {
            // For a sell order to be a maker, it must be above the current highest bid
            double highestBid = orderBook.getBestBid() != null ? orderBook.getBestBid().getPrice() : 0;
            return highestBid > 0 && price > highestBid;
        }
    }
    
    @Override
    public Fee getMakerFee() {
        return makerFee;
    }
    
    @Override
    public Fee getTakerFee() {
        return takerFee;
    }
    
    @Override
    public Fee getMakerFee(String tradingPair) {
        // Default implementation just returns the standard maker fee.
        // Specific exchanges can override this for special pairs.
        return makerFee;
    }
    
    @Override
    public Fee getTakerFee(String tradingPair) {
        // Default implementation just returns the standard taker fee.
        // Specific exchanges can override this for special pairs.
        return takerFee;
    }
    
    @Override
    public void updateFeesTiers(double thirtyDayVolume) {
        this.thirtyDayTradingVolume = thirtyDayVolume;
        
        // Get updated fees based on volume
        ExchangeFeeFactory feeFactory = ExchangeFeeFactory.getInstance();
        
        // For Binance, consider BNB discount
        boolean bnbDiscount = hasFeeDiscount("BNB");
        this.makerFee = feeFactory.createFee(exchangeName, thirtyDayVolume, true, bnbDiscount);
        this.takerFee = feeFactory.createFee(exchangeName, thirtyDayVolume, false, bnbDiscount);
        
        logInfo("Updated fee tiers for " + exchangeName + " based on $" + thirtyDayVolume + " volume");
    }
    
    @Override
    public double calculateAndTrackFee(String tradingPair, double amount, boolean isMaker) {
        // Determine the appropriate fee structure
        Fee fee = isMaker ? getMakerFee(tradingPair) : getTakerFee(tradingPair);
        
        // Calculate the fee amount
        double feeAmount = fee.calculateFee(amount);
        
        // Determine the fee percentage for tracking
        double feePercentage = getEffectiveFeePercentage(fee, amount);
        
        // Create a transaction fee record
        TransactionFee transactionFee = new TransactionFee(
                "tx-" + System.currentTimeMillis(),
                exchangeName,
                tradingPair,
                feeAmount,
                fee.getType(),
                null,
                fee.getDescription(),
                feePercentage,
                getFeeDiscountRate(tradingPair),
                isMaker
        );
        
        // Track the fee
        feeTracker.trackFee(transactionFee);
        
        return feeAmount;
    }
    
    /**
     * Get the discount rate for a trading pair.
     * This can be overridden by specific exchanges.
     *
     * @param tradingPair The trading pair
     * @return The discount rate (e.g., 0.25 for 25% discount)
     */
    protected double getFeeDiscountRate(String tradingPair) {
        // Default implementation
        return hasFeeDiscount("BNB") ? 0.25 : 0.0;
    }
    
    /**
     * Safely extract the effective fee percentage from any Fee implementation.
     *
     * @param fee The fee object
     * @param amount The transaction amount to calculate percentage from if needed
     * @return The fee percentage as a decimal (e.g., 0.001 for 0.1%)
     */
    protected double getEffectiveFeePercentage(Fee fee, double amount) {
        if (fee instanceof PercentageFee) {
            return ((PercentageFee) fee).getPercentage();
        } else if (fee instanceof TieredFee) {
            return ((TieredFee) fee).getCurrentFeeRate();
        } else if (fee instanceof FixedFee) {
            // For fixed fees, calculate an effective percentage based on the amount
            double fixedAmount = ((FixedFee) fee).getFeeAmount();
            return amount > 0 ? fixedAmount / amount : 0;
        } else {
            // For any other fee type, calculate the effective percentage
            double feeAmount = fee.calculateFee(amount);
            return amount > 0 ? feeAmount / amount : 0;
        }
    }
    
    @Override
    public FeeTracker getFeeTracker() {
        return feeTracker;
    }
    
    @Override
    public void setFeeDiscount(String discountType, boolean enabled) {
        boolean oldValue = feeDiscounts.getOrDefault(discountType, false);
        if (oldValue != enabled) {
            feeDiscounts.put(discountType, enabled);
            
            // Refresh fees as the discount status changed
            updateFeesTiers(thirtyDayTradingVolume);
        }
    }
    
    @Override
    public boolean hasFeeDiscount(String discountType) {
        return feeDiscounts.getOrDefault(discountType, false);
    }
    
    @Override
    public boolean initializeWebSocket(List<String> symbols) {
        return webSocketProvider.initializeWebSocket(symbols);
    }
    
    @Override
    public void closeWebSocket() {
        webSocketProvider.closeWebSocket();
    }
    
    @Override
    public boolean isWebSocketConnected() {
        return webSocketProvider.isWebSocketConnected();
    }
    
    @Override
    public void addWebSocketListener(IWebSocketUpdateListener listener) {
        webSocketProvider.addWebSocketListener(listener);
    }
    
    @Override
    public void removeWebSocketListener(IWebSocketUpdateListener listener) {
        webSocketProvider.removeWebSocketListener(listener);
    }
    
    @Override
    public void setNotificationService(INotificationService notificationService) {
        this.notificationService = notificationService;
        
        // Pass it to the WebSocket provider as well
        if (webSocketProvider != null) {
            webSocketProvider.notificationService = notificationService;
        }
    }
    
    /**
     * Protected method for setting trading pairs.
     * This updates the internal cache.
     *
     * @param pairs The list of trading pairs
     */
    protected void setTradingPairs(List<TradingPair> pairs) {
        this.tradingPairs = pairs;
        logInfo("Loaded " + pairs.size() + " trading pairs for " + exchangeName);
    }
    
    /**
     * Log an informational message if notification service is available.
     *
     * @param message The message to log
     */
    protected void logInfo(String message) {
        if (notificationService != null) {
            notificationService.logInfo(exchangeName + ": " + message);
        }
    }
    
    /**
     * Log a warning message if notification service is available.
     *
     * @param message The message to log
     */
    protected void logWarning(String message) {
        if (notificationService != null) {
            notificationService.logWarning(exchangeName + ": " + message);
        }
    }
    
    /**
     * Log an error message if notification service is available.
     *
     * @param message The message to log
     * @param t The throwable associated with the error
     */
    protected void logError(String message, Throwable t) {
        if (notificationService != null) {
            notificationService.logError(exchangeName + ": " + message, t);
        }
    }
    
    /**
     * Log a debug message if notification service is available.
     *
     * @param message The message to log
     */
    protected void logDebug(String message) {
        if (notificationService != null) {
            notificationService.logDebug(exchangeName + ": " + message);
        }
    }
} 