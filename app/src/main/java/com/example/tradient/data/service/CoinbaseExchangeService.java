package com.example.tradient.data.service;

import com.example.tradient.data.fee.Fee;
import com.example.tradient.data.fee.PercentageFee;
import com.example.tradient.data.http.HttpService;
import com.example.tradient.data.http.HttpClientProvider;
import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.interfaces.IWebSocketUpdateListener;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Random;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import androidx.annotation.Nullable;

import com.example.tradient.data.service.websocket.CoinbaseWebSocketProvider;
import com.example.tradient.data.service.websocket.BaseWebSocketProvider;

/**
 * CoinbaseExchangeService provides implementations for fetching data from Coinbase's API.
 *
 * Endpoints used:
 * - Trading Pairs: GET https://api.exchange.coinbase.com/products
 * - Ticker Data:  GET https://api.exchange.coinbase.com/products/{symbol}/ticker
 * - Order Book:   GET https://api.exchange.coinbase.com/products/{symbol}/book?level=2
 * - WebSocket:    wss://ws-feed.exchange.coinbase.com
 *
 * Important log messages are now accumulated internally and can be retrieved using getLogMessages().
 */
public class CoinbaseExchangeService extends ExchangeService {

    private static final String BASE_URL = "https://api.exchange.coinbase.com";
    private static final String WS_BASE_URL = "wss://ws-feed.exchange.coinbase.com";

    // HTTP client
    private final HttpService httpService;
    
    // WebSocket client and connection
    private OkHttpClient wsClient;
    private WebSocket webSocket;
    private CoinbaseWebSocketListener webSocketListener;
    private volatile boolean websocketConnected = false;

    // Accumulates important log messages.
    private StringBuilder logBuilder = new StringBuilder();

    // Add the listeners list as a class variable near the other class variables
    private final List<IWebSocketUpdateListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a CoinbaseExchangeService instance.
     *
     * @param fees The trading fee as a percentage (e.g., 0.001 for 0.1%).
     */
    public CoinbaseExchangeService(double fees) {
        super("Coinbase", fees);
        this.httpService = new HttpService();
        this.wsClient = new OkHttpClient.Builder()
                .connectTimeout(HttpClientProvider.DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .build();
        this.webSocketListener = new CoinbaseWebSocketListener();
    }

    /**
     * Returns the accumulated log messages as a String.
     *
     * @return A String containing log messages.
     */
    public String getLogMessages() {
        return logBuilder.toString();
    }

    /**
     * Fetches and caches the list of trading pairs (products) available on Coinbase.
     * Endpoint: GET https://api.exchange.coinbase.com/products
     *
     * @return A list of TradingPair objects.
     */
    @Override
    public List<TradingPair> fetchTradingPairs() {
        List<TradingPair> tradingPairs = new ArrayList<>();
        try {
            String endpoint = BASE_URL + "/products";
            String response = httpService.get(endpoint);

            // Parse JSON response (an array of product objects)
            JSONArray products = HttpService.parseJsonArray(response);
            for (int i = 0; i < products.length(); i++) {
                JSONObject productObj = products.getJSONObject(i);
                String status = productObj.optString("status", "online");
                if ("online".equalsIgnoreCase(status)) {
                    String symbol = productObj.getString("id");
                    TradingPair pair = new TradingPair(symbol);
                    tradingPairs.add(pair);
                }
            }
            // Update the internal cache in ExchangeService
            setTradingPairs(tradingPairs);
        } catch (Exception e) {
            logBuilder.append("Error fetching trading pairs from Coinbase: ")
                    .append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        return tradingPairs;
    }

    /**
     * Retrieves the latest ticker data for the specified symbol using REST API.
     * This is used as a fallback when WebSocket data is not available.
     *
     * Endpoint: GET https://api.exchange.coinbase.com/products/{symbol}/ticker
     *
     * @param symbol The trading pair symbol (e.g., "ARB-USD").
     * @return A Ticker object containing bid, ask, price, volume, and timestamp.
     */
    @Override
    public Ticker fetchTickerDataREST(String symbol) {
        Ticker ticker = null;
        try {
            String endpoint = BASE_URL + "/products/" + symbol + "/ticker";
            String response = httpService.get(endpoint);

            JSONObject json = HttpService.parseJsonObject(response);
            double bid = json.getDouble("bid");
            double ask = json.getDouble("ask");
            double price = json.getDouble("price");
            double volume = json.getDouble("volume");
            Date timestamp = new Date();

            ticker = new Ticker(bid, ask, price, volume, timestamp);
        } catch (Exception e) {
            logBuilder.append("Error fetching ticker data from Coinbase for ")
                    .append(symbol).append(": ").append(e.getMessage()).append("\n");
        }
        return ticker;
    }

    /**
     * Retrieves the current order book for the specified trading pair using REST API.
     * This is used as a fallback when WebSocket data is not available.
     *
     * Endpoint: GET https://api.exchange.coinbase.com/products/{symbol}/book?level=2
     *
     * @param symbol The trading pair symbol (e.g., "ARB-USD").
     * @return An OrderBook object with bids and asks.
     */
    @Override
    public OrderBook fetchOrderBookREST(String symbol) {
        OrderBook orderBook = null;
        try {
            String endpoint = BASE_URL + "/products/" + symbol + "/book?level=2";
            String response = httpService.get(endpoint);

            JSONObject json = HttpService.parseJsonObject(response);
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

            Date timestamp = new Date();
            orderBook = new OrderBook(symbol, bids, asks, timestamp);
        } catch (Exception e) {
            logBuilder.append("Error fetching order book from Coinbase for ")
                    .append(symbol).append(": ").append(e.getMessage()).append("\n");
        }
        return orderBook;
    }

    /**
     * Initializes WebSocket connections for market data streaming from Coinbase.
     *
     * @param symbols List of symbols to subscribe to.
     * @return true if successfully connected, false otherwise.
     */
    @Override
    public boolean initializeWebSocket(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            logBuilder.append("No symbols provided for Coinbase WebSocket initialization\n");
            return false;
        }

        try {
            // Close existing connection if any
            if (webSocket != null) {
                closeWebSocket();
            }

            logBuilder.append("Connecting to Coinbase WebSocket...\n");

            // Connect to Coinbase WebSocket stream
            Request request = new Request.Builder()
                    .url(WS_BASE_URL)
                    .build();
            webSocket = wsClient.newWebSocket(request, webSocketListener);

            // Create subscription message for ticker and level2 (order book) channels
            JSONObject subscribeMsg = new JSONObject();
            subscribeMsg.put("type", "subscribe");

            logBuilder.append("Subscribing to Coinbase products: ").append(symbols).append("\n");

            JSONArray productIds = new JSONArray();
            for (String symbol : symbols) {
                productIds.put(symbol);
            }
            subscribeMsg.put("product_ids", productIds);

            JSONArray channels = new JSONArray();
            channels.put("ticker");
            channels.put("level2");
            subscribeMsg.put("channels", channels);

            String subMessage = subscribeMsg.toString();
            logBuilder.append("Sending Coinbase subscription message: ").append(subMessage).append("\n");
            webSocket.send(subMessage);

            websocketConnected = true;
            logBuilder.append("Coinbase WebSocket connected for symbols: ").append(symbols).append("\n");
            return true;
        } catch (Exception e) {
            logBuilder.append("Failed to connect to Coinbase WebSocket: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
            websocketConnected = false;
            return false;
        }
    }

    /**
     * Closes the WebSocket connection.
     */
    @Override
    public void closeWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing connection");
                websocketConnected = false;
                logBuilder.append("Coinbase WebSocket connection closed\n");
        }
    }

    /**
     * WebSocket listener for Coinbase data.
     */
    private class CoinbaseWebSocketListener extends WebSocketListener {
        private StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            logBuilder.append("Coinbase WebSocket opened\n");
            websocketConnected = true;
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            processMessage(text);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            websocketConnected = false;
            logBuilder.append("Coinbase WebSocket closing: ").append(reason).append("\n");
            notifyWebSocketDisconnected(code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            websocketConnected = false;
            logBuilder.append("Coinbase WebSocket error: ").append(t.getMessage()).append("\n");
            t.printStackTrace();
            notifyWebSocketError(t);
        }

        private void processMessage(String message) {
            try {
                JSONObject json = new JSONObject(message);
                String type = json.optString("type", "");

                if ("ticker".equals(type)) {
                    String symbol = json.getString("product_id");
                    double price = json.getDouble("price");
                    double bid = json.getDouble("best_bid");
                    double ask = json.getDouble("best_ask");
                    double volume = json.getDouble("volume_24h");
                    Date timestamp = new Date();
                    
                    Ticker ticker = new Ticker(bid, ask, price, volume, timestamp);
                    notifyTickerUpdate(symbol, ticker);
                }
                else if ("snapshot".equals(type)) {
                    String symbol = json.getString("product_id");
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

                    OrderBook orderBook = new OrderBook(symbol, bids, asks, new Date());
                    notifyOrderBookUpdate(symbol, orderBook);
                }
                else if ("l2update".equals(type)) {
                    // Handle incremental orderbook updates
                    // Implementation depends on specific application needs
                }

            } catch (Exception e) {
                logBuilder.append("Error processing Coinbase WebSocket message: ")
                        .append(e.getMessage()).append("\n");
                e.printStackTrace();
            }
        }

        private void updateOrderBookSide(List<OrderBookEntry> entries, double price, double size) {
            // Find the entry with matching price
            for (int i = 0; i < entries.size(); i++) {
                OrderBookEntry entry = entries.get(i);
                if (entry.getPrice() == price) {
                    if (size > 0) {
                        // Update size
                        entry.setVolume(size);
                    } else {
                        // Remove entry
                        entries.remove(i);
                    }
                    return;
                }
            }
            
            // If not found and size > 0, add new entry
            if (size > 0) {
                entries.add(new OrderBookEntry(price, size));
                
                // Sort bids in descending order by price
                entries.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
            }
        }
    }

    @Override
    public boolean hasFeeDiscount(String discountType) {
        // Coinbase doesn't have the same fee discount structure as Binance
        // This is a simplified implementation
        return false;
    }
    
    @Override
    public void setFeeDiscount(String discountType, boolean enabled) {
        // Coinbase doesn't support the same fee discount options
        // This is a no-op implementation
    }

    @Override
    public Fee getMakerFee(String tradingPair) {
        // Return the standard fee configured during initialization
        // Real implementation might vary fees by trading pair or tier
        return new PercentageFee(getFeePercentage(), true);
    }

    @Override
    public Fee getTakerFee(String tradingPair) {
        // Taker fees are typically higher than maker fees
        // This is a simplified implementation
        return new PercentageFee(getFeePercentage() * 1.5, false);
    }

    /**
     * Get the base fee percentage for this exchange.
     *
     * @return The fee percentage as a decimal (e.g., 0.001 for 0.1%)
     */
    private double getFeePercentage() {
        return getFees(); // Use the deprecated method for backward compatibility
    }

    @Override
    public void addWebSocketListener(IWebSocketUpdateListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeWebSocketListener(IWebSocketUpdateListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners of a ticker update.
     *
     * @param symbol The trading pair symbol
     * @param ticker The updated ticker data
     */
    protected void notifyTickerUpdate(String symbol, Ticker ticker) {
        for (IWebSocketUpdateListener listener : listeners) {
            try {
                listener.onTickerUpdate(symbol, ticker);
            } catch (Exception e) {
                logBuilder.append("Error in ticker update listener: ").append(e.getMessage()).append("\n");
            }
        }
    }
    
    /**
     * Notify all listeners of an order book update.
     *
     * @param symbol The trading pair symbol
     * @param orderBook The updated order book
     */
    protected void notifyOrderBookUpdate(String symbol, OrderBook orderBook) {
        for (IWebSocketUpdateListener listener : listeners) {
            try {
                listener.onOrderBookUpdate(symbol, orderBook);
            } catch (Exception e) {
                logBuilder.append("Error in order book update listener: ").append(e.getMessage()).append("\n");
            }
        }
    }
    
    /**
     * Notify all listeners of a WebSocket error.
     *
     * @param error The error that occurred
     */
    protected void notifyWebSocketError(Throwable error) {
        for (IWebSocketUpdateListener listener : listeners) {
            try {
                listener.onWebSocketError(this, error);
            } catch (Exception e) {
                logBuilder.append("Error in WebSocket error listener: ").append(e.getMessage()).append("\n");
            }
        }
    }
    
    /**
     * Notify all listeners of a WebSocket disconnection.
     *
     * @param code The status code
     * @param reason The reason for disconnection
     */
    protected void notifyWebSocketDisconnected(int code, String reason) {
        for (IWebSocketUpdateListener listener : listeners) {
            try {
                listener.onWebSocketDisconnected(this, code, reason);
            } catch (Exception e) {
                logBuilder.append("Error in WebSocket disconnection listener: ").append(e.getMessage()).append("\n");
            }
        }
    }

    /**
     * Creates a WebSocket provider for Coinbase.
     *
     * @return The WebSocket provider
     */
    protected BaseWebSocketProvider createWebSocketProvider() {
        return new CoinbaseWebSocketProvider(getNotificationService());
    }

    /**
     * Get the exchange fee for a symbol and order type
     * @param symbol The trading symbol
     * @param isMaker Whether the order is a maker order
     * @return The fee as a decimal percentage (e.g., 0.001 for 0.1%)
     */
    @Override
    public double getExchangeFee(String symbol, boolean isMaker) {
        // Return standard Coinbase fees
        return isMaker ? 0.005 : 0.006; // 0.5% maker, 0.6% taker
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
        // Coinbase API typically has higher latency
        return 500;
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