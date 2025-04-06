package com.example.tradient.domain.risk;

import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.domain.engine.ArbitrageOpportunity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core service for assessing risk of arbitrage opportunities.
 * Provides methods to calculate risk scores, success probabilities, and risk visualizations.
 */
public class RiskAssessmentService {
    
    private final Map<String, RiskProfile> riskProfiles;
    private String activeProfileName;
    
    /**
     * Initializes the risk assessment service with available risk profiles.
     */
    public RiskAssessmentService() {
        this.riskProfiles = new HashMap<>();
        
        // Register available risk profiles
        RiskProfile standardProfile = new StandardRiskProfile();
        RiskProfile conservativeProfile = new ConservativeRiskProfile();
        
        riskProfiles.put(standardProfile.getName(), standardProfile);
        riskProfiles.put(conservativeProfile.getName(), conservativeProfile);
        
        // Determine active profile from configuration
        this.activeProfileName = ConfigurationFactory.getString("risk.activeProfile", "Standard Risk Profile");
    }
    
    /**
     * Calculate the overall risk score for an arbitrage opportunity.
     *
     * @param opportunity The arbitrage opportunity to assess
     * @return A normalized risk score between 0.0 (highest risk) and 1.0 (lowest risk)
     */
    public double calculateRiskScore(ArbitrageOpportunity opportunity) {
        RiskProfile profile = getActiveProfile();
        List<RiskFactor> factors = profile.getRiskFactors();
        
        double weightedScoreSum = 0.0;
        double weightSum = 0.0;
        
        // Calculate weighted average of all risk factor scores
        for (RiskFactor factor : factors) {
            double score = factor.calculateRiskScore(opportunity);
            double weight = factor.getWeight();
            
            weightedScoreSum += score * weight;
            weightSum += weight;
        }
        
        // Normalize the result
        return weightSum > 0 ? weightedScoreSum / weightSum : 0.5;
    }
    
    /**
     * Estimate the success probability of an arbitrage opportunity.
     *
     * @param opportunity The arbitrage opportunity to assess
     * @return The probability of successful execution (0.0 to 1.0)
     */
    public double estimateSuccessRate(ArbitrageOpportunity opportunity) {
        double riskScore = calculateRiskScore(opportunity);
        
        // Convert risk score to success probability using a sigmoidal function
        // This provides a more realistic distribution with diminishing returns
        double baseRate = ConfigurationFactory.getDouble("risk.baseSuccessRate", 0.5);
        double maxRate = ConfigurationFactory.getDouble("risk.maxSuccessRate", 0.99);
        
        // Sigmoid function maps risk score to success probability
        double successRate = baseRate + (maxRate - baseRate) * (1.0 / (1.0 + Math.exp(-10 * (riskScore - 0.5))));
        
        return Math.min(maxRate, Math.max(0.0, successRate));
    }
    
    /**
     * Checks if an arbitrage opportunity meets the minimum risk threshold.
     *
     * @param opportunity The arbitrage opportunity to assess
     * @return true if the opportunity meets the risk threshold, false otherwise
     */
    public boolean meetsRiskThreshold(ArbitrageOpportunity opportunity) {
        double riskScore = calculateRiskScore(opportunity);
        return riskScore >= getActiveProfile().getMinAcceptableRiskScore();
    }
    
    /**
     * Gets the active risk profile.
     *
     * @return The currently active risk profile
     */
    public RiskProfile getActiveProfile() {
        return riskProfiles.getOrDefault(activeProfileName, riskProfiles.values().iterator().next());
    }
    
    /**
     * Gets detailed risk information for each factor.
     *
     * @param opportunity The arbitrage opportunity to assess
     * @return A map of risk factor names to their individual scores
     */
    public Map<String, Double> getDetailedRiskBreakdown(ArbitrageOpportunity opportunity) {
        Map<String, Double> breakdown = new HashMap<>();
        
        for (RiskFactor factor : getActiveProfile().getRiskFactors()) {
            double score = factor.calculateRiskScore(opportunity);
            breakdown.put(factor.getName(), score);
        }
        
        return breakdown;
    }
    
    /**
     * Changes the active risk profile.
     *
     * @param profileName The name of the profile to activate
     * @return true if the profile was found and activated, false otherwise
     */
    public boolean setActiveProfile(String profileName) {
        if (riskProfiles.containsKey(profileName)) {
            this.activeProfileName = profileName;
            return true;
        }
        return false;
    }
} 