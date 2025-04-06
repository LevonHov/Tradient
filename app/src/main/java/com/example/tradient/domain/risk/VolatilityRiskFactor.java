package com.example.tradient.domain.risk;

import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.domain.engine.ArbitrageOpportunity;

/**
 * Evaluates risk associated with market volatility.
 * Higher volatility increases the risk of price movement during execution.
 */
public class VolatilityRiskFactor implements RiskFactor {
    
    private final double weight;
    
    public VolatilityRiskFactor() {
        // Load weight from configuration
        this.weight = ConfigurationFactory.getDouble("risk.factors.volatility.weight", 0.25);
    }
    
    @Override
    public double calculateRiskScore(ArbitrageOpportunity opportunity) {
        double volatilityIndex = opportunity.getVolatilityScore();
        
        // Volatility is already normalized between 0 and 1
        // where 1 indicates low volatility (low risk)
        return volatilityIndex;
    }
    
    @Override
    public double getWeight() {
        return weight;
    }
    
    @Override
    public String getName() {
        return "Volatility Risk";
    }
} 