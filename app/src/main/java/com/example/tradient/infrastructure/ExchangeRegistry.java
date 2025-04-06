package com.example.tradient.infrastructure;

import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.model.ExchangeConfiguration;
import com.example.tradient.data.service.BinanceExchangeService;
import com.example.tradient.data.service.BybitV5ExchangeService;
import com.example.tradient.data.service.CoinbaseExchangeService;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.data.service.KrakenExchangeService;
import com.example.tradient.data.service.OkxExchangeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for creating and managing exchange service instances.
 * This class helps with creating and configuring exchange services based on application configuration.
 */
public class ExchangeRegistry {
    
    private static ExchangeRegistry instance;
    private final Map<String, ExchangeService> exchanges = new HashMap<>();
    private final ExchangeConfiguration config;
    private final INotificationService notificationService;
    
    /**
     * Private constructor for singleton pattern.
     * 
     * @param notificationService Notification service for logging
     */
    private ExchangeRegistry(INotificationService notificationService) {
        this.config = ConfigurationFactory.getExchangeConfig();
        this.notificationService = notificationService;
        initializeExchanges();
    }
    
    /**
     * Get the singleton instance.
     * 
     * @param notificationService Notification service for logging
     * @return The ExchangeRegistry instance
     */
    public static synchronized ExchangeRegistry getInstance(INotificationService notificationService) {
        if (instance == null) {
            instance = new ExchangeRegistry(notificationService);
        }
        return instance;
    }
    
    /**
     * Initialize all supported exchanges.
     */
    private void initializeExchanges() {
        // Create exchange services for each supported exchange
        if (config.isExchangeEnabled("binance")) {
            BinanceExchangeService binance = new BinanceExchangeService(
                    config.getExchangeFee("binance"), notificationService);
            binance.setBnbDiscount(ConfigurationFactory.getBoolean("exchanges.binance.bnbDiscount", false));
            exchanges.put("binance", binance);
        }
        
        if (config.isExchangeEnabled("bybit")) {
            BybitV5ExchangeService bybit = new BybitV5ExchangeService(
                    config.getExchangeFee("bybit"), notificationService);
            exchanges.put("bybit", bybit);
        }
        
        if (config.isExchangeEnabled("coinbase")) {
            CoinbaseExchangeService coinbase = new CoinbaseExchangeService(
                    config.getExchangeFee("coinbase"));
            coinbase.setNotificationService(notificationService);
            exchanges.put("coinbase", coinbase);
        }
        
        if (config.isExchangeEnabled("kraken")) {
            KrakenExchangeService kraken = new KrakenExchangeService(
                    config.getExchangeFee("kraken"));
            kraken.setNotificationService(notificationService);
            exchanges.put("kraken", kraken);
        }
        
        if (config.isExchangeEnabled("okx")) {
            OkxExchangeService okx = new OkxExchangeService(
                    config.getExchangeFee("okx"));
            okx.setNotificationService(notificationService);
            exchanges.put("okx", okx);
        }
        
        // Update fee tiers for all exchanges based on configured volumes
        for (ExchangeService exchange : exchanges.values()) {
            exchange.updateFeesTiers(0.0); // Start with zero volume - will be updated later
        }
    }
    
    /**
     * Get all enabled exchanges.
     * 
     * @return List of enabled exchange services
     */
    public List<ExchangeService> getAllExchanges() {
        return new ArrayList<>(exchanges.values());
    }
    
    /**
     * Get a specific exchange by name.
     * 
     * @param exchangeName The exchange name (case insensitive)
     * @return The exchange service or null if not found
     */
    public ExchangeService getExchange(String exchangeName) {
        return exchanges.get(exchangeName.toLowerCase());
    }
    
    /**
     * Check if an exchange is registered.
     * 
     * @param exchangeName The exchange name (case insensitive)
     * @return true if the exchange is registered
     */
    public boolean hasExchange(String exchangeName) {
        return exchanges.containsKey(exchangeName.toLowerCase());
    }
    
    /**
     * Initialize WebSocket connections for all registered exchanges.
     * 
     * @param symbols The trading pair symbols to subscribe to
     * @return Map of exchange names to their initialization success status
     */
    public Map<String, Boolean> initializeWebSockets(List<String> symbols) {
        Map<String, Boolean> results = new HashMap<>();
        
        for (Map.Entry<String, ExchangeService> entry : exchanges.entrySet()) {
            String name = entry.getKey();
            ExchangeService exchange = entry.getValue();
            
            try {
                boolean success = exchange.initializeWebSocket(symbols);
                results.put(name, success);
            } catch (Exception e) {
                results.put(name, false);
                if (notificationService != null) {
                    notificationService.logError("Failed to initialize WebSocket for " + name, e);
                }
            }
        }
        
        return results;
    }
    
    /**
     * Close all WebSocket connections.
     */
    public void closeAllWebSockets() {
        for (ExchangeService exchange : exchanges.values()) {
            try {
                exchange.closeWebSocket();
            } catch (Exception e) {
                if (notificationService != null) {
                    notificationService.logError("Error closing WebSocket for " + exchange.getExchangeName(), e);
                }
            }
        }
    }
} 