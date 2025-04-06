package com.example.tradient.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced RiskAssessment model that captures comprehensive risk metrics
 * for arbitrage opportunities, including early warning indicators,
 * market regime information, and predictive analytics data.
 */
public class RiskAssessment implements Parcelable {

    // Basic risk metrics
    private double liquidityScore;
    private double volatilityScore;
    private double feeImpact;
    private double overallRiskScore;
    
    // Enhanced risk metrics
    private double marketDepthScore;
    private double executionSpeedRisk;
    private double slippageRisk;
    private double marketRegimeScore;
    private double sentimentScore;
    private double anomalyScore;
    private double correlationScore;
    
    // Early warning indicators
    private boolean earlyWarningTriggered;
    private Map<String, Double> warningThresholds;
    private Map<String, Double> warningIndicators;
    
    // Predictive analytics
    private double predictedRiskScore;
    private double predictionConfidence;
    
    // Timestamp for risk assessment
    private Date assessmentTime;
    
    // Additional field for depth score (separate from marketDepthScore for backward compatibility)
    private double depthScore;
    
    /**
     * Default constructor
     */
    public RiskAssessment() {
        // Initialize default values
        this.liquidityScore = 0.5;
        this.volatilityScore = 0.5;
        this.feeImpact = 0.5;
        this.marketDepthScore = 0.5;
        this.executionSpeedRisk = 0.5;
        this.slippageRisk = 0.5;
        this.marketRegimeScore = 0.5;
        this.sentimentScore = 0.5;
        this.anomalyScore = 0.5;
        this.correlationScore = 0.5;
        this.overallRiskScore = 0.5;
        this.assessmentTime = new Date();
        this.warningThresholds = new HashMap<>();
        this.warningIndicators = new HashMap<>();
    }
    
    /**
     * Constructor for common risk factors
     */
    public RiskAssessment(double liquidityScore, double volatilityScore, double feeImpact, double marketDepthScore) {
        this();
        this.liquidityScore = liquidityScore;
        this.volatilityScore = volatilityScore;
        this.feeImpact = feeImpact;
        this.marketDepthScore = marketDepthScore;
        this.executionSpeedRisk = 0.5;
        this.slippageRisk = 0.5;
        this.marketRegimeScore = 0.5;
        this.sentimentScore = 0.5;
        this.anomalyScore = 0.5;
        this.correlationScore = 0.5;
        this.overallRiskScore = 0.5;
    }
    
    /**
     * Constructor with all risk factors
     */
    public RiskAssessment(double liquidityScore, double volatilityScore, double feeImpact,
                           double marketDepthScore, double executionSpeedRisk, double slippageRisk,
                           double marketRegimeScore, double sentimentScore, double anomalyScore,
                           double correlationScore, double overallRiskScore) {
        this();
        this.liquidityScore = liquidityScore;
        this.volatilityScore = volatilityScore;
        this.feeImpact = feeImpact;
        this.marketDepthScore = marketDepthScore;
        this.executionSpeedRisk = executionSpeedRisk;
        this.slippageRisk = slippageRisk;
        this.marketRegimeScore = marketRegimeScore;
        this.sentimentScore = sentimentScore;
        this.anomalyScore = anomalyScore;
        this.correlationScore = correlationScore;
        this.overallRiskScore = overallRiskScore;
    }
    
    /**
     * Sets an early warning indicator and checks if it exceeds the threshold
     * 
     * @param indicatorName The name of the warning indicator
     * @param value The current value of the indicator
     * @param threshold The threshold value that triggers a warning
     * @return true if the warning threshold is exceeded
     */
    public boolean setWarningIndicator(String indicatorName, double value, double threshold) {
        warningIndicators.put(indicatorName, value);
        warningThresholds.put(indicatorName, threshold);
        
        boolean thresholdExceeded = value > threshold;
        if (thresholdExceeded) {
            earlyWarningTriggered = true;
        }
        
        return thresholdExceeded;
    }
    
    /**
     * Sets predictive risk analytics data
     * 
     * @param predictedRisk The predicted risk score
     * @param confidence The confidence level of the prediction (0-1)
     */
    public void setPredictiveAnalytics(double predictedRisk, double confidence) {
        this.predictedRiskScore = predictedRisk;
        this.predictionConfidence = confidence;
    }
    
    /**
     * Adds a severe warning to this risk assessment.
     * 
     * @param warningMessage The warning message
     */
    public void addSevereWarning(String warningMessage) {
        // This is a simple implementation - in a production system,
        // you might want to maintain a list of warnings with severity levels
        this.setWarningIndicator("severeWarning", 1.0, 0.5);
        
        // If you need to store the actual message, you could add a map of warning messages
        // For now, we'll just use the warning indicator system
    }
    
    /**
     * Sets the slippage risk score for this assessment.
     * 
     * @param slippageRisk The new slippage risk score
     */
    public void setSlippageRisk(double slippageRisk) {
        this.slippageRisk = slippageRisk;
    }
    
    /**
     * Sets the liquidity score
     * @param liquidityScore The liquidity score
     */
    public void setLiquidityScore(double liquidityScore) {
        this.liquidityScore = liquidityScore;
    }
    
    /**
     * Sets the volatility score
     * @param volatilityScore The volatility score
     */
    public void setVolatilityScore(double volatilityScore) {
        this.volatilityScore = volatilityScore;
    }
    
    /**
     * Sets the fee impact
     * @param feeImpact The fee impact
     */
    public void setFeeImpact(double feeImpact) {
        this.feeImpact = feeImpact;
    }
    
    /**
     * Sets the market depth score
     * @param marketDepthScore The market depth score
     */
    public void setMarketDepthScore(double marketDepthScore) {
        this.marketDepthScore = marketDepthScore;
    }
    
    /**
     * Sets the execution speed risk
     * @param executionSpeedRisk The execution speed risk
     */
    public void setExecutionSpeedRisk(double executionSpeedRisk) {
        this.executionSpeedRisk = executionSpeedRisk;
    }
    
    // Getters and setters
    
    public double getLiquidityScore() {
        return liquidityScore;
    }
    
    public double getVolatilityScore() {
        return volatilityScore;
    }
    
    public double getFeeImpact() {
        return feeImpact;
    }
    
    public double getOverallRiskScore() {
        return overallRiskScore;
    }
    
    public double getMarketDepthScore() {
        return marketDepthScore;
    }
    
    public double getExecutionSpeedRisk() {
        return executionSpeedRisk;
    }
    
    public double getSlippageRisk() {
        return slippageRisk;
    }
    
    public double getMarketRegimeScore() {
        return marketRegimeScore;
    }
    
    public double getSentimentScore() {
        return sentimentScore;
    }
    
    public double getAnomalyScore() {
        return anomalyScore;
    }
    
    public double getCorrelationScore() {
        return correlationScore;
    }
    
    public boolean isEarlyWarningTriggered() {
        return earlyWarningTriggered;
    }
    
    public Map<String, Double> getWarningIndicators() {
        return warningIndicators;
    }
    
    public Map<String, Double> getWarningThresholds() {
        return warningThresholds;
    }
    
    public double getPredictedRiskScore() {
        return predictedRiskScore;
    }
    
    public double getPredictionConfidence() {
        return predictionConfidence;
    }
    
    public Date getAssessmentTime() {
        return assessmentTime;
    }
    
    public void setOverallRiskScore(double overallRiskScore) {
        this.overallRiskScore = overallRiskScore;
    }
    
    /**
     * Gets the depth score, which evaluates the order book depth
     * @return The depth score (0-1, higher is better)
     */
    public double getDepthScore() {
        return depthScore != 0 ? depthScore : marketDepthScore;
    }
    
    /**
     * Sets the depth score
     * @param depthScore The depth score value
     */
    public void setDepthScore(double depthScore) {
        this.depthScore = depthScore;
    }
    
    /**
     * Gets the execution risk (compatibility alias for executionSpeedRisk)
     * @return The execution risk score
     */
    public double getExecutionRisk() {
        return executionSpeedRisk;
    }
    
    /**
     * Sets the execution risk (compatibility alias for executionSpeedRisk)
     * @param executionRisk The execution risk value
     */
    public void setExecutionRisk(double executionRisk) {
        this.executionSpeedRisk = executionRisk;
    }

    protected RiskAssessment(Parcel in) {
        liquidityScore = in.readDouble();
        volatilityScore = in.readDouble();
        feeImpact = in.readDouble();
        overallRiskScore = in.readDouble();
        marketDepthScore = in.readDouble();
        executionSpeedRisk = in.readDouble();
        slippageRisk = in.readDouble();
        marketRegimeScore = in.readDouble();
        sentimentScore = in.readDouble();
        anomalyScore = in.readDouble();
        correlationScore = in.readDouble();
        earlyWarningTriggered = in.readByte() != 0;
        predictedRiskScore = in.readDouble();
        predictionConfidence = in.readDouble();
        long tmpTimestamp = in.readLong();
        assessmentTime = tmpTimestamp == -1 ? null : new Date(tmpTimestamp);
        depthScore = in.readDouble();
        
        // Read warning thresholds and indicators
        int thresholdSize = in.readInt();
        warningThresholds = new HashMap<>(thresholdSize);
        for (int i = 0; i < thresholdSize; i++) {
            String key = in.readString();
            double value = in.readDouble();
            warningThresholds.put(key, value);
        }
        
        int indicatorSize = in.readInt();
        warningIndicators = new HashMap<>(indicatorSize);
        for (int i = 0; i < indicatorSize; i++) {
            String key = in.readString();
            double value = in.readDouble();
            warningIndicators.put(key, value);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(liquidityScore);
        dest.writeDouble(volatilityScore);
        dest.writeDouble(feeImpact);
        dest.writeDouble(overallRiskScore);
        dest.writeDouble(marketDepthScore);
        dest.writeDouble(executionSpeedRisk);
        dest.writeDouble(slippageRisk);
        dest.writeDouble(marketRegimeScore);
        dest.writeDouble(sentimentScore);
        dest.writeDouble(anomalyScore);
        dest.writeDouble(correlationScore);
        dest.writeByte((byte) (earlyWarningTriggered ? 1 : 0));
        dest.writeDouble(predictedRiskScore);
        dest.writeDouble(predictionConfidence);
        dest.writeLong(assessmentTime != null ? assessmentTime.getTime() : -1);
        dest.writeDouble(depthScore);
        
        // Write warning thresholds and indicators
        dest.writeInt(warningThresholds.size());
        for (Map.Entry<String, Double> entry : warningThresholds.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeDouble(entry.getValue());
        }
        
        dest.writeInt(warningIndicators.size());
        for (Map.Entry<String, Double> entry : warningIndicators.entrySet()) {
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
}
