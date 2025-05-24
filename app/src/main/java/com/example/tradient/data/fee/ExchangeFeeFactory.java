package com.example.tradient.data.fee;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Factory class for creating exchange-specific fee structures.
 * This class encapsulates the knowledge of fee structures for different exchanges
 * and provides methods to create appropriate Fee objects for each exchange.
 */
public class ExchangeFeeFactory {
    
    // Singleton instance
    private static ExchangeFeeFactory instance;
    
    // Map of exchange names to their default maker fees
    private final Map<String, Fee> defaultMakerFees;
    
    // Map of exchange names to their default taker fees
    private final Map<String, Fee> defaultTakerFees;
    
    /**
     * Private constructor for singleton pattern.
     */
    private ExchangeFeeFactory() {
        defaultMakerFees = new HashMap<>();
        defaultTakerFees = new HashMap<>();
        initializeDefaultFees();
    }
    
    /**
     * Get the singleton instance.
     *
     * @return The ExchangeFeeFactory instance
     */
    public static synchronized ExchangeFeeFactory getInstance() {
        if (instance == null) {
            instance = new ExchangeFeeFactory();
        }
        return instance;
    }
    
    /**
     * Initialize the default fee structures for all supported exchanges.
     */
    private void initializeDefaultFees() {
        // Binance fees (VIP0: 0.02% maker, 0.04% taker spot)
        defaultMakerFees.put("Binance", new PercentageFee(0.0002, true, "Binance maker fee"));
        defaultTakerFees.put("Binance", new PercentageFee(0.0004, false, "Binance taker fee"));
        
        // Coinbase fees (VIP0: 0.40% maker, 0.60% taker)
        defaultMakerFees.put("Coinbase", new PercentageFee(0.0040, true, "Coinbase maker fee"));
        defaultTakerFees.put("Coinbase", new PercentageFee(0.0060, false, "Coinbase taker fee"));
        
        // Kraken fees (VIP0: 0.16% maker, 0.26% taker for most pairs)
        defaultMakerFees.put("Kraken", new PercentageFee(0.0016, true, "Kraken maker fee"));
        defaultTakerFees.put("Kraken", new PercentageFee(0.0026, false, "Kraken taker fee"));
        
        // Bybit fees (VIP0: 0.01% maker, 0.10% taker for spot trading)
        defaultMakerFees.put("Bybit", new PercentageFee(0.0001, true, "Bybit maker fee"));
        defaultTakerFees.put("Bybit", new PercentageFee(0.0010, false, "Bybit taker fee"));
        
        // OKX fees (VIP0: 0.08% maker, 0.10% taker)
        defaultMakerFees.put("OKX", new PercentageFee(0.0008, true, "OKX maker fee"));
        defaultTakerFees.put("OKX", new PercentageFee(0.0010, false, "OKX taker fee"));
        
        // KuCoin fees (VIP0: 0.08% maker, 0.10% taker)
        defaultMakerFees.put("KuCoin", new PercentageFee(0.0008, true, "KuCoin maker fee"));
        defaultTakerFees.put("KuCoin", new PercentageFee(0.0010, false, "KuCoin taker fee"));

        // Gemini fees (VIP0: 0.25% maker, 0.35% taker)
        defaultMakerFees.put("Gemini", new PercentageFee(0.0025, true, "Gemini maker fee"));
        defaultTakerFees.put("Gemini", new PercentageFee(0.0035, false, "Gemini taker fee"));

        // Bitfinex fees (VIP0: 0.10% maker, 0.20% taker)
        defaultMakerFees.put("Bitfinex", new PercentageFee(0.0010, true, "Bitfinex maker fee"));
        defaultTakerFees.put("Bitfinex", new PercentageFee(0.0020, false, "Bitfinex taker fee"));
    }
    
    /**
     * Get the default maker fee for an exchange.
     *
     * @param exchangeName The name of the exchange
     * @return The default maker fee, or null if the exchange is not supported
     */
    public Fee getDefaultMakerFee(String exchangeName) {
        return defaultMakerFees.get(exchangeName);
    }
    
    /**
     * Get the default taker fee for an exchange.
     *
     * @param exchangeName The name of the exchange
     * @return The default taker fee, or null if the exchange is not supported
     */
    public Fee getDefaultTakerFee(String exchangeName) {
        return defaultTakerFees.get(exchangeName);
    }
    
    /**
     * Create a tiered fee structure for Binance based on 30-day trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @param isBnbPair Whether this is for a BNB trading pair (applies special rates)
     * @param hasBnbDiscount Whether BNB payment discount is applied
     * @return A Fee object with the appropriate structure
     */
    public Fee createBinanceFee(double thirtyDayVolume, boolean isMaker, boolean isBnbPair, boolean hasBnbDiscount) {
        Map<Double, Double> tierRates = new TreeMap<>();
        
        if (isMaker) {
            // Maker fees - based on recent data, futures rates (spot would be different)
            tierRates.put(0.0, 0.0002);          // Default: 0.02%
            tierRates.put(250000.0, 0.00016);    // 250K+ volume: 0.016%
            tierRates.put(750000.0, 0.00012);    // 750K+ volume: 0.012%
            tierRates.put(2500000.0, 0.00008);   // 2.5M+ volume: 0.008%
            tierRates.put(7500000.0, 0.00004);   // 7.5M+ volume: 0.004%
            tierRates.put(22500000.0, 0.00002);  // 22.5M+ volume: 0.002%
            tierRates.put(50000000.0, 0.0);      // 50M+ volume: 0.000%
        } else {
            // Taker fees
            tierRates.put(0.0, 0.0005);          // Default: 0.05%
            tierRates.put(250000.0, 0.00045);    // 250K+ volume: 0.045%
            tierRates.put(750000.0, 0.0004);     // 750K+ volume: 0.04%
            tierRates.put(2500000.0, 0.00035);   // 2.5M+ volume: 0.035%
            tierRates.put(7500000.0, 0.0003);    // 7.5M+ volume: 0.03%
            tierRates.put(22500000.0, 0.00025);  // 22.5M+ volume: 0.025%
            tierRates.put(50000000.0, 0.00017);  // 50M+ volume: 0.017%
        }
        
        Fee fee = new TieredFee(tierRates, thirtyDayVolume, isMaker);
  
        
        return fee;
    }
    
    /**
     * Create a tiered fee structure for Binance based on 30-day trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @param isBnbPair Whether this is for a BNB trading pair (applies special rates)
     * @return A Fee object with the appropriate structure
     */
    public Fee createBinanceFee(double thirtyDayVolume, boolean isMaker, boolean isBnbPair) {
        return createBinanceFee(thirtyDayVolume, isMaker, isBnbPair, false);
    }
    
    /**
     * Create a tiered fee structure for Coinbase based on 30-day trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @return A Fee object with the appropriate structure
     */
    public Fee createCoinbaseFee(double thirtyDayVolume, boolean isMaker) {
        Map<Double, Double> tierRates = new TreeMap<>();
        
        if (isMaker) {
            // Maker fees
            tierRates.put(0.0, 0.4);           // 0-10K: 0.40%
            tierRates.put(10000.0, 0.35);      // 10K-50K: 0.35%
            tierRates.put(50000.0, 0.25);      // 50K-100K: 0.25%
            tierRates.put(100000.0, 0.2);      // 100K-1M: 0.20%
            tierRates.put(1000000.0, 0.18);    // 1M-5M: 0.18%
            tierRates.put(5000000.0, 0.15);    // 5M-15M: 0.15%
            tierRates.put(15000000.0, 0.1);    // 15M-75M: 0.10%
            tierRates.put(75000000.0, 0.08);   // 75M-250M: 0.08%
            tierRates.put(250000000.0, 0.05);  // 250M-400M: 0.05%
            tierRates.put(400000000.0, 0.03);  // 400M+: 0.03%
            tierRates.put(1000000000.0, 0.0);    // 1B+: 0.00%
        } else {
            // Taker fees
            tierRates.put(0.0, 0.6);           // 0-10K: 0.60%
            tierRates.put(10000.0, 0.5);       // 10K-50K: 0.50%
            tierRates.put(50000.0, 0.35);      // 50K-100K: 0.35%
            tierRates.put(100000.0, 0.3);      // 100K-1M: 0.30%
            tierRates.put(1000000.0, 0.25);    // 1M-5M: 0.25%
            tierRates.put(5000000.0, 0.2);     // 5M-15M: 0.20%
            tierRates.put(15000000.0, 0.18);   // 15M-75M: 0.18%
            tierRates.put(75000000.0, 0.15);   // 75M-250M: 0.15%
            tierRates.put(250000000.0, 0.1);   // 250M-400M: 0.10%
            tierRates.put(400000000.0, 0.08);  // 400M+: 0.08%
            tierRates.put(1000000000.0, 0.05); // 1B+: 0.05%
        }
        
        return new TieredFee(tierRates, thirtyDayVolume, isMaker);
    }
    
    /**
     * Create a tiered fee structure for Kraken based on 30-day trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @return A Fee object with the appropriate structure
     */
    public Fee createKrakenFee(double thirtyDayVolume, boolean isMaker) {
        Map<Double, Double> tierRates = new TreeMap<>();
        
        if (isMaker) {
            // Maker fees - using spot trading fees
            tierRates.put(0.0, 0.25);         // 0-50K: 0.25%
            tierRates.put(50000.0, 0.24);     // 50K-100K: 0.24%
            tierRates.put(100000.0, 0.22);    // 100K-250K: 0.22%
            tierRates.put(250000.0, 0.2);     // 250K-500K: 0.20%
            tierRates.put(500000.0, 0.18);    // 500K-1M: 0.18%
            tierRates.put(1000000.0, 0.16);   // 1M-2.5M: 0.16%
            tierRates.put(2500000.0, 0.14);   // 2.5M-5M: 0.14%
            tierRates.put(5000000.0, 0.12);   // 5M-10M: 0.12%
            tierRates.put(10000000.0, 0.1);   // 10M+: 0.10%
            tierRates.put(100000000.0, 0.08); // 100M+: 0.08%
            tierRates.put(500000000.0, 0.0);    // 500M+: 0.00%
        } else {
            // Taker fees
            tierRates.put(0.0, 0.4);          // 0-50K: 0.40%
            tierRates.put(50000.0, 0.38);     // 50K-100K: 0.38%
            tierRates.put(100000.0, 0.36);    // 100K-250K: 0.36%
            tierRates.put(250000.0, 0.34);    // 250K-500K: 0.34%
            tierRates.put(500000.0, 0.32);    // 500K-1M: 0.32%
            tierRates.put(1000000.0, 0.3);    // 1M-2.5M: 0.30%
            tierRates.put(2500000.0, 0.28);   // 2.5M-5M: 0.28%
            tierRates.put(5000000.0, 0.26);   // 5M-10M: 0.26%
            tierRates.put(10000000.0, 0.24);  // 10M+: 0.24%
            tierRates.put(100000000.0, 0.2);  // 100M+: 0.20%
            tierRates.put(500000000.0, 0.1);  // 500M+: 0.10%
        }
        
        return new TieredFee(tierRates, thirtyDayVolume, isMaker);
    }
    
    /**
     * Create a tiered fee structure for Bybit based on 30-day trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @return A Fee object with the appropriate structure
     */
    public Fee createBybitFee(double thirtyDayVolume, boolean isMaker) {
        Map<Double, Double> tierRates = new TreeMap<>();
        
        if (isMaker) {
            // Maker fees - Spot trading
            tierRates.put(0.0, 0.02);          // All tiers: 0.02%
        } else {
            // Taker fees - Spot trading
            tierRates.put(0.0, 0.055);         // All tiers: 0.055%
        }
        
        return new TieredFee(tierRates, thirtyDayVolume, isMaker);
    }
    
    /**
     * Create the appropriate fee structure for an exchange based on all parameters.
     *
     * @param exchangeName The name of the exchange
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @param bnbPayment Whether BNB payment discount is applied (for Binance only)
     * @param isBnbPair Whether this is a BNB trading pair (for Binance only)
     * @return A Fee object with the appropriate structure
     */
    public Fee createFee(String exchangeName, double thirtyDayVolume, boolean isMaker, boolean bnbPayment, boolean isBnbPair) {
        switch (exchangeName) {
            case "Binance":
                return createBinanceFee(thirtyDayVolume, isMaker, isBnbPair, bnbPayment);
            case "Coinbase":
                return createCoinbaseFee(thirtyDayVolume, isMaker);
            case "Kraken":
                return createKrakenFee(thirtyDayVolume, isMaker);
            case "Bybit":
                return createBybitFee(thirtyDayVolume, isMaker);
            default:
                // Return default fee if exchange is not explicitly supported
                return isMaker ? defaultMakerFees.getOrDefault(exchangeName, 
                               new PercentageFee(0.1, true)) :
                               defaultTakerFees.getOrDefault(exchangeName,
                               new PercentageFee(0.1, false));
        }
    }
    
    /**
     * Create the appropriate fee structure for an exchange based on volume and maker/taker status.
     *
     * @param exchangeName The name of the exchange
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @param bnbPayment Whether BNB payment discount is applied (for Binance only)
     * @return A Fee object with the appropriate structure
     */
    public Fee createFee(String exchangeName, double thirtyDayVolume, boolean isMaker, boolean bnbPayment) {
        return createFee(exchangeName, thirtyDayVolume, isMaker, bnbPayment, false);
    }
    
    /**
     * Create the appropriate fee structure for an exchange based on volume and maker/taker status.
     *
     * @param exchangeName The name of the exchange
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @return A Fee object with the appropriate structure
     */
    public Fee createFee(String exchangeName, double thirtyDayVolume, boolean isMaker) {
        return createFee(exchangeName, thirtyDayVolume, isMaker, false, false);
    }
    
    /**
     * Create the default fee structure for an exchange based on maker/taker status.
     *
     * @param exchangeName The name of the exchange
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @return A Fee object with the appropriate structure
     */
    public Fee createFee(String exchangeName, boolean isMaker) {
        return createFee(exchangeName, 0.0, isMaker, false, false);
    }
} 