package com.example.tradient.domain.risk;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.util.RiskAssessmentAdapter;

/**
 * Utility class for calculating risk assessments for arbitrage opportunities.
 * Handles both domain model and data model entities to ensure consistent 
 * risk assessment across the application.
 */
public class RiskAssessmentUtil {

    // Constants for risk calculation
    private static final double DEFAULT_LIQUIDITY_WEIGHT = 0.25;
    private static final double DEFAULT_VOLATILITY_WEIGHT = 0.25;
    private static final double DEFAULT_FEE_WEIGHT = 0.20;
    private static final double DEFAULT_DEPTH_WEIGHT = 0.15;
    private static final double DEFAULT_EXECUTION_WEIGHT = 0.15;
    
    // Thresholds for risk levels
    private static final double HIGH_RISK_THRESHOLD = 0.3;
    private static final double MEDIUM_RISK_THRESHOLD = 0.6;
    
    /**
     * Calculate risk assessment for a domain model opportunity
     * 
     * @param opportunity The domain model opportunity
     * @return A RiskAssessment object
     */
    public static RiskAssessment calculateRiskForDomain(com.example.tradient.domain.engine.ArbitrageOpportunity opportunity) {
        if (opportunity == null) {
            return createDefaultRiskAssessment();
        }
        
        // Create and configure the risk assessment
        RiskAssessment assessment = new RiskAssessment();
        
        // Calculate liquidity score
        double liquidity = calculateLiquidityScore(
            opportunity.getLiquidityScore(), 
            opportunity.getBuyTicker(), 
            opportunity.getSellTicker()
        );
        assessment.setLiquidityScore(liquidity);
        
        // Calculate volatility score
        double volatility = opportunity.getVolatility();
        assessment.setVolatilityScore(normalizeVolatilityScore(volatility));
        
        // Calculate fee impact
        double feeImpact = calculateFeeImpact(
            opportunity.getBuyFeePercentage(), 
            opportunity.getSellFeePercentage()
        );
        assessment.setFeeImpact(feeImpact);
        
        // Calculate market depth score
        assessment.setMarketDepthScore(opportunity.getLiquidityScore());
        
        // Calculate slippage risk
        assessment.setSlippageRisk(opportunity.getSlippage());
        
        // Calculate overall risk score
        double overallScore = calculateOverallRiskScore(assessment);
        assessment.setOverallRiskScore(overallScore);
        
        // Set risk level based on overall score
        assessment.setRiskLevel(determineRiskLevel(overallScore));
        
        // Set exchange information
        assessment.setExchangeBuy(opportunity.getBuyExchange());
        assessment.setExchangeSell(opportunity.getSellExchange());
        
        // Set fee information
        assessment.setBuyFeePercentage(opportunity.getBuyFeePercentage());
        assessment.setSellFeePercentage(opportunity.getSellFeePercentage());
        
        return assessment;
    }
    
    /**
     * Calculate risk assessment for a data model opportunity
     * 
     * @param opportunity The data model opportunity
     * @return A RiskAssessment object
     */
    public static RiskAssessment calculateRiskForData(com.example.tradient.data.model.ArbitrageOpportunity opportunity) {
        if (opportunity == null) {
            return createDefaultRiskAssessment();
        }
        
        // Create and configure the risk assessment
        RiskAssessment assessment = new RiskAssessment();
        
        // Calculate liquidity score
        double liquidity = calculateLiquidityScore(
            opportunity.getLiquidity(), 
            opportunity.getBuyTicker(), 
            opportunity.getSellTicker()
        );
        assessment.setLiquidityScore(liquidity);
        
        // Calculate volatility score
        double volatility = opportunity.getVolatility();
        assessment.setVolatilityScore(normalizeVolatilityScore(volatility));
        
        // Calculate fee impact
        double feeImpact = calculateFeeImpact(
            opportunity.getBuyFeePercentage(), 
            opportunity.getSellFeePercentage()
        );
        assessment.setFeeImpact(feeImpact);
        
        // Calculate market depth score
        assessment.setMarketDepthScore(opportunity.getLiquidity());
        
        // Calculate slippage risk
        assessment.setSlippageRisk(opportunity.getSlippage());
        
        // Calculate overall risk score
        double overallScore = calculateOverallRiskScore(assessment);
        assessment.setOverallRiskScore(overallScore);
        
        // Set risk level based on overall score
        assessment.setRiskLevel(determineRiskLevel(overallScore));
        
        // Set exchange information
        assessment.setExchangeBuy(opportunity.getExchangeBuy());
        assessment.setExchangeSell(opportunity.getExchangeSell());
        
        // Set fee information
        assessment.setBuyFeePercentage(opportunity.getBuyFeePercentage());
        assessment.setSellFeePercentage(opportunity.getSellFeePercentage());
        
        return assessment;
    }
    
    /**
     * Apply risk assessment to a domain model opportunity
     * 
     * @param opportunity The opportunity to update
     * @param assessment The risk assessment to apply
     */
    public static void applyRiskToDomain(com.example.tradient.domain.engine.ArbitrageOpportunity opportunity, RiskAssessment assessment) {
        if (opportunity == null || assessment == null) {
            return;
        }
        
        opportunity.setRiskAssessment(assessment);
        opportunity.setRiskScore(assessment.getOverallRiskScore());
        opportunity.setLiquidityScore(assessment.getLiquidityScore());
        opportunity.setVolatility(assessment.getVolatilityScore());
        opportunity.setSlippage(assessment.getSlippageRisk());
        
        // Determine if the opportunity is viable based on risk assessment
        opportunity.setViable(isViable(assessment));
    }
    
    /**
     * Apply risk assessment to a data model opportunity
     * 
     * @param opportunity The opportunity to update
     * @param assessment The risk assessment to apply
     */
    public static void applyRiskToData(com.example.tradient.data.model.ArbitrageOpportunity opportunity, RiskAssessment assessment) {
        if (opportunity == null || assessment == null) {
            return;
        }
        
        // Use adapter to set the risk assessment
        RiskAssessmentAdapter.setRiskAssessment(opportunity, assessment);
        
        // Set individual risk metrics using adapter pattern
        RiskMetricsSetter.setRiskScore(opportunity, assessment.getOverallRiskScore());
        RiskMetricsSetter.setLiquidity(opportunity, assessment.getLiquidityScore());
        RiskMetricsSetter.setVolatility(opportunity, assessment.getVolatilityScore());
        RiskMetricsSetter.setSlippage(opportunity, assessment.getSlippageRisk());
        
        // Determine if the opportunity is viable based on risk assessment
        opportunity.setViable(isViable(assessment));
    }
    
    /**
     * Checks if an opportunity is viable based on its risk assessment
     * 
     * @param assessment The risk assessment to evaluate
     * @return true if the opportunity is considered viable
     */
    public static boolean isViable(RiskAssessment assessment) {
        if (assessment == null) {
            return false;
        }
        
        // An opportunity is viable if its overall risk score is above the medium risk threshold
        // and no early warnings have been triggered
        return assessment.getOverallRiskScore() >= MEDIUM_RISK_THRESHOLD && 
               !assessment.isEarlyWarningTriggered();
    }
    
    /**
     * Creates a default risk assessment with moderate values
     * 
     * @return A default risk assessment
     */
    private static RiskAssessment createDefaultRiskAssessment() {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setOverallRiskScore(0.5); // Moderate risk
        assessment.setLiquidityScore(0.5);
        assessment.setVolatilityScore(0.5);
        assessment.setFeeImpact(0.5);
        assessment.setMarketDepthScore(0.5);
        assessment.setSlippageRisk(0.5);
        assessment.setRiskLevel("MEDIUM");
        return assessment;
    }
    
    /**
     * Calculate the liquidity score based on available data
     * 
     * @param baseScore Base liquidity score
     * @param buyTicker Buy side ticker
     * @param sellTicker Sell side ticker
     * @return The calculated liquidity score
     */
    private static double calculateLiquidityScore(double baseScore, Ticker buyTicker, Ticker sellTicker) {
        // If we don't have ticker data, return the base score
        if (buyTicker == null || sellTicker == null) {
            return Math.max(0.3, Math.min(baseScore, 1.0));
        }
        
        // Calculate liquidity based on ticker data
        double buyVolume = buyTicker.getVolume();
        double sellVolume = sellTicker.getVolume();
        
        // Normalize volumes and calculate average
        double normalizedBuyVolume = normalizeVolume(buyVolume);
        double normalizedSellVolume = normalizeVolume(sellVolume);
        double averageVolume = (normalizedBuyVolume + normalizedSellVolume) / 2.0;
        
        // Combine with base score (giving 70% weight to ticker volume data)
        double liquidityScore = 0.3 * baseScore + 0.7 * averageVolume;
        
        // Ensure the result is between 0 and 1
        return Math.max(0.0, Math.min(liquidityScore, 1.0));
    }
    
    /**
     * Normalize a volume value to a score between 0 and 1
     * 
     * @param volume The volume to normalize
     * @return A normalized score between 0 and 1
     */
    private static double normalizeVolume(double volume) {
        // Apply a logarithmic scale to handle wide range of volumes
        if (volume <= 0) {
            return 0.0;
        }
        
        double logVolume = Math.log10(volume);
        
        // Assuming typical volumes are between 1 and 10^8
        // Normalize to 0-1 range
        return Math.min(logVolume / 8.0, 1.0);
    }
    
    /**
     * Normalize a volatility value to a score between 0 and 1
     * 
     * @param volatility The raw volatility value
     * @return A normalized score between 0 and 1 (higher is better/less volatile)
     */
    private static double normalizeVolatilityScore(double volatility) {
        // Invert the volatility so higher scores mean lower volatility (less risk)
        if (volatility <= 0) {
            return 1.0; // No volatility is best
        }
        
        if (volatility >= 1.0) {
            return 0.1; // Very high volatility, but still give a small non-zero score
        }
        
        return 1.0 - volatility;
    }
    
    /**
     * Calculate the impact of fees on profitability
     * 
     * @param buyFee Buy side fee percentage
     * @param sellFee Sell side fee percentage
     * @return A score between 0 and 1 representing fee impact (higher is better/lower fees)
     */
    private static double calculateFeeImpact(double buyFee, double sellFee) {
        double totalFee = buyFee + sellFee;
        
        // Higher score means lower fees (less risk)
        if (totalFee >= 0.01) { // 1% or higher fees
            return 0.2; // Very high fee impact
        }
        
        if (totalFee <= 0.0005) { // 0.05% or lower fees
            return 1.0; // Very low fee impact
        }
        
        // Linear scale between 0.05% and 1%
        return 1.0 - ((totalFee - 0.0005) / 0.0095 * 0.8);
    }
    
    /**
     * Calculate the overall risk score based on individual risk factors
     * 
     * @param assessment The risk assessment with individual scores
     * @return The overall risk score between 0 and 1
     */
    private static double calculateOverallRiskScore(RiskAssessment assessment) {
        double liquidityScore = assessment.getLiquidityScore();
        double volatilityScore = assessment.getVolatilityScore();
        double feeImpact = assessment.getFeeImpact();
        double depthScore = assessment.getMarketDepthScore();
        double executionRisk = 1.0 - assessment.getSlippageRisk(); // Invert slippage risk
        
        // Weighted average of all risk factors
        double weightedScore = 
            liquidityScore * DEFAULT_LIQUIDITY_WEIGHT +
            volatilityScore * DEFAULT_VOLATILITY_WEIGHT +
            feeImpact * DEFAULT_FEE_WEIGHT +
            depthScore * DEFAULT_DEPTH_WEIGHT +
            executionRisk * DEFAULT_EXECUTION_WEIGHT;
        
        // Ensure the result is between 0 and 1
        return Math.max(0.0, Math.min(weightedScore, 1.0));
    }
    
    /**
     * Determine the risk level based on the overall risk score
     * 
     * @param overallScore The overall risk score
     * @return The risk level as a string ("HIGH", "MEDIUM", or "LOW")
     */
    private static String determineRiskLevel(double overallScore) {
        if (overallScore < HIGH_RISK_THRESHOLD) {
            return "HIGH";
        } else if (overallScore < MEDIUM_RISK_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
} 