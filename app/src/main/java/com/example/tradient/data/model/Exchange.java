package com.example.tradient.data.model;

/**
 * Enum representing cryptocurrency exchanges supported by the application
 */
public enum Exchange {
    BINANCE,
    COINBASE,
    KRAKEN,
    BYBIT,
    OKX,
    KUCOIN,
    HUOBI,
    BITFINEX,
    BITTREX,
    GEMINI;
    
    public String getName() {
        return this.name().charAt(0) + this.name().substring(1).toLowerCase();
    }
} 