package com.example.tradient.data.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a trading pair (e.g., BTC/USD)
 */
public class TradingPair implements Parcelable {
    private String symbol;
    private String name;
    private String baseAsset;
    private String quoteAsset;
    
    /**
     * Constructor with symbol and name
     */
    public TradingPair(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
        
        parseSymbol(symbol);
    }
    
    /**
     * Constructor with just symbol
     */
    public TradingPair(String symbol) {
        this.symbol = symbol;
        this.name = symbol; // Use symbol as name if no specific name is provided
        
        parseSymbol(symbol);
    }
    
    /**
     * Parse the symbol to extract base and quote assets
     */
    private void parseSymbol(String symbol) {
        String[] parts = symbol.split("/");
        if (parts.length == 2) {
            this.baseAsset = parts[0];
            this.quoteAsset = parts[1];
        } else {
            this.baseAsset = symbol;
            this.quoteAsset = "";
        }
    }
    
    protected TradingPair(Parcel in) {
        symbol = in.readString();
        name = in.readString();
        baseAsset = in.readString();
        quoteAsset = in.readString();
    }
    
    public static final Creator<TradingPair> CREATOR = new Creator<TradingPair>() {
        @Override
        public TradingPair createFromParcel(Parcel in) {
            return new TradingPair(in);
        }
        
        @Override
        public TradingPair[] newArray(int size) {
            return new TradingPair[size];
        }
    };
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getBaseAsset() {
        return baseAsset;
    }
    
    public String getQuoteAsset() {
        return quoteAsset;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(symbol);
        dest.writeString(name);
        dest.writeString(baseAsset);
        dest.writeString(quoteAsset);
    }
} 