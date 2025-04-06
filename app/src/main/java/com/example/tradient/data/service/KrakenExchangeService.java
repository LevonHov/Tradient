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
import java.util.Iterator;
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

import com.example.tradient.data.service.websocket.KrakenWebSocketProvider;
import com.example.tradient.data.service.websocket.BaseWebSocketProvider;

/**
 * KrakenExchangeService provides implementations for fetching data from Kraken's API.
 *
 * Endpoints used:
 * - Trading Pairs: GET https://api.kraken.com/0/public/AssetPairs
 * - Ticker Data:  GET https://api.kraken.com/0/public/Ticker?pair={symbols}
 * - Order Book:   GET https://api.kraken.com/0/public/Depth?pair={symbol}
 * - WebSocket:    wss://ws.kraken.com
 */
public class KrakenExchangeService extends ExchangeService {

    private static final String BASE_URL = "https://api.kraken.com/0/public";
    private static final String WS_BASE_URL = "wss://ws.kraken.com";

    // HTTP client
    private final HttpService httpService;

    // WebSocket client and connection
    private OkHttpClient wsClient;
    private WebSocket webSocket;
    private KrakenWebSocketListener webSocketListener;
    private volatile boolean websocketConnected = false;

    // Accumulates important log messages
    private StringBuilder logBuilder = new StringBuilder();

    // Add the listeners list as a class variable near the other class variables
    private final List<IWebSocketUpdateListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a KrakenExchangeService instance.
     *
     * @param fees The trading fee percentage (e.g., 0.0026 for 0.26%).
     */
    public KrakenExchangeService(double fees) {
        super("Kraken", fees);
        this.httpService = new HttpService();
        this.wsClient = new OkHttpClient.Builder()
                .connectTimeout(HttpClientProvider.DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .build();
        this.webSocketListener = new KrakenWebSocketListener();
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
     * Fetches and caches the list of trading pairs (AssetPairs) available on Kraken.
     * Endpoint: GET https://api.kraken.com/0/public/AssetPairs
     *
     * @return A list of TradingPair objects
     */
    @Override
    public List<TradingPair> fetchTradingPairs() {
        List<TradingPair> tradingPairs = new ArrayList<>();
        try {
            String endpoint = BASE_URL + "/AssetPairs";
            String response = httpService.get(endpoint);

            JSONObject json = HttpService.parseJsonObject(response);
            JSONObject result = json.getJSONObject("result");
            
            for (Iterator<String> it = result.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONObject pairInfo = result.getJSONObject(key);
                
                // Skip darkpool pairs and staking pairs
                if (!key.startsWith(".") && !key.contains(".d")) {
                    tradingPairs.add(new TradingPair(key));
                }
            }
            
            // Update the internal cache in ExchangeService
            setTradingPairs(tradingPairs);
        } catch (Exception e) {
            logBuilder.append("Error fetching trading pairs from Kraken: ")
                    .append(e.getMessage()).append("\n");
        }
        return tradingPairs;
    }

    /**
     * Retrieves the latest ticker data for the specified symbol using REST API.
     *
     * Endpoint: GET https://api.kraken.com/0/public/Ticker?pair={symbols}
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSD").
     * @return A Ticker object containing bid, ask, price, volume, and timestamp.
     */
    @Override
    public Ticker fetchTickerDataREST(String symbol) {
        Ticker ticker = null;
        try {
            String endpoint = BASE_URL + "/Ticker?pair=" + symbol;
            String response = httpService.get(endpoint);

            JSONObject json = HttpService.parseJsonObject(response);
            JSONObject result = json.getJSONObject("result");
            
            // Kraken returns data in an object with the pair name as key
            JSONObject tickerData = null;
            for (Iterator<String> it = result.keys(); it.hasNext(); ) {
                String key = it.next();
                if (key.equals(symbol)) {
                    tickerData = result.getJSONObject(key);
                        break;
                }
            }
            
            if (tickerData != null) {
                JSONArray asks = tickerData.getJSONArray("a");
                double ask = Double.parseDouble(asks.getString(0));
                
                JSONArray bids = tickerData.getJSONArray("b");
                double bid = Double.parseDouble(bids.getString(0));
                
                JSONArray priceInfo = tickerData.getJSONArray("c");
                double price = Double.parseDouble(priceInfo.getString(0));
                
                JSONArray volumeInfo = tickerData.getJSONArray("v");
                double volume = Double.parseDouble(volumeInfo.getString(1)); // 24h volume
                
                ticker = new Ticker(bid, ask, price, volume, new Date());
            }
        } catch (Exception e) {
            logBuilder.append("Error fetching ticker data from Kraken for ")
                    .append(symbol).append(": ").append(e.getMessage()).append("\n");
        }
        return ticker;
    }

    /**
     * Retrieves the current order book for the specified trading pair using REST API.
     *
     * Endpoint: GET https://api.kraken.com/0/public/Depth?pair={symbol}
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSD").
     * @return An OrderBook object with bids and asks.
     */
    @Override
    public OrderBook fetchOrderBookREST(String symbol) {
        OrderBook orderBook = null;
        try {
            String endpoint = BASE_URL + "/Depth?pair=" + symbol;
            String response = httpService.get(endpoint);

            JSONObject json = HttpService.parseJsonObject(response);
            JSONObject result = json.getJSONObject("result");
            
            // Kraken returns data in an object with the pair name as key
            JSONObject bookData = null;
            for (Iterator<String> it = result.keys(); it.hasNext(); ) {
                String key = it.next();
                if (key.equals(symbol)) {
                    bookData = result.getJSONObject(key);
                    break;
                }
            }
            
            if (bookData != null) {
                JSONArray asksArray = bookData.getJSONArray("asks");
            List<OrderBookEntry> asks = new ArrayList<>();
            for (int i = 0; i < asksArray.length(); i++) {
                JSONArray entry = asksArray.getJSONArray(i);
                double price = Double.parseDouble(entry.getString(0));
                double volume = Double.parseDouble(entry.getString(1));
                asks.add(new OrderBookEntry(price, volume));
            }
                
                JSONArray bidsArray = bookData.getJSONArray("bids");
                List<OrderBookEntry> bids = new ArrayList<>();
                for (int i = 0; i < bidsArray.length(); i++) {
                    JSONArray entry = bidsArray.getJSONArray(i);
                    double price = Double.parseDouble(entry.getString(0));
                    double volume = Double.parseDouble(entry.getString(1));
                    bids.add(new OrderBookEntry(price, volume));
            }

            orderBook = new OrderBook(symbol, bids, asks, new Date());
            }
        } catch (Exception e) {
            logBuilder.append("Error fetching order book from Kraken for ")
                    .append(symbol).append(": ").append(e.getMessage()).append("\n");
        }
        return orderBook;
    }

    /**
     * Initializes WebSocket connections for market data streaming from Kraken.
     *
     * @param symbols List of symbols to subscribe to.
     * @return true if successfully connected, false otherwise.
     */
    @Override
    public boolean initializeWebSocket(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            logBuilder.append("No symbols provided for Kraken WebSocket initialization\n");
            return false;
        }

        try {
            // Close existing connection if any
            if (webSocket != null) {
                closeWebSocket();
            }

            logBuilder.append("Connecting to Kraken WebSocket...\n");

            // Connect to Kraken WebSocket stream
            Request request = new Request.Builder()
                    .url(WS_BASE_URL)
                    .build();
            webSocket = wsClient.newWebSocket(request, webSocketListener);

            // Create subscription message for ticker and book channels
            JSONObject subscribeMsg = new JSONObject();
            subscribeMsg.put("name", "subscribe");

            JSONArray pairs = new JSONArray();
            for (String symbol : symbols) {
                pairs.put(symbol);
            }

            JSONObject subscription = new JSONObject();
            subscription.put("name", "ticker");
            
            subscribeMsg.put("pairs", pairs);
            subscribeMsg.put("subscription", subscription);

            String tickerSubMessage = subscribeMsg.toString();
            logBuilder.append("Sending Kraken ticker subscription: ").append(tickerSubMessage).append("\n");
            webSocket.send(tickerSubMessage);
            
            // Also subscribe to order book
            JSONObject bookSubscription = new JSONObject();
            bookSubscription.put("name", "book");
            bookSubscription.put("depth", 10);
            
            JSONObject bookSubscribeMsg = new JSONObject();
            bookSubscribeMsg.put("name", "subscribe");
            bookSubscribeMsg.put("pairs", pairs);
            bookSubscribeMsg.put("subscription", bookSubscription);
            
            String bookSubMessage = bookSubscribeMsg.toString();
            logBuilder.append("Sending Kraken book subscription: ").append(bookSubMessage).append("\n");
            webSocket.send(bookSubMessage);

            websocketConnected = true;
            logBuilder.append("Kraken WebSocket connected for symbols: ").append(symbols).append("\n");
            return true;
        } catch (Exception e) {
            logBuilder.append("Failed to connect to Kraken WebSocket: ").append(e.getMessage()).append("\n");
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
            logBuilder.append("Kraken WebSocket connection closed\n");
        }
    }

    /**
     * WebSocket listener for Kraken data.
     */
    private class KrakenWebSocketListener extends WebSocketListener {
        private StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            logBuilder.append("Kraken WebSocket opened\n");
            websocketConnected = true;
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            processMessage(text);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            websocketConnected = false;
            logBuilder.append("Kraken WebSocket closing: ").append(reason).append("\n");
            notifyWebSocketDisconnected(code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            websocketConnected = false;
            logBuilder.append("Kraken WebSocket error: ").append(t.getMessage()).append("\n");
            t.printStackTrace();
            notifyWebSocketError(t);
        }

        private void processMessage(String message) {
            try {
                // Kraken has a complex message format, let's try to parse it
                JSONArray jsonArray = new JSONArray(message);
                
                // Check if it's a subscription confirmation message
                if (jsonArray.length() == 3 && jsonArray.opt(1) instanceof String) {
                    String event = jsonArray.getString(1);
                    if ("subscriptionStatus".equals(event)) {
                        JSONObject status = jsonArray.getJSONObject(2);
                        String statusString = status.getString("status");
                        logBuilder.append("Kraken subscription: ").append(statusString).append(" for ")
                                .append(status.optString("pair", "unknown")).append("\n");
                        return;
                    }
                }
                
                // Check if it's a ticker update
                if (jsonArray.length() >= 3 && jsonArray.opt(2) instanceof JSONObject) {
                    JSONObject tickerData = jsonArray.getJSONObject(2);
                    String symbol = jsonArray.getString(3);
                    
                    if (jsonArray.opt(1) instanceof String && "ticker".equals(jsonArray.getString(1))) {
                        double bid = 0;
                        if (tickerData.has("b")) {
                            JSONArray bidArray = tickerData.getJSONArray("b");
                            bid = Double.parseDouble(bidArray.getString(0));
                        }
                        
                        double ask = 0;
                        if (tickerData.has("a")) {
                            JSONArray askArray = tickerData.getJSONArray("a");
                            ask = Double.parseDouble(askArray.getString(0));
                        }
                        
                        double price = 0;
                        if (tickerData.has("c")) {
                            JSONArray priceArray = tickerData.getJSONArray("c");
                            price = Double.parseDouble(priceArray.getString(0));
                        }
                        
                        double volume = 0;
                        if (tickerData.has("v")) {
                            JSONArray volumeArray = tickerData.getJSONArray("v");
                            volume = Double.parseDouble(volumeArray.getString(1)); // 24h volume
                        }
                        
                        Ticker ticker = new Ticker(bid, ask, price, volume, new Date());
                        notifyTickerUpdate(symbol, ticker);
                    }
                    // Check if it's an order book update
                    else if (jsonArray.opt(1) instanceof String && "book".equals(jsonArray.getString(1))) {
                            List<OrderBookEntry> bids = new ArrayList<>();
                            List<OrderBookEntry> asks = new ArrayList<>();

                        if (tickerData.has("bs")) {
                            JSONArray bidsArray = tickerData.getJSONArray("bs");
                            for (int i = 0; i < bidsArray.length(); i++) {
                                JSONArray entry = bidsArray.getJSONArray(i);
                                    double price = Double.parseDouble(entry.getString(0));
                                    double volume = Double.parseDouble(entry.getString(1));
                                        bids.add(new OrderBookEntry(price, volume));
                            }
                        }
                        
                        if (tickerData.has("as")) {
                            JSONArray asksArray = tickerData.getJSONArray("as");
                            for (int i = 0; i < asksArray.length(); i++) {
                                JSONArray entry = asksArray.getJSONArray(i);
                                    double price = Double.parseDouble(entry.getString(0));
                                    double volume = Double.parseDouble(entry.getString(1));
                                        asks.add(new OrderBookEntry(price, volume));
                            }
                        }
                        
                        if (!bids.isEmpty() || !asks.isEmpty()) {
                            OrderBook orderBook = new OrderBook(symbol, bids, asks, new Date());
                            notifyOrderBookUpdate(symbol, orderBook);
                        }
                    }
                }
            } catch (Exception e) {
                logBuilder.append("Error processing Kraken WebSocket message: ")
                        .append(e.getMessage()).append("\n");
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean hasFeeDiscount(String discountType) {
        // Kraken doesn't have the same fee discount structure as Binance
        return false;
    }

    @Override
    public void setFeeDiscount(String discountType, boolean enabled) {
        // No-op for Kraken
    }

    @Override
    public Fee getMakerFee(String tradingPair) {
        // Return standard fee (real implementation would vary by volume tier)
        return new PercentageFee(getFeePercentage(), true);
    }

    @Override
    public Fee getTakerFee(String tradingPair) {
        // Taker fees are higher
        return new PercentageFee(getFeePercentage() * 1.5, false);
    }

    /**
     * Get the base fee percentage for this exchange.
     * 
     * @return The fee percentage as a decimal (e.g., 0.0026 for 0.26%)
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
     * Creates a WebSocket provider for Kraken.
     *
     * @return The WebSocket provider
     */
    protected BaseWebSocketProvider createWebSocketProvider() {
        return new KrakenWebSocketProvider(getNotificationService());
    }

    /**
     * Get the exchange fee for a symbol and order type
     * @param symbol The trading symbol
     * @param isMaker Whether the order is a maker order
     * @return The fee as a decimal percentage (e.g., 0.001 for 0.1%)
     */
    @Override
    public double getExchangeFee(String symbol, boolean isMaker) {
        // Return standard Kraken fees
        return isMaker ? 0.0016 : 0.0026; // 0.16% maker, 0.26% taker
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
        // Kraken API can be slower during high load periods
        return 700;
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