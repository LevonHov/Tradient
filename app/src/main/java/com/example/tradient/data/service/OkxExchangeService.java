package com.example.tradient.data.service;

import com.example.tradient.data.fee.Fee;
import com.example.tradient.data.http.HttpService;
import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.interfaces.IWebSocketUpdateListener;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;
import com.example.tradient.data.service.websocket.OkxWebSocketProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * OkxExchangeService provides concrete implementations for fetching market data
 * from OKX APIs for spot markets.
 *
 * Endpoints used:
 * - Trading Pairs: GET https://www.okx.com/api/v5/public/instruments?instType=SPOT
 * - Ticker Data:  GET https://www.okx.com/api/v5/market/ticker?instId={symbol}
 * - Order Book:   GET https://www.okx.com/api/v5/market/books?instId={symbol}&sz=20
 * - WebSocket:    wss://ws.okx.com:8443/ws/v5/public
 */
public class OkxExchangeService extends ExchangeService {

    private static final String BASE_URL = "https://www.okx.com";
    private final HttpService httpService;
    private OkxWebSocketProvider webSocketProvider;

    /**
     * Constructs an OkxExchangeService instance.
     *
     * @param fees The trading fee as a percentage (e.g., 0.001 for 0.1%).
     */
    public OkxExchangeService(double fees) {
        super("OKX", fees);
        this.httpService = new HttpService();
        this.webSocketProvider = createWebSocketProvider();
    }
    
    /**
     * Constructs an OkxExchangeService instance with notification service.
     *
     * @param fees The trading fee as a percentage (e.g., 0.001 for 0.1%).
     * @param notificationService The notification service for logging.
     */
    public OkxExchangeService(double fees, INotificationService notificationService) {
        super("OKX", fees);
        setNotificationService(notificationService);
        this.httpService = new HttpService();
        this.webSocketProvider = createWebSocketProvider();
    }
    
    /**
     * Creates a WebSocket provider for this exchange.
     *
     * @return The WebSocket provider
     */
    protected OkxWebSocketProvider createWebSocketProvider() {
        return new OkxWebSocketProvider(getNotificationService());
    }
    
    /**
     * Check if a specific fee discount is enabled.
     * OKX doesn't support the same fee discounts as Binance.
     *
     * @param discountType The type of discount
     * @return Always false for OKX
     */
    @Override
    public boolean hasFeeDiscount(String discountType) {
        return false;
    }
    
    /**
     * Set a fee discount.
     * No-op for OKX as it doesn't support the same discounts.
     *
     * @param discountType The discount type
     * @param enabled Whether it's enabled
     */
    @Override
    public void setFeeDiscount(String discountType, boolean enabled) {
        // No-op, OKX doesn't support special discounts like Binance
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
     * Fetches and caches the list of trading pairs available on OKX.
     * Endpoint: GET https://www.okx.com/api/v5/public/instruments?instType=SPOT
     *
     * @return A list of TradingPair objects.
     */
    @Override
    public List<TradingPair> fetchTradingPairs() {
        List<TradingPair> tradingPairs = new ArrayList<>();
        try {
            String endpoint = BASE_URL + "/api/v5/public/instruments?instType=SPOT";
            String response = httpService.get(endpoint);
            
            JSONObject json = HttpService.parseJsonObject(response);
            if ("0".equals(json.getString("code"))) {
                JSONArray data = json.getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject pairInfo = data.getJSONObject(i);
                    String symbol = pairInfo.getString("instId");
                    TradingPair pair = new TradingPair(symbol);
                    tradingPairs.add(pair);
                }
                setTradingPairs(tradingPairs);
                logInfo("Fetched " + tradingPairs.size() + " trading pairs from OKX");
            } else {
                logWarning("Error fetching instruments: " + json.optString("msg"));
            }
        } catch (Exception e) {
            logError("Error fetching trading pairs", e);
        }
        return tradingPairs;
    }

    /**
     * Retrieves the latest ticker data for the specified symbol using REST API.
     * This is used as a fallback when WebSocket data is not available.
     *
     * Endpoint: GET https://www.okx.com/api/v5/market/ticker?instId={symbol}
     *
     * @param symbol The trading pair symbol (e.g., "BTC-USDT").
     * @return A Ticker object containing bid, ask, last price, volume, and timestamp.
     */
    @Override
    public Ticker fetchTickerDataREST(String symbol) {
        Ticker ticker = null;
        try {
            String endpoint = BASE_URL + "/api/v5/market/ticker?instId=" + symbol;
            String response = httpService.get(endpoint);
            
            JSONObject json = HttpService.parseJsonObject(response);
            if ("0".equals(json.getString("code"))) {
                JSONArray data = json.getJSONArray("data");
                if (data.length() > 0) {
                    JSONObject tickerData = data.getJSONObject(0);
                    double bid = tickerData.getDouble("bidPx");
                    double ask = tickerData.getDouble("askPx");
                    double lastPrice = tickerData.getDouble("last");
                    double volume = tickerData.getDouble("vol24h");
                    
                    ticker = new Ticker(bid, ask, lastPrice, volume, new Date());
                }
            } else {
                logWarning("Error fetching ticker data: " + json.optString("msg"));
            }
        } catch (Exception e) {
            logError("Error fetching ticker data for " + symbol, e);
        }
        return ticker;
    }

    /**
     * Retrieves the current order book for the specified trading pair using REST API.
     * This is used as a fallback when WebSocket data is not available.
     *
     * Endpoint: GET https://www.okx.com/api/v5/market/books?instId={symbol}&sz=20
     *
     * @param symbol The trading pair symbol (e.g., "BTC-USDT").
     * @return An OrderBook object with bids and asks.
     */
    @Override
    public OrderBook fetchOrderBookREST(String symbol) {
        OrderBook orderBook = null;
        try {
            String endpoint = BASE_URL + "/api/v5/market/books?instId=" + symbol + "&sz=20";
            String response = httpService.get(endpoint);
            
            JSONObject json = HttpService.parseJsonObject(response);
            if ("0".equals(json.getString("code"))) {
                JSONArray data = json.getJSONArray("data");
                if (data.length() > 0) {
                    JSONObject bookData = data.getJSONObject(0);
                    
                    JSONArray bidsArray = bookData.getJSONArray("bids");
                    List<OrderBookEntry> bids = new ArrayList<>();
                    for (int i = 0; i < bidsArray.length(); i++) {
                        JSONArray entry = bidsArray.getJSONArray(i);
                        double price = Double.parseDouble(entry.getString(0));
                        double volume = Double.parseDouble(entry.getString(1));
                        bids.add(new OrderBookEntry(price, volume));
                    }
                    
                    JSONArray asksArray = bookData.getJSONArray("asks");
                    List<OrderBookEntry> asks = new ArrayList<>();
                    for (int i = 0; i < asksArray.length(); i++) {
                        JSONArray entry = asksArray.getJSONArray(i);
                        double price = Double.parseDouble(entry.getString(0));
                        double volume = Double.parseDouble(entry.getString(1));
                        asks.add(new OrderBookEntry(price, volume));
                    }
                    
                    orderBook = new OrderBook(symbol, bids, asks, new Date());
                }
            } else {
                logWarning("Error fetching order book: " + json.optString("msg"));
            }
        } catch (Exception e) {
            logError("Error fetching order book for " + symbol, e);
        }
        return orderBook;
    }

    /**
     * Get the maker fee for a specific trading pair.
     * OKX uses the same fee structure for all pairs.
     *
     * @param tradingPair The trading pair
     * @return The maker fee
     */
    @Override
    public Fee getMakerFee(String tradingPair) {
        return super.getMakerFee();
    }
    
    /**
     * Get the taker fee for a specific trading pair.
     * OKX uses the same fee structure for all pairs.
     *
     * @param tradingPair The trading pair
     * @return The taker fee
     */
    @Override
    public Fee getTakerFee(String tradingPair) {
        return super.getTakerFee();
    }

    /**
     * Get the exchange fee for a symbol and order type
     * @param symbol The trading symbol
     * @param isMaker Whether the order is a maker order
     * @return The fee as a decimal percentage (e.g., 0.001 for 0.1%)
     */
    @Override
    public double getExchangeFee(String symbol, boolean isMaker) {
        // Return standard OKX fees
        return isMaker ? 0.0008 : 0.001; // 0.08% maker, 0.1% taker
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
        // OKX API average response time
        return 350;
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
                
                tickers.add(historicalTicker);
            }
        }
        
        return tickers;
    }
} 