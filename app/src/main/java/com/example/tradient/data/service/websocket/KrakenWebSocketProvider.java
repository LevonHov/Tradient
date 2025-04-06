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
 * Kraken-specific implementation of WebSocket provider.
 * Handles WebSocket connection and message processing for Kraken exchange.
 */
public class KrakenWebSocketProvider extends BaseWebSocketProvider {

    private static final String WS_BASE_URL = "wss://ws.kraken.com";
    
    // Maps to store market data for each symbol
    private final Map<String, Ticker> tickerMap = new HashMap<>();
    private final Map<String, OrderBook> orderBookMap = new HashMap<>();
    
    // Map to handle special Kraken symbol names (they have different formats in WebSocket API)
    private final Map<String, String> symbolMapping = new HashMap<>();

    /**
     * Constructor with notification service.
     *
     * @param notificationService Optional notification service for logging
     */
    public KrakenWebSocketProvider(INotificationService notificationService) {
        super("Kraken", notificationService);
    }

    @Override
    protected URI getWebSocketEndpoint(List<String> symbols) {
        return URI.create(WS_BASE_URL);
    }
    
    @Override
    protected List<String> createSubscriptionMessages(List<String> symbols) {
        List<String> messages = new ArrayList<>();
        
        try {
            // Prepare ticker subscription
            JSONObject tickerSub = new JSONObject();
            tickerSub.put("name", "subscribe");
            
            JSONObject tickerArgs = new JSONObject();
            tickerArgs.put("name", "ticker");
            
            // Convert symbols to Kraken WebSocket format (XBT instead of BTC, etc.)
            JSONArray pairs = new JSONArray();
            for (String symbol : symbols) {
                String krakenSymbol = formatKrakenSymbol(symbol);
                symbolMapping.put(krakenSymbol, symbol); // Store the mapping
                pairs.put(krakenSymbol);
            }
            
            tickerArgs.put("pair", pairs);
            tickerSub.put("subscription", tickerArgs);
            messages.add(tickerSub.toString());
            
            // Prepare order book subscription
            JSONObject bookSub = new JSONObject();
            bookSub.put("name", "subscribe");
            
            JSONObject bookArgs = new JSONObject();
            bookArgs.put("name", "book");
            bookArgs.put("depth", 25); // Limit to 25 levels
            bookArgs.put("pair", pairs);
            
            bookSub.put("subscription", bookArgs);
            messages.add(bookSub.toString());
            
        } catch (Exception e) {
            logError("Error creating subscription messages", e);
        }
        
        return messages;
    }
    
    /**
     * Convert standard symbol format to Kraken WebSocket format.
     * 
     * @param symbol The standard symbol (e.g., "BTC/USD")
     * @return Kraken WebSocket symbol (e.g., "XBT/USD")
     */
    private String formatKrakenSymbol(String symbol) {
        // Kraken uses XBT instead of BTC
        String krakenSymbol = symbol.replace("BTC/", "XBT/").replace("/BTC", "/XBT");
        
        // Remove slashes for WebSocket API
        return krakenSymbol.replace("/", "");
    }
    
    @Override
    protected WebSocketListener createWebSocketListener() {
        return new KrakenWebSocketListener();
    }
    
    /**
     * WebSocket listener for Kraken.
     */
    private class KrakenWebSocketListener extends WebSocketListener {
        
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            logInfo("Kraken WebSocket connection opened");
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
                // Kraken uses JSON arrays for WebSocket messages
                JSONArray json = new JSONArray(message);
                
                // Subscription status message
                if (json.length() == 1 && json.get(0) instanceof JSONObject) {
                    JSONObject status = json.getJSONObject(0);
                    if (status.has("status")) {
                        logInfo("Kraken subscription status: " + status.getString("status"));
                    }
                    return;
                }
                
                // Data message format: [channelID, data, channelName, pair]
                if (json.length() >= 4) {
                    JSONArray data = json.getJSONArray(1);
                    String channelName = json.getString(2);
                    String krakenSymbol = json.getString(3);
                    
                    // Convert Kraken symbol back to standard format
                    String symbol = symbolMapping.getOrDefault(krakenSymbol, krakenSymbol);
                    
                    // Handle ticker updates
                    if ("ticker".equals(channelName)) {
                        processTickerUpdate(symbol, data);
                        return;
                    }
                    
                    // Handle order book updates
                    if ("book".equals(channelName)) {
                        processOrderBookUpdate(symbol, data);
                        return;
                    }
                }
                
            } catch (Exception e) {
                logError("Error processing Kraken WebSocket message", e);
            }
        }
        
        /**
         * Process ticker update from Kraken.
         * 
         * @param symbol The trading pair symbol
         * @param data The ticker data
         */
        private void processTickerUpdate(String symbol, JSONArray data) {
            try {
                JSONObject tickerData = data.getJSONObject(0);
                
                // Extract bid & ask from the first level
                JSONArray bidArray = tickerData.getJSONArray("b");
                double bid = bidArray.getDouble(0);
                
                JSONArray askArray = tickerData.getJSONArray("a");
                double ask = askArray.getDouble(0);
                
                // Extract last price & volume
                JSONArray closeArray = tickerData.getJSONArray("c");
                double lastPrice = closeArray.getDouble(0);
                
                JSONArray volumeArray = tickerData.getJSONArray("v");
                double volume = volumeArray.getDouble(1); // 24h volume
                
                Ticker ticker = new Ticker(bid, ask, lastPrice, volume, new Date());
                tickerMap.put(symbol, ticker);
                notifyTickerUpdate(symbol, ticker);
                
            } catch (Exception e) {
                logError("Error processing Kraken ticker data", e);
            }
        }
        
        /**
         * Process order book update from Kraken.
         * 
         * @param symbol The trading pair symbol
         * @param data The order book data
         */
        private void processOrderBookUpdate(String symbol, JSONObject data) {
            try {
                // Get or create order book
                OrderBook orderBook = orderBookMap.getOrDefault(symbol, 
                        new OrderBook(symbol, new ArrayList<>(), new ArrayList<>(), new Date()));
                
                // Process asks updates
                if (data.has("a")) {
                    JSONArray asksArray = data.getJSONArray("a");
                    for (int i = 0; i < asksArray.length(); i++) {
                        JSONArray askEntry = asksArray.getJSONArray(i);
                        double price = askEntry.getDouble(0);
                        double volume = askEntry.getDouble(1);
                        
                        updateOrderBookEntries(orderBook.getAsks(), price, volume);
                    }
                }
                
                // Process bids updates
                if (data.has("b")) {
                    JSONArray bidsArray = data.getJSONArray("b");
                    for (int i = 0; i < bidsArray.length(); i++) {
                        JSONArray bidEntry = bidsArray.getJSONArray(i);
                        double price = bidEntry.getDouble(0);
                        double volume = bidEntry.getDouble(1);
                        
                        updateOrderBookEntries(orderBook.getBids(), price, volume);
                    }
                }
                
                orderBook.setTimestamp(new Date());
                orderBookMap.put(symbol, orderBook);
                notifyOrderBookUpdate(symbol, orderBook);
                
            } catch (Exception e) {
                logError("Error processing Kraken order book data", e);
            }
        }
        
        /**
         * Process order book update from Kraken when it's a snapshot.
         * 
         * @param symbol The trading pair symbol
         * @param data The order book data
         */
        private void processOrderBookUpdate(String symbol, JSONArray data) {
            try {
                JSONObject bookData = data.getJSONObject(0);
                processOrderBookUpdate(symbol, bookData);
            } catch (Exception e) {
                logError("Error processing Kraken order book array data", e);
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
                if (entries == orderBookMap.values().iterator().next().getBids()) {
                    // This is a bid list
                    entries.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                } else {
                    // This is an ask list
                    entries.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                }
            }
        }
    }
} 