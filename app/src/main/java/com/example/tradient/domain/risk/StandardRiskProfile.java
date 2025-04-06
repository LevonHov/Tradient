package com.example.tradient.domain.risk;

import com.example.tradient.config.ConfigurationFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard implementation of a risk profile with a balanced approach to risk assessment.
 * Includes all standard risk factors with configuration-driven weights.
 */
public class StandardRiskProfile implements RiskProfile {
    
    private final List<RiskFactor> riskFactors;
    private final String name;
    private final double minAcceptableRiskScore;
    
    public StandardRiskProfile() {
        this.name = "Standard Risk Profile";
        this.riskFactors = new ArrayList<>();
        
        // Add all standard risk factors
        this.riskFactors.add(new SlippageRiskFactor());
        this.riskFactors.add(new VolatilityRiskFactor());
        this.riskFactors.add(new LiquidityRiskFactor());
        this.riskFactors.add(new ExchangeReliabilityRiskFactor());
        
        // Load minimum acceptable risk score from configuration
        this.minAcceptableRiskScore = ConfigurationFactory.getDouble("risk.profile.standard.minAcceptableScore", 0.7);
    }
    
    @Override
    public List<RiskFactor> getRiskFactors() {
        return riskFactors;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public double getMinAcceptableRiskScore() {
        return minAcceptableRiskScore;
    }
} 