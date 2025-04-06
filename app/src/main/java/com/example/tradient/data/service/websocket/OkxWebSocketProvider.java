package com.example.tradient.data.service.websocket;

import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Response;
import androidx.annotation.Nullable;

/**
 * OKX-specific implementation of WebSocket provider.
 * Handles WebSocket connection and message processing for OKX exchange.
 */
public class OkxWebSocketProvider extends BaseWebSocketProvider {

    private static final String WS_BASE_URL = "wss://ws.okx.com:8443/ws/v5/public";
    
    // Maps to store market data for each symbol
    private final Map<String, Ticker> tickerMap = new HashMap<>();
    private final Map<String, OrderBook> orderBookMap = new HashMap<>();

    /**
     * Constructor with notification service.
     *
     * @param notificationService Optional notification service for logging
     */
    public OkxWebSocketProvider(INotificationService notificationService) {
        super("OKX", notificationService);
    }

    @Override
    protected URI getWebSocketEndpoint(List<String> symbols) {
        return URI.create(WS_BASE_URL);
    }
    
    @Override
    protected List<String> createSubscriptionMessages(List<String> symbols) {
        List<String> messages = new ArrayList<>();
        
        try {
            for (String symbol : symbols) {
                // Create ticker subscription
                JSONObject tickerSub = new JSONObject();
                JSONObject tickerArgs = new JSONObject();
                tickerArgs.put("channel", "tickers");
                tickerArgs.put("instId", symbol);
                
                JSONArray tickerArgsArray = new JSONArray();
                tickerArgsArray.put(tickerArgs);
                
                tickerSub.put("op", "subscribe");
                tickerSub.put("args", tickerArgsArray);
                messages.add(tickerSub.toString());
                
                // Create order book subscription
                JSONObject bookSub = new JSONObject();
                JSONObject bookArgs = new JSONObject();
                bookArgs.put("channel", "books");
                bookArgs.put("instId", symbol);
                
                JSONArray bookArgsArray = new JSONArray();
                bookArgsArray.put(bookArgs);
                
                bookSub.put("op", "subscribe");
                bookSub.put("args", bookArgsArray);
                messages.add(bookSub.toString());
            }
        } catch (Exception e) {
            logError("Error creating subscription messages", e);
        }
        
        return messages;
    }
    
    @Override
    protected WebSocketListener createWebSocketListener() {
        return new OkxWebSocketListener();
    }
    
    /**
     * WebSocket listener for OKX.
     */
    private class OkxWebSocketListener extends WebSocketListener {
        private StringBuilder buffer = new StringBuilder();
        
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            logInfo("OKX WebSocket connection opened");
            websocketConnected = true;
        }
        
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            processMessage(text);
        }
        
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            notifyWebSocketDisconnected(code, reason);
            websocketConnected = false;
        }
        
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            notifyWebSocketError(t);
            websocketConnected = false;
        }
        
        /**
         * Process WebSocket message.
         *
         * @param message The message received
         */
        private void processMessage(String message) {
            try {
                JSONObject json = new JSONObject(message);
                
                // Handle ping message (need to respond with pong)
                if (json.has("event") && "ping".equals(json.getString("event"))) {
                    JSONObject pong = new JSONObject();
                    pong.put("event", "pong");
                    webSocket.send(pong.toString());
                    return;
                }
                
                // Handle subscription confirmation
                if (json.has("event") && "subscribe".equals(json.getString("event"))) {
                    logInfo("Subscription confirmed: " + json.toString());
                    return;
                }
                
                // Handle data messages
                if (json.has("data") && json.has("arg")) {
                    JSONObject arg = json.getJSONObject("arg");
                    String channel = arg.getString("channel");
                    String symbol = arg.getString("instId");
                    
                    // Handle ticker data
                    if ("tickers".equals(channel)) {
                        JSONArray data = json.getJSONArray("data");
                        if (data.length() > 0) {
                            JSONObject tickerData = data.getJSONObject(0);
                            
                            double bidPrice = tickerData.getDouble("bidPx");
                            double askPrice = tickerData.getDouble("askPx");
                            double lastPrice = tickerData.getDouble("last");
                            double volume = tickerData.getDouble("vol24h");
                            
                            Ticker ticker = new Ticker(bidPrice, askPrice, lastPrice, volume, new Date());
                            tickerMap.put(symbol, ticker);
                            notifyTickerUpdate(symbol, ticker);
                        }
                    }
                    
                    // Handle order book data
                    if ("books".equals(channel)) {
                        JSONArray data = json.getJSONArray("data");
                        if (data.length() > 0) {
                            JSONObject bookData = data.getJSONObject(0);
                            
                            OrderBook orderBook = orderBookMap.getOrDefault(symbol, 
                                    new OrderBook(symbol, new ArrayList<>(), new ArrayList<>(), new Date()));
                            
                            // Process bids
                            if (bookData.has("bids")) {
                                JSONArray bidsArray = bookData.getJSONArray("bids");
                                List<OrderBookEntry> bids = new ArrayList<>();
                                for (int i = 0; i < bidsArray.length(); i++) {
                                    JSONArray entry = bidsArray.getJSONArray(i);
                                    double price = Double.parseDouble(entry.getString(0));
                                    double volume = Double.parseDouble(entry.getString(1));
                                    bids.add(new OrderBookEntry(price, volume));
                                }
                                orderBook.setBids(bids);
                            }
                            
                            // Process asks
                            if (bookData.has("asks")) {
                                JSONArray asksArray = bookData.getJSONArray("asks");
                                List<OrderBookEntry> asks = new ArrayList<>();
                                for (int i = 0; i < asksArray.length(); i++) {
                                    JSONArray entry = asksArray.getJSONArray(i);
                                    double price = Double.parseDouble(entry.getString(0));
                                    double volume = Double.parseDouble(entry.getString(1));
                                    asks.add(new OrderBookEntry(price, volume));
                                }
                                orderBook.setAsks(asks);
                            }
                            
                            // Update timestamp and notify
                            orderBook.setTimestamp(new Date());
                            orderBookMap.put(symbol, orderBook);
                            notifyOrderBookUpdate(symbol, orderBook);
                        }
                    }
                }
            } catch (Exception e) {
                logError("Error processing OKX WebSocket message", e);
            }
        }
    }
} 