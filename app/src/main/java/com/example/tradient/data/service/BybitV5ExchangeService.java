package com.example.tradient.data.service;

import com.example.tradient.data.fee.Fee;
import com.example.tradient.data.http.HttpService;
import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.interfaces.IWebSocketUpdateListener;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;
import com.example.tradient.data.service.websocket.BaseWebSocketProvider;
import com.example.tradient.data.service.websocket.BybitWebSocketProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * BybitV5ExchangeService provides concrete implementations for fetching market data
 * from Bybit V5 APIs for spot markets.
 *
 * Endpoints used:
 * - Trading Pairs: GET https://api.bybit.com/v5/market/instruments-info?category=spot
 * - Ticker Data:  GET https://api.bybit.com/v5/market/tickers?category=spot
 * - Order Book:   GET https://api.bybit.com/v5/market/orderbook?category=spot&symbol={symbol}&limit=5
 * - WebSocket:    wss://stream.bybit.com/v5/public/spot
 */
public class BybitV5ExchangeService extends ExchangeService {

    private static final String BASE_URL = "https://api.bybit.com";
    private final HttpService httpService;
    private BaseWebSocketProvider webSocketProvider;

    /**
     * Constructs a BybitV5ExchangeService instance.
     *
     * @param fees The trading fee as a percentage (e.g., 0.001 for 0.1%).
     */
    public BybitV5ExchangeService(double fees) {
        super("Bybit", fees);
        this.httpService = new HttpService();
        this.webSocketProvider = createWebSocketProvider();
    }
    
    /**
     * Constructs a BybitV5ExchangeService instance with notification service.
     *
     * @param fees The trading fee as a percentage (e.g., 0.001 for 0.1%).
     * @param notificationService The notification service for logging.
     */
    public BybitV5ExchangeService(double fees, INotificationService notificationService) {
        super("Bybit", fees);
        setNotificationService(notificationService);
        this.httpService = new HttpService();
        this.webSocketProvider = createWebSocketProvider();
    }
    
    /**
     * Creates a WebSocket provider for this exchange.
     *
     * @return The WebSocket provider
     */
    protected BaseWebSocketProvider createWebSocketProvider() {
        return new BybitWebSocketProvider(null);
    }
    
    /**
     * Check if a specific fee discount is enabled.
     * Bybit doesn't support fee discounts like Binance's BNB.
     *
     * @param discountType The type of discount
     * @return Always false for Bybit
     */
    @Override
    public boolean hasFeeDiscount(String discountType) {
        return false;
    }
    
    /**
     * Set a fee discount.
     * No-op for Bybit as it doesn't support special discounts.
     *
     * @param discountType The discount type
     * @param enabled Whether it's enabled
     */
    @Override
    public void setFeeDiscount(String discountType, boolean enabled) {
        // No-op, Bybit doesn't support special discounts
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
     * Fetches and caches the list of trading pairs (instruments) available on Bybit V5.
     * Endpoint: GET https://api.bybit.com/v5/market/instruments-info?category=spot
     *
     * @return A list of TradingPair objects.
     */
    @Override
    public List<TradingPair> fetchTradingPairs() {
        List<TradingPair> tradingPairs = new ArrayList<>();
        try {
            String endpoint = BASE_URL + "/v5/market/instruments-info?category=spot";
            String response = httpService.get(endpoint);
            
            JSONObject json = HttpService.parseJsonObject(response);
            int retCode = json.optInt("retCode", -1);
            if (retCode == 0) {
                JSONObject result = json.getJSONObject("result");
                JSONArray list = result.getJSONArray("list");
                for (int i = 0; i < list.length(); i++) {
                    JSONObject instrument = list.getJSONObject(i);
                    String symbol = instrument.getString("symbol");
                    TradingPair pair = new TradingPair(symbol);
                    tradingPairs.add(pair);
                }
                setTradingPairs(tradingPairs);
            } else {
                logWarning("Error fetching instruments: " + json.optString("retMsg"));
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
     * Endpoint: GET https://api.bybit.com/v5/market/tickers?category=spot
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSDT").
     * @return A Ticker object containing bid, ask, last price, volume, and timestamp.
     */
    @Override
    public Ticker fetchTickerDataREST(String symbol) {
        Ticker ticker = null;
        try {
            String endpoint = BASE_URL + "/v5/market/tickers?category=spot";
            String response = httpService.get(endpoint);
            
            JSONObject json = HttpService.parseJsonObject(response);
            int retCode = json.optInt("retCode", -1);
            if (retCode == 0) {
                JSONObject result = json.getJSONObject("result");
                JSONArray list = result.getJSONArray("list");
                for (int i = 0; i < list.length(); i++) {
                    JSONObject tickerObj = list.getJSONObject(i);
                    if (tickerObj.getString("symbol").equalsIgnoreCase(symbol)) {
                        double lastPrice = tickerObj.getDouble("lastPrice");
                        double bid = lastPrice;
                        double ask = lastPrice;
                        double volume = tickerObj.getDouble("volume24h");
                        Date timestamp = new Date();
                        ticker = new Ticker(bid, ask, lastPrice, volume, timestamp);
                        break;
                    }
                }
            } else {
                logWarning("Error fetching tickers: " + json.optString("retMsg"));
            }
        } catch (Exception e) {
            logError("Error fetching ticker data", e);
        }
        return ticker;
    }

    /**
     * Retrieves the current order book for the specified trading pair using REST API.
     * This is used as a fallback when WebSocket data is not available.
     *
     * Endpoint: GET https://api.bybit.com/v5/market/orderbook?category=spot&symbol={symbol}&limit=5
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSDT").
     * @return An OrderBook object with bids and asks.
     */
    @Override
    public OrderBook fetchOrderBookREST(String symbol) {
        OrderBook orderBook = null;
        try {
            String endpoint = BASE_URL + "/v5/market/orderbook?category=spot&symbol=" + symbol + "&limit=5";
            String response = httpService.get(endpoint);
            
            JSONObject json = HttpService.parseJsonObject(response);
            int retCode = json.optInt("retCode", -1);
            if (retCode == 0) {
                JSONObject result = json.getJSONObject("result");
                JSONArray bidsArray = result.getJSONArray("b");
                JSONArray asksArray = result.getJSONArray("a");

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

                Date timestamp = new Date();
                orderBook = new OrderBook(symbol, bids, asks, timestamp);
            } else {
                logWarning("Error fetching order book: " + json.optString("retMsg"));
            }
        } catch (Exception e) {
            logError("Error fetching order book", e);
        }
        return orderBook;
    }

    /**
     * Get the maker fee for a specific trading pair.
     * Bybit uses the same fee structure for all pairs.
     *
     * @param tradingPair The trading pair
     * @return The maker fee
     */
    @Override
    public Fee getMakerFee(String tradingPair) {
        return getMakerFee(); // Use the default maker fee
    }
    
    /**
     * Get the taker fee for a specific trading pair.
     * Bybit uses the same fee structure for all pairs.
     *
     * @param tradingPair The trading pair
     * @return The taker fee
     */
    @Override
    public Fee getTakerFee(String tradingPair) {
        return getTakerFee(); // Use the default taker fee
    }

    /**
     * Get the exchange fee for a symbol and order type
     * @param symbol The trading symbol
     * @param isMaker Whether the order is a maker order
     * @return The fee as a decimal percentage (e.g., 0.001 for 0.1%)
     */
    @Override
    public double getExchangeFee(String symbol, boolean isMaker) {
        // Return standard Bybit fees
        return isMaker ? 0.0006 : 0.001; // 0.06% maker, 0.1% taker
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
        // Bybit API average response time
        return 400;
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