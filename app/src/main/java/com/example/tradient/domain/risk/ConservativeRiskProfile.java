package com.example.tradient.domain.risk;

import com.example.tradient.config.ConfigurationFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A conservative risk profile that prioritizes safety over profitability.
 * Has higher thresholds for acceptable risk and may include additional risk factors.
 */
public class ConservativeRiskProfile implements RiskProfile {
    
    private final List<RiskFactor> riskFactors;
    private final String name;
    private final double minAcceptableRiskScore;
    
    public ConservativeRiskProfile() {
        this.name = "Conservative Risk Profile";
        this.riskFactors = new ArrayList<>();
        
        // Add standard risk factors with potentially adjusted weights
        this.riskFactors.add(new SlippageRiskFactor() {
            @Override
            public double getWeight() {
                return 0.35; // Higher weight on slippage for conservative profile
            }
        });
        this.riskFactors.add(new VolatilityRiskFactor() {
            @Override
            public double getWeight() {
                return 0.30; // Higher weight on volatility for conservative profile
            }
        });
        this.riskFactors.add(new LiquidityRiskFactor());
        this.riskFactors.add(new ExchangeReliabilityRiskFactor());
        
        // Load minimum acceptable risk score from configuration (higher than standard)
        this.minAcceptableRiskScore = ConfigurationFactory.getDouble("risk.profile.conservative.minAcceptableScore", 0.85);
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