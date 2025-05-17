package com.example.tradient.util;

import android.util.Log;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;

/**
 * Utility class that provides a universal interface for working with 
 * risk assessments across different ArbitrageOpportunity implementations.
 */
public class RiskAssessmentAdapter {
    private static final String TAG = "RiskAssessmentAdapter";
    
    /**
     * Get the risk assessment from any opportunity object.
     * Works with both data model and domain model classes.
     * 
     * @param opportunity The opportunity to get risk assessment from
     * @return The risk assessment, or a default if not available
     */
    public static RiskAssessment getRiskAssessment(Object opportunity) {
        if (opportunity == null) {
            Log.w(TAG, "Attempted to get risk assessment from null opportunity");
            return createDefaultRiskAssessment();
        }
        
        try {
            // Handle data model implementation directly
            if (opportunity instanceof ArbitrageOpportunity) {
                RiskAssessment assessment = ((ArbitrageOpportunity) opportunity).getRiskAssessment();
                if (assessment != null) {
                    return assessment;
                }
                Log.d(TAG, "Creating default assessment for ArbitrageOpportunity");
                return createDefaultRiskAssessment();
            }
            
            // Try reflection for all other implementations
            java.lang.reflect.Method method = opportunity.getClass().getMethod("getRiskAssessment");
            Object result = method.invoke(opportunity);
            if (result instanceof RiskAssessment) {
                return (RiskAssessment) result;
            }
        } catch (Exception e) {
            // Create a default assessment if methods aren't available
            Log.d(TAG, "Creating default assessment due to reflection error: " + e.getMessage());
            return createDefaultRiskAssessment();
        }
        
        return createDefaultRiskAssessment();
    }
    
    /**
     * Set the risk assessment on any opportunity object.
     * Works with both data model and domain model classes.
     * 
     * @param opportunity The opportunity to set risk assessment on
     * @param assessment The risk assessment to set
     * @return True if successfully set, false otherwise
     */
    public static boolean setRiskAssessment(Object opportunity, RiskAssessment assessment) {
        if (opportunity == null || assessment == null) {
            Log.w(TAG, "Cannot set risk assessment: " + 
                  (opportunity == null ? "opportunity is null" : "assessment is null"));
            return false;
        }
        
        try {
            // Handle data model implementation directly
            if (opportunity instanceof ArbitrageOpportunity) {
                ((ArbitrageOpportunity) opportunity).setRiskAssessment(assessment);
                Log.d(TAG, "Successfully set risk assessment on ArbitrageOpportunity");
                return true;
            }
            
            // Try reflection for all other implementations
            java.lang.reflect.Method method = opportunity.getClass().getMethod("setRiskAssessment", RiskAssessment.class);
            method.invoke(opportunity, assessment);
            return true;
        } catch (Exception e) {
            // Try setting individual properties if setting the whole assessment fails
            Log.d(TAG, "Failed to set risk assessment directly, trying individual properties: " + e.getMessage());
            boolean success = false;
            
            try {
                // Set individual properties using reflection
                java.lang.reflect.Method setRiskScore = opportunity.getClass().getMethod("setRiskScore", double.class);
                java.lang.reflect.Method setLiquidity = opportunity.getClass().getMethod("setLiquidity", double.class);
                java.lang.reflect.Method setVolatility = opportunity.getClass().getMethod("setVolatility", double.class);
                
                setRiskScore.invoke(opportunity, assessment.getOverallRiskScore());
                setLiquidity.invoke(opportunity, assessment.getLiquidityScore());
                setVolatility.invoke(opportunity, assessment.getVolatilityScore());
                success = true;
            } catch (Exception ex1) {
                // Try alternate method names
                Log.d(TAG, "First attempt to set properties failed: " + ex1.getMessage());
                try {
                    java.lang.reflect.Method setLiquidityScore = opportunity.getClass().getMethod("setLiquidityScore", double.class);
                    java.lang.reflect.Method setVolatilityScore = opportunity.getClass().getMethod("setVolatilityScore", double.class);
                    
                    setLiquidityScore.invoke(opportunity, assessment.getLiquidityScore());
                    setVolatilityScore.invoke(opportunity, assessment.getVolatilityScore());
                    success = true;
                } catch (Exception ex2) {
                    Log.w(TAG, "Failed to set individual risk properties: " + ex2.getMessage());
                }
            }
            
            return success;
        }
    }
    
    /**
     * Check if the opportunity has a risk assessment.
     * 
     * @param opportunity The opportunity to check
     * @return True if the opportunity has a risk assessment, false otherwise
     */
    public static boolean hasRiskAssessment(Object opportunity) {
        if (opportunity == null) {
            return false;
        }
        
        RiskAssessment assessment = getRiskAssessment(opportunity);
        return assessment != null && assessment.isComplete();
    }
    
    /**
     * Create a default risk assessment.
     * 
     * @return A new risk assessment with default values
     */
    public static RiskAssessment createDefaultRiskAssessment() {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setOverallRiskScore(0.5); // Moderate risk
        assessment.setLiquidityScore(0.5);
        assessment.setVolatilityScore(0.5);
        assessment.setFeeImpact(0.5);
        assessment.setMarketDepthScore(0.5);
        assessment.setSlippageRisk(0.01); // 1% slippage
        assessment.setRiskLevel("MEDIUM");
        return assessment;
    }
    
    /**
     * Create or update the risk assessment on an opportunity.
     * If the opportunity already has a risk assessment, it will be updated.
     * Otherwise, a new one will be created.
     * 
     * @param opportunity The opportunity to update
     * @return The risk assessment that was created or updated
     */
    public static RiskAssessment ensureRiskAssessment(Object opportunity) {
        RiskAssessment assessment = getRiskAssessment(opportunity);
        
        if (assessment == null) {
            assessment = createDefaultRiskAssessment();
            setRiskAssessment(opportunity, assessment);
        }
        
        return assessment;
    }
    
    /**
     * Update fees in the risk assessment from the opportunity.
     * 
     * @param opportunity The opportunity with fee data
     * @param assessment The risk assessment to update
     */
    public static void syncFeesFromOpportunity(Object opportunity, RiskAssessment assessment) {
        if (opportunity == null || assessment == null) {
            return;
        }
        
        try {
            // Try to get fee information
            double buyFee = 0.1;  // Default 0.1%
            double sellFee = 0.1; // Default 0.1%
            
            try {
                java.lang.reflect.Method getBuyFee = opportunity.getClass().getMethod("getBuyFeePercentage");
                buyFee = (double) getBuyFee.invoke(opportunity);
            } catch (Exception e) {
                Log.d(TAG, "Could not get buy fee: " + e.getMessage());
            }
            
            try {
                java.lang.reflect.Method getSellFee = opportunity.getClass().getMethod("getSellFeePercentage");
                sellFee = (double) getSellFee.invoke(opportunity);
            } catch (Exception e) {
                Log.d(TAG, "Could not get sell fee: " + e.getMessage());
            }
            
            // Update assessment with fees
            assessment.setBuyFeePercentage(buyFee);
            assessment.setSellFeePercentage(sellFee);
            assessment.setFeeImpact((buyFee + sellFee) / 200.0); // Convert to 0-1 scale
            
            Log.d(TAG, "Synced fees: buy=" + buyFee + "%, sell=" + sellFee + "%");
        } catch (Exception e) {
            Log.w(TAG, "Error syncing fees: " + e.getMessage());
        }
    }
} 