package com.example.tradient.domain.risk;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.data.model.Ticker;

import java.lang.reflect.Method;

/**
 * Utility class for calculating risk assessments for arbitrage opportunities.
 * Handles both domain model and data model entities to ensure consistent 
 * risk assessment across the application.
 */
public class RiskAssessmentCalculator {

    // Constants for risk calculation
    private static final double DEFAULT_LIQUIDITY_WEIGHT = 0.25;
    private static final double DEFAULT_VOLATILITY_WEIGHT = 0.25;
    private static final double DEFAULT_FEE_WEIGHT = 0.20;
    private static final double DEFAULT_DEPTH_WEIGHT = 0.15;
    private static final double DEFAULT_EXECUTION_WEIGHT = 0.15;
    
    // Thresholds for risk levels
    private static final double HIGH_RISK_THRESHOLD = 0.3;
    private static final double MEDIUM_RISK_THRESHOLD = 0.6;
    
    /**
     * Calculate risk assessment for any ArbitrageOpportunity instance.
     * This method uses reflection to handle both domain and data model instances.
     * 
     * @param opportunity The arbitrage opportunity
     * @return A RiskAssessment object
     */
    public static RiskAssessment calculateRisk(Object opportunity) {
        if (opportunity == null) {
            return createDefaultRiskAssessment();
        }
        
        // Create and configure the risk assessment
        RiskAssessment assessment = new RiskAssessment();
        
        try {
            // Get core values using reflection
            double liquidityScore = getDoubleValue(opportunity, "getLiquidityScore", "getLiquidity", 0.5);
            double volatility = getDoubleValue(opportunity, "getVolatility", null, 0.3);
            double buyFeePercentage = getDoubleValue(opportunity, "getBuyFeePercentage", null, 0.1);
            double sellFeePercentage = getDoubleValue(opportunity, "getSellFeePercentage", null, 0.1);
            double slippage = getDoubleValue(opportunity, "getSlippage", null, 0.2);
            String buyExchange = getStringValue(opportunity, "getBuyExchange", "getExchangeBuy", "Unknown");
            String sellExchange = getStringValue(opportunity, "getSellExchange", "getExchangeSell", "Unknown");
            
            // Get ticker objects
            Ticker buyTicker = (Ticker) invokeMethod(opportunity, "getBuyTicker", null);
            Ticker sellTicker = (Ticker) invokeMethod(opportunity, "getSellTicker", null);
            
            // Set all metrics in the assessment
            assessment.setLiquidityScore(calculateLiquidityScore(liquidityScore, buyTicker, sellTicker));
            assessment.setVolatilityScore(normalizeVolatilityScore(volatility));
            assessment.setFeeImpact(calculateFeeImpact(buyFeePercentage, sellFeePercentage));
            assessment.setMarketDepthScore(liquidityScore);
            assessment.setSlippageRisk(slippage);
            
            // Calculate overall risk score
            double overallScore = calculateOverallRiskScore(assessment);
            assessment.setOverallRiskScore(overallScore);
            
            // Set risk level based on overall score
            assessment.setRiskLevel(determineRiskLevel(overallScore));
            
            // Set exchange information
            assessment.setExchangeBuy(buyExchange);
            assessment.setExchangeSell(sellExchange);
            
            // Set fee information
            assessment.setBuyFeePercentage(buyFeePercentage);
            assessment.setSellFeePercentage(sellFeePercentage);
            
        } catch (Exception e) {
            // In case of reflection errors, fall back to default assessment
            assessment = createDefaultRiskAssessment();
        }
        
        return assessment;
    }
    
    /**
     * Apply risk assessment to any ArbitrageOpportunity instance.
     * Uses reflection to detect available methods and apply values.
     * 
     * @param opportunity The opportunity to update
     * @param assessment The risk assessment to apply
     */
    public static void applyRiskAssessment(Object opportunity, RiskAssessment assessment) {
        if (opportunity == null || assessment == null) {
            return;
        }
        
        try {
            // Set risk assessment using reflection
            invokeMethod(opportunity, "setRiskAssessment", assessment);
            
            // Set individual risk metrics based on available setters
            tryInvokeMethod(opportunity, "setRiskScore", assessment.getOverallRiskScore());
            tryInvokeMethod(opportunity, "setLiquidityScore", assessment.getLiquidityScore());
            tryInvokeMethod(opportunity, "setLiquidity", assessment.getLiquidityScore());
            tryInvokeMethod(opportunity, "setVolatility", assessment.getVolatilityScore());
            tryInvokeMethod(opportunity, "setVolatilityScore", assessment.getVolatilityScore());
            tryInvokeMethod(opportunity, "setSlippage", assessment.getSlippageRisk());
            tryInvokeMethod(opportunity, "setSlippageRisk", assessment.getSlippageRisk());
            
            // Set viability flag
            tryInvokeMethod(opportunity, "setViable", isViable(assessment));
            
        } catch (Exception e) {
            // Silently fail if methods aren't available
        }
    }
    
    /**
     * Helper method to get a double value from an object using reflection.
     * Tries the primary method name first, then falls back to the alternative method name.
     * 
     * @param object The object to invoke the method on
     * @param primaryMethodName The primary method name to try
     * @param alternativeMethodName The alternative method name to try if primary fails
     * @param defaultValue The default value to return if both methods fail
     * @return The double value from the object or the default value
     */
    private static double getDoubleValue(Object object, String primaryMethodName, 
                                        String alternativeMethodName, double defaultValue) {
        try {
            Object result = invokeMethod(object, primaryMethodName, null);
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
        } catch (Exception e) {
            // Try alternative method name if provided
            if (alternativeMethodName != null) {
                try {
                    Object result = invokeMethod(object, alternativeMethodName, null);
                    if (result instanceof Number) {
                        return ((Number) result).doubleValue();
                    }
                } catch (Exception ex) {
                    // Fall back to default
                }
            }
        }
        return defaultValue;
    }
    
    /**
     * Helper method to get a string value from an object using reflection.
     * Tries the primary method name first, then falls back to the alternative method name.
     * 
     * @param object The object to invoke the method on
     * @param primaryMethodName The primary method name to try
     * @param alternativeMethodName The alternative method name to try if primary fails
     * @param defaultValue The default value to return if both methods fail
     * @return The string value from the object or the default value
     */
    private static String getStringValue(Object object, String primaryMethodName, 
                                        String alternativeMethodName, String defaultValue) {
        try {
            Object result = invokeMethod(object, primaryMethodName, null);
            if (result instanceof String) {
                return (String) result;
            }
        } catch (Exception e) {
            // Try alternative method name if provided
            if (alternativeMethodName != null) {
                try {
                    Object result = invokeMethod(object, alternativeMethodName, null);
                    if (result instanceof String) {
                        return (String) result;
                    }
                } catch (Exception ex) {
                    // Fall back to default
                }
            }
        }
        return defaultValue;
    }
    
    /**
     * Invoke a method on an object using reflection.
     * 
     * @param object The object to invoke the method on
     * @param methodName The name of the method to invoke
     * @param parameter The parameter to pass to the method (or null for no parameter)
     * @return The result of the method invocation
     * @throws Exception If the method cannot be found or invoked
     */
    private static Object invokeMethod(Object object, String methodName, Object parameter) throws Exception {
        Class<?> clazz = object.getClass();
        Method method;
        
        if (parameter == null) {
            // No-parameter method
            method = clazz.getMethod(methodName);
            return method.invoke(object);
        } else {
            // Find the right method based on parameter type
            Method[] methods = clazz.getMethods();
            for (Method m : methods) {
                if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                    Class<?>[] paramTypes = m.getParameterTypes();
                    if (paramTypes[0].isAssignableFrom(parameter.getClass())) {
                        return m.invoke(object, parameter);
                    }
                }
            }
            throw new NoSuchMethodException("No suitable method found: " + methodName);
        }
    }
    
    /**
     * Try to invoke a method without throwing exceptions.
     * 
     * @param object The object to invoke the method on
     * @param methodName The name of the method to invoke
     * @param parameter The parameter to pass to the method
     */
    private static void tryInvokeMethod(Object object, String methodName, Object parameter) {
        try {
            invokeMethod(object, methodName, parameter);
        } catch (Exception e) {
            // Silently ignore if method doesn't exist
        }
    }
    
    /**
     * Checks if an opportunity is viable based on its risk assessment
     * 
     * @param assessment The risk assessment to evaluate
     * @return true if the opportunity is considered viable
     */
    public static boolean isViable(RiskAssessment assessment) {
        if (assessment == null) {
            return false;
        }
        
        // An opportunity is viable if its overall risk score is above the medium risk threshold
        // and no early warnings have been triggered
        return assessment.getOverallRiskScore() >= MEDIUM_RISK_THRESHOLD && 
               !assessment.isEarlyWarningTriggered();
    }
    
    /**
     * Creates a default risk assessment with moderate values
     * 
     * @return A default risk assessment
     */
    private static RiskAssessment createDefaultRiskAssessment() {
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
     * Calculate the liquidity score based on available data
     * 
     * @param baseScore Base liquidity score
     * @param buyTicker Buy side ticker
     * @param sellTicker Sell side ticker
     * @return The calculated liquidity score
     */
    private static double calculateLiquidityScore(double baseScore, Ticker buyTicker, Ticker sellTicker) {
        // If we don't have ticker data, return the base score
        if (buyTicker == null || sellTicker == null) {
            return Math.max(0.3, Math.min(baseScore, 1.0));
        }
        
        // Calculate liquidity based on ticker data
        double buyVolume = buyTicker.getVolume();
        double sellVolume = sellTicker.getVolume();
        
        // Normalize volumes and calculate average
        double normalizedBuyVolume = normalizeVolume(buyVolume);
        double normalizedSellVolume = normalizeVolume(sellVolume);
        double averageVolume = (normalizedBuyVolume + normalizedSellVolume) / 2.0;
        
        // Combine with base score (giving 70% weight to ticker volume data)
        double liquidityScore = 0.3 * baseScore + 0.7 * averageVolume;
        
        // Ensure the result is between 0 and 1
        return Math.max(0.0, Math.min(liquidityScore, 1.0));
    }
    
    /**
     * Normalize a volume value to a score between 0 and 1
     * 
     * @param volume The volume to normalize
     * @return A normalized score between 0 and 1
     */
    private static double normalizeVolume(double volume) {
        // Apply a logarithmic scale to handle wide range of volumes
        if (volume <= 0) {
            return 0.0;
        }
        
        double logVolume = Math.log10(volume);
        
        // Assuming typical volumes are between 1 and 10^8
        // Normalize to 0-1 range
        return Math.min(logVolume / 8.0, 1.0);
    }
    
    /**
     * Normalize a volatility value to a score between 0 and 1
     * 
     * @param volatility The raw volatility value
     * @return A normalized score between 0 and 1 (higher is better/less volatile)
     */
    private static double normalizeVolatilityScore(double volatility) {
        // Invert the volatility so higher scores mean lower volatility (less risk)
        if (volatility <= 0) {
            return 1.0; // No volatility is best
        }
        
        if (volatility >= 1.0) {
            return 0.1; // Very high volatility, but still give a small non-zero score
        }
        
        return 1.0 - volatility;
    }
    
    /**
     * Calculate the impact of fees on profitability
     * 
     * @param buyFee Buy side fee percentage
     * @param sellFee Sell side fee percentage
     * @return A score between 0 and 1 representing fee impact (higher is better/lower fees)
     */
    private static double calculateFeeImpact(double buyFee, double sellFee) {
        double totalFee = buyFee + sellFee;
        
        // Higher score means lower fees (less risk)
        if (totalFee >= 0.01) { // 1% or higher fees
            return 0.2; // Very high fee impact
        }
        
        if (totalFee <= 0.0005) { // 0.05% or lower fees
            return 1.0; // Very low fee impact
        }
        
        // Linear scale between 0.05% and 1%
        return 1.0 - ((totalFee - 0.0005) / 0.0095 * 0.8);
    }
    
    /**
     * Calculate the overall risk score based on individual risk factors
     * 
     * @param assessment The risk assessment with individual scores
     * @return The overall risk score between 0 and 1
     */
    private static double calculateOverallRiskScore(RiskAssessment assessment) {
        double liquidityScore = assessment.getLiquidityScore();
        double volatilityScore = assessment.getVolatilityScore();
        double feeImpact = assessment.getFeeImpact();
        double depthScore = assessment.getMarketDepthScore();
        double executionRisk = 1.0 - assessment.getSlippageRisk(); // Invert slippage risk
        
        // Weighted average of all risk factors
        double weightedScore = 
            liquidityScore * DEFAULT_LIQUIDITY_WEIGHT +
            volatilityScore * DEFAULT_VOLATILITY_WEIGHT +
            feeImpact * DEFAULT_FEE_WEIGHT +
            depthScore * DEFAULT_DEPTH_WEIGHT +
            executionRisk * DEFAULT_EXECUTION_WEIGHT;
        
        // Ensure the result is between 0 and 1
        return Math.max(0.0, Math.min(weightedScore, 1.0));
    }
    
    /**
     * Determine the risk level based on the overall risk score
     * 
     * @param overallScore The overall risk score
     * @return The risk level as a string ("HIGH", "MEDIUM", or "LOW")
     */
    private static String determineRiskLevel(double overallScore) {
        if (overallScore < HIGH_RISK_THRESHOLD) {
            return "HIGH";
        } else if (overallScore < MEDIUM_RISK_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
} 