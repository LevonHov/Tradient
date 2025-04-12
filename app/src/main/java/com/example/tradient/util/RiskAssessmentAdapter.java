package com.example.tradient.util;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;

/**
 * Utility class that provides a universal interface for working with 
 * risk assessments across different ArbitrageOpportunity implementations.
 */
public class RiskAssessmentAdapter {
    
    /**
     * Get the risk assessment from any opportunity object.
     * Works with both data model and domain model classes.
     * 
     * @param opportunity The opportunity to get risk assessment from
     * @return The risk assessment, or null if not available
     */
    public static RiskAssessment getRiskAssessment(Object opportunity) {
        if (opportunity == null) {
            return null;
        }
        
        try {
            // Try reflection for all implementations
            java.lang.reflect.Method method = opportunity.getClass().getMethod("getRiskAssessment");
            Object result = method.invoke(opportunity);
            if (result instanceof RiskAssessment) {
                return (RiskAssessment) result;
            }
        } catch (Exception e) {
            // Silently fail if methods aren't available
            // Create a default assessment
            return createDefaultRiskAssessment();
        }
        
        return null;
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
            return false;
        }
        
        try {
            // Try reflection for all implementations
            java.lang.reflect.Method method = opportunity.getClass().getMethod("setRiskAssessment", RiskAssessment.class);
            method.invoke(opportunity, assessment);
            return true;
        } catch (Exception e) {
            // Try setting individual properties if setting the whole assessment fails
            try {
                // Set individual properties using reflection
                java.lang.reflect.Method setRiskScore = opportunity.getClass().getMethod("setRiskScore", double.class);
                java.lang.reflect.Method setLiquidity = opportunity.getClass().getMethod("setLiquidity", double.class);
                java.lang.reflect.Method setVolatility = opportunity.getClass().getMethod("setVolatility", double.class);
                
                setRiskScore.invoke(opportunity, assessment.getOverallRiskScore());
                setLiquidity.invoke(opportunity, assessment.getLiquidityScore());
                setVolatility.invoke(opportunity, assessment.getVolatilityScore());
                return true;
            } catch (Exception ex) {
                // Try alternate method names
                try {
                    java.lang.reflect.Method setLiquidityScore = opportunity.getClass().getMethod("setLiquidityScore", double.class);
                    java.lang.reflect.Method setVolatilityScore = opportunity.getClass().getMethod("setVolatilityScore", double.class);
                    
                    setLiquidityScore.invoke(opportunity, assessment.getLiquidityScore());
                    setVolatilityScore.invoke(opportunity, assessment.getVolatilityScore());
                    return true;
                } catch (Exception ex2) {
                    // Silently fail
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if the opportunity has a risk assessment.
     * 
     * @param opportunity The opportunity to check
     * @return True if the opportunity has a risk assessment, false otherwise
     */
    public static boolean hasRiskAssessment(Object opportunity) {
        return getRiskAssessment(opportunity) != null;
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
        assessment.setSlippageRisk(0.5);
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
} 