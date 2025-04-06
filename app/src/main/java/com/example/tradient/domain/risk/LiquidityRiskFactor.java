package com.example.tradient.domain.risk;

import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.domain.engine.ArbitrageOpportunity;

/**
 * Evaluates risk associated with market liquidity.
 * Lower liquidity increases the risk of failed or partial execution.
 */
public class LiquidityRiskFactor implements RiskFactor {
    
    private final double weight;
    
    public LiquidityRiskFactor() {
        // Load weight from configuration
        this.weight = ConfigurationFactory.getDouble("risk.factors.liquidity.weight", 0.25);
    }
    
    @Override
    public double calculateRiskScore(ArbitrageOpportunity opportunity) {
        double liquidityScore = opportunity.getLiquidityScore();
        
        // Liquidity is already normalized between 0 and 1
        // where 1 indicates high liquidity (low risk)
        return liquidityScore;
    }
    
    @Override
    public double getWeight() {
        return weight;
    }
    
    @Override
    public String getName() {
        return "Liquidity Risk";
    }
} 