package com.example.tradient.data.service;

import com.example.tradient.data.fee.FeeType;
import com.example.tradient.data.fee.FixedFee;
import com.example.tradient.data.fee.PercentageFee;
import com.example.tradient.data.fee.TieredFee;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;
import com.example.tradient.data.fee.ExchangeFeeFactory;
import com.example.tradient.data.fee.Fee;
import com.example.tradient.data.fee.FeeTracker;
import com.example.tradient.data.fee.TransactionFee;
import com.example.tradient.data.interfaces.IExchangeService;
import com.example.tradient.data.interfaces.INotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Base abstract class for cryptocurrency exchange services.
 * 
 * This class provides a foundation for exchange-specific implementations,
 * handling common functionality such as caching, fee calculation, and
 * data management.
 * 
 * Key features:
 * - Caching of ticker data, order books, and trading pairs
 * - Advanced fee calculation with support for tiered and discounted fees
 * - WebSocket connection management for real-time data
 * - Fee tracking and reporting
 * - Support for BNB fee discounts (Binance specific)
 * 
 * When extended by exchange-specific implementations, this provides
 * a consistent interface for interacting with various cryptocurrency
 * exchanges while handling the unique aspects of each platform.
 * 
 * Compatible with Android platform.
 */
public abstract class ExchangeService implements IExchangeService {

    // Unique exchange identifier
    private String exchangeName;

    // Exchange logo reference for UI display
    private String logoResource;

    // Property to cache trading pairs
    private List<TradingPair> tradingPairs;

    // Cache for real-time ticker data
    protected ConcurrentHashMap<String, Ticker> tickerCache;

    // Cache for real-time order book data
    protected ConcurrentHashMap<String, OrderBook> orderBookCache;

    // Flag to track if WebSocket connection is active
    protected volatile boolean websocketConnected = false;

    // Fee structures
    private Fee makerFee;
    private Fee takerFee;
    
    // Fee tracker for this exchange
    private FeeTracker feeTracker;
    
    // The 30-day trading volume used for fee tier calculations
    protected double thirtyDayTradingVolume;
    
    // Whether this exchange has BNB discount (Binance only)
    protected boolean hasBnbDiscount;

    // Optional notification service
    private INotificationService notificationService;

    // Constructor to initialize the exchange name and the trading pairs cache
    public ExchangeService(String exchangeName, double fees) {
        this.exchangeName = exchangeName;
        this.tradingPairs = new ArrayList<>();
        this.tickerCache = new ConcurrentHashMap<>();
        this.orderBookCache = new ConcurrentHashMap<>();
        this.feeTracker = new FeeTracker();
        this.thirtyDayTradingVolume = 0.0;
        this.hasBnbDiscount = false;
        
        // Initialize with simple percentage fees to maintain backward compatibility
        this.makerFee = ExchangeFeeFactory.getInstance().getDefaultMakerFee(exchangeName);
        this.takerFee = ExchangeFeeFactory.getInstance().getDefaultTakerFee(exchangeName);
    }
    
    /**
     * Constructor with expanded fee options.
     *
     * @param exchangeName The exchange name
     * @param makerFee The maker fee
     * @param takerFee The taker fee
     */
    public ExchangeService(String exchangeName, Fee makerFee, Fee takerFee) {
        this.exchangeName = exchangeName;
        this.tradingPairs = new ArrayList<>();
        this.tickerCache = new ConcurrentHashMap<>();
        this.orderBookCache = new ConcurrentHashMap<>();
        this.feeTracker = new FeeTracker();
        this.thirtyDayTradingVolume = 0.0;
        this.hasBnbDiscount = false;
        this.makerFee = makerFee;
        this.takerFee = takerFee;
    }

    /**
     * Returns the cached list of trading pairs.
     */
    public List<TradingPair> getTradingPairs() {
        if (tradingPairs == null || tradingPairs.isEmpty()) {
            tradingPairs = fetchTradingPairs();
        }
        return tradingPairs;
    }

    /**
     * Updates the trading pairs cache with a new list.
     */
    protected void setTradingPairs(List<TradingPair> pairs) {
        this.tradingPairs = pairs;
        logInfo("Loaded " + pairs.size() + " trading pairs for " + exchangeName);
    }

    /**
     * Fetches the list of trading pairs from the exchange API,
     * updates the internal cache, and returns the updated list.
     */
    public abstract List<TradingPair> fetchTradingPairs();

    /**
     * Retrieves the latest ticker data for the specified symbol.
     * Will try to use cached WebSocket data if available, otherwise falls back to REST.
     */
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

    /**
     * Retrieves the latest ticker data using REST API (fallback method)
     */
    public abstract Ticker fetchTickerDataREST(String symbol);

    /**
     * Retrieves the current order book for the specified trading pair.
     * Will try to use cached WebSocket data if available, otherwise falls back to REST.
     */
    public OrderBook getOrderBook(String symbol) {
        OrderBook orderBook = orderBookCache.get(symbol);
        
        if (orderBook == null || isOrderBookStale(orderBook)) {
            orderBook = fetchOrderBookREST(symbol);
            if (orderBook != null) {
                orderBookCache.put(symbol, orderBook);
            }
        }
        return orderBook;
    }

    /**
     * Retrieves the current order book using REST API (fallback method)
     */
    public abstract OrderBook fetchOrderBookREST(String symbol);

    /**
     * Initialize WebSocket connection.
     *
     * @param symbols The trading pair symbols to subscribe to
     * @return true if the WebSocket connection was successfully initialized
     */
    public boolean initializeWebSocket(List<String> symbols) {
        INotificationService notificationService = getNotificationService();
        if (notificationService != null) {
            notificationService.logInfo(exchangeName + ": Initializing WebSocket connection for " + symbols.size() + " symbols");
            
            // Log some of the symbols we're subscribing to for debugging
            if (!symbols.isEmpty()) {
                StringBuilder sampleSymbols = new StringBuilder("Sample symbols: ");
                for (int i = 0; i < Math.min(5, symbols.size()); i++) {
                    sampleSymbols.append(symbols.get(i)).append(", ");
                }
                notificationService.logDebug(exchangeName + ": " + sampleSymbols.toString());
            }
        }
        
        try {
            // Call the implementation-specific method
            return initializeWebSocketImpl(symbols);
        } catch (Exception e) {
            if (notificationService != null) {
                notificationService.logError(exchangeName + ": Error initializing WebSocket", e);
            }
            return false;
        }
    }
    
    /**
     * Implementation-specific WebSocket initialization.
     * This should be overridden by exchange-specific implementations.
     *
     * @param symbols The trading pair symbols to subscribe to
     * @return true if the WebSocket connection was successfully initialized
     */
    protected boolean initializeWebSocketImpl(List<String> symbols) {
        // Default implementation does nothing
        return false;
    }

    /**
     * Closes the WebSocket connections
     */
    public abstract void closeWebSocket();

    /**
     * Returns the exchange's unique name.
     */
    public String getExchangeName() {
        return exchangeName;
    }

    /**
     * Get the maker fee for this exchange.
     * 
     * @return The maker fee structure
     */
    public Fee getMakerFee() {
        return makerFee;
    }
    
    /**
     * Get the taker fee for this exchange.
     * 
     * @return The taker fee structure
     */
    public Fee getTakerFee() {
        return takerFee;
    }
    
    /**
     * Update the fee structures based on trading volume.
     * 
     * @param thirtyDayVolume The 30-day trading volume in USD
     */
    public void updateFeesTiers(double thirtyDayVolume) {
        this.thirtyDayTradingVolume = thirtyDayVolume;
        
        // Get updated fees based on volume
        ExchangeFeeFactory feeFactory = ExchangeFeeFactory.getInstance();
        this.makerFee = feeFactory.createFee(exchangeName, thirtyDayVolume, true, hasBnbDiscount);
        this.takerFee = feeFactory.createFee(exchangeName, thirtyDayVolume, false, hasBnbDiscount);
        
        logInfo("Updated fee tiers for " + exchangeName + " based on $" + thirtyDayVolume + " volume");
    }
    
    /**
     * Set whether BNB discount is applied (for Binance only).
     * 
     * @param hasBnbDiscount true if BNB discount is applied
     */
    public void setBnbDiscount(boolean hasBnbDiscount) {
        if (this.hasBnbDiscount != hasBnbDiscount) {
            this.hasBnbDiscount = hasBnbDiscount;
            // Refresh fee structures
            updateFeesTiers(thirtyDayTradingVolume);
        }
    }
    
    /**
     * Get the fee tracker for this exchange.
     * 
     * @return The fee tracker
     */
    public FeeTracker getFeeTracker() {
        return feeTracker;
    }
    
    /**
     * Calculate and track the fee for a transaction.
     * 
     * @param tradingPair The trading pair symbol
     * @param amount The transaction amount
     * @param isMaker Whether this is a maker order (true) or taker order (false)
     * @return The calculated fee amount
     */
    public double calculateAndTrackFee(String tradingPair, double amount, boolean isMaker) {
        // Check if this is a BNB trading pair (free trading)
        // Make sure we properly recognize all BNB pairs in various formats
        boolean isBnbPair = false;
        if (tradingPair != null) {
            String pair = tradingPair.toUpperCase();
            isBnbPair = pair.startsWith("BNB") || pair.endsWith("BNB") || 
                       pair.contains("BNB-") || pair.contains("-BNB") ||
                       pair.contains("BNB/") || pair.contains("/BNB");
        }
        
        // **SPECIAL CASE FOR BNB PAIRS - WE'RE FORCING EXPLICIT PATH HERE**
        // Apply zero fees for BNB pairs on Binance, but only on Binance
        if (isBnbPair && "Binance".equals(exchangeName)) {
            // Log for traceability
            logDebug("Zero fees applied for BNB pair: " + tradingPair);
            
            // Create a zero-fee transaction record with special description
            TransactionFee transactionFee = new TransactionFee(
                    "tx-" + System.currentTimeMillis(),
                    exchangeName,
                    tradingPair,
                    0.0, // zero fee amount
                    FeeType.FIXED, // fee type
                    null,
                    "Zero fees for BNB pair - Special Rule",
                    0.0, // zero fee percentage
                    0.0, // no discount (since it's already free)
                    isMaker
            );
            
            // Track the fee
            feeTracker.trackFee(transactionFee);
            
            // Return exactly zero
            return 0.0;
        }
        
        // For non-BNB pairs, use the standard fee logic
        Fee fee = isMaker ? makerFee : takerFee;
        
        // For Binance, the BNB discount applies to all other assets when enabled
        if ("Binance".equals(exchangeName) && hasBnbDiscount) {
            logDebug("Using Binance fee with BNB discount for " + tradingPair);
        }
        
        double feeAmount = fee.calculateFee(amount);
        
        // Safely extract the fee percentage without assuming PercentageFee type
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
                hasBnbDiscount ? 0.25 : 0.0,
                isMaker
        );
        
        // Track the fee
        feeTracker.trackFee(transactionFee);
        
        return feeAmount;
    }
    
    /**
     * Safely extract the effective fee percentage from any Fee implementation.
     * 
     * @param fee The fee object
     * @param amount The transaction amount to calculate percentage from if needed
     * @return The fee percentage as a decimal (e.g., 0.001 for 0.1%)
     */
    private double getEffectiveFeePercentage(Fee fee, double amount) {
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
    
    /**
     * Calculate the buy fee for a transaction.
     * 
     * @param price The price
     * @param quantity The quantity
     * @return The fee amount
     */
    public double calculateBuyFee(double price, double quantity) {
        double amount = price * quantity;
        return takerFee.calculateFee(amount);
    }
    
    /**
     * Calculate the sell fee for a transaction.
     * 
     * @param price The price
     * @param quantity The quantity
     * @return The fee amount
     */
    public double calculateSellFee(double price, double quantity) {
        double amount = price * quantity;
        return takerFee.calculateFee(amount);
    }

    /**
     * Checks if the WebSocket connection is active
     */
    public boolean isWebSocketConnected() {
        return websocketConnected;
    }

    /**
     * Gets the trading fee for this exchange.
     *
     * @return The trading fee as a decimal (e.g., 0.001 for 0.1%)
     * @deprecated Use getMakerFee() or getTakerFee() instead
     */
    @Deprecated
    public double getTradingFees() {
        // For backward compatibility, safely get the taker fee percentage
        return getEffectiveFeePercentage(takerFee, 10000); // Use a standard amount for percentage calculation
    }
    
    /**
     * Gets the legacy fee value for backward compatibility.
     * 
     * @deprecated Use getMakerFee() or getTakerFee() instead
     */
    @Deprecated
    public double getFees() {
        return getTradingFees();
    }

    /**
     * Check if ticker data is stale and should be refreshed.
     * Default implementation considers data older than 30 seconds as stale.
     * 
     * @param ticker The ticker to check
     * @return true if the ticker data is stale, false otherwise
     */
    public boolean isTickerStale(Ticker ticker) {
        if (ticker == null) {
            return true;
        }
        
        // Check if data is older than 30 seconds
        long currentTime = System.currentTimeMillis();
        long tickerTime = ticker.getTimestamp().getTime();
        return (currentTime - tickerTime) > 30000;
    }
    
    /**
     * Check if order book data is stale and should be refreshed.
     * Default implementation considers data older than 10 seconds as stale.
     * 
     * @param orderBook The order book to check
     * @return true if the order book data is stale, false otherwise
     */
    public boolean isOrderBookStale(OrderBook orderBook) {
        if (orderBook == null) {
            return true;
        }
        
        // Check if data is older than 10 seconds
        long currentTime = System.currentTimeMillis();
        // Get timestamp value directly as it's now a long
        long orderBookTime = orderBook.getTimestamp();
        return (currentTime - orderBookTime) > 10000;
    }

    /**
     * Check if a specific fee discount is enabled.
     *
     * @param discountType The type of discount
     * @return true if the discount is enabled, false otherwise
     */
    public boolean hasFeeDiscount(String discountType) {
        if ("BNB".equals(discountType)) {
            return hasBnbDiscount;
        }
        return false;
    }

    /**
     * Set whether a specific fee discount is enabled.
     *
     * @param discountType The type of discount
     * @param enabled Whether the discount is enabled
     */
    public void setFeeDiscount(String discountType, boolean enabled) {
        if ("BNB".equals(discountType)) {
            // Don't call setBnbDiscount() here to avoid infinite recursion
            this.hasBnbDiscount = enabled;
        }
    }

    /**
     * Set the notification service for this exchange.
     *
     * @param notificationService The notification service to use
     */
    public void setNotificationService(INotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Get the current notification service.
     *
     * @return The notification service or null if not set
     */
    public INotificationService getNotificationService() {
        return notificationService;
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

    /**
     * Determines if a trade would be executed as a maker or taker order.
     * 
     * @param symbol The trading pair
     * @param price The price at which the order would be placed
     * @param isBuy Whether this is a buy order (true) or sell order (false)
     * @return true if this would likely be a maker order, false if it would be a taker
     */
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

    /**
     * Get a ticker for a specific trading pair.
     * This is an alias for getTickerData for consistency with interface.
     *
     * @param tradingPair The trading pair to get the ticker for
     * @return The ticker, or null if not available
     */
    public Ticker getTicker(String tradingPair) {
        return getTickerData(tradingPair);
    }

    /**
     * Gets the fee percentage for a specific trading pair and maker/taker status
     *
     * @param tradingPair The trading pair
     * @param isMaker Whether the order is a maker (true) or taker (false)
     * @return The fee percentage as a decimal (e.g., 0.001 for 0.1%)
     */
    public double getFeePercentage(String tradingPair, boolean isMaker) {
        // Use the appropriate fee based on maker/taker status
        Fee fee = isMaker ? getMakerFee() : getTakerFee();
        
        // Use the helper method that safely gets the fee percentage from any Fee implementation
        return getEffectiveFeePercentage(fee, 10000); // Using standard amount for calculation
    }

    /**
     * Sets the logo resource for the exchange
     *
     * @param logoResource The logo resource identifier
     */
    public void setLogoResource(String logoResource) {
        // Store the logo resource identifier
        this.logoResource = logoResource;
    }

    /**
     * Gets the logo resource for the exchange
     *
     * @return The logo resource identifier
     */
    public String getLogoResource() {
        return logoResource;
    }

    /**
     * Get historical ticker data for a symbol
     * @param symbol The trading symbol (e.g., BTC/USD)
     * @param hoursBack Number of hours of historical data to retrieve
     * @return List of ticker data ordered from oldest to newest
     */
    public abstract List<Ticker> getHistoricalTickers(String symbol, int hoursBack);

    /**
     * Get the estimated API response time in milliseconds
     * @return The estimated response time
     */
    public abstract int getEstimatedResponseTimeMs();

    /**
     * Get available trading pairs
     * @return List of trading pairs
     */
    public abstract List<TradingPair> getAvailableTradingPairs();

    /**
     * Get the exchange fee for a symbol and order type
     * @param symbol The trading symbol
     * @param isMaker Whether the order is a maker order
     * @return The fee as a decimal percentage (e.g., 0.001 for 0.1%)
     */
    public abstract double getExchangeFee(String symbol, boolean isMaker);
}