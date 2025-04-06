package com.example.tradient.domain.risk;

import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.domain.engine.ArbitrageOpportunity;
import com.example.tradient.data.model.ExchangeConfiguration;

/**
 * Evaluates risk associated with exchange reliability.
 * Considers historical performance and current status of each exchange.
 */
public class ExchangeReliabilityRiskFactor implements RiskFactor {
    
    private final double weight;
    private final ExchangeConfiguration exchangeConfig;
    
    public ExchangeReliabilityRiskFactor() {
        // Load weight from configuration
        this.weight = ConfigurationFactory.getDouble("risk.factors.exchangeReliability.weight", 0.2);
        this.exchangeConfig = ConfigurationFactory.getExchangeConfig();
    }
    
    @Override
    public double calculateRiskScore(ArbitrageOpportunity opportunity) {
        String buyExchange = opportunity.getBuyExchange();
        String sellExchange = opportunity.getSellExchange();
        
        // Get reliability scores for both exchanges (default to 0.9 if not specified)
        double buyExchangeReliability = exchangeConfig.getReliabilityScore(buyExchange, 0.9);
        double sellExchangeReliability = exchangeConfig.getReliabilityScore(sellExchange, 0.9);
        
        // The overall reliability is the product of both exchanges' reliability
        // This reflects that both exchanges must be reliable for the arbitrage to succeed
        return buyExchangeReliability * sellExchangeReliability;
    }
    
    @Override
    public double getWeight() {
        return weight;
    }
    
    @Override
    public String getName() {
        return "Exchange Reliability Risk";
    }
} 