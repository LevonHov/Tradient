package com.example.tradient.util;

import com.example.tradient.R;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to map exchange names to their corresponding logo resources.
 * This provides a centralized place to manage exchange logo mappings.
 */
public class AssetLogoMap {
    
    private static final Map<String, Integer> EXCHANGE_LOGOS = new HashMap<>();
    
    static {
        // Initialize the mapping of exchange names to logo resource IDs
        EXCHANGE_LOGOS.put("binance", R.drawable.binance_logo);
        EXCHANGE_LOGOS.put("coinbase", R.drawable.coinbase_logo);
        EXCHANGE_LOGOS.put("kraken", R.drawable.kraken_logo);
        EXCHANGE_LOGOS.put("bybit", R.drawable.bybit_logo);
        EXCHANGE_LOGOS.put("okx", R.drawable.okx_logo);
    }
    
    /**
     * Get the logo resource ID for a given exchange name.
     * 
     * @param exchangeName The name of the exchange (case-insensitive)
     * @return The resource ID of the exchange logo, or a default if not found
     */
    public static int getExchangeLogo(String exchangeName) {
        if (exchangeName == null) {
            return R.drawable.ic_opportunities; // Default fallback
        }
        
        Integer resourceId = EXCHANGE_LOGOS.get(exchangeName.toLowerCase());
        return resourceId != null ? resourceId : R.drawable.ic_opportunities;
    }
    
    /**
     * Check if a logo exists for the given exchange name.
     * 
     * @param exchangeName The name of the exchange (case-insensitive)
     * @return True if a logo exists for this exchange, false otherwise
     */
    public static boolean hasLogo(String exchangeName) {
        return exchangeName != null && EXCHANGE_LOGOS.containsKey(exchangeName.toLowerCase());
    }
} 