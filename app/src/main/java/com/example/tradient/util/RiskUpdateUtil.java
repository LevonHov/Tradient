package com.example.tradient.util;

import android.util.Log;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.domain.risk.UnifiedRiskCalculator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for consistently updating risk assessments in ArbitrageOpportunity objects
 * using the UnifiedRiskCalculator throughout the application.
 */
public class RiskUpdateUtil {
    private static final String TAG = "RiskUpdateUtil";
    private static final UnifiedRiskCalculator riskCalculator = UnifiedRiskCalculator.getInstance();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * Update the risk assessment for a single opportunity.
     * @param opportunity The opportunity to update
     * @param forceRecalculation Whether to force recalculation even if a valid assessment exists
     */
    public static void updateRisk(ArbitrageOpportunity opportunity, boolean forceRecalculation) {
        if (opportunity == null) return;
        
        try {
            // Check if opportunity already has a valid risk assessment
            if (!forceRecalculation) {
                RiskAssessment existingAssessment = opportunity.getRiskAssessment();
                if (existingAssessment != null && existingAssessment.isValid()) {
                    // No need to recalculate, just ensure consistent values
                    riskCalculator.applyRiskAssessment(opportunity, existingAssessment);
                    return;
                }
            }
            
            // Calculate new risk assessment
            RiskAssessment assessment = riskCalculator.calculateRisk(opportunity);
            
            // Apply assessment to opportunity
            riskCalculator.applyRiskAssessment(opportunity, assessment);
            
            Log.d(TAG, String.format(
                "Updated risk for %s: Risk=%.2f, Liquidity=%.2f, Volatility=%.2f",
                opportunity.getSymbol(),
                assessment.getOverallRiskScore(),
                assessment.getLiquidityScore(),
                assessment.getVolatilityScore()));
                
        } catch (Exception e) {
            Log.e(TAG, "Error updating risk: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update risk assessments for a list of opportunities.
     * @param opportunities The list of opportunities to update
     * @param forceRecalculation Whether to force recalculation even if valid assessments exist
     */
    public static void updateRiskBatch(List<ArbitrageOpportunity> opportunities, boolean forceRecalculation) {
        if (opportunities == null || opportunities.isEmpty()) return;
        
        Log.d(TAG, "Updating risk for " + opportunities.size() + " opportunities");
        
        for (ArbitrageOpportunity opportunity : opportunities) {
            updateRisk(opportunity, forceRecalculation);
        }
    }
    
    /**
     * Update risk assessments for a list of opportunities asynchronously.
     * @param opportunities The list of opportunities to update
     * @param forceRecalculation Whether to force recalculation even if valid assessments exist
     * @param callback Optional callback to notify when updates are complete
     */
    public static void updateRiskAsync(List<ArbitrageOpportunity> opportunities, 
                                       boolean forceRecalculation,
                                       Runnable callback) {
        if (opportunities == null || opportunities.isEmpty()) {
            if (callback != null) callback.run();
            return;
        }
        
        Log.d(TAG, "Asynchronously updating risk for " + opportunities.size() + " opportunities");
        
        executorService.submit(() -> {
            updateRiskBatch(opportunities, forceRecalculation);
            
            // Run callback on completion if provided
            if (callback != null) {
                callback.run();
            }
        });
    }
    
    /**
     * Clean up any resources when no longer needed
     */
    public static void shutdown() {
        executorService.shutdown();
    }
} 