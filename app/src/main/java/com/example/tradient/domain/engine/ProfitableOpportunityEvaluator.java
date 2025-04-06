package com.example.tradient.domain.engine;

import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.domain.risk.RiskAssessmentService;

/**
 * Evaluates if an arbitrage opportunity is profitable and acceptable considering
 * all risk factors, fees, and minimum profit thresholds.
 */
public class ProfitableOpportunityEvaluator {
    
    private final RiskAssessmentService riskService;
    private final double minProfitThreshold;
    private final double minRiskAdjustedProfit;
    
    /**
     * Initializes the evaluator with required services and configuration.
     *
     * @param riskService The risk assessment service
     */
    public ProfitableOpportunityEvaluator(RiskAssessmentService riskService) {
        this.riskService = riskService;
        this.minProfitThreshold = ConfigurationFactory.getDouble("arbitrage.minProfitThreshold", 0.1);
        this.minRiskAdjustedProfit = ConfigurationFactory.getDouble("arbitrage.minRiskAdjustedProfit", 0.05);
    }
    
    /**
     * Evaluates if an opportunity is profitable and meets risk criteria.
     *
     * @param opportunity The arbitrage opportunity to evaluate
     * @return true if the opportunity is profitable and acceptable, false otherwise
     */
    public boolean isViableOpportunity(ArbitrageOpportunity opportunity) {
        // Check raw profit threshold
        if (opportunity.getNetProfitPercentage() < minProfitThreshold) {
            return false;
        }
        
        // Check risk threshold
        if (!riskService.meetsRiskThreshold(opportunity)) {
            return false;
        }
        
        // Calculate risk-adjusted profit
        double successRate = riskService.estimateSuccessRate(opportunity);
        double riskAdjustedProfit = opportunity.getNetProfitPercentage() * successRate;
        
        // Check risk-adjusted profit threshold
        return riskAdjustedProfit >= minRiskAdjustedProfit;
    }
    
    /**
     * Calculates the expected value of an arbitrage opportunity.
     *
     * @param opportunity The arbitrage opportunity
     * @param investmentAmount The amount to invest in this opportunity
     * @return The expected profit amount considering success probability
     */
    public double calculateExpectedValue(ArbitrageOpportunity opportunity, double investmentAmount) {
        double successRate = riskService.estimateSuccessRate(opportunity);
        double profitOnSuccess = investmentAmount * (opportunity.getNetProfitPercentage() / 100.0);
        
        // In a complete model, we would also consider losses on failure
        double lossOnFailure = estimateLossOnFailure(opportunity, investmentAmount);
        
        return (successRate * profitOnSuccess) + ((1 - successRate) * lossOnFailure);
    }
    
    /**
     * Estimates the potential loss if an arbitrage attempt fails.
     * This includes partial execution risk, market movement losses, etc.
     *
     * @param opportunity The arbitrage opportunity
     * @param investmentAmount The amount invested
     * @return The estimated loss amount (typically negative)
     */
    private double estimateLossOnFailure(ArbitrageOpportunity opportunity, double investmentAmount) {
        // In a failed arbitrage, we typically lose fees plus slippage and possibly more
        double totalFeePercentage = opportunity.getTotalFeePercentage();
        double totalSlippagePercentage = opportunity.getTotalSlippagePercentage();
        
        // Add a market movement risk factor
        double marketMovementRisk = ConfigurationFactory.getDouble("risk.marketMovementRisk", 0.2);
        
        // Conservative estimate of loss on failure as percentage of investment
        double estimatedLossPercentage = -(totalFeePercentage + totalSlippagePercentage + marketMovementRisk);
        
        return investmentAmount * (estimatedLossPercentage / 100.0);
    }
} 