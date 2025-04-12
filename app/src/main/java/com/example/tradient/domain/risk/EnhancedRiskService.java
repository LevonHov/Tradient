package com.example.tradient.domain.risk;

import android.util.Log;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced risk service that uses real-time API data to calculate more accurate
 * risk assessments for arbitrage opportunities.
 */
public class EnhancedRiskService {
    private static final String TAG = "EnhancedRiskService";
    
    private final RealTimeRiskCalculator realTimeRiskCalculator;
    private final ExecutorService executor;
    
    // Singleton instance
    private static EnhancedRiskService instance;
    
    // Default trade size for calculations
    private static final double DEFAULT_TRADE_SIZE = 1000.0; // $1000 USD
    
    private EnhancedRiskService() {
        this.realTimeRiskCalculator = new RealTimeRiskCalculator();
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
        
        Log.d(TAG, "Starting risk calculation for opportunity");
        
        // Use the enhanced calculation with order book and liquidity data
        return realTimeRiskCalculator.calculateRiskFromOpportunity(opportunity)
                .exceptionally(ex -> {
                    Log.e(TAG, "Error calculating risk: " + ex.getMessage(), ex);
                    return createDefaultRiskAssessment(0.4); // Medium risk for failed calculations
                });
    }
    
    /**
     * Create a default risk assessment when API data isn't available
     * @param riskLevel Risk level from 0.0 to 1.0 (higher is less risky)
     */
    private RiskAssessment createDefaultRiskAssessment(double riskLevel) {
        double inverseRisk = 1.0 - riskLevel;
        
        // Generate risk components based on overall risk level
        double liquidityScore = riskLevel * 0.7 + 0.2;
        double volatilityScore = riskLevel * 0.8 + 0.1;
        double exchangeRiskScore = riskLevel * 0.6 + 0.3;
        double slippageEstimate = 0.001 + (inverseRisk * 0.049); // 0.1% to 5%
        
        // Calculate derived metrics
        double executionTimeEstimate = 1.0 + (inverseRisk * 9.0); // 1 to 10 minutes
        double roiEfficiency = 0.01 * (60.0 / executionTimeEstimate); // 1% profit 
        double optimalTradeSize = 100.0 + (riskLevel * 900.0); // $100 to $1000
        
        // Return assembled assessment
        return new RiskAssessment(
                riskLevel,
                liquidityScore,
                volatilityScore,
                exchangeRiskScore,
                riskLevel,
                slippageEstimate,
                executionTimeEstimate,
                roiEfficiency,
                optimalTradeSize
        );
    }
} 