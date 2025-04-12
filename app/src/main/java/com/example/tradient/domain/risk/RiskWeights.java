package com.example.tradient.domain.risk;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages weights for different risk factors in the risk calculation.
 * Ensures weights are properly normalized and validated.
 */
public class RiskWeights {
    private final Map<String, Double> weights;
    private double totalWeight;

    public RiskWeights() {
        this.weights = new HashMap<>();
        initializeDefaultWeights();
    }

    private void initializeDefaultWeights() {
        weights.put("liquidity", RiskScoreConstants.DEFAULT_LIQUIDITY_WEIGHT);
        weights.put("volatility", RiskScoreConstants.DEFAULT_VOLATILITY_WEIGHT);
        weights.put("slippage", RiskScoreConstants.DEFAULT_SLIPPAGE_WEIGHT);
        weights.put("marketDepth", RiskScoreConstants.DEFAULT_MARKET_DEPTH_WEIGHT);
        weights.put("executionSpeed", RiskScoreConstants.DEFAULT_EXECUTION_SPEED_WEIGHT);
        weights.put("fees", RiskScoreConstants.DEFAULT_FEE_WEIGHT);
        weights.put("marketRegime", RiskScoreConstants.DEFAULT_MARKET_REGIME_WEIGHT);
        weights.put("sentiment", RiskScoreConstants.DEFAULT_SENTIMENT_WEIGHT);
        
        validateAndNormalizeWeights();
    }

    public void setWeight(String factor, double weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("Weight cannot be negative: " + weight);
        }
        weights.put(factor, weight);
        validateAndNormalizeWeights();
    }

    public double getWeight(String factor) {
        return weights.getOrDefault(factor, 0.0);
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    private void validateAndNormalizeWeights() {
        totalWeight = weights.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (totalWeight <= 0) {
            throw new IllegalStateException("Total weight must be positive");
        }

        // Normalize weights to sum to 1.0
        weights.replaceAll((k, v) -> v / totalWeight);
        totalWeight = 1.0;
    }

    public Map<String, Double> getWeights() {
        return new HashMap<>(weights);
    }

    public void resetToDefaults() {
        initializeDefaultWeights();
    }
} 