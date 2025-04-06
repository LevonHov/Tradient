package com.example.tradient.domain.risk;

import com.example.tradient.domain.engine.ArbitrageOpportunity;

/**
 * Represents a single risk factor that contributes to the overall risk assessment.
 * Each risk factor evaluates a specific aspect of an arbitrage opportunity.
 */
public interface RiskFactor {
    /**
     * Calculates a risk score for a specific aspect of an arbitrage opportunity.
     * Scores are normalized between 0.0 (highest risk) and 1.0 (lowest risk).
     *
     * @param opportunity The arbitrage opportunity to evaluate
     * @return A normalized risk score between 0.0 and 1.0
     */
    double calculateRiskScore(ArbitrageOpportunity opportunity);
    
    /**
     * Gets the weight of this risk factor in the overall risk calculation.
     * Weights determine the relative importance of different risk factors.
     *
     * @return The weight value between 0.0 and 1.0
     */
    double getWeight();
    
    /**
     * Gets the name of this risk factor for identification and logging.
     *
     * @return The name of the risk factor
     */
    String getName();
} 