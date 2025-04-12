package com.example.tradient.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced risk assessment model that includes detailed risk metrics
 */
public class RiskAssessment implements Parcelable {

    // Risk components (0-1 scale, higher = better/less risky)
    private double overallRiskScore;    // Overall risk score (aggregate of all factors)
    private double liquidityRiskScore;  // Market liquidity score
    private double volatilityRiskScore; // Price volatility score (higher = less volatile)
    private double exchangeRiskScore;   // Exchange reliability score
    private double transactionRiskScore; // Transaction risk score
    
    // Specific metrics
    private double slippageEstimate;     // Estimated price slippage (e.g., 0.005 = 0.5%)
    private double executionTimeEstimate; // Estimated trade execution time in minutes
    private double roiEfficiency;         // ROI per hour estimate
    private double optimalTradeSize;      // Optimal trade size in USD
    
    // Risk level categories
    public static final String RISK_LEVEL_LOW = "LOW";
    public static final String RISK_LEVEL_MEDIUM = "MEDIUM";
    public static final String RISK_LEVEL_HIGH = "HIGH";
    public static final String RISK_LEVEL_EXTREME = "EXTREME";
    public static final String RISK_LEVEL_UNKNOWN = "UNKNOWN";
    
    // Fields for backward compatibility
    private double liquidityScore;
    private double volatilityScore;
    private double feeImpact;
    private double marketDepthScore;
    private double executionSpeedRisk;
    private double slippageRisk;
    private double marketRegimeScore;
    private double sentimentScore;
    private double anomalyScore;
    private double correlationScore;
    private String riskLevel;
    private boolean earlyWarningTriggered;
    private Date assessmentTime;
    private double depthScore;
    private double volume;
    private double orderBookDepth;
    private double priceVolatility;
    private double totalSlippagePercentage;
    private String exchangeBuy;
    private String exchangeSell;
    private double buyFeePercentage;
    private double sellFeePercentage;

    // Additional risk factors in a map for extensibility
    private Map<String, Double> riskFactors = new HashMap<>();

    // Required empty constructor for Parcelable 
    public RiskAssessment() {
        this.overallRiskScore = 0.5;
        this.liquidityRiskScore = 0.5;
        this.volatilityRiskScore = 0.5;
        this.exchangeRiskScore = 0.5;
        this.transactionRiskScore = 0.5;
        this.slippageEstimate = 0.005;
        this.executionTimeEstimate = 5.0;
        this.roiEfficiency = 0.002;
        this.optimalTradeSize = 1000.0;
        this.assessmentTime = new Date();
    }
    
    /**
     * Full constructor for RiskAssessment
     */
    public RiskAssessment(double overallRiskScore, double liquidityRiskScore, 
                           double volatilityRiskScore, double exchangeRiskScore,
                           double transactionRiskScore, double slippageEstimate,
                           double executionTimeEstimate, double roiEfficiency,
                           double optimalTradeSize) {
        this.overallRiskScore = overallRiskScore;
        this.liquidityRiskScore = liquidityRiskScore;
        this.volatilityRiskScore = volatilityRiskScore;
        this.exchangeRiskScore = exchangeRiskScore;
        this.transactionRiskScore = transactionRiskScore;
        this.slippageEstimate = slippageEstimate;
        this.executionTimeEstimate = executionTimeEstimate;
        this.roiEfficiency = roiEfficiency;
        this.optimalTradeSize = optimalTradeSize;
        
        // Set compatibility fields
        this.liquidityScore = liquidityRiskScore;
        this.volatilityScore = volatilityRiskScore;
        this.slippageRisk = 1.0 - slippageEstimate; // Invert for compatibility
        this.assessmentTime = new Date();
    }
    
    // Overall risk score (higher is safer)
    public double getRiskScore() {
        return overallRiskScore;
    }
    
    public void setRiskScore(double riskScore) {
        this.overallRiskScore = riskScore;
    }
    
    // Backwards compatibility for older code
    public double getOverallRiskScore() {
        return overallRiskScore;
    }
    
    public void setOverallRiskScore(double overallRiskScore) {
        this.overallRiskScore = overallRiskScore;
    }
    
    // Liquidity risk score
    public double getLiquidityRiskScore() {
        return liquidityRiskScore;
    }
    
    public void setLiquidityRiskScore(double liquidityRiskScore) {
        this.liquidityRiskScore = liquidityRiskScore;
        this.liquidityScore = liquidityRiskScore; // For compatibility
    }
    
    // Volatility risk score
    public double getVolatilityRiskScore() {
        return volatilityRiskScore;
    }
    
    public void setVolatilityRiskScore(double volatilityRiskScore) {
        this.volatilityRiskScore = volatilityRiskScore;
        this.volatilityScore = volatilityRiskScore; // For compatibility
    }
    
    // Exchange risk score
    public double getExchangeRiskScore() {
        return exchangeRiskScore;
    }
    
    public void setExchangeRiskScore(double exchangeRiskScore) {
        this.exchangeRiskScore = exchangeRiskScore;
    }
    
    // Transaction risk score
    public double getTransactionRiskScore() {
        return transactionRiskScore;
    }
    
    public void setTransactionRiskScore(double transactionRiskScore) {
        this.transactionRiskScore = transactionRiskScore;
    }
    
    // Slippage estimate
    public double getSlippageEstimate() {
        return slippageEstimate;
    }
    
    public void setSlippageEstimate(double slippageEstimate) {
        this.slippageEstimate = slippageEstimate;
        this.totalSlippagePercentage = slippageEstimate; // For compatibility
        this.slippageRisk = 1.0 - slippageEstimate; // Invert for compatibility
    }
    
    // Execution time estimate in minutes
    public double getExecutionTimeEstimate() {
        return executionTimeEstimate;
    }
    
    public void setExecutionTimeEstimate(double executionTimeEstimate) {
        this.executionTimeEstimate = executionTimeEstimate;
    }
    
    // ROI efficiency (profit per hour)
    public double getRoiEfficiency() {
        return roiEfficiency;
    }
    
    public void setRoiEfficiency(double roiEfficiency) {
        this.roiEfficiency = roiEfficiency;
    }
    
    // Optimal trade size
    public double getOptimalTradeSize() {
        return optimalTradeSize;
    }
    
    public void setOptimalTradeSize(double optimalTradeSize) {
        this.optimalTradeSize = optimalTradeSize;
    }
    
    // Backwards compatibility methods (Legacy API)
    
    public double getLiquidityScore() {
        return liquidityScore;
    }
    
    public void setLiquidityScore(double liquidityScore) {
        this.liquidityScore = liquidityScore;
        this.liquidityRiskScore = liquidityScore; // Update new field
    }
    
    public double getVolatilityScore() {
        return volatilityScore;
    }
    
    public void setVolatilityScore(double volatilityScore) {
        this.volatilityScore = volatilityScore;
        this.volatilityRiskScore = volatilityScore; // Update new field
    }
    
    public double getFeeImpact() {
        return feeImpact;
    }
    
    public void setFeeImpact(double feeImpact) {
        this.feeImpact = feeImpact;
    }
    
    public double getMarketDepthScore() {
        return marketDepthScore;
    }
    
    public void setMarketDepthScore(double marketDepthScore) {
        this.marketDepthScore = marketDepthScore;
        this.depthScore = marketDepthScore; // Update companion field
    }
    
    public double getExecutionSpeedRisk() {
        return executionSpeedRisk;
    }
    
    public void setExecutionSpeedRisk(double executionSpeedRisk) {
        this.executionSpeedRisk = executionSpeedRisk;
    }
    
    public double getSlippageRisk() {
        return slippageRisk;
    }
    
    public void setSlippageRisk(double slippageRisk) {
        this.slippageRisk = slippageRisk;
        this.slippageEstimate = 1.0 - slippageRisk; // Invert for new field
    }
    
    public double getMarketRegimeScore() {
        return marketRegimeScore;
    }
    
    public void setMarketRegimeScore(double marketRegimeScore) {
        this.marketRegimeScore = marketRegimeScore;
    }
    
    public double getSentimentScore() {
        return sentimentScore;
    }
    
    public void setSentimentScore(double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }
    
    public double getAnomalyScore() {
        return anomalyScore;
    }
    
    public void setAnomalyScore(double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }
    
    public double getCorrelationScore() {
        return correlationScore;
    }
    
    public void setCorrelationScore(double correlationScore) {
        this.correlationScore = correlationScore;
    }
    
    public boolean isEarlyWarningTriggered() {
        return earlyWarningTriggered;
    }
    
    public void setEarlyWarningTriggered(boolean earlyWarningTriggered) {
        this.earlyWarningTriggered = earlyWarningTriggered;
    }
    
    public String getRiskLevel() {
        if (overallRiskScore >= 0.75) {
            return RISK_LEVEL_LOW;
        } else if (overallRiskScore >= 0.5) {
            return RISK_LEVEL_MEDIUM;
        } else if (overallRiskScore >= 0.25) {
            return RISK_LEVEL_HIGH;
        } else if (overallRiskScore >= 0) {
            return RISK_LEVEL_EXTREME;
        } else {
            return RISK_LEVEL_UNKNOWN;
        }
    }
    
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    public Date getAssessmentTime() {
        return assessmentTime;
    }
    
    public void setAssessmentTime(Date assessmentTime) {
        this.assessmentTime = assessmentTime;
    }
    
    public double getDepthScore() {
        return depthScore != 0 ? depthScore : marketDepthScore;
    }
    
    public void setDepthScore(double depthScore) {
        this.depthScore = depthScore;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getOrderBookDepth() {
        return orderBookDepth;
    }

    public void setOrderBookDepth(double orderBookDepth) {
        this.orderBookDepth = orderBookDepth;
    }

    public double getPriceVolatility() {
        return priceVolatility;
    }

    public void setPriceVolatility(double priceVolatility) {
        this.priceVolatility = priceVolatility;
    }

    public double getTotalSlippagePercentage() {
        return totalSlippagePercentage;
    }

    public void setTotalSlippagePercentage(double totalSlippagePercentage) {
        this.totalSlippagePercentage = totalSlippagePercentage;
        this.slippageEstimate = totalSlippagePercentage; // Update new field
    }

    public String getExchangeBuy() {
        return exchangeBuy;
    }

    public void setExchangeBuy(String exchangeBuy) {
        this.exchangeBuy = exchangeBuy;
    }

    public String getExchangeSell() {
        return exchangeSell;
    }

    public void setExchangeSell(String exchangeSell) {
        this.exchangeSell = exchangeSell;
    }

    public double getBuyFeePercentage() {
        return buyFeePercentage;
    }

    public void setBuyFeePercentage(double buyFeePercentage) {
        this.buyFeePercentage = buyFeePercentage;
    }

    public double getSellFeePercentage() {
        return sellFeePercentage;
    }

    public void setSellFeePercentage(double sellFeePercentage) {
        this.sellFeePercentage = sellFeePercentage;
    }

    // Add a risk factor to the map
    public void addRiskFactor(String name, double value) {
        riskFactors.put(name, value);
    }
    
    // Get a risk factor from the map
    public double getRiskFactor(String name) {
        return riskFactors.getOrDefault(name, 0.0);
    }
    
    // Get all risk factors
    public Map<String, Double> getAllRiskFactors() {
        return riskFactors;
    }
    
    // Parcelable implementation
    protected RiskAssessment(Parcel in) {
        overallRiskScore = in.readDouble();
        liquidityRiskScore = in.readDouble();
        volatilityRiskScore = in.readDouble();
        exchangeRiskScore = in.readDouble();
        transactionRiskScore = in.readDouble();
        slippageEstimate = in.readDouble();
        executionTimeEstimate = in.readDouble();
        roiEfficiency = in.readDouble();
        optimalTradeSize = in.readDouble();
        
        // Read compatibility fields
        liquidityScore = in.readDouble();
        volatilityScore = in.readDouble();
        feeImpact = in.readDouble();
        marketDepthScore = in.readDouble();
        executionSpeedRisk = in.readDouble();
        slippageRisk = in.readDouble();
        marketRegimeScore = in.readDouble();
        sentimentScore = in.readDouble();
        anomalyScore = in.readDouble();
        correlationScore = in.readDouble();
        riskLevel = in.readString();
        earlyWarningTriggered = in.readByte() != 0;
        long tmpTime = in.readLong();
        assessmentTime = tmpTime == -1 ? null : new Date(tmpTime);
        depthScore = in.readDouble();
        volume = in.readDouble();
        orderBookDepth = in.readDouble();
        priceVolatility = in.readDouble();
        totalSlippagePercentage = in.readDouble();
        exchangeBuy = in.readString();
        exchangeSell = in.readString();
        buyFeePercentage = in.readDouble();
        sellFeePercentage = in.readDouble();
        
        // Read risk factors map
        int mapSize = in.readInt();
        for (int i = 0; i < mapSize; i++) {
            String key = in.readString();
            double value = in.readDouble();
            riskFactors.put(key, value);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(overallRiskScore);
        dest.writeDouble(liquidityRiskScore);
        dest.writeDouble(volatilityRiskScore);
        dest.writeDouble(exchangeRiskScore);
        dest.writeDouble(transactionRiskScore);
        dest.writeDouble(slippageEstimate);
        dest.writeDouble(executionTimeEstimate);
        dest.writeDouble(roiEfficiency);
        dest.writeDouble(optimalTradeSize);
        
        // Write compatibility fields
        dest.writeDouble(liquidityScore);
        dest.writeDouble(volatilityScore);
        dest.writeDouble(feeImpact);
        dest.writeDouble(marketDepthScore);
        dest.writeDouble(executionSpeedRisk);
        dest.writeDouble(slippageRisk);
        dest.writeDouble(marketRegimeScore);
        dest.writeDouble(sentimentScore);
        dest.writeDouble(anomalyScore);
        dest.writeDouble(correlationScore);
        dest.writeString(riskLevel);
        dest.writeByte((byte) (earlyWarningTriggered ? 1 : 0));
        dest.writeLong(assessmentTime != null ? assessmentTime.getTime() : -1);
        dest.writeDouble(depthScore);
        dest.writeDouble(volume);
        dest.writeDouble(orderBookDepth);
        dest.writeDouble(priceVolatility);
        dest.writeDouble(totalSlippagePercentage);
        dest.writeString(exchangeBuy);
        dest.writeString(exchangeSell);
        dest.writeDouble(buyFeePercentage);
        dest.writeDouble(sellFeePercentage);
        
        // Write risk factors map
        dest.writeInt(riskFactors.size());
        for (Map.Entry<String, Double> entry : riskFactors.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeDouble(entry.getValue());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RiskAssessment> CREATOR = new Creator<RiskAssessment>() {
        @Override
        public RiskAssessment createFromParcel(Parcel in) {
            return new RiskAssessment(in);
        }

        @Override
        public RiskAssessment[] newArray(int size) {
            return new RiskAssessment[size];
        }
    };

    /**
     * Get a normalized (0-100) risk score for progress bars and visualizations
     * @return Integer risk score from 0-100
     */
    public int getNormalizedRiskScore() {
        return (int) Math.round(overallRiskScore * 100);
    }
    
    /**
     * Get human-readable execution time
     * @return Formatted time string (e.g., "2.5 min" or "30 sec")
     */
    public String getFormattedExecutionTime() {
        if (executionTimeEstimate < 1.0) {
            // Convert to seconds for times less than 1 minute
            int seconds = (int) Math.round(executionTimeEstimate * 60);
            return seconds + " sec";
        } else {
            // Keep as minutes for times 1 minute or longer
            return String.format("%.1f min", executionTimeEstimate);
        }
    }
    
    /**
     * Check if this risk assessment has valid data
     * @return true if data is valid
     */
    public boolean isValid() {
        return overallRiskScore >= 0 && overallRiskScore <= 1;
    }
}
