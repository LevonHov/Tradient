package com.example.tradient.domain.risk;

import java.util.List;

/**
 * Represents a risk profile that determines how risk is assessed.
 * A risk profile encapsulates a collection of risk factors with their weights.
 */
public interface RiskProfile {
    /**
     * Gets all the risk factors that contribute to this risk profile.
     *
     * @return List of risk factors
     */
    List<RiskFactor> getRiskFactors();
    
    /**
     * Gets the name of this risk profile.
     *
     * @return The profile name
     */
    String getName();
    
    /**
     * Gets the minimum acceptable risk score for this profile.
     * Opportunities below this threshold are considered too risky.
     *
     * @return The minimum acceptable risk score (0.0 to 1.0)
     */
    double getMinAcceptableRiskScore();
} 