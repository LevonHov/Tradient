package com.example.tradient.domain.risk;

import android.util.Log;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;

/**
 * Utility class to ensure consistent risk calculations across all app screens.
 * This helps maintain the same risk values in different views like cards, opportunity lists,
 * and detail screens.
 */
public class RiskEnsurer {
    private static final String TAG = "RiskEnsurer";
    
    private static final UnifiedRiskCalculator riskCalculator = UnifiedRiskCalculator.getInstance();
    
    /**
     * Updates an opportunity with consistent risk values.
     * Use this method before displaying any opportunity to ensure values are correct.
     * 
     * @param opportunity The opportunity to update
     * @param forceRecalculation Whether to force recalculation even if an assessment exists
     * @return The same opportunity with updated risk values
     */
    public static ArbitrageOpportunity ensureRiskValues(ArbitrageOpportunity opportunity, boolean forceRecalculation) {
        if (opportunity == null) {
            Log.e(TAG, "Cannot ensure risk values for null opportunity");
            return null;
        }
        
        try {
            // Check if opportunity already has a valid risk assessment
            RiskAssessment existingRisk = opportunity.getRiskAssessment();
            
            if (!forceRecalculation && existingRisk != null && existingRisk.isValid()) {
                // Just make sure the values are properly copied to the opportunity
                Log.d(TAG, "Using existing risk assessment for opportunity");
                copyRiskValuesToOpportunity(opportunity, existingRisk);
                return opportunity;
            }
            
            // Calculate a fresh risk assessment
            Log.d(TAG, "Calculating fresh risk assessment for opportunity");
            RiskAssessment newRisk = riskCalculator.calculateRisk(opportunity);
            
            // Apply the assessment and make sure values are properly copied
            riskCalculator.applyRiskAssessment(opportunity, newRisk);
            
            // Double-check that values were properly copied
            copyRiskValuesToOpportunity(opportunity, newRisk);
            
            Log.d(TAG, String.format(
                "Risk values ensured - Risk: %.2f (%s), Liquidity: %.2f, Volatility: %.2f",
                opportunity.getRiskScore(),
                riskCalculator.getRiskLevelText(opportunity.getRiskScore()),
                opportunity.getLiquidity(),
                opportunity.getVolatility()));
                
            return opportunity;
            
        } catch (Exception e) {
            Log.e(TAG, "Error ensuring risk values: " + e.getMessage(), e);
            return opportunity;
        }
    }
    
    /**
     * Explicitly copies values from a risk assessment to an opportunity.
     * This ensures all risk fields are properly set even if the opportunity's
     * setRiskAssessment method doesn't handle it correctly.
     * 
     * @param opportunity The opportunity to update
     * @param assessment The risk assessment to copy values from
     */
    private static void copyRiskValuesToOpportunity(ArbitrageOpportunity opportunity, RiskAssessment assessment) {
        if (opportunity == null || assessment == null) return;
        
        // Directly set all risk-related fields
        opportunity.setRiskScore(assessment.getOverallRiskScore());
        opportunity.setLiquidity(assessment.getLiquidityScore());
        opportunity.setVolatility(assessment.getVolatilityScore());
        opportunity.setSlippage(assessment.getSlippageEstimate());
        opportunity.setEstimatedTimeMinutes(assessment.getExecutionTimeEstimate());
        
        // Set ROI efficiency if valid
        if (assessment.getRoiEfficiency() > 0) {
            opportunity.setRoiEfficiency(assessment.getRoiEfficiency());
        }
        
        // Set fees if valid
        if (assessment.getBuyFeePercentage() > 0) {
            opportunity.setBuyFeePercentage(assessment.getBuyFeePercentage());
        }
        
        if (assessment.getSellFeePercentage() > 0) {
            opportunity.setSellFeePercentage(assessment.getSellFeePercentage());
        }
    }
} 