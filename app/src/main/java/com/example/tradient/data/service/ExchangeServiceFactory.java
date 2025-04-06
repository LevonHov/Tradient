package com.example.tradient.data.service;

import com.example.tradient.data.model.Exchange;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for creating and managing ExchangeService instances
 */
public class ExchangeServiceFactory {
    
    private static final Map<Exchange, ExchangeService> serviceCache = new HashMap<>();
    
    // Default fee values for different exchanges
    private static final double BINANCE_DEFAULT_FEE = 0.001;  // 0.1% 
    private static final double COINBASE_DEFAULT_FEE = 0.005; // 0.5%
    private static final double KRAKEN_DEFAULT_FEE = 0.0026;  // 0.26%
    private static final double BYBIT_DEFAULT_FEE = 0.001;    // 0.1%
    private static final double OKX_DEFAULT_FEE = 0.001;      // 0.1%
    
    /**
     * Get an ExchangeService for a specific exchange
     * @param exchange The exchange to get a service for
     * @return An ExchangeService instance
     */
    public static ExchangeService getExchangeService(Exchange exchange) {
        if (serviceCache.containsKey(exchange)) {
            return serviceCache.get(exchange);
        }
        
        ExchangeService service;
        switch (exchange) {
            case BINANCE:
                service = new BinanceExchangeService(BINANCE_DEFAULT_FEE);
                break;
            case COINBASE:
                service = new CoinbaseExchangeService(COINBASE_DEFAULT_FEE);
                break;
            case KRAKEN:
                service = new KrakenExchangeService(KRAKEN_DEFAULT_FEE);
                break;
            case BYBIT:
                service = new BybitV5ExchangeService(BYBIT_DEFAULT_FEE);
                break;
            case OKX:
                service = new OkxExchangeService(OKX_DEFAULT_FEE);
                break;
            default:
                // For any unsupported exchange, use Binance as a fallback
                service = new BinanceExchangeService(BINANCE_DEFAULT_FEE);
                break;
        }
        
        serviceCache.put(exchange, service);
        return service;
    }
} 