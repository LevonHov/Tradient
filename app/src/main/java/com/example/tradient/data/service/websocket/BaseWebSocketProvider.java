package com.example.tradient.data.service.websocket;

import com.example.tradient.data.http.HttpClientProvider;
import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.interfaces.IWebSocketProvider;
import com.example.tradient.data.interfaces.IWebSocketUpdateListener;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Base implementation of WebSocket functionality to be extended by specific exchange implementations.
 * Provides common WebSocket management and listener notification functionality.
 */
public abstract class BaseWebSocketProvider implements IWebSocketProvider {
    
    // Timeout settings
    private static final int CONNECT_TIMEOUT_SECONDS = 15;
    private static final int READ_TIMEOUT_SECONDS = 15;
    private static final int WRITE_TIMEOUT_SECONDS = 15;
    
    protected String exchangeName;
    protected OkHttpClient wsClient;
    protected WebSocket webSocket;
    protected volatile boolean websocketConnected = false;
    protected final List<IWebSocketUpdateListener> listeners = new CopyOnWriteArrayList<>();
    public INotificationService notificationService;
    
    /**
     * Constructor with exchange name and notification service.
     *
     * @param exchangeName The name of the exchange
     * @param notificationService Optional notification service for logging
     */
    public BaseWebSocketProvider(String exchangeName, INotificationService notificationService) {
        this.exchangeName = exchangeName;
        this.notificationService = notificationService;
        
        // Initialize OkHttpClient with custom timeout settings
        this.wsClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
    
    /**
     * Get the WebSocket endpoint URL for the specified symbols.
     *
     * @param symbols The symbols to subscribe to
     * @return The WebSocket endpoint URI
     */
    protected abstract URI getWebSocketEndpoint(List<String> symbols);
    
    /**
     * Create subscription messages for the WebSocket connection.
     *
     * @param symbols The symbols to subscribe to
     * @return A list of subscription messages to send
     */
    protected abstract List<String> createSubscriptionMessages(List<String> symbols);
    
    /**
     * Create a new WebSocket listener for the specific exchange.
     *
     * @return The WebSocket listener
     */
    protected abstract WebSocketListener createWebSocketListener();
    
    @Override
    public boolean initializeWebSocket(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            logWarning("No symbols provided for WebSocket initialization");
            return false;
        }

        // Clean up any existing connection first
        if (webSocket != null) {
            try {
                if (websocketConnected) {
                    logInfo("Closing existing WebSocket connection before reconnecting");
                    webSocket.close(1000, "Reconnecting");
                }
                webSocket = null;
                websocketConnected = false;
            } catch (Exception e) {
                logError("Error closing existing WebSocket", e);
            }
        }
        
        try {
            // Get the WebSocket endpoint URL
            URI wsEndpoint = getWebSocketEndpoint(symbols);
            if (wsEndpoint == null) {
                logError("Failed to get WebSocket endpoint", null);
                return false;
            }
            
            logDebug("Connecting to WebSocket: " + wsEndpoint);
            
            // Create the WebSocket listener
            WebSocketListener wsListener = createWebSocketListener();
            
            // Build and connect the WebSocket
            Request request = new Request.Builder()
                    .url(wsEndpoint.toString())
                    .build();
                    
            webSocket = wsClient.newWebSocket(request, wsListener);
            
            // Give it a moment to establish connection
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                // Ignore interruption
            }
            
            // Send subscription messages
            List<String> subscriptionMessages = createSubscriptionMessages(symbols);
            if (subscriptionMessages != null) {
                for (String message : subscriptionMessages) {
                    if (message != null && !message.isEmpty()) {
                        boolean sent = webSocket.send(message);
                        if (!sent) {
                            logWarning("Failed to send subscription message: " + message);
                        }
                    }
                }
            }
            
            logInfo("WebSocket connection established for " + symbols.size() + " symbols");
            websocketConnected = true;
            
            // Notify listeners
            for (IWebSocketUpdateListener listener : listeners) {
                try {
                    listener.onWebSocketConnected(this);
                } catch (Exception e) {
                    logError("Error notifying listener of connection", e);
                }
            }
            
            return true;
        } catch (Exception e) {
            logError("Error initializing WebSocket", e);
            websocketConnected = false;
            return false;
        }
    }
    
    @Override
    public void closeWebSocket() {
        if (webSocket != null) {
            try {
                webSocket.close(1000, "Closing connection");
                websocketConnected = false;
                logInfo("WebSocket connection closed");
                
                // Notify listeners
                for (IWebSocketUpdateListener listener : listeners) {
                    try {
                        listener.onWebSocketDisconnected(this, 1000, "Closed by user");
                    } catch (Exception e) {
                        logError("Error notifying listener of disconnection", e);
                    }
                }
            } catch (Exception e) {
                logError("Error closing WebSocket", e);
            } finally {
                webSocket = null;
            }
        }
    }
    
    @Override
    public boolean isWebSocketConnected() {
        return websocketConnected;
    }
    
    @Override
    public void addWebSocketListener(IWebSocketUpdateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
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
                logError("Error in ticker update listener", e);
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
                logError("Error in order book update listener", e);
            }
        }
    }
    
    /**
     * Notify all listeners of a WebSocket error.
     *
     * @param error The error that occurred
     */
    protected void notifyWebSocketError(Throwable error) {
        websocketConnected = false;
        for (IWebSocketUpdateListener listener : listeners) {
            try {
                listener.onWebSocketError(this, error);
            } catch (Exception e) {
                logError("Error in WebSocket error listener", e);
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
        websocketConnected = false;
        for (IWebSocketUpdateListener listener : listeners) {
            try {
                listener.onWebSocketDisconnected(this, code, reason);
            } catch (Exception e) {
                logError("Error in WebSocket disconnection listener", e);
            }
        }
    }
    
    /**
     * Log an informational message.
     *
     * @param message The message to log
     */
    protected void logInfo(String message) {
        if (notificationService != null) {
            notificationService.logInfo(exchangeName + ": " + message);
        }
    }
    
    /**
     * Log a warning message.
     *
     * @param message The message to log
     */
    protected void logWarning(String message) {
        if (notificationService != null) {
            notificationService.logWarning(exchangeName + ": " + message);
        }
    }
    
    /**
     * Log an error message.
     *
     * @param message The message to log
     * @param error The error that occurred
     */
    protected void logError(String message, Throwable error) {
        if (notificationService != null) {
            notificationService.logError(exchangeName + ": " + message, error);
        }
    }
    
    /**
     * Log a debug message.
     *
     * @param message The message to log
     */
    protected void logDebug(String message) {
        if (notificationService != null) {
            notificationService.logDebug(exchangeName + ": " + message);
        }
    }
} 