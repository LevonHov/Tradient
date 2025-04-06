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
import java.util.UUID;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Response;
import androidx.annotation.Nullable;

/**
 * Coinbase-specific implementation of WebSocket provider.
 * Handles WebSocket connection and message processing for Coinbase exchange.
 */
public class CoinbaseWebSocketProvider extends BaseWebSocketProvider {

    private static final String WS_BASE_URL = "wss://ws-feed.exchange.coinbase.com";
    
    // Maps to store market data for each symbol
    private final Map<String, Ticker> tickerMap = new HashMap<>();
    private final Map<String, OrderBook> orderBookMap = new HashMap<>();

    /**
     * Constructor with notification service.
     *
     * @param notificationService Optional notification service for logging
     */
    public CoinbaseWebSocketProvider(INotificationService notificationService) {
        super("Coinbase", notificationService);
    }

    @Override
    protected URI getWebSocketEndpoint(List<String> symbols) {
        return URI.create(WS_BASE_URL);
    }
    
    @Override
    protected List<String> createSubscriptionMessages(List<String> symbols) {
        List<String> messages = new ArrayList<>();
        
        try {
            // Coinbase WebSocket uses a single subscription message for all channels and products
            JSONObject subscription = new JSONObject();
            subscription.put("type", "subscribe");
            
            // Add ticker channel (level1)
            JSONArray channels = new JSONArray();
            
            // Add ticker channel (level1)
            JSONObject tickerChannel = new JSONObject();
            tickerChannel.put("name", "ticker");
            tickerChannel.put("product_ids", new JSONArray(symbols));
            channels.put(tickerChannel);
            
            // Add level2 channel for order book
            JSONObject level2Channel = new JSONObject();
            level2Channel.put("name", "level2");
            level2Channel.put("product_ids", new JSONArray(symbols));
            channels.put(level2Channel);
            
            subscription.put("channels", channels);
            messages.add(subscription.toString());
            
        } catch (Exception e) {
            logError("Error creating subscription messages", e);
        }
        
        return messages;
    }
    
    @Override
    protected WebSocketListener createWebSocketListener() {
        return new CoinbaseWebSocketListener();
    }
    
    /**
     * WebSocket listener for Coinbase.
     */
    private class CoinbaseWebSocketListener extends WebSocketListener {
        
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            logInfo("Coinbase WebSocket connection opened");
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
                String type = json.optString("type");
                
                // Handle subscription confirmation
                if ("subscriptions".equals(type)) {
                    logInfo("Coinbase subscription confirmed: " + json.toString());
                    return;
                }
                
                // Handle ticker updates
                if ("ticker".equals(type)) {
                    String symbol = json.getString("product_id");
                    double bid = json.getDouble("best_bid");
                    double ask = json.getDouble("best_ask");
                    double lastPrice = json.getDouble("price");
                    double volume = json.getDouble("volume_24h");
                    
                    Ticker ticker = new Ticker(bid, ask, lastPrice, volume, new Date());
                    tickerMap.put(symbol, ticker);
                    notifyTickerUpdate(symbol, ticker);
                    return;
                }
                
                // Handle level2 snapshot (full order book)
                if ("snapshot".equals(type)) {
                    String symbol = json.getString("product_id");
                    
                    JSONArray bidsArray = json.getJSONArray("bids");
                    List<OrderBookEntry> bids = new ArrayList<>();
                    for (int i = 0; i < bidsArray.length(); i++) {
                        JSONArray entry = bidsArray.getJSONArray(i);
                        double price = Double.parseDouble(entry.getString(0));
                        double volume = Double.parseDouble(entry.getString(1));
                        bids.add(new OrderBookEntry(price, volume));
                    }
                    
                    JSONArray asksArray = json.getJSONArray("asks");
                    List<OrderBookEntry> asks = new ArrayList<>();
                    for (int i = 0; i < asksArray.length(); i++) {
                        JSONArray entry = asksArray.getJSONArray(i);
                        double price = Double.parseDouble(entry.getString(0));
                        double volume = Double.parseDouble(entry.getString(1));
                        asks.add(new OrderBookEntry(price, volume));
                    }
                    
                    OrderBook orderBook = new OrderBook(symbol, bids, asks, new Date());
                    orderBookMap.put(symbol, orderBook);
                    notifyOrderBookUpdate(symbol, orderBook);
                    return;
                }
                
                // Handle order book updates (l2update)
                if ("l2update".equals(type)) {
                    String symbol = json.getString("product_id");
                    JSONArray changes = json.getJSONArray("changes");
                    
                    // Get existing order book or create new one
                    OrderBook orderBook = orderBookMap.get(symbol);
                    if (orderBook == null) {
                        // We don't have the initial snapshot yet, skip this update
                        return;
                    }
                    
                    // Update order book with changes
                    for (int i = 0; i < changes.length(); i++) {
                        JSONArray change = changes.getJSONArray(i);
                        String side = change.getString(0);
                        double price = Double.parseDouble(change.getString(1));
                        double size = Double.parseDouble(change.getString(2));
                        
                        if ("buy".equals(side)) {
                            updateOrderBookEntries(orderBook.getBids(), price, size);
                        } else if ("sell".equals(side)) {
                            updateOrderBookEntries(orderBook.getAsks(), price, size);
                        }
                    }
                    
                    orderBook.setTimestamp(new Date());
                    notifyOrderBookUpdate(symbol, orderBook);
                }
                
            } catch (Exception e) {
                logError("Error processing Coinbase WebSocket message", e);
            }
        }
        
        /**
         * Update order book entries with the new price/size.
         * 
         * @param entries The list of entries to update
         * @param price The price level
         * @param size The new size (0 means remove the level)
         */
        private void updateOrderBookEntries(List<OrderBookEntry> entries, double price, double size) {
            // Find if this price level already exists
            boolean found = false;
            for (int i = 0; i < entries.size(); i++) {
                OrderBookEntry entry = entries.get(i);
                if (entry.getPrice() == price) {
                    if (size == 0) {
                        // Remove the level
                        entries.remove(i);
                    } else {
                        // Update the size
                        entry.setAmount(size);
                    }
                    found = true;
                    break;
                }
            }
            
            // If it's a new price level and size > 0, add it
            if (!found && size > 0) {
                entries.add(new OrderBookEntry(price, size));
                
                // Re-sort bids in descending order, asks in ascending order
                if (entries.size() > 0 && entries.get(0).getAmount() > 0) {
                    // This is a bid list (positive amounts)
                    entries.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                } else {
                    // This is an ask list
                    entries.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                }
            }
        }
    }
} 