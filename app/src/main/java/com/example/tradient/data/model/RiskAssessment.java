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
        
        // Initialize backward compatibility fields
        this.liquidityScore = 0.5;
        this.volatilityScore = 0.5;
        this.feeImpact = 0.01;         // Default 1% fee impact
        this.marketDepthScore = 0.5;
        this.executionSpeedRisk = 0.5;
        this.slippageRisk = 0.01;      // Default 1% slippage risk
        this.buyFeePercentage = 0.001;   // Default 0.1% buy fee
        this.sellFeePercentage = 0.001;  // Default 0.1% sell fee
        
        // Set default risk level
        this.riskLevel = RISK_LEVEL_MEDIUM;
        
        // Initialize risk factors map
        this.riskFactors = new HashMap<>();
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
    
    /**
     * Add a string risk factor to the map
     * Converts the string to a hashcode-based double value for storage
     * 
     * @param name The risk factor name
     * @param value The string value to store
     */
    public void addRiskFactorString(String name, String value) {
        if (value == null) {
            riskFactors.put(name, 0.0);
            return;
        }
        // Store the string in a separate map or convert to a numeric representation
        // Here we'll use a simple approach - store a value signifying it's a string
        // In a real implementation, you might want to store strings separately
        riskFactors.put(name + "_isString", 1.0);
        riskFactors.put(name, (double)value.hashCode());
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
        
        // Add safety check for execution time
        if (executionTimeEstimate <= 0) {
            executionTimeEstimate = 3.0; // Default to 3 minutes if no value was set
        }
        
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
        // Safety check for zero or negative values
        if (executionTimeEstimate <= 0) {
            return "3.0 min"; // Default value if no estimate is available
        }
        
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
     * Check if this risk assessment has valid values
     * @return true if the assessment has valid values, false otherwise
     */
    public boolean isValid() {
        // Check core risk metrics
        if (Double.isNaN(overallRiskScore) || overallRiskScore < 0 || overallRiskScore > 1) {
            return false;
        }
        
        if (Double.isNaN(liquidityRiskScore) || liquidityRiskScore < 0 || liquidityRiskScore > 1) {
            return false;
        }
        
        if (Double.isNaN(volatilityRiskScore) || volatilityRiskScore < 0 || volatilityRiskScore > 1) {
            return false;
        }
        
        // Check slippage
        if (Double.isNaN(slippageEstimate) || slippageEstimate < 0) {
            return false;
        }
        
        // Check execution time - this is especially important for the detail view
        if (Double.isNaN(executionTimeEstimate) || executionTimeEstimate <= 0) {
            return false;
        }
        
        // All checks passed
        return true;
    }

    /**
     * Create a risk assessment with specified values for the most important metrics.
     * 
     * @param overallRisk Overall risk score (0-1, higher is better/less risky)
     * @param liquidity Liquidity score (0-1, higher is better)
     * @param volatility Volatility score (0-1, higher is better)
     * @param slippage Slippage estimate as a percentage (e.g., 0.005 for 0.5%)
     * @return A configured risk assessment
     */
    public static RiskAssessment createWithValues(
            double overallRisk,
            double liquidity, 
            double volatility,
            double slippage) {
        
        RiskAssessment assessment = new RiskAssessment();
        assessment.setOverallRiskScore(overallRisk);
        assessment.setLiquidityScore(liquidity);
        assessment.setVolatilityScore(volatility);
        assessment.setSlippageEstimate(slippage);
        assessment.setAssessmentTime(new Date());
        
        return assessment;
    }
    
    /**
     * Creates a risk assessment in an error state
     * This can be used when risk calculation fails due to an exception
     * 
     * @param ex The exception that caused the error (can be null)
     * @return A risk assessment configured to represent an error state
     */
    public static RiskAssessment createErrorState(Throwable ex) {
        RiskAssessment assessment = new RiskAssessment();
        // Set values indicating an error state
        assessment.setOverallRiskScore(0.0);  // Highest risk
        assessment.setLiquidityScore(0.0);
        assessment.setVolatilityScore(0.0);
        assessment.setSlippageEstimate(0.1);  // High slippage (10%)
        assessment.setExecutionTimeEstimate(10.0); // Long execution time
        assessment.setRiskLevel(RISK_LEVEL_EXTREME);
        
        // Store the error message in risk factors if exception provided
        if (ex != null) {
            assessment.addRiskFactorString("error_message", ex.getMessage());
            assessment.addRiskFactorString("error_type", ex.getClass().getSimpleName());
        }
        
        return assessment;
    }
    
    /**
     * Creates a risk assessment in an unknown state
     * This can be used when risk calculation returns null or invalid data
     * 
     * @return A risk assessment configured to represent an unknown state
     */
    public static RiskAssessment createUnknownState() {
        RiskAssessment assessment = new RiskAssessment();
        // Set neutral/unknown values
        assessment.setOverallRiskScore(0.25);  // Leaning toward higher risk
        assessment.setLiquidityScore(0.25);
        assessment.setVolatilityScore(0.25);
        assessment.setSlippageEstimate(0.05);  // 5% slippage
        assessment.setExecutionTimeEstimate(5.0); // Moderate execution time
        assessment.setRiskLevel(RISK_LEVEL_UNKNOWN);
        
        return assessment;
    }
    
    /**
     * Creates a risk assessment for suspiciously high profit opportunities
     * This can be used when profit percentage is unnaturally high (likely too good to be true)
     * 
     * @param profitPercent The profit percentage that triggered the suspicion
     * @return A risk assessment configured to represent a high-risk state due to suspiciously high profit
     */
    public static RiskAssessment createSuspiciouslyHighProfitState(double profitPercent) {
        RiskAssessment assessment = new RiskAssessment();
        // Set values indicating a suspicious profit state (high risk)
        
        // Calculate a risk score inversely proportional to the profit
        // Higher profits = lower risk score (higher risk)
        double riskScore = Math.max(0.1, 0.4 - (profitPercent - 2.0) / 10.0);
        
        assessment.setOverallRiskScore(riskScore);  // Lower score = higher risk
        assessment.setLiquidityScore(0.3);  // Assume low liquidity
        assessment.setVolatilityScore(0.2);  // Assume high volatility
        assessment.setSlippageEstimate(0.05);  // Assume high slippage (5%)
        assessment.setExecutionTimeEstimate(8.0);  // Assume long execution time
        assessment.setRiskLevel(RISK_LEVEL_HIGH);
        
        // Add risk factors for explanation
        assessment.addRiskFactor("suspiciously_high_profit", profitPercent);
        assessment.addRiskFactor("too_good_to_be_true_factor", 1.0);
        
        return assessment;
    }
    
    /**
     * Get a risk level description based on the overall risk score.
     * This provides a consistent text representation of the risk level.
     *
     * @return A descriptive risk level
     */
    public String getRiskLevelDescription() {
        double score = getOverallRiskScore();
        
        if (score >= 0.8) {
            return "Low Risk";
        } else if (score >= 0.6) {
            return "Medium-Low Risk";
        } else if (score >= 0.4) {
            return "Medium Risk";
        } else if (score >= 0.2) {
            return "Medium-High Risk";
        } else {
            return "High Risk";
        }
    }
    
    /**
     * Verify if this risk assessment has all required values properly set.
     * Helps detect incomplete risk assessments.
     *
     * @return True if this is a complete, valid risk assessment
     */
    public boolean isComplete() {
        // Check that all primary metrics are set to non-default values
        return assessmentTime != null && 
               liquidityScore != 0.0 &&
               volatilityScore != 0.0 &&
               slippageRisk != 0.0 &&
               (buyFeePercentage > 0.0 || sellFeePercentage > 0.0);
    }
}
