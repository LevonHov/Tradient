package com.example.tradient.ui.filter;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class that encapsulates all filter criteria for arbitrage opportunities
 */
public class FilterCriteria implements Parcelable {
    // Default values
    private double minProfitPercentage = 0.0;
    private double maxProfitPercentage = 50.0;
    private double maxSlippagePercentage = 2.0;
    private double minExecutionTime = 0.0;
    private double maxExecutionTime = 300.0; // 5 minutes in seconds
    private String riskLevel = "Medium";
    private List<String> sourceExchanges;
    private List<String> destinationExchanges;
    private Set<String> riskLevels = new HashSet<>();
    private Set<String> exchanges = new HashSet<>();

    /**
     * Default constructor with default values
     */
    public FilterCriteria() {
        // Default values initialized in field declarations
        // Initialize default risk levels
        if (riskLevel != null) {
            riskLevels.add(riskLevel);
        }
    }

    /**
     * Constructor to create a filter with specific values
     */
    public FilterCriteria(double minProfitPercentage, double maxProfitPercentage,
                         double maxSlippagePercentage, double minExecutionTime,
                         double maxExecutionTime, String riskLevel,
                         List<String> sourceExchanges, List<String> destinationExchanges) {
        this.minProfitPercentage = minProfitPercentage;
        this.maxProfitPercentage = maxProfitPercentage;
        this.maxSlippagePercentage = maxSlippagePercentage;
        this.minExecutionTime = minExecutionTime;
        this.maxExecutionTime = maxExecutionTime;
        this.riskLevel = riskLevel;
        this.sourceExchanges = sourceExchanges;
        this.destinationExchanges = destinationExchanges;
    }

    /**
     * Constructor to recreate object from a parcel
     */
    protected FilterCriteria(Parcel in) {
        minProfitPercentage = in.readDouble();
        maxProfitPercentage = in.readDouble();
        maxSlippagePercentage = in.readDouble();
        minExecutionTime = in.readDouble();
        maxExecutionTime = in.readDouble();
        riskLevel = in.readString();
        
        // Read source exchanges (handle null case)
        if (in.readInt() == 1) {
            sourceExchanges = new ArrayList<>();
            in.readStringList(sourceExchanges);
        }
        
        // Read destination exchanges (handle null case)
        if (in.readInt() == 1) {
            destinationExchanges = new ArrayList<>();
            in.readStringList(destinationExchanges);
        }
        
        // Read risk levels set
        if (in.readInt() == 1) {
            List<String> riskLevelsList = new ArrayList<>();
            in.readStringList(riskLevelsList);
            riskLevels = new HashSet<>(riskLevelsList);
        } else {
            riskLevels = new HashSet<>();
            if (riskLevel != null) {
                riskLevels.add(riskLevel);
            }
        }
        
        // Read exchanges set
        if (in.readInt() == 1) {
            List<String> exchangesList = new ArrayList<>();
            in.readStringList(exchangesList);
            exchanges = new HashSet<>(exchangesList);
        } else {
            exchanges = getExchanges(); // Use the helper method to generate from source/dest
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(minProfitPercentage);
        dest.writeDouble(maxProfitPercentage);
        dest.writeDouble(maxSlippagePercentage);
        dest.writeDouble(minExecutionTime);
        dest.writeDouble(maxExecutionTime);
        dest.writeString(riskLevel);
        
        // Write source exchanges (handle null case)
        dest.writeInt(sourceExchanges != null ? 1 : 0);
        if (sourceExchanges != null) {
            dest.writeStringList(sourceExchanges);
        }
        
        // Write destination exchanges (handle null case)
        dest.writeInt(destinationExchanges != null ? 1 : 0);
        if (destinationExchanges != null) {
            dest.writeStringList(destinationExchanges);
        }
        
        // Write risk levels set
        dest.writeInt(riskLevels != null && !riskLevels.isEmpty() ? 1 : 0);
        if (riskLevels != null && !riskLevels.isEmpty()) {
            dest.writeStringList(new ArrayList<>(riskLevels));
        }
        
        // Write exchanges set
        dest.writeInt(exchanges != null && !exchanges.isEmpty() ? 1 : 0);
        if (exchanges != null && !exchanges.isEmpty()) {
            dest.writeStringList(new ArrayList<>(exchanges));
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FilterCriteria> CREATOR = new Creator<FilterCriteria>() {
        @Override
        public FilterCriteria createFromParcel(Parcel in) {
            return new FilterCriteria(in);
        }

        @Override
        public FilterCriteria[] newArray(int size) {
            return new FilterCriteria[size];
        }
    };

    /**
     * Checks if any filters are active (non-default values)
     */
    public boolean hasActiveFilters() {
        return minProfitPercentage > 0
                || maxProfitPercentage < 50.0
                || maxSlippagePercentage != 2.0
                || minExecutionTime > 0
                || maxExecutionTime < 300.0
                || (sourceExchanges != null && !sourceExchanges.isEmpty())
                || (destinationExchanges != null && !destinationExchanges.isEmpty())
                || !"Medium".equals(riskLevel);
    }

    /**
     * Returns a short summary of active filters for display
     */
    public String getFilterSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (minProfitPercentage > 0 || maxProfitPercentage < 50.0) {
            summary.append(String.format("Profit: %.1f%%-%s", 
                           minProfitPercentage, 
                           maxProfitPercentage >= 50.0 ? "∞" : String.format("%.1f%%", maxProfitPercentage)));
        }
        
        if (riskLevel != null && !"Medium".equals(riskLevel)) {
            if (summary.length() > 0) summary.append(", ");
            summary.append("Risk: ").append(riskLevel);
        }
        
        if (summary.length() == 0) {
            return "Filters active";
        }
        
        return summary.toString();
    }

    @Override
    public String toString() {
        return "FilterCriteria{" +
                "minProfit=" + minProfitPercentage +
                "%, maxProfit=" + maxProfitPercentage +
                "%, maxSlippage=" + maxSlippagePercentage +
                "%, execTime=" + minExecutionTime + "-" + maxExecutionTime +
                "s, risk=" + riskLevel +
                "}";
    }

    // Getters and setters
    public double getMinProfitPercentage() {
        return minProfitPercentage;
    }

    public void setMinProfitPercentage(double minProfitPercentage) {
        this.minProfitPercentage = minProfitPercentage;
    }

    public double getMaxProfitPercentage() {
        return maxProfitPercentage;
    }

    public void setMaxProfitPercentage(double maxProfitPercentage) {
        this.maxProfitPercentage = maxProfitPercentage;
    }

    public double getMaxSlippagePercentage() {
        return maxSlippagePercentage;
    }

    public void setMaxSlippagePercentage(double maxSlippagePercentage) {
        this.maxSlippagePercentage = maxSlippagePercentage;
    }

    public double getMinExecutionTime() {
        return minExecutionTime;
    }

    public void setMinExecutionTime(double minExecutionTime) {
        this.minExecutionTime = minExecutionTime;
    }

    public double getMaxExecutionTime() {
        return maxExecutionTime;
    }

    public void setMaxExecutionTime(double maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<String> getSourceExchanges() {
        return sourceExchanges;
    }

    public void setSourceExchanges(List<String> sourceExchanges) {
        this.sourceExchanges = sourceExchanges;
    }

    public List<String> getDestinationExchanges() {
        return destinationExchanges;
    }

    public void setDestinationExchanges(List<String> destinationExchanges) {
        this.destinationExchanges = destinationExchanges;
    }

    // Compatibility methods for FilterDialogFragment
    
    /**
     * @return The minimum profit percentage as used by FilterDialogFragment
     */
    public double getMinProfitPercent() {
        return minProfitPercentage;
    }
    
    /**
     * @param percent The minimum profit percentage to set
     */
    public void setMinProfitPercent(float percent) {
        this.minProfitPercentage = percent;
    }
    
    /**
     * @return The maximum profit percentage as used by FilterDialogFragment
     */
    public double getMaxProfitPercent() {
        return maxProfitPercentage;
    }
    
    /**
     * @param percent The maximum profit percentage to set
     */
    public void setMaxProfitPercent(float percent) {
        this.maxProfitPercentage = percent;
    }
    
    /**
     * @return The set of risk levels selected
     */
    public Set<String> getRiskLevels() {
        // If we have a single risk level, convert to a set
        if (riskLevels.isEmpty() && riskLevel != null) {
            riskLevels.add(riskLevel);
        }
        return riskLevels;
    }
    
    /**
     * @param riskLevels The set of risk levels to apply
     */
    public void setRiskLevels(Set<String> riskLevels) {
        this.riskLevels = riskLevels;
        
        // Also update the single risk level for backward compatibility
        if (riskLevels != null && !riskLevels.isEmpty()) {
            this.riskLevel = riskLevels.iterator().next();
        }
    }
    
    /**
     * @return The set of exchanges selected
     */
    public Set<String> getExchanges() {
        // Combine source and destination exchanges
        Set<String> allExchanges = new HashSet<>();
        if (sourceExchanges != null) {
            allExchanges.addAll(sourceExchanges);
        }
        if (destinationExchanges != null) {
            allExchanges.addAll(destinationExchanges);
        }
        if (allExchanges.isEmpty() && !exchanges.isEmpty()) {
            return exchanges;
        }
        return allExchanges;
    }
    
    /**
     * @param exchanges The set of exchanges to apply
     */
    public void setExchanges(Set<String> exchanges) {
        this.exchanges = exchanges;
        
        // For backward compatibility, set both source and destination
        if (exchanges != null && !exchanges.isEmpty()) {
            sourceExchanges = new ArrayList<>(exchanges);
            destinationExchanges = new ArrayList<>(exchanges);
        }
    }
}
