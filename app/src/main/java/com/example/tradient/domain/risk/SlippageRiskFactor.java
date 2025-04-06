package com.example.tradient.domain.risk;

import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.domain.engine.ArbitrageOpportunity;

/**
 * Evaluates the risk associated with order book slippage.
 * Considers both buy-side and sell-side slippage in the context of order size.
 */
public class SlippageRiskFactor implements RiskFactor {
    
    private final double weight;
    
    public SlippageRiskFactor() {
        // Load weight from configuration
        this.weight = ConfigurationFactory.getDouble("risk.factors.slippage.weight", 0.3);
    }
    
    @Override
    public double calculateRiskScore(ArbitrageOpportunity opportunity) {
        double totalSlippage = opportunity.getTotalSlippagePercentage();
        double maxAcceptableSlippage = ConfigurationFactory.getDouble("risk.slippage.maxAcceptable", 0.5);
        
        // A higher slippage means higher risk (lower score)
        if (totalSlippage >= maxAcceptableSlippage) {
            return 0.0; // Maximum risk
        }
        
        // Normalize slippage to a 0-1 scale where 0 is max risk
        return 1.0 - (totalSlippage / maxAcceptableSlippage);
    }
    
    @Override
    public double getWeight() {
        return weight;
    }
    
    @Override
    public String getName() {
        return "Slippage Risk";
    }
} 