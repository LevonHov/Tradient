package com.example.tradient.api;

import android.util.Log;

import com.example.tradient.data.model.Candle;
import com.example.tradient.data.model.Order;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;
import com.example.tradient.data.model.Ticker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Enhanced Binance API adapter with direct debugging
 */
public class BinanceApiAdapter implements ExchangeApiAdapter {
    private static final String TAG = "BinanceApiAdapter";
    private static final String BASE_URL = "https://api.binance.com";
    
    private final OkHttpClient client;
    private final Executor executor;
    
    public BinanceApiAdapter() {
        // Configure client with longer timeouts for reliability
        this.client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)  // Increased from 10 to 15
            .readTimeout(40, TimeUnit.SECONDS)     // Increased from 30 to 40
            .writeTimeout(20, TimeUnit.SECONDS)    // Increased from 15 to 20
            .retryOnConnectionFailure(true)        // Add retry on connection failure
            .build();
        this.executor = Executors.newCachedThreadPool();
        
        Log.d(TAG, "Initialized Binance API adapter with extended timeouts");
        
        // Test connectivity at initialization to verify connection
        testConnectivity();
    }
    
    /**
     * Test connectivity to the Binance API
     */
    private void testConnectivity() {
        String url = BASE_URL + "/api/v3/ping";
        Request request = new Request.Builder().url(url).build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ Binance connectivity test failed: " + e.getMessage(), e);
            }
            
            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Binance connectivity test successful: " + response.code());
                    } else {
                        Log.e(TAG, "❌ Binance connectivity test failed with HTTP: " + response.code() + " - " + response.message());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error testing Binance connectivity: " + e.getMessage(), e);
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<Ticker> getTicker(String symbol) {
        CompletableFuture<Ticker> future = new CompletableFuture<>();
        
        Log.d(TAG, "Fetching ticker for " + symbol + " from Binance");
        
        if (symbol == null || symbol.isEmpty()) {
            Log.e(TAG, "Invalid symbol provided: " + symbol);
            future.completeExceptionally(new IllegalArgumentException("Symbol cannot be null or empty"));
            return future;
        }
        
        // Binance ticker endpoint: /api/v3/ticker/24hr?symbol={symbol}
        String url = BASE_URL + "/api/v3/ticker/24hr?symbol=" + symbol;
        Request request = new Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Tradient-App")  // Add proper user agent
            .build();
        
        Log.d(TAG, "Making request to: " + url);
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching ticker for " + symbol + ": " + e.getMessage(), e);
                future.completeExceptionally(e);
            }
            
            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        String errorMsg = "HTTP Error: " + response.code() + " - " + response.message();
                        Log.e(TAG, "Binance ticker error: " + errorMsg);
                        
                        // If the error is 400, it could mean the symbol doesn't exist
                        if (response.code() == 400 && responseBody != null) {
                            String responseData = responseBody.string();
                            Log.e(TAG, "Binance error response: " + responseData);
                            if (responseData.contains("Invalid symbol")) {
                                errorMsg = "Invalid symbol: " + symbol;
                            }
                        }
                        
                        future.completeExceptionally(new IOException(errorMsg));
                        return;
                    }
                    
                    if (responseBody == null) {
                        future.completeExceptionally(new IOException("Empty response body"));
                        return;
                    }
                    
                    String responseData = responseBody.string();
                    
                    // Log the first part of the response (truncated if too long)
                    if (responseData.length() > 500) {
                        Log.d(TAG, "Ticker response (truncated): " + responseData.substring(0, 500) + "...");
                    } else {
                        Log.d(TAG, "Ticker response: " + responseData);
                    }
                    
                    JSONObject json = new JSONObject(responseData);
                    
                    // Create new ticker with data
                    Ticker ticker = new Ticker();
                    ticker.setSymbol(symbol);
                    ticker.setLastPrice(json.getDouble("lastPrice"));
                    ticker.setBidPrice(json.getDouble("bidPrice"));
                    ticker.setAskPrice(json.getDouble("askPrice"));
                    ticker.setVolume(json.getDouble("volume"));
                    ticker.setTimestamp(new Date(json.getLong("closeTime")));
                    ticker.setExchangeName("Binance");
                    
                    Log.d(TAG, String.format("Binance ticker for %s: bid=%.8f, ask=%.8f, vol=%.2f", 
                        symbol, ticker.getBidPrice(), ticker.getAskPrice(), ticker.getVolume()));
                    
                    future.complete(ticker);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing ticker data for " + symbol + ": " + e.getMessage(), e);
                    future.completeExceptionally(e);
                }
            }
        });
        
        return future;
    }
    
    @Override
    public CompletableFuture<OrderBook> getOrderBook(String symbol, int depth) {
        CompletableFuture<OrderBook> future = new CompletableFuture<>();
        
        Log.d(TAG, "Fetching order book for " + symbol + " with depth " + depth + " from Binance");
        
        // Binance order book endpoint: /api/v3/depth?symbol={symbol}&limit={limit}
        String url = BASE_URL + "/api/v3/depth?symbol=" + symbol + "&limit=" + depth;
        Request request = new Request.Builder().url(url).build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching order book for " + symbol + ": " + e.getMessage());
                future.completeExceptionally(e);
            }
            
            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        String errorMsg = "HTTP Error: " + response.code() + " - " + response.message();
                        Log.e(TAG, "Binance order book error: " + errorMsg);
                        future.completeExceptionally(new IOException(errorMsg));
                        return;
                    }
                    
                    if (responseBody == null) {
                        future.completeExceptionally(new IOException("Empty response body"));
                        return;
                    }
                    
                    String responseData = responseBody.string();
                    JSONObject json = new JSONObject(responseData);
                    
                    // Create order book
                    OrderBook orderBook = new OrderBook();
                    
                    // Use the setter that accepts a String parameter if available
                    try {
                        java.lang.reflect.Method setSymbolMethod = OrderBook.class.getMethod("setSymbol", String.class);
                        setSymbolMethod.invoke(orderBook, symbol);
                    } catch (Exception e) {
                        Log.w(TAG, "Could not set symbol on OrderBook: " + e.getMessage());
                    }
                    
                    // Set timestamp as Date
                    orderBook.setTimestamp(new Date(System.currentTimeMillis()));
                    
                    // Parse bids - track total volume for logging
                    List<OrderBookEntry> bids = new ArrayList<>();
                    JSONArray bidsArray = json.getJSONArray("bids");
                    double totalBidVolume = 0;
                    
                    for (int i = 0; i < bidsArray.length(); i++) {
                        JSONArray bidData = bidsArray.getJSONArray(i);
                        double price = bidData.getDouble(0);
                        double quantity = bidData.getDouble(1);
                        totalBidVolume += quantity;
                        bids.add(new OrderBookEntry(price, quantity));
                    }
                    orderBook.setBids(bids);
                    
                    // Parse asks - track total volume for logging
                    List<OrderBookEntry> asks = new ArrayList<>();
                    JSONArray asksArray = json.getJSONArray("asks");
                    double totalAskVolume = 0;
                    
                    for (int i = 0; i < asksArray.length(); i++) {
                        JSONArray askData = asksArray.getJSONArray(i);
                        double price = askData.getDouble(0);
                        double quantity = askData.getDouble(1);
                        totalAskVolume += quantity;
                        asks.add(new OrderBookEntry(price, quantity));
                    }
                    orderBook.setAsks(asks);
                    
                    // Log depth information
                    Log.d(TAG, String.format("Binance order book for %s: %d bids (volume: %.4f), %d asks (volume: %.4f)", 
                        symbol, bids.size(), totalBidVolume, asks.size(), totalAskVolume));
                    
                    // Calculate market depth for 1% and 2% for debugging
                    try {
                        double depth1pct = orderBook.getDepth(1.0);
                        double depth2pct = orderBook.getDepth(2.0);
                        Log.d(TAG, String.format("Binance market depth for %s: 1%% depth=%.4f, 2%% depth=%.4f",
                            symbol, depth1pct, depth2pct));
                    } catch (Exception e) {
                        Log.w(TAG, "Could not calculate market depth: " + e.getMessage());
                    }
                    
                    future.complete(orderBook);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing order book data for " + symbol + ": " + e.getMessage());
                    future.completeExceptionally(e);
                }
            }
        });
        
        return future;
    }
    
    @Override
    public CompletableFuture<List<Candle>> getHistoricalData(String symbol, String interval, int limit) {
        CompletableFuture<List<Candle>> future = new CompletableFuture<>();
        
        Log.d(TAG, "Fetching historical data for " + symbol + " with interval " + interval + " from Binance");
        
        // Binance klines endpoint: /api/v3/klines?symbol={symbol}&interval={interval}&limit={limit}
        String url = BASE_URL + "/api/v3/klines?symbol=" + symbol + "&interval=" + interval + "&limit=" + limit;
        Request request = new Request.Builder().url(url).build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching historical data for " + symbol + ": " + e.getMessage());
                future.completeExceptionally(e);
            }
            
            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        String errorMsg = "HTTP Error: " + response.code() + " - " + response.message();
                        Log.e(TAG, "Binance historical data error: " + errorMsg);
                        future.completeExceptionally(new IOException(errorMsg));
                        return;
                    }
                    
                    if (responseBody == null) {
                        future.completeExceptionally(new IOException("Empty response body"));
                        return;
                    }
                    
                    String responseData = responseBody.string();
                    JSONArray jsonArray = new JSONArray(responseData);
                    
                    List<Candle> candles = new ArrayList<>();
                    double averageVolume = 0;
                    double highestVolume = 0;
                    double averageRange = 0;
                    
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONArray candleData = jsonArray.getJSONArray(i);
                        
                        Candle candle = new Candle();
                        candle.setOpenTime(candleData.getLong(0));
                        candle.setOpen(candleData.getDouble(1));
                        candle.setHigh(candleData.getDouble(2));
                        candle.setLow(candleData.getDouble(3));
                        candle.setClose(candleData.getDouble(4));
                        candle.setVolume(candleData.getDouble(5));
                        candle.setCloseTime(candleData.getLong(6));
                        
                        // Track statistics for logging
                        averageVolume += candle.getVolume();
                        highestVolume = Math.max(highestVolume, candle.getVolume());
                        averageRange += candle.getPriceRangePercent();
                        
                        candles.add(candle);
                    }
                    
                    // Calculate averages for logging
                    if (!candles.isEmpty()) {
                        averageVolume /= candles.size();
                        averageRange /= candles.size();
                    }
                    
                    Log.d(TAG, String.format("Binance historical data for %s: %d candles, avg volume=%.2f, avg range=%.2f%%", 
                        symbol, candles.size(), averageVolume, averageRange));
                    
                    future.complete(candles);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing historical data for " + symbol + ": " + e.getMessage());
                    future.completeExceptionally(e);
                }
            }
        });
        
        return future;
    }
    
    @Override
    public CompletableFuture<Double> getTradingFee(String symbol) {
        // For public access, we can use the default fee rate since
        // the actual fee endpoint requires authentication
        CompletableFuture<Double> future = new CompletableFuture<>();
        double defaultTakerFee = 0.001; // 0.1% default fee
        Log.d(TAG, "Using default trading fee for Binance: " + (defaultTakerFee * 100) + "%");
        future.complete(defaultTakerFee);
        return future;
    }
    
    @Override
    public String convertSymbolToExchangeFormat(String normalizedSymbol) {
        if (normalizedSymbol == null || normalizedSymbol.isEmpty()) {
            Log.e(TAG, "Cannot convert null or empty symbol to Binance format");
            return "";
        }
        
        // Binance uses no separator, e.g., "BTC/USDT" -> "BTCUSDT"
        String formatted = normalizedSymbol.replace("/", "")
                                          .replace("-", "")
                                          .replace(" ", "")
                                          .toUpperCase(); 
        Log.d(TAG, "Converted " + normalizedSymbol + " to Binance format: " + formatted);
        return formatted;
    }
    
    @Override
    public String getExchangeName() {
        return "Binance";
    }
} 