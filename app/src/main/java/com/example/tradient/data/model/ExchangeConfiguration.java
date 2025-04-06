package com.example.tradient.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration model for exchange-specific parameters.
 * Contains settings for APIs, fees, rate limits, and exchange-specific behaviors.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeConfiguration {
    
    /**
     * Base fee rates for each exchange (default is maker rate)
     */
    private Map<String, Double> baseFees = new HashMap<>();
    
    /**
     * API base URLs for each exchange
     */
    private Map<String, String> apiUrls = new HashMap<>();
    
    /**
     * WebSocket URLs for each exchange
     */
    private Map<String, String> wsUrls = new HashMap<>();
    
    /**
     * API rate limits for each exchange (requests per minute)
     */
    private Map<String, Integer> rateLimits = new HashMap<>();
    
    /**
     * Connection timeouts for each exchange (in milliseconds)
     */
    private Map<String, Integer> connectionTimeouts = new HashMap<>();
    
    /**
     * API key IDs (stored in external secure storage)
     */
    private Map<String, String> apiKeyReferences = new HashMap<>();
    
    /**
     * Enable specific exchange integrations
     */
    private Map<String, Boolean> enabled = new HashMap<>();
    
    private List<String> enabledExchanges = new ArrayList<>();
    private Map<String, ExchangeSettings> exchanges = new HashMap<>();
    private Map<String, Double> reliabilityScores = new HashMap<>();
    
    /**
     * Gets the minimum profit percentage for arbitrage opportunities
     * @return The minimum profit percentage
     */
    private double minProfitPercent;
    
    /**
     * Gets the available capital for trading
     * @return The available capital
     */
    private double availableCapital;
    
    /**
     * Gets the maximum position size as a percentage of available capital
     * @return The maximum position percentage
     */
    private double maxPositionPercent;
    
    /**
     * Gets the maximum allowed slippage percentage
     * @return The maximum slippage percentage
     */
    private double maxSlippagePercent;
    
    /**
     * Constructor
     */
    public ExchangeConfiguration() {
        // Initialize with default values for maker fees
        baseFees.put("binance", 0.001);   // 0.1% maker fee
        baseFees.put("coinbase", 0.006);  // 0.6% maker fee
        baseFees.put("kraken", 0.002);    // 0.2% maker fee
        baseFees.put("bybit", 0.001);     // 0.1% maker fee
        baseFees.put("okx", 0.001);       // 0.1% maker fee
        
        apiUrls.put("binance", "https://api.binance.com");
        apiUrls.put("coinbase", "https://api.coinbase.com");
        apiUrls.put("kraken", "https://api.kraken.com");
        apiUrls.put("bybit", "https://api.bybit.com");
        apiUrls.put("okx", "https://www.okx.com");
        
        wsUrls.put("binance", "wss://stream.binance.com:9443/ws");
        wsUrls.put("coinbase", "wss://ws-feed.exchange.coinbase.com");
        wsUrls.put("kraken", "wss://ws.kraken.com");
        wsUrls.put("bybit", "wss://stream.bybit.com/realtime");
        wsUrls.put("okx", "wss://ws.okx.com:8443/ws/v5/public");
        
        rateLimits.put("binance", 1200);
        rateLimits.put("coinbase", 300);
        rateLimits.put("kraken", 60);
        rateLimits.put("bybit", 600);
        rateLimits.put("okx", 500);
        
        connectionTimeouts.put("binance", 30000);
        connectionTimeouts.put("coinbase", 30000);
        connectionTimeouts.put("kraken", 30000);
        connectionTimeouts.put("bybit", 30000);
        connectionTimeouts.put("okx", 30000);
        
        enabled.put("binance", true);
        enabled.put("coinbase", true);
        enabled.put("kraken", true);
        enabled.put("bybit", true);
        enabled.put("okx", true);
        
        // Initialize reliability scores (1.0 = most reliable)
        reliabilityScores.put("binance", 0.95);
        reliabilityScores.put("coinbase", 0.95);
        reliabilityScores.put("kraken", 0.90);
        reliabilityScores.put("bybit", 0.90);
        reliabilityScores.put("okx", 0.90);
    }

    public Map<String, Double> getBaseFees() {
        return baseFees;
    }

    public void setBaseFees(Map<String, Double> baseFees) {
        this.baseFees = baseFees;
    }

    public Map<String, String> getApiUrls() {
        return apiUrls;
    }

    public void setApiUrls(Map<String, String> apiUrls) {
        this.apiUrls = apiUrls;
    }

    public Map<String, String> getWsUrls() {
        return wsUrls;
    }

    public void setWsUrls(Map<String, String> wsUrls) {
        this.wsUrls = wsUrls;
    }

    public Map<String, Integer> getRateLimits() {
        return rateLimits;
    }

    public void setRateLimits(Map<String, Integer> rateLimits) {
        this.rateLimits = rateLimits;
    }

    public Map<String, Integer> getConnectionTimeouts() {
        return connectionTimeouts;
    }

    public void setConnectionTimeouts(Map<String, Integer> connectionTimeouts) {
        this.connectionTimeouts = connectionTimeouts;
    }

    public Map<String, String> getApiKeyReferences() {
        return apiKeyReferences;
    }

    public void setApiKeyReferences(Map<String, String> apiKeyReferences) {
        this.apiKeyReferences = apiKeyReferences;
    }

    public Map<String, Boolean> getEnabled() {
        return enabled;
    }

    public void setEnabled(Map<String, Boolean> enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get the base fee for a specific exchange
     * 
     * @param exchangeName The exchange name
     * @return The base fee or default if not configured
     */
    public double getExchangeFee(String exchangeName) {
        return baseFees.getOrDefault(exchangeName.toLowerCase(), 0.001);
    }
    
    /**
     * Get the API URL for a specific exchange
     * 
     * @param exchangeName The exchange name
     * @return The API URL or null if not configured
     */
    public String getApiUrl(String exchangeName) {
        return apiUrls.get(exchangeName.toLowerCase());
    }
    
    /**
     * Get the WebSocket URL for a specific exchange
     * 
     * @param exchangeName The exchange name
     * @return The WebSocket URL or null if not configured
     */
    public String getWsUrl(String exchangeName) {
        return wsUrls.get(exchangeName.toLowerCase());
    }
    
    /**
     * Check if an exchange is enabled
     * 
     * @param exchangeName The exchange name
     * @return true if the exchange is enabled
     */
    public boolean isExchangeEnabled(String exchangeName) {
        return enabled.getOrDefault(exchangeName.toLowerCase(), false);
    }

    /**
     * Gets the list of enabled exchanges.
     *
     * @return List of enabled exchange names
     */
    public List<String> getEnabledExchanges() {
        return enabledExchanges;
    }
    
    /**
     * Sets the list of enabled exchanges.
     *
     * @param enabled List of enabled exchange names
     */
    public void setEnabledExchanges(List<String> enabled) {
        this.enabledExchanges = enabled;
    }
    
    /**
     * Gets the settings for all exchanges.
     *
     * @return Map of exchange names to settings
     */
    public Map<String, ExchangeSettings> getExchanges() {
        return exchanges;
    }
    
    /**
     * Sets the settings for all exchanges.
     *
     * @param exchanges Map of exchange names to settings
     */
    public void setExchanges(Map<String, ExchangeSettings> exchanges) {
        this.exchanges = exchanges;
    }
    
    /**
     * Get the fee percentage for a specific exchange and operation
     * 
     * @param exchangeName The exchange name
     * @param isMaker Whether this is a maker (true) or taker (false) operation
     * @return The fee percentage (as a decimal)
     */
    public double getFeePercentage(String exchangeName, boolean isMaker) {
        String key = exchangeName.toLowerCase();
        
        // First check if we have detailed exchange settings
        if (exchanges.containsKey(key) && exchanges.get(key).getFees() != null) {
            return isMaker ? exchanges.get(key).getFees().getMaker() : exchanges.get(key).getFees().getTaker();
        }
        
        // Otherwise use the default fee rates based on exchange
        if (isMaker) {
            switch (key) {
                case "binance": return 0.0002; // 0.02% maker fee (spot)
                case "coinbase": return 0.0040; // 0.40% maker fee
                case "kraken": return 0.0016;  // 0.16% maker fee
                case "bybit": return 0.0001;   // 0.01% maker fee
                case "okx": return 0.0008;     // 0.08% maker fee
                default: return baseFees.getOrDefault(key, 0.001);
            }
        } else {
            // Taker fees
            switch (key) {
                case "binance": return 0.0004; // 0.04% taker fee (spot)
                case "coinbase": return 0.0060; // 0.60% taker fee
                case "kraken": return 0.0026;  // 0.26% taker fee
                case "bybit": return 0.0010;   // 0.10% taker fee
                case "okx": return 0.0010;     // 0.10% taker fee (reduced from 0.20%)
                default: return baseFees.getOrDefault(key, 0.001) * 1.5; // Default taker is 50% higher than maker
            }
        }
    }
    
    /**
     * Get the reliability score for an exchange
     *
     * @param exchangeName The exchange name
     * @param defaultScore Default score if not configured
     * @return The reliability score (0.0-1.0)
     */
    public double getReliabilityScore(String exchangeName, double defaultScore) {
        return reliabilityScores.getOrDefault(exchangeName.toLowerCase(), defaultScore);
    }
    
    /**
     * Set the reliability scores for exchanges
     *
     * @param reliabilityScores Map of exchange names to reliability scores
     */
    public void setReliabilityScores(Map<String, Double> reliabilityScores) {
        this.reliabilityScores = reliabilityScores;
    }
    
    /**
     * Settings for a specific exchange
     */
    public static class ExchangeSettings {
        private String apiKey;
        private String apiSecret;
        private FeeStructure fees;
        private boolean enabled;
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getApiSecret() {
            return apiSecret;
        }
        
        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }
        
        public FeeStructure getFees() {
            return fees;
        }
        
        public void setFees(FeeStructure fees) {
            this.fees = fees;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    /**
     * Fee structure with separate maker and taker fees
     */
    public static class FeeStructure {
        private double maker;
        private double taker;
        
        public double getMaker() {
            return maker;
        }
        
        public void setMaker(double maker) {
            this.maker = maker;
        }
        
        public double getTaker() {
            return taker;
        }
        
        public void setTaker(double taker) {
            this.taker = taker;
        }
    }

    /**
     * Gets the minimum profit percentage for arbitrage opportunities
     * @return The minimum profit percentage
     */
    public double getMinProfitPercent() {
        return minProfitPercent;
    }
    
    /**
     * Gets the available capital for trading
     * @return The available capital
     */
    public double getAvailableCapital() {
        return availableCapital;
    }
    
    /**
     * Gets the maximum position size as a percentage of available capital
     * @return The maximum position percentage
     */
    public double getMaxPositionPercent() {
        return maxPositionPercent;
    }
    
    /**
     * Gets the maximum allowed slippage percentage
     * @return The maximum slippage percentage
     */
    public double getMaxSlippagePercent() {
        return maxSlippagePercent;
    }
} 