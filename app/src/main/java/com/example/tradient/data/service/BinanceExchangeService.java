package com.example.tradient.data.service;

import com.example.tradient.data.fee.ExchangeFeeFactory;
import com.example.tradient.data.fee.Fee;
import com.example.tradient.data.http.HttpService;
import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.interfaces.IWebSocketUpdateListener;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;
import com.example.tradient.data.service.websocket.BaseWebSocketProvider;
import com.example.tradient.data.service.websocket.BinanceWebSocketProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Binance exchange service implementation.
 * Follows SOLID principles with proper dependency injection and no direct console output.
 */
public class BinanceExchangeService extends ExchangeService {

    // API endpoints
    private static final String BASE_URL = "https://api.binance.com";
    
    // HTTP client for REST API calls
    private final HttpService httpService;
    
    // Special fee structures for BNB pairs
    private Fee bnbMakerFee;
    private Fee bnbTakerFee;
    private Fee nonBnbMakerFee;
    private Fee nonBnbTakerFee;
    
    // WebSocket provider
    private BaseWebSocketProvider webSocketProvider;
    
    /**
     * Constructor with notification service.
     *
     * @param fees Default fee rate
     * @param notificationService Notification service for logging
     */
    public BinanceExchangeService(double fees, INotificationService notificationService) {
        super("Binance", fees);
        setNotificationService(notificationService);
        
        // Initialize HTTP client
        this.httpService = new HttpService();
                
        // Initialize special fee handling for BNB pairs
        initializeSpecialFees();
        
        // Initialize WebSocket provider
        this.webSocketProvider = createWebSocketProvider();
    }
    
    /**
     * Simple constructor without notification service.
     *
     * @param fees Default fee rate
     */
    public BinanceExchangeService(double fees) {
        this(fees, null);
    }
    
    /**
     * Creates a WebSocket provider for Binance.
     *
     * @return The WebSocket provider
     */
    protected BaseWebSocketProvider createWebSocketProvider() {
        return new BinanceWebSocketProvider(null);
    }
    
    /**
     * Initialize special fees for BNB pairs vs regular pairs.
     * This is needed because BNB pairs have different fee structures.
     */
    private void initializeSpecialFees() {
        // Standard fee factory
        ExchangeFeeFactory feeFactory = ExchangeFeeFactory.getInstance();
        
        // Get the current 30-day volume
        double volume = thirtyDayTradingVolume;
        
        // Check if BNB discount is enabled
        boolean hasBnbDiscount = hasFeeDiscount("BNB");
        
        // Create specialized fees for BNB pairs
        this.bnbMakerFee = feeFactory.createBinanceFee(volume, true, true, false);
        this.bnbTakerFee = feeFactory.createBinanceFee(volume, false, true, false);
        
        // Create specialized fees for non-BNB pairs
        this.nonBnbMakerFee = feeFactory.createBinanceFee(volume, true, false, hasBnbDiscount);
        this.nonBnbTakerFee = feeFactory.createBinanceFee(volume, false, false, hasBnbDiscount);
        
        logInfo("Initialized BNB and non-BNB specific fees");
    }
    
    @Override
    public Fee getMakerFee(String tradingPair) {
        boolean isBnbPair = tradingPair != null && 
                          (tradingPair.startsWith("BNB") || tradingPair.endsWith("BNB"));
                          
        return isBnbPair ? bnbMakerFee : nonBnbMakerFee;
    }
    
    @Override
    public Fee getTakerFee(String tradingPair) {
        boolean isBnbPair = tradingPair != null && 
                          (tradingPair.startsWith("BNB") || tradingPair.endsWith("BNB"));
                          
        return isBnbPair ? bnbTakerFee : nonBnbTakerFee;
    }
    
    @Override
    public void updateFeesTiers(double thirtyDayVolume) {
        super.updateFeesTiers(thirtyDayVolume);
        
        // Also update specialized fees
        ExchangeFeeFactory feeFactory = ExchangeFeeFactory.getInstance();
        
        // Check if BNB discount is enabled
        boolean hasBnbDiscount = hasFeeDiscount("BNB");
        
        // Update BNB pair fees
        this.bnbMakerFee = feeFactory.createBinanceFee(thirtyDayVolume, true, true, false);
        this.bnbTakerFee = feeFactory.createBinanceFee(thirtyDayVolume, false, true, false);
        
        // Update non-BNB pair fees
        this.nonBnbMakerFee = feeFactory.createBinanceFee(thirtyDayVolume, true, false, hasBnbDiscount);
        this.nonBnbTakerFee = feeFactory.createBinanceFee(thirtyDayVolume, false, false, hasBnbDiscount);
        
        logInfo("Updated BNB and non-BNB fees for volume: $" + thirtyDayVolume);
    }
    
    @Override
    public double calculateAndTrackFee(String tradingPair, double amount, boolean isMaker) {
        // Check if this is a BNB trading pair (free trading)
        boolean isBnbPair = false;
        if (tradingPair != null) {
            String pair = tradingPair.toUpperCase();
            isBnbPair = pair.startsWith("BNB") || pair.endsWith("BNB") || 
                       pair.contains("BNB-") || pair.contains("-BNB") ||
                       pair.contains("BNB/") || pair.contains("/BNB");
        }
        
        // Apply standard fee calculation
        return super.calculateAndTrackFee(tradingPair, amount, isMaker);
    }
    
    protected double getFeeDiscountRate(String tradingPair) {
        // Special case for BNB discount
        boolean isBnbPair = tradingPair != null && 
                          (tradingPair.startsWith("BNB") || tradingPair.endsWith("BNB"));
                          
        return !isBnbPair && hasFeeDiscount("BNB") ? 0.25 : 0.0;
    }
    
    @Override
    public List<TradingPair> fetchTradingPairs() {
        List<TradingPair> pairs = new ArrayList<>();
        
        try {
            String endpoint = BASE_URL + "/api/v3/exchangeInfo";
            String response = httpService.get(endpoint);
            
            JSONObject json = HttpService.parseJsonObject(response);
            JSONArray symbols = json.getJSONArray("symbols");

            for (int i = 0; i < symbols.length(); i++) {
                JSONObject symbol = symbols.getJSONObject(i);
                if ("TRADING".equals(symbol.getString("status"))) {
                    String baseAsset = symbol.getString("baseAsset");
                    String quoteAsset = symbol.getString("quoteAsset");
                    pairs.add(new TradingPair(baseAsset, quoteAsset));
                }
            }
            
            setTradingPairs(pairs);
            logInfo("Fetched " + pairs.size() + " trading pairs from Binance");
        } catch (IOException e) {
            logError("Error fetching trading pairs", e);
        } catch (org.json.JSONException e) {
            logError("Error parsing trading pairs JSON", e);
        }
        
        return pairs;
    }
    
    @Override
    public Ticker fetchTickerDataREST(String symbol) {
        try {
            String endpoint = BASE_URL + "/api/v3/ticker/bookTicker?symbol=" + symbol;
            String response = httpService.get(endpoint);
            
            JSONObject json = HttpService.parseJsonObject(response);
            double bidPrice = json.getDouble("bidPrice");
            double askPrice = json.getDouble("askPrice");
            
            // Get 24h volume from a separate endpoint
            endpoint = BASE_URL + "/api/v3/ticker/24hr?symbol=" + symbol;
            response = httpService.get(endpoint);
            
            double volume = 0;
            double lastPrice = 0;
            JSONObject volumeJson = HttpService.parseJsonObject(response);
            volume = volumeJson.getDouble("volume");
            lastPrice = volumeJson.getDouble("lastPrice");
            
            return new Ticker(bidPrice, askPrice, lastPrice, volume, new Date());
        } catch (IOException e) {
            logError("Error fetching ticker data for " + symbol, e);
        } catch (org.json.JSONException e) {
            logError("Error parsing ticker data JSON for " + symbol, e);
        }
        
        return null;
    }
    
    /**
     * Fetch the order book from Binance's REST API
     * 
     * @param symbol The trading pair symbol
     * @return The order book for the specified symbol
     */
    @Override
    public OrderBook fetchOrderBookREST(String symbol) {
        OrderBook orderBook = null;
        try {
            // Request a deeper order book (500 levels) for better market depth analysis
            String endpoint = BASE_URL + "/api/v3/depth?symbol=" + symbol.replace("/", "") + "&limit=500";
            String response = httpService.get(endpoint);
            
            JSONObject json = HttpService.parseJsonObject(response);
            
            // Parse bids and asks
            JSONArray bidsArray = json.getJSONArray("bids");
            JSONArray asksArray = json.getJSONArray("asks");
            
            List<OrderBookEntry> bids = new ArrayList<>();
            for (int i = 0; i < bidsArray.length(); i++) {
                JSONArray entry = bidsArray.getJSONArray(i);
                double price = Double.parseDouble(entry.getString(0));
                double volume = Double.parseDouble(entry.getString(1));
                bids.add(new OrderBookEntry(price, volume));
            }
            
            List<OrderBookEntry> asks = new ArrayList<>();
            for (int i = 0; i < asksArray.length(); i++) {
                JSONArray entry = asksArray.getJSONArray(i);
                double price = Double.parseDouble(entry.getString(0));
                double volume = Double.parseDouble(entry.getString(1));
                asks.add(new OrderBookEntry(price, volume));
            }
            
            // Create the order book with the current timestamp
            orderBook = new OrderBook(symbol, bids, asks, new Date());
            
            // Calculate and store additional metrics for market analysis
            if (orderBook != null) {
                // Calculate total volume within top price levels
                double totalBidVolume = calculateVolumeSum(bids, 10);
                double totalAskVolume = calculateVolumeSum(asks, 10);
                
                // Store these metrics in the order book for use in UI
                orderBook.setMetadata("totalBidVolume", totalBidVolume);
                orderBook.setMetadata("totalAskVolume", totalAskVolume);
                
                // Calculate bid-ask spread
                if (!bids.isEmpty() && !asks.isEmpty()) {
                    double highestBid = bids.get(0).getPrice();
                    double lowestAsk = asks.get(0).getPrice();
                    double spread = lowestAsk - highestBid;
                    double spreadPercentage = spread / lowestAsk * 100;
                    
                    orderBook.setMetadata("spreadAmount", spread);
                    orderBook.setMetadata("spreadPercentage", spreadPercentage);
                }
            }
            
        } catch (Exception e) {
            logError("Error fetching order book from Binance", e);
        }
        
        return orderBook;
    }
    
    /**
     * Calculate the sum of volumes for a specified number of price levels
     * 
     * @param entries The list of order book entries
     * @param levels The number of top levels to include
     * @return The sum of volumes
     */
    private double calculateVolumeSum(List<OrderBookEntry> entries, int levels) {
        double sum = 0;
        int count = 0;
        
        for (OrderBookEntry entry : entries) {
            sum += entry.getQuantity();
            count++;
            
            if (count >= levels) {
                break;
            }
        }
        
        return sum;
    }
    
    /**
     * Sets whether BNB fee discount is applied.
     * Implements the standard setFeeDiscount for Binance's BNB discount.
     * 
     * @param enabled true to enable BNB discount, false otherwise
     */
    public void setBnbDiscount(boolean enabled) {
        setFeeDiscount("BNB", enabled);
    }
    
    /**
     * Set whether a specific fee discount is enabled.
     * For Binance, this handles the BNB discount.
     *
     * @param discountType The type of discount
     * @param enabled Whether the discount is enabled
     */
    @Override
    public void setFeeDiscount(String discountType, boolean enabled) {
        if ("BNB".equals(discountType)) {
            super.setFeeDiscount(discountType, enabled);
            // Also update specialized fee structures
            initializeSpecialFees();
            logInfo("BNB discount " + (enabled ? "enabled" : "disabled"));
        } else {
            logWarning("Unsupported discount type: " + discountType);
        }
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

    /**
     * Get the exchange fee for a symbol and order type
     * @param symbol The trading symbol
     * @param isMaker Whether the order is a maker order
     * @return The fee as a decimal percentage (e.g., 0.001 for 0.1%)
     */
    @Override
    public double getExchangeFee(String symbol, boolean isMaker) {
        // Return standard Binance fees (0.1% maker and taker)
        return 0.001; // 0.1%
    }

    /**
     * Get the list of available trading pairs.
     * This implementation returns the cached trading pairs.
     *
     * @return List of trading pairs
     */
    @Override
    public List<TradingPair> getAvailableTradingPairs() {
        return getTradingPairs();
    }
    
    /**
     * Get the estimated response time for API calls to this exchange
     * @return The estimated response time in milliseconds
     */
    @Override
    public int getEstimatedResponseTimeMs() {
        // Binance API is generally fast, estimated average: 300ms
        return 300;
    }
    
    /**
     * Get historical ticker data for a given symbol
     * 
     * @param symbol The trading pair symbol
     * @param hoursBack Number of hours to look back
     * @return List of historical tickers
     */
    @Override
    public List<Ticker> getHistoricalTickers(String symbol, int hoursBack) {
        List<Ticker> tickers = new ArrayList<>();
        
        try {
            // Calculate end time (now) and start time (hoursBack hours ago)
            long endTime = System.currentTimeMillis();
            long startTime = endTime - (hoursBack * 3600 * 1000);
            
            // Construct Binance API endpoint for k-line data
            String interval = "1h"; // 1 hour candles
            String endpoint = BASE_URL + "/api/v3/klines" +
                    "?symbol=" + symbol.replace("/", "") + 
                    "&interval=" + interval +
                    "&startTime=" + startTime +
                    "&endTime=" + endTime +
                    "&limit=" + (hoursBack + 1);
            
            String response = httpService.get(endpoint);
            JSONArray dataArray = HttpService.parseJsonArray(response);
            
            // Process kline data into Ticker objects
            for (int i = 0; i < dataArray.length(); i++) {
                JSONArray candle = dataArray.getJSONArray(i);
                
                // Parse candle data (Binance format)
                long timestamp = candle.getLong(0);  // Open time
                double open = candle.getDouble(1);   // Open price
                double high = candle.getDouble(2);   // High price
                double low = candle.getDouble(3);    // Low price
                double close = candle.getDouble(4);  // Close price
                double volume = candle.getDouble(5); // Volume
                
                // Create ticker with historical data
                Ticker ticker = new Ticker();
                ticker.setLastPrice(close);
                ticker.setBidPrice(close * 0.999); // Approximate bid as slightly below close
                ticker.setAskPrice(close * 1.001); // Approximate ask as slightly above close
                ticker.setVolume(volume);
                ticker.setTimestamp(new Date(timestamp));
                
                tickers.add(ticker);
            }
            
            // Sort by timestamp (oldest first for chart display)
            Collections.sort(tickers, (t1, t2) -> t1.getTimestamp().compareTo(t2.getTimestamp()));
            
        } catch (Exception e) {
            logError("Error fetching historical data from Binance", e);
            
            // Fallback to mock data if API fails
            return generateMockHistoricalData(symbol, hoursBack);
        }
        
        // If we couldn't get real data, generate mock data
        if (tickers.isEmpty()) {
            return generateMockHistoricalData(symbol, hoursBack);
        }
        
        return tickers;
    }
    
    /**
     * Generate mock historical ticker data when real data is unavailable
     */
    private List<Ticker> generateMockHistoricalData(String symbol, int hoursBack) {
        List<Ticker> mockTickers = new ArrayList<>();
        // For now, generate mock historical data based on current ticker
        Ticker currentTicker = getTickerData(symbol);
        
        if (currentTicker != null) {
            double basePrice = currentTicker.getLastPrice();
            
            // Generate historical tickers with some randomness
            Random random = new Random();
            for (int i = hoursBack; i >= 0; i--) {
                Ticker historicalTicker = new Ticker();
                
                // Add some trend and randomness to price history
                double trend = (hoursBack - i) / (double) hoursBack * 0.05; // Slight upward trend (5%)
                double noise = random.nextDouble() * 0.02 - 0.01; // Random noise ±1%
                double factor = 1.0 - trend + noise;
                
                double price = basePrice * factor;
                
                historicalTicker.setLastPrice(price);
                historicalTicker.setBidPrice(price * 0.998);
                historicalTicker.setAskPrice(price * 1.002);
                historicalTicker.setVolume(50000 + random.nextDouble() * 50000);
                historicalTicker.setTimestamp(new Date(System.currentTimeMillis() - (i * 3600000))); // i hours ago
                
                mockTickers.add(historicalTicker);
            }
        }
        
        return mockTickers;
    }
}