package com.example.tradient.domain.risk;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.util.RiskAssessmentAdapter;

import java.lang.reflect.Method;

/**
 * Utility class for safely setting risk metrics on ArbitrageOpportunity objects.
 * Works with both data model and domain model classes.
 */
public class RiskMetricsSetter {

    /**
     * Set the risk score on an opportunity.
     * 
     * @param opportunity The opportunity to update
     * @param riskScore The risk score to set
     */
    public static void setRiskScore(Object opportunity, double riskScore) {
        if (opportunity == null) {
            return;
        }
        
        try {
            // Try using reflection through a method that avoids direct class casting
            trySetDoubleProperty(opportunity, "setRiskScore", riskScore);
        } catch (Exception e) {
            // Log error but continue
            System.err.println("Failed to set risk score: " + e.getMessage());
        }
    }
    
    /**
     * Set the liquidity on an opportunity.
     * 
     * @param opportunity The opportunity to update
     * @param liquidity The liquidity to set
     */
    public static void setLiquidity(Object opportunity, double liquidity) {
        if (opportunity == null) {
            return;
        }
        
        try {
            // Try reflection with both method names
            boolean success = trySetDoubleProperty(opportunity, "setLiquidity", liquidity);
            if (!success) {
                trySetDoubleProperty(opportunity, "setLiquidityScore", liquidity);
            }
        } catch (Exception e) {
            // Log error but continue
            System.err.println("Failed to set liquidity: " + e.getMessage());
        }
    }
    
    /**
     * Set the volatility on an opportunity.
     * 
     * @param opportunity The opportunity to update
     * @param volatility The volatility to set
     */
    public static void setVolatility(Object opportunity, double volatility) {
        if (opportunity == null) {
            return;
        }
        
        try {
            // Try reflection
            trySetDoubleProperty(opportunity, "setVolatility", volatility);
        } catch (Exception e) {
            // Log error but continue
            System.err.println("Failed to set volatility: " + e.getMessage());
        }
    }
    
    /**
     * Set the slippage on an opportunity.
     * 
     * @param opportunity The opportunity to update
     * @param slippage The slippage to set
     */
    public static void setSlippage(Object opportunity, double slippage) {
        if (opportunity == null) {
            return;
        }
        
        try {
            // Try reflection
            trySetDoubleProperty(opportunity, "setSlippage", slippage);
        } catch (Exception e) {
            // Log error but continue
            System.err.println("Failed to set slippage: " + e.getMessage());
        }
    }
    
    /**
     * Set the viability of an opportunity.
     * 
     * @param opportunity The opportunity to update
     * @param isViable Whether the opportunity is viable
     */
    public static void setViable(Object opportunity, boolean isViable) {
        if (opportunity == null) {
            return;
        }
        
        try {
            // Try reflection
            trySetBooleanProperty(opportunity, "setViable", isViable);
        } catch (Exception e) {
            // Log error but continue
            System.err.println("Failed to set viable flag: " + e.getMessage());
        }
    }
    
    /**
     * Update an opportunity with risk assessment values.
     * 
     * @param opportunity The opportunity to update
     * @param assessment The risk assessment to apply
     */
    public static void applyRiskAssessment(Object opportunity, RiskAssessment assessment) {
        if (opportunity == null || assessment == null) {
            return;
        }
        
        // First try to set the whole assessment
        RiskAssessmentAdapter.setRiskAssessment(opportunity, assessment);
        
        // Then set individual fields
        setRiskScore(opportunity, assessment.getOverallRiskScore());
        setLiquidity(opportunity, assessment.getLiquidityScore());
        setVolatility(opportunity, assessment.getVolatilityScore());
        setSlippage(opportunity, assessment.getSlippageRisk());
        setViable(opportunity, assessment.getOverallRiskScore() >= 0.6);
    }
    
    /**
     * Try to set a double property using reflection.
     * 
     * @param object The object to update
     * @param methodName The name of the setter method
     * @param value The value to set
     * @return True if successful, false otherwise
     */
    private static boolean trySetDoubleProperty(Object object, String methodName, double value) {
        try {
            Method method = object.getClass().getMethod(methodName, double.class);
            method.invoke(object, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Try to set a boolean property using reflection.
     * 
     * @param object The object to update
     * @param methodName The name of the setter method
     * @param value The value to set
     * @return True if successful, false otherwise
     */
    private static boolean trySetBooleanProperty(Object object, String methodName, boolean value) {
        try {
            Method method = object.getClass().getMethod(methodName, boolean.class);
            method.invoke(object, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 