package com.example.tradient.ui.opportunities;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data class for storing filter parameters for arbitrage opportunities
 */
public class Filter implements Parcelable {
    
    private float minProfitPercent = 0f;
    private float maxProfitPercent = 50f;
    private Set<String> selectedExchanges = new HashSet<>();
    private float maxRiskLevel = 5f;
    private int maxExecutionTimeMinutes = -1; // -1 means any time
    private float minVolume = 1f;
    private boolean isFilterApplied = false;
    
    // Added for compatibility with old code
    private float minProfit = 0f;
    private float maxProfit = Float.MAX_VALUE;
    private Set<String> riskLevels = new HashSet<>();
    private Set<String> exchanges = new HashSet<>();
    private Set<String> cryptocurrencies = new HashSet<>();
    
    /**
     * Default constructor
     */
    public Filter() {
        // Default values are set in field declarations
    }
    
    /**
     * Constructor with all parameters
     */
    public Filter(float minProfitPercent, float maxProfitPercent, Set<String> selectedExchanges,
                 float maxRiskLevel, int maxExecutionTimeMinutes, float minVolume) {
        this.minProfitPercent = minProfitPercent;
        this.maxProfitPercent = maxProfitPercent;
        this.selectedExchanges = selectedExchanges != null ? selectedExchanges : new HashSet<>();
        this.maxRiskLevel = maxRiskLevel;
        this.maxExecutionTimeMinutes = maxExecutionTimeMinutes;
        this.minVolume = minVolume;
        this.isFilterApplied = true;
    }
    
    /**
     * Copy constructor
     */
    public Filter(Filter other) {
        if (other != null) {
            this.minProfitPercent = other.minProfitPercent;
            this.maxProfitPercent = other.maxProfitPercent;
            this.selectedExchanges = new HashSet<>(other.selectedExchanges);
            this.maxRiskLevel = other.maxRiskLevel;
            this.maxExecutionTimeMinutes = other.maxExecutionTimeMinutes;
            this.minVolume = other.minVolume;
            this.isFilterApplied = other.isFilterApplied;
            
            // Also copy the old fields
            this.minProfit = other.minProfit;
            this.maxProfit = other.maxProfit;
            this.riskLevels = new HashSet<>(other.riskLevels);
            this.exchanges = new HashSet<>(other.exchanges);
            this.cryptocurrencies = new HashSet<>(other.cryptocurrencies);
        }
    }
    
    /**
     * Constructor for Parcelable
     */
    protected Filter(Parcel in) {
        minProfitPercent = in.readFloat();
        maxProfitPercent = in.readFloat();
        maxRiskLevel = in.readFloat();
        maxExecutionTimeMinutes = in.readInt();
        minVolume = in.readFloat();
        isFilterApplied = in.readByte() != 0;
        minProfit = in.readFloat();
        maxProfit = in.readFloat();
        
        // Read selected exchanges
        int exchangeCount = in.readInt();
        selectedExchanges = new HashSet<>(exchangeCount);
        if (exchangeCount > 0) {
            String[] exchanges = new String[exchangeCount];
            in.readStringArray(exchanges);
            for (String exchange : exchanges) {
                selectedExchanges.add(exchange);
            }
        }
        
        // Read risk levels
        int riskCount = in.readInt();
        riskLevels = new HashSet<>(riskCount);
        if (riskCount > 0) {
            String[] risks = new String[riskCount];
            in.readStringArray(risks);
            for (String risk : risks) {
                riskLevels.add(risk);
            }
        }
        
        // Read exchanges for old code
        int oldExchangeCount = in.readInt();
        exchanges = new HashSet<>(oldExchangeCount);
        if (oldExchangeCount > 0) {
            String[] exch = new String[oldExchangeCount];
            in.readStringArray(exch);
            for (String e : exch) {
                exchanges.add(e);
            }
        }
        
        // Read cryptocurrencies
        int cryptoCount = in.readInt();
        cryptocurrencies = new HashSet<>(cryptoCount);
        if (cryptoCount > 0) {
            String[] cryptos = new String[cryptoCount];
            in.readStringArray(cryptos);
            for (String crypto : cryptos) {
                cryptocurrencies.add(crypto);
            }
        }
    }
    
    /**
     * Creates a Filter object from a Parcel
     */
    public static final Creator<Filter> CREATOR = new Creator<Filter>() {
        @Override
        public Filter createFromParcel(Parcel in) {
            return new Filter(in);
        }
        
        @Override
        public Filter[] newArray(int size) {
            return new Filter[size];
        }
    };
    
    // Getters and setters
    
    public float getMinProfitPercent() {
        return minProfitPercent;
    }
    
    public void setMinProfitPercent(float minProfitPercent) {
        this.minProfitPercent = minProfitPercent;
        this.isFilterApplied = true;
    }
    
    public float getMaxProfitPercent() {
        return maxProfitPercent;
    }
    
    public void setMaxProfitPercent(float maxProfitPercent) {
        this.maxProfitPercent = maxProfitPercent;
        this.isFilterApplied = true;
    }
    
    public Set<String> getSelectedExchanges() {
        return selectedExchanges;
    }
    
    public void setSelectedExchanges(Set<String> selectedExchanges) {
        this.selectedExchanges = selectedExchanges != null ? selectedExchanges : new HashSet<>();
        this.isFilterApplied = true;
    }
    
    public float getMaxRiskLevel() {
        return maxRiskLevel;
    }
    
    public void setMaxRiskLevel(float maxRiskLevel) {
        this.maxRiskLevel = maxRiskLevel;
        this.isFilterApplied = true;
    }
    
    public int getMaxExecutionTimeMinutes() {
        return maxExecutionTimeMinutes;
    }
    
    public void setMaxExecutionTimeMinutes(int maxExecutionTimeMinutes) {
        this.maxExecutionTimeMinutes = maxExecutionTimeMinutes;
        this.isFilterApplied = true;
    }
    
    public float getMinVolume() {
        return minVolume;
    }
    
    public void setMinVolume(float minVolume) {
        this.minVolume = minVolume;
        this.isFilterApplied = true;
    }
    
    public boolean isFilterApplied() {
        return isFilterApplied;
    }
    
    public void setFilterApplied(boolean filterApplied) {
        isFilterApplied = filterApplied;
    }
    
    // Legacy getters and setters
    
    public float getMinProfit() {
        return minProfit;
    }
    
    public void setMinProfit(float minProfit) {
        this.minProfit = minProfit;
        this.isFilterApplied = true;
    }
    
    public float getMaxProfit() {
        return maxProfit;
    }
    
    public void setMaxProfit(float maxProfit) {
        this.maxProfit = maxProfit;
        this.isFilterApplied = true;
    }
    
    public Set<String> getRiskLevels() {
        return riskLevels;
    }
    
    public void setRiskLevels(Set<String> riskLevels) {
        this.riskLevels = riskLevels != null ? riskLevels : new HashSet<>();
        this.isFilterApplied = true;
    }
    
    public Set<String> getExchanges() {
        return exchanges;
    }
    
    public void setExchanges(Set<String> exchanges) {
        this.exchanges = exchanges != null ? exchanges : new HashSet<>();
        this.isFilterApplied = true;
    }
    
    public Set<String> getCryptocurrencies() {
        return cryptocurrencies;
    }
    
    public void setCryptocurrencies(Set<String> cryptocurrencies) {
        this.cryptocurrencies = cryptocurrencies != null ? cryptocurrencies : new HashSet<>();
        this.isFilterApplied = true;
    }
    
    // Method to check if any filters are active
    public boolean hasActiveFilters() {
        return isFilterApplied;
    }
    
    // Parcelable implementation
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(minProfitPercent);
        dest.writeFloat(maxProfitPercent);
        dest.writeFloat(maxRiskLevel);
        dest.writeInt(maxExecutionTimeMinutes);
        dest.writeFloat(minVolume);
        dest.writeByte((byte) (isFilterApplied ? 1 : 0));
        dest.writeFloat(minProfit);
        dest.writeFloat(maxProfit);
        
        // Write selected exchanges
        dest.writeInt(selectedExchanges.size());
        if (!selectedExchanges.isEmpty()) {
            dest.writeStringArray(selectedExchanges.toArray(new String[0]));
        }
        
        // Write risk levels
        dest.writeInt(riskLevels.size());
        if (!riskLevels.isEmpty()) {
            dest.writeStringArray(riskLevels.toArray(new String[0]));
        }
        
        // Write exchanges for old code
        dest.writeInt(exchanges.size());
        if (!exchanges.isEmpty()) {
            dest.writeStringArray(exchanges.toArray(new String[0]));
        }
        
        // Write cryptocurrencies
        dest.writeInt(cryptocurrencies.size());
        if (!cryptocurrencies.isEmpty()) {
            dest.writeStringArray(cryptocurrencies.toArray(new String[0]));
        }
    }
    
    /**
     * Resets the filter to default values
     */
    public void reset() {
        minProfitPercent = 0f;
        maxProfitPercent = 50f;
        selectedExchanges.clear();
        maxRiskLevel = 5f;
        maxExecutionTimeMinutes = -1;
        minVolume = 1f;
        isFilterApplied = false;
        
        // Reset old fields too
        minProfit = 0f;
        maxProfit = Float.MAX_VALUE;
        riskLevels.clear();
        exchanges.clear();
        cryptocurrencies.clear();
    }
} 