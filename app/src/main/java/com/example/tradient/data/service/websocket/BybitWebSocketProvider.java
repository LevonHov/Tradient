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
import java.util.List;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Response;
import androidx.annotation.Nullable;

/**
 * Bybit-specific implementation of WebSocket provider.
 * Handles WebSocket connection and message processing for Bybit V5 exchange.
 */
public class BybitWebSocketProvider extends BaseWebSocketProvider {

    private static final String WS_BASE_URL = "wss://stream.bybit.com/v5/public/spot";
    
    /**
     * Constructor with notification service.
     *
     * @param notificationService Optional notification service for logging
     */
    public BybitWebSocketProvider(INotificationService notificationService) {
        super("Bybit", notificationService);
    }
    
    @Override
    protected URI getWebSocketEndpoint(List<String> symbols) {
        return URI.create(WS_BASE_URL);
    }
    
    @Override
    protected List<String> createSubscriptionMessages(List<String> symbols) {
        List<String> messages = new ArrayList<>();
        
        // Create ticker subscription messages
        for (String symbol : symbols) {
            String tickerSubRequest = String.format(
                    "{\"op\":\"subscribe\",\"args\":[\"tickers.%s\"]}",
                    symbol
            );
            messages.add(tickerSubRequest);
            
            // Create order book subscription messages
            String orderbookSubRequest = String.format(
                    "{\"op\":\"subscribe\",\"args\":[\"orderbook.20.%s\"]}",
                    symbol
            );
            messages.add(orderbookSubRequest);
        }
        
        return messages;
    }
    
    @Override
    protected WebSocketListener createWebSocketListener() {
        return new BybitWebSocketListener();
    }
    
    /**
     * WebSocket listener for Bybit data.
     */
    private class BybitWebSocketListener extends WebSocketListener {
        private StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            logInfo("Bybit WebSocket connection opened");
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
         * Process the WebSocket message and update the cache.
         *
         * @param message The WebSocket message to process
         */
        private void processMessage(String message) {
            try {
                JSONObject json = new JSONObject(message);

                // Handle subscription confirmation
                if (json.has("op") && json.getString("op").equals("subscribe")) {
                    logInfo("Bybit subscription success: " + json.toString());
                    return;
                }

                // Handle data messages
                if (json.has("topic") && json.has("data")) {
                    String topic = json.getString("topic");
                    JSONObject data = json.getJSONObject("data");

                    if (topic.startsWith("tickers.")) {
                        String symbol = topic.substring("tickers.".length());
                        double lastPrice = data.getDouble("lastPrice");
                        double bidPrice = data.optDouble("bid1Price", lastPrice);
                        double askPrice = data.optDouble("ask1Price", lastPrice);
                        double volume = data.getDouble("volume24h");
                        
                        Ticker ticker = new Ticker(bidPrice, askPrice, lastPrice, volume, new Date());
                        notifyTickerUpdate(symbol, ticker);
                    }
                    else if (topic.startsWith("orderbook.")) {
                        String[] parts = topic.split("\\.");
                        String symbol = parts[2];
                        JSONArray bidsArray = data.optJSONArray("b");
                        JSONArray asksArray = data.optJSONArray("a");

                        if (bidsArray != null && asksArray != null) {
                            List<OrderBookEntry> bids = new ArrayList<>();
                            for (int i = 0; i < bidsArray.length(); i++) {
                                JSONArray entry = bidsArray.getJSONArray(i);
                                double price = entry.getDouble(0);
                                double volume = entry.getDouble(1);
                                bids.add(new OrderBookEntry(price, volume));
                            }

                            List<OrderBookEntry> asks = new ArrayList<>();
                            for (int i = 0; i < asksArray.length(); i++) {
                                JSONArray entry = asksArray.getJSONArray(i);
                                double price = entry.getDouble(0);
                                double volume = entry.getDouble(1);
                                asks.add(new OrderBookEntry(price, volume));
                            }

                            OrderBook orderBook = new OrderBook(symbol, bids, asks, new Date());
                            notifyOrderBookUpdate(symbol, orderBook);
                        }
                    }
                }
            } catch (Exception e) {
                logError("Error processing Bybit WebSocket message", e);
            }
        }
    }
} 