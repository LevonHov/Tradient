package com.example.tradient.domain.risk;

import android.util.Log;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced risk service that uses UnifiedRiskCalculator to ensure consistent
 * risk calculations throughout the application.
 */
public class EnhancedRiskService {
    private static final String TAG = "EnhancedRiskService";
    
    private final UnifiedRiskCalculator riskCalculator;
    private final ExecutorService executor;
    
    // Singleton instance
    private static EnhancedRiskService instance;
    
    // Default trade size for calculations
    private static final double DEFAULT_TRADE_SIZE = 1000.0; // $1000 USD
    
    private EnhancedRiskService() {
        this.riskCalculator = UnifiedRiskCalculator.getInstance();
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * Get the singleton instance of the risk service
     */
    public static synchronized EnhancedRiskService getInstance() {
        if (instance == null) {
            instance = new EnhancedRiskService();
        }
        return instance;
    }
    
    /**
     * Calculate risk assessment for an arbitrage opportunity
     * @param opportunity The arbitrage opportunity to assess
     * @return CompletableFuture with the risk assessment
     */
    public CompletableFuture<RiskAssessment> calculateRisk(ArbitrageOpportunity opportunity) {
        if (opportunity == null) {
            CompletableFuture<RiskAssessment> future = new CompletableFuture<>();
            future.complete(createDefaultRiskAssessment(0.3)); // Medium-high risk for null opportunity
            return future;
        }
        
        Log.d(TAG, "Starting risk calculation for opportunity using UnifiedRiskCalculator");
        
        // Use a CompletableFuture to run the calculation asynchronously
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use the UnifiedRiskCalculator for consistent risk calculation
                RiskAssessment assessment = riskCalculator.calculateRisk(opportunity);
                
                // Apply the assessment to update all fields in the opportunity
                riskCalculator.applyRiskAssessment(opportunity, assessment);
                
                Log.d(TAG, String.format(
                    "Risk calculation completed: Score=%.2f, Liquidity=%.2f, Volatility=%.2f",
                    assessment.getOverallRiskScore(),
                    assessment.getLiquidityScore(),
                    assessment.getVolatilityScore()));
                
                return assessment;
            } catch (Exception e) {
                Log.e(TAG, "Error calculating risk: " + e.getMessage(), e);
                return createDefaultRiskAssessment(0.4); // Medium risk for failed calculations
            }
        }, executor);
    }
    
    /**
     * Create a default risk assessment when calculation fails
     * @param riskLevel Risk level from 0.0 to 1.0 (higher is less risky)
     */
    private RiskAssessment createDefaultRiskAssessment(double riskLevel) {
        // Use UnifiedRiskCalculator's implementation for consistency
        RiskAssessment assessment = new RiskAssessment();
        assessment.setOverallRiskScore(riskLevel);
        assessment.setLiquidityScore(riskLevel * 0.7 + 0.2);
        assessment.setVolatilityScore(riskLevel * 0.8 + 0.1);
        assessment.setExchangeRiskScore(riskLevel * 0.6 + 0.3);
        assessment.setTransactionRiskScore(riskLevel);
        assessment.setSlippageEstimate(0.001 + ((1.0 - riskLevel) * 0.049)); // 0.1% to 5%
        assessment.setExecutionTimeEstimate(1.0 + ((1.0 - riskLevel) * 9.0)); // 1 to 10 minutes
        assessment.setRoiEfficiency(0.01 * (60.0 / assessment.getExecutionTimeEstimate())); // Hourly ROI
        assessment.setOptimalTradeSize(100.0 + (riskLevel * 900.0)); // $100 to $1000
        
        return assessment;
    }
} 