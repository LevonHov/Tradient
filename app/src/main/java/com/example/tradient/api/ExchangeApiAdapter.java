package com.example.tradient.api;

import com.example.tradient.data.model.Candle;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for all exchange API adapters.
 * Each exchange implementation will provide methods to fetch the necessary
 * data for risk calculations from their respective APIs.
 */
public interface ExchangeApiAdapter {
    /**
     * Get current ticker data for a trading pair
     * @param symbol Trading pair in exchange format
     * @return Ticker with current price and volume information
     */
    CompletableFuture<Ticker> getTicker(String symbol);
    
    /**
     * Get order book data for a trading pair
     * @param symbol Trading pair in exchange format
     * @param depth Number of levels to fetch (50-5000 depending on exchange)
     * @return OrderBook with bids and asks
     */
    CompletableFuture<OrderBook> getOrderBook(String symbol, int depth);
    
    /**
     * Get historical candle data for a trading pair
     * @param symbol Trading pair in exchange format
     * @param interval Candle interval (e.g., "1h" for 1 hour)
     * @param limit Number of candles to fetch
     * @return List of candles ordered from newest to oldest
     */
    CompletableFuture<List<Candle>> getHistoricalData(String symbol, String interval, int limit);
    
    /**
     * Get trading fee for a trading pair
     * @param symbol Trading pair in exchange format
     * @return Trading fee as a decimal (e.g., 0.001 for 0.1%)
     */
    CompletableFuture<Double> getTradingFee(String symbol);
    
    /**
     * Convert a normalized symbol to exchange-specific format
     * @param normalizedSymbol Symbol in standard format (e.g., "BTC/USDT")
     * @return Symbol in exchange-specific format (e.g., "BTCUSDT" for Binance)
     */
    String convertSymbolToExchangeFormat(String normalizedSymbol);
    
    /**
     * Get exchange name
     * @return Exchange name (e.g., "Binance", "Kraken")
     */
    String getExchangeName();
} 