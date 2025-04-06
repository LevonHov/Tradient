package com.example.tradient.data.service.websocket;

import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Response;
import androidx.annotation.Nullable;

/**
 * Binance-specific implementation of WebSocket provider.
 * Handles WebSocket connection and message processing for Binance exchange.
 */
public class BinanceWebSocketProvider extends BaseWebSocketProvider {

    private static final String WS_BASE_URL = "wss://stream.binance.com/ws";
    
    /**
     * Constructor with exchange name and notification service.
     *
     * @param notificationService Optional notification service for logging
     */
    public BinanceWebSocketProvider(INotificationService notificationService) {
        super("Binance", notificationService);
    }
    
    @Override
    protected URI getWebSocketEndpoint(List<String> symbols) {
        StringBuilder streams = new StringBuilder();
        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i).toLowerCase();
            streams.append(symbol).append("@bookTicker");
            if (i < symbols.size() - 1) {
                streams.append("/");
            }
        }
        
        String wsUrl = WS_BASE_URL + "/" + streams.toString();
        return URI.create(wsUrl);
    }
    
    @Override
    protected List<String> createSubscriptionMessages(List<String> symbols) {
        // For Binance, subscriptions are handled in the URL, not with messages after connection
        return new ArrayList<>();
    }
    
    @Override
    protected WebSocketListener createWebSocketListener() {
        return new BinanceWebSocketListener();
    }
    
    /**
     * WebSocket listener for Binance.
     */
    private class BinanceWebSocketListener extends WebSocketListener {
        private StringBuilder buffer = new StringBuilder();
        
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            try {
                logDebug("WebSocket connection opened");
                websocketConnected = true;
            } catch (Exception e) {
                logError("Error in onOpen", e);
            }
        }
        
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                processMessage(text);
            } catch (Exception e) {
                logError("Error processing WebSocket message: " + text, e);
                // Continue processing other messages
            }
        }
        
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            try {
                notifyWebSocketDisconnected(code, reason);
                websocketConnected = false;
            } catch (Exception e) {
                logError("Error in onClosing", e);
            }
        }
        
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            try {
                notifyWebSocketError(t);
                websocketConnected = false;
            } catch (Exception e) {
                logError("Error in onFailure", e);
            }
        }
        
        /**
         * Process WebSocket message.
         *
         * @param message The message received
         */
        private void processMessage(String message) {
            try {
                if (message == null || message.isEmpty()) {
                    logWarning("Received empty message");
                    return;
                }
                
                JSONObject json = new JSONObject(message);
                
                if (json.has("s") && json.has("b") && json.has("a")) {
                    String symbol = json.getString("s");
                    
                    // Parse numeric values with validation
                    double bidPrice = parseDoubleWithValidation(json, "b");
                    double askPrice = parseDoubleWithValidation(json, "a");
                    double bidQty = parseDoubleWithValidation(json, "B");
                    double askQty = parseDoubleWithValidation(json, "A");
                    
                    // Validate price and quantity
                    if (bidPrice <= 0 || askPrice <= 0) {
                        logWarning("Invalid price values for " + symbol + ": bid=" + bidPrice + ", ask=" + askPrice);
                        return;
                    }
                    
                    // Create ticker update
                    Ticker ticker = new Ticker(bidPrice, askPrice, 0, 0, new Date());
                    notifyTickerUpdate(symbol, ticker);
                    
                    // Create a simple order book with just the best bid and ask
                    List<OrderBookEntry> bids = new ArrayList<>();
                    if (bidQty > 0) {
                        bids.add(new OrderBookEntry(bidPrice, bidQty));
                    }
                    
                    List<OrderBookEntry> asks = new ArrayList<>();
                    if (askQty > 0) {
                        asks.add(new OrderBookEntry(askPrice, askQty));
                    }
                    
                    OrderBook orderBook = new OrderBook(symbol, bids, asks, new Date());
                    notifyOrderBookUpdate(symbol, orderBook);
                    
                    logDebug("Updated ticker and order book for " + symbol);
                } else {
                    logDebug("Received message in unexpected format: " + message);
                }
            } catch (Exception e) {
                logError("Error parsing WebSocket message: " + message, e);
            }
        }
        
        /**
         * Parse a double value from a JSON object with validation
         * 
         * @param json The JSON object
         * @param key The key to extract
         * @return The parsed double value, or 0 if invalid
         */
        private double parseDoubleWithValidation(JSONObject json, String key) {
            try {
                if (json.has(key)) {
                    String valueStr = json.getString(key);
                    if (valueStr != null && !valueStr.isEmpty()) {
                        return Double.parseDouble(valueStr);
                    }
                }
                return 0;
            } catch (Exception e) {
                logWarning("Error parsing value for key " + key);
                return 0;
            }
        }
    }
} 