package com.example.tradient.domain.risk;

import android.graphics.Color;
import android.util.Log;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.util.RiskAssessmentAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified risk calculator that provides consistent risk calculation throughout the app.
 * SCALE INTERPRETATION: 0.0 = highest risk, 1.0 = lowest risk
 * This scale is used consistently throughout the application.
 */
public class UnifiedRiskCalculator {
    private static final String TAG = "UnifiedRiskCalculator";
    
    // Singleton instance
    private static UnifiedRiskCalculator instance;
    
    // Risk level display text
    private static final String RISK_LEVEL_MINIMAL = "Minimal Risk";
    private static final String RISK_LEVEL_VERY_LOW = "Very Low Risk";
    private static final String RISK_LEVEL_LOW = "Low Risk";
    private static final String RISK_LEVEL_LOW_MEDIUM = "Low-Medium Risk";
    private static final String RISK_LEVEL_MEDIUM = "Medium Risk";
    private static final String RISK_LEVEL_MEDIUM_HIGH = "Medium-High Risk";
    private static final String RISK_LEVEL_HIGH = "High Risk";
    private static final String RISK_LEVEL_VERY_HIGH = "Very High Risk";
    private static final String RISK_LEVEL_EXTREME = "Extreme Risk";
    private static final String RISK_LEVEL_CRITICAL = "Critical Risk";
    
    // Risk colors
    private static final int COLOR_RISK_MINIMAL = Color.parseColor("#00C853");      // Green
    private static final int COLOR_RISK_VERY_LOW = Color.parseColor("#64DD17");     // Light Green  
    private static final int COLOR_RISK_LOW = Color.parseColor("#AEEA00");          // Lime
    private static final int COLOR_RISK_LOW_MEDIUM = Color.parseColor("#CDDC39");   // Lime/Yellow
    private static final int COLOR_RISK_MEDIUM = Color.parseColor("#FFEB3B");       // Yellow
    private static final int COLOR_RISK_MEDIUM_HIGH = Color.parseColor("#FFC107");  // Amber
    private static final int COLOR_RISK_HIGH = Color.parseColor("#FF9800");         // Orange
    private static final int COLOR_RISK_VERY_HIGH = Color.parseColor("#FF5722");    // Deep Orange
    private static final int COLOR_RISK_EXTREME = Color.parseColor("#F44336");      // Red
    private static final int COLOR_RISK_CRITICAL = Color.parseColor("#B71C1C");     // Dark Red
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private UnifiedRiskCalculator() {
        // Private constructor
    }
    
    /**
     * Get the singleton instance
     */
    public static synchronized UnifiedRiskCalculator getInstance() {
        if (instance == null) {
            instance = new UnifiedRiskCalculator();
        }
        return instance;
    }
    
    /**
     * Calculate risk assessment for an arbitrage opportunity
     * @param opportunity The opportunity to assess
     * @return A comprehensive risk assessment
     */
    public RiskAssessment calculateRisk(ArbitrageOpportunity opportunity) {
        if (opportunity == null) {
            Log.e(TAG, "Cannot calculate risk for null opportunity");
            return createDefaultRiskAssessment();
        }
        
        try {
            Log.d(TAG, "Calculating risk for " + opportunity.getSymbol());
            
            // Check for suspiciously high profit first - likely to be too good to be true
            double profitPercent = opportunity.getProfitPercent();
            if (profitPercent > 3.5) {
                // Very high profit (> 3.5%) is suspicious in crypto arbitrage
                Log.w(TAG, "Suspiciously high profit detected: " + profitPercent + "% for " + opportunity.getSymbol());
                return RiskAssessment.createSuspiciouslyHighProfitState(profitPercent);
            }
            
            // Create a new risk assessment
            RiskAssessment assessment = new RiskAssessment();
            
            // Calculate liquidity risk (0.0-1.0, higher is better/less risky)
            double liquidityScore = calculateLiquidityScore(opportunity);
            assessment.setLiquidityScore(liquidityScore);
            
            // Calculate volatility risk (0.0-1.0, higher is better/less risky)
            double volatilityScore = calculateVolatilityScore(opportunity);
            assessment.setVolatilityScore(volatilityScore);
            
            // Calculate exchange risk (0.0-1.0, higher is better/less risky)
            double exchangeRiskScore = calculateExchangeRiskScore(opportunity);
            assessment.setExchangeRiskScore(exchangeRiskScore);
            
            // Calculate transaction risk (0.0-1.0, higher is better/less risky)
            double transactionRiskScore = calculateTransactionRiskScore(opportunity);
            assessment.setTransactionRiskScore(transactionRiskScore);
            
            // Calculate slippage estimate (0.0-1.0, decimal percentage)
            double slippageEstimate = calculateSlippageEstimate(opportunity);
            assessment.setSlippageEstimate(slippageEstimate);
            
            // Calculate execution time estimate (in minutes)
            double executionTimeEstimate = calculateExecutionTimeEstimate(opportunity);
            assessment.setExecutionTimeEstimate(executionTimeEstimate);
            
            // Calculate optimal trade size
            double optimalTradeSize = calculateOptimalTradeSize(opportunity);
            assessment.setOptimalTradeSize(optimalTradeSize);
            
            // Calculate ROI efficiency (profit per hour)
            double roiEfficiency = calculateRoiEfficiency(opportunity, executionTimeEstimate);
            assessment.setRoiEfficiency(roiEfficiency);
            
            // Calculate overall risk score (0.0-1.0, higher is better/less risky)
            // Use profit-aware version of risk calculation
            double overallRiskScore = calculateOverallRiskScoreWithProfit(
                liquidityScore, volatilityScore, exchangeRiskScore, transactionRiskScore, profitPercent);
            assessment.setOverallRiskScore(overallRiskScore);
            
            // Store buy/sell fee percentages
            assessment.setBuyFeePercentage(opportunity.getBuyFeePercentage());
            assessment.setSellFeePercentage(opportunity.getSellFeePercentage());
            
            // Set other fields as needed
            
            Log.d(TAG, String.format(
                "Risk assessment complete - Overall: %.2f, Liquidity: %.2f, Volatility: %.2f",
                overallRiskScore, liquidityScore, volatilityScore));
            
            return assessment;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating risk assessment: " + e.getMessage(), e);
            return createDefaultRiskAssessment();
        }
    }
    
    /**
     * Apply a risk assessment to an opportunity.
     * This ensures that the risk assessment is properly reflected in all object fields.
     *
     * @param opportunity The opportunity to update
     * @param assessment The risk assessment to apply
     */
    public void applyRiskAssessment(ArbitrageOpportunity opportunity, RiskAssessment assessment) {
        if (opportunity == null || assessment == null) {
            return;
        }
        
        try {
            // Store the risk assessment properly first
            opportunity.setRiskAssessment(assessment);
            
            // Then ensure all risk-related fields are updated directly from assessment values
            // This prevents discrepancies between the stored assessment and field values
            
            // Set the risk scores
            opportunity.setRiskScore(assessment.getOverallRiskScore());
            opportunity.setLiquidity(assessment.getLiquidityScore());
            opportunity.setVolatility(assessment.getVolatilityScore());
            
            // Set slippage
            opportunity.setSlippage(assessment.getSlippageEstimate());
            
            // Set execution time estimate
            opportunity.setEstimatedTimeMinutes(assessment.getExecutionTimeEstimate());
            
            // Set other related fields
            opportunity.setBuyFeePercentage(assessment.getBuyFeePercentage());
            opportunity.setSellFeePercentage(assessment.getSellFeePercentage());
            
            Log.d(TAG, String.format(
                "Applied risk assessment to opportunity %s - Risk: %.2f, Liquidity: %.2f, Volatility: %.2f",
                opportunity.getSymbol(),
                assessment.getOverallRiskScore(),
                assessment.getLiquidityScore(),
                assessment.getVolatilityScore()));
                
        } catch (Exception e) {
            Log.e(TAG, "Error applying risk assessment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get the appropriate risk level text for a risk score
     * @param riskScore Risk score (0.0 = highest risk, 1.0 = lowest risk)
     * @return Human-readable risk level
     */
    public String getRiskLevelText(double riskScore) {
        // Ensure the score is in the valid range
        riskScore = Math.max(0.0, Math.min(1.0, riskScore));
        
        // Map score to risk level text
        if (riskScore >= 0.95) return RISK_LEVEL_MINIMAL;
        if (riskScore >= 0.85) return RISK_LEVEL_VERY_LOW;
        if (riskScore >= 0.75) return RISK_LEVEL_LOW;
        if (riskScore >= 0.65) return RISK_LEVEL_LOW_MEDIUM;
        if (riskScore >= 0.55) return RISK_LEVEL_MEDIUM;
        if (riskScore >= 0.45) return RISK_LEVEL_MEDIUM_HIGH;
        if (riskScore >= 0.35) return RISK_LEVEL_HIGH;
        if (riskScore >= 0.25) return RISK_LEVEL_VERY_HIGH;
        if (riskScore >= 0.15) return RISK_LEVEL_EXTREME;
        return RISK_LEVEL_CRITICAL;
    }
    
    /**
     * Get the appropriate risk color for a risk score
     * @param riskScore Risk score (0.0 = highest risk, 1.0 = lowest risk)
     * @return Color value for the risk level
     */
    public int getRiskColor(double riskScore) {
        // Ensure the score is in the valid range
        riskScore = Math.max(0.0, Math.min(1.0, riskScore));
        
        // Map score to risk color
        if (riskScore >= 0.95) return COLOR_RISK_MINIMAL;
        if (riskScore >= 0.85) return COLOR_RISK_VERY_LOW;
        if (riskScore >= 0.75) return COLOR_RISK_LOW;
        if (riskScore >= 0.65) return COLOR_RISK_LOW_MEDIUM;
        if (riskScore >= 0.55) return COLOR_RISK_MEDIUM;
        if (riskScore >= 0.45) return COLOR_RISK_MEDIUM_HIGH;
        if (riskScore >= 0.35) return COLOR_RISK_HIGH;
        if (riskScore >= 0.25) return COLOR_RISK_VERY_HIGH;
        if (riskScore >= 0.15) return COLOR_RISK_EXTREME;
        return COLOR_RISK_CRITICAL;
    }
    
    /**
     * Create a default risk assessment for when calculation fails
     */
    private RiskAssessment createDefaultRiskAssessment() {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setOverallRiskScore(0.5); // Medium risk
        assessment.setLiquidityScore(0.5);
        assessment.setVolatilityScore(0.5);
        assessment.setExchangeRiskScore(0.5);
        assessment.setTransactionRiskScore(0.5);
        assessment.setSlippageEstimate(0.01); // 1% slippage
        assessment.setExecutionTimeEstimate(3.0); // 3 minutes
        assessment.setRoiEfficiency(0.01); // 1% per hour
        assessment.setOptimalTradeSize(500.0); // $500
        return assessment;
    }
    
    // Risk calculation methods
    
    private double calculateLiquidityScore(ArbitrageOpportunity opportunity) {
        try {
            // Get volume data from tickers
            Ticker buyTicker = opportunity.getBuyTicker();
            Ticker sellTicker = opportunity.getSellTicker();
            
            double buyVolume = (buyTicker != null) ? buyTicker.getVolume() : 0;
            double sellVolume = (sellTicker != null) ? sellTicker.getVolume() : 0;
            
            // Calculate average volume (with safety checks)
            double avgVolume = (buyVolume + sellVolume) / 2.0;
            
            // If volume data is missing, fall back to exchange liquidity estimates
            if (avgVolume <= 0) {
                double buyLiquidity = opportunity.getBuyExchangeLiquidity();
                double sellLiquidity = opportunity.getSellExchangeLiquidity();
                return (buyLiquidity + sellLiquidity) / 2.0;
            }
            
            // Map volume to a score between 0 and 1
            // Higher volume = higher score (better liquidity)
            double score;
            
            if (avgVolume < 10000) {
                // Very low volume
                score = Math.max(0.1, avgVolume / 10000.0);
            } else if (avgVolume < 100000) {
                // Low volume
                score = 0.1 + (avgVolume - 10000) / 90000.0 * 0.2;
            } else if (avgVolume < 1000000) {
                // Medium volume
                score = 0.3 + (avgVolume - 100000) / 900000.0 * 0.3;
            } else if (avgVolume < 10000000) {
                // High volume
                score = 0.6 + (avgVolume - 1000000) / 9000000.0 * 0.3;
            } else {
                // Very high volume
                score = 0.9 + Math.min(0.1, (avgVolume - 10000000) / 90000000.0);
            }
            
            // Ensure the score is in the valid range
            return Math.max(0.0, Math.min(1.0, score));
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating liquidity score: " + e.getMessage(), e);
            return 0.5; // Default medium liquidity
        }
    }
    
    private double calculateVolatilityScore(ArbitrageOpportunity opportunity) {
        try {
            // Get price data
            double buyPrice = opportunity.getBuyPrice();
            double sellPrice = opportunity.getSellPrice();
            
            // Calculate price difference percentage
            double priceDiffPercent = Math.abs(sellPrice - buyPrice) / buyPrice;
            
            // Calculate volatility score - lower price difference gets higher score
            // because it's less risky
            double score;
            
            if (priceDiffPercent < 0.001) {
                // Very small price difference (< 0.1%)
                score = 0.9;
            } else if (priceDiffPercent < 0.005) {
                // Small price difference (0.1% - 0.5%)
                score = 0.9 - (priceDiffPercent - 0.001) / 0.004 * 0.1;
            } else if (priceDiffPercent < 0.01) {
                // Moderate price difference (0.5% - 1%)
                score = 0.8 - (priceDiffPercent - 0.005) / 0.005 * 0.1;
            } else if (priceDiffPercent < 0.02) {
                // Significant price difference (1% - 2%)
                score = 0.7 - (priceDiffPercent - 0.01) / 0.01 * 0.1;
            } else if (priceDiffPercent < 0.05) {
                // Large price difference (2% - 5%)
                score = 0.6 - (priceDiffPercent - 0.02) / 0.03 * 0.1;
            } else {
                // Very large price difference (> 5%)
                score = Math.max(0.1, 0.5 - (priceDiffPercent - 0.05) / 0.15);
            }
            
            // Check for high profit margins that may indicate manipulation
            double profitPercent = opportunity.getProfitPercent();
            
            // Much more aggressive penalties for high profits
            if (profitPercent > 3.0) {
                // Very suspicious profit levels (> 3%)
                double excessProfit = (profitPercent - 3.0) / 3.0;
                score = Math.max(0.1, score - excessProfit * 0.7); // 70% penalty for excess profit
            } else if (profitPercent > 2.0) {
                // Moderately suspicious profit levels (2-3%)
                double excessProfit = (profitPercent - 2.0);
                score = Math.max(0.2, score - excessProfit * 0.4); // 40% penalty per % point
            } else if (profitPercent > 1.5) {
                // Slightly suspicious profit levels (1.5-2%)
                double excessProfit = (profitPercent - 1.5) * 2;
                score = Math.max(0.3, score - excessProfit * 0.2); // 20% penalty per % point
            }
            
            // Ensure the score is in the valid range
            return Math.max(0.0, Math.min(1.0, score));
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating volatility score: " + e.getMessage(), e);
            return 0.5; // Default medium volatility
        }
    }
    
    private double calculateExchangeRiskScore(ArbitrageOpportunity opportunity) {
        try {
            // Get exchange names
            String buyExchange = opportunity.getBuyExchangeName();
            String sellExchange = opportunity.getSellExchangeName();
            
            // Get reliability factors for each exchange
            double buyReliability = getExchangeReliability(buyExchange);
            double sellReliability = getExchangeReliability(sellExchange);
            
            // Average the reliability factors
            double score = (buyReliability + sellReliability) / 2.0;
            
            // Add a small penalty for using the same exchange (less arbitrage opportunity)
            if (buyExchange != null && sellExchange != null && 
                buyExchange.equalsIgnoreCase(sellExchange)) {
                score = Math.max(0.0, score - 0.1);
            }
            
            // Ensure the score is in the valid range
            return Math.max(0.0, Math.min(1.0, score));
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating exchange risk score: " + e.getMessage(), e);
            return 0.5; // Default medium exchange risk
        }
    }
    
    private double calculateTransactionRiskScore(ArbitrageOpportunity opportunity) {
        try {
            // Calculate transaction risk based on fees and profit margin
            double buyFee = opportunity.getBuyFeePercentage() / 100.0; // Convert to decimal
            double sellFee = opportunity.getSellFeePercentage() / 100.0; // Convert to decimal
            double totalFees = buyFee + sellFee;
            
            // Get profit percentage
            double profitPercent = opportunity.getProfitPercent() / 100.0; // Convert to decimal
            
            // Calculate score based on ratio of profit to fees
            double profitToFeeRatio = (totalFees > 0) ? profitPercent / totalFees : 10.0;
            
            double score;
            
            if (profitToFeeRatio < 1.0) {
                // Fees exceed profit - very risky
                score = 0.1;
            } else if (profitToFeeRatio < 1.5) {
                // Fees are significant relative to profit
                score = 0.1 + (profitToFeeRatio - 1.0) / 0.5 * 0.2;
            } else if (profitToFeeRatio < 2.0) {
                // Fees are moderate relative to profit
                score = 0.3 + (profitToFeeRatio - 1.5) / 0.5 * 0.2;
            } else if (profitToFeeRatio < 3.0) {
                // Fees are low relative to profit
                score = 0.5 + (profitToFeeRatio - 2.0) / 1.0 * 0.2;
            } else if (profitToFeeRatio < 5.0) {
                // Fees are very low relative to profit
                score = 0.7 + (profitToFeeRatio - 3.0) / 2.0 * 0.2;
            } else {
                // Fees are negligible relative to profit
                score = 0.9;
            }
            
            // Ensure the score is in the valid range
            return Math.max(0.0, Math.min(1.0, score));
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating transaction risk score: " + e.getMessage(), e);
            return 0.5; // Default medium transaction risk
        }
    }
    
    private double calculateSlippageEstimate(ArbitrageOpportunity opportunity) {
        try {
            // Simplified slippage estimate based on liquidity and volatility
            double buyVolume = opportunity.getBuyTicker() != null ? opportunity.getBuyTicker().getVolume() : 0;
            double sellVolume = opportunity.getSellTicker() != null ? opportunity.getSellTicker().getVolume() : 0;
            
            // Default slippage percentage (1%)
            double baseSlippage = 0.01;
            
            // Adjust based on volume
            double avgVolume = (buyVolume + sellVolume) / 2.0;
            double volumeFactor = 1.0;
            
            if (avgVolume > 10000000) {
                volumeFactor = 0.5; // Reduce slippage for high volume
            } else if (avgVolume > 1000000) {
                volumeFactor = 0.7;
            } else if (avgVolume > 100000) {
                volumeFactor = 1.0;
            } else if (avgVolume > 10000) {
                volumeFactor = 1.3;
            } else {
                volumeFactor = 1.5; // Increase slippage for low volume
            }
            
            // Adjust based on exchange
            String buyExchange = opportunity.getBuyExchangeName();
            String sellExchange = opportunity.getSellExchangeName();
            double exchangeFactor = 1.0;
            
            if (isHighLiquidityExchange(buyExchange) && isHighLiquidityExchange(sellExchange)) {
                exchangeFactor = 0.8; // Reduce slippage for major exchanges
            } else if (isHighLiquidityExchange(buyExchange) || isHighLiquidityExchange(sellExchange)) {
                exchangeFactor = 0.9;
            }
            
            // Calculate final slippage estimate
            double slippage = baseSlippage * volumeFactor * exchangeFactor;
            
            // Ensure slippage is reasonable
            return Math.max(0.001, Math.min(0.05, slippage));
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating slippage estimate: " + e.getMessage(), e);
            return 0.01; // Default 1% slippage
        }
    }
    
    private double calculateExecutionTimeEstimate(ArbitrageOpportunity opportunity) {
        try {
            // Base execution time in minutes
            double baseTime = 3.0;
            
            // Get exchange names
            String buyExchange = opportunity.getBuyExchangeName();
            String sellExchange = opportunity.getSellExchangeName();
            
            // Adjust time based on exchanges
            double exchangeFactor = 1.0;
            
            // Different exchanges have different execution speeds
            double buySpeed = getExchangeSpeed(buyExchange);
            double sellSpeed = getExchangeSpeed(sellExchange);
            double avgSpeed = (buySpeed + sellSpeed) / 2.0;
            
            // Faster exchanges (higher speed value) reduce execution time
            exchangeFactor = 2.0 - avgSpeed;
            
            // Calculate final execution time estimate
            double executionTime = baseTime * exchangeFactor;
            
            // Ensure execution time is reasonable
            return Math.max(1.0, Math.min(10.0, executionTime));
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating execution time estimate: " + e.getMessage(), e);
            return 3.0; // Default 3 minutes
        }
    }
    
    private double calculateOptimalTradeSize(ArbitrageOpportunity opportunity) {
        try {
            // Default trade size
            double baseSize = 500.0; // $500 USD
            
            // Get liquidity data
            double buyVolume = opportunity.getBuyTicker() != null ? opportunity.getBuyTicker().getVolume() : 0;
            double sellVolume = opportunity.getSellTicker() != null ? opportunity.getSellTicker().getVolume() : 0;
            double avgVolume = (buyVolume + sellVolume) / 2.0;
            
            // Adjust based on volume - higher volume allows larger trades
            double volumeFactor = 1.0;
            
            if (avgVolume > 10000000) {
                volumeFactor = 3.0; // Larger trades for high volume
            } else if (avgVolume > 1000000) {
                volumeFactor = 2.0;
            } else if (avgVolume > 100000) {
                volumeFactor = 1.0;
            } else if (avgVolume > 10000) {
                volumeFactor = 0.7;
            } else {
                volumeFactor = 0.5; // Smaller trades for low volume
            }
            
            // Adjust based on profit percentage - higher profits allow taking more risk
            double profitPercent = opportunity.getProfitPercent();
            double profitFactor = 1.0;
            
            if (profitPercent > 5.0) {
                profitFactor = 1.5;
            } else if (profitPercent > 2.0) {
                profitFactor = 1.2;
            } else if (profitPercent > 1.0) {
                profitFactor = 1.0;
            } else if (profitPercent > 0.5) {
                profitFactor = 0.8;
            } else {
                profitFactor = 0.6;
            }
            
            // Calculate final optimal trade size
            double optimalSize = baseSize * volumeFactor * profitFactor;
            
            // Ensure trade size is reasonable
            return Math.max(100.0, Math.min(2000.0, optimalSize));
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating optimal trade size: " + e.getMessage(), e);
            return 500.0; // Default $500 trade size
        }
    }
    
    private double calculateRoiEfficiency(ArbitrageOpportunity opportunity, double executionTimeMinutes) {
        try {
            // Get profit percentage
            double profitPercent = opportunity.getProfitPercent();
            
            // Calculate hourly ROI
            double hourlyROI;
            
            if (executionTimeMinutes > 0) {
                // Convert to hourly rate
                hourlyROI = (profitPercent / executionTimeMinutes) * 60.0;
            } else {
                // Default to profit percent if execution time is invalid
                hourlyROI = profitPercent * 20.0; // Assume 3 minutes
            }
            
            // Ensure ROI is reasonable
            return Math.max(0.0, Math.min(1000.0, hourlyROI));
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating ROI efficiency: " + e.getMessage(), e);
            return opportunity.getProfitPercent() * 20.0; // Default hourly ROI
        }
    }
    
    private double calculateOverallRiskScore(double liquidityScore, double volatilityScore, 
                                           double exchangeRiskScore, double transactionRiskScore) {
        try {
            // Weight the different risk components
            double liquidityWeight = 0.35;    // Liquidity is very important
            double volatilityWeight = 0.25;   // Volatility is important
            double exchangeWeight = 0.2;      // Exchange reliability matters
            double transactionWeight = 0.2;   // Transaction costs matter
            
            // Calculate weighted average
            double weightedScore = 
                (liquidityScore * liquidityWeight) +
                (volatilityScore * volatilityWeight) +
                (exchangeRiskScore * exchangeWeight) +
                (transactionRiskScore * transactionWeight);
            
            // Ensure the score is in the valid range
            return Math.max(0.0, Math.min(1.0, weightedScore));
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating overall risk score: " + e.getMessage(), e);
            return 0.5; // Default medium risk
        }
    }
    
    /**
     * Calculate overall risk score with profit percentage taken into account
     * Note: This version considers the profit percentage directly in the risk calculation
     * 
     * @param liquidityScore Liquidity risk score
     * @param volatilityScore Volatility risk score
     * @param exchangeRiskScore Exchange risk score
     * @param transactionRiskScore Transaction risk score
     * @param profitPercent Profit percentage
     * @return Overall risk score
     */
    private double calculateOverallRiskScoreWithProfit(double liquidityScore, double volatilityScore, 
                                          double exchangeRiskScore, double transactionRiskScore,
                                          double profitPercent) {
        try {
            // Weight the different risk components
            double liquidityWeight = 0.3;     // Slightly reduced weight
            double volatilityWeight = 0.2;    // Slightly reduced weight
            double exchangeWeight = 0.15;     // Slightly reduced weight
            double transactionWeight = 0.15;  // Slightly reduced weight
            double profitWeight = 0.2;        // New weight for profit factor
            
            // Calculate standard weighted average for standard risk factors
            double standardWeightedScore = 
                (liquidityScore * liquidityWeight) +
                (volatilityScore * volatilityWeight) +
                (exchangeRiskScore * exchangeWeight) +
                (transactionRiskScore * transactionWeight);
            
            // Calculate profit factor (inverse relationship - higher profit = higher risk)
            double profitFactor;
            if (profitPercent < 0.5) {
                profitFactor = 0.9;  // Low profit is generally safe
            } else if (profitPercent < 1.0) {
                profitFactor = 0.8;  // Moderate profit
            } else if (profitPercent < 1.5) {
                profitFactor = 0.7;  // Good profit but not suspicious
            } else if (profitPercent < 2.0) {
                profitFactor = 0.5;  // Starting to look suspicious
            } else if (profitPercent < 2.5) {
                profitFactor = 0.3;  // Suspicious profit level
            } else if (profitPercent < 3.0) {
                profitFactor = 0.2;  // Very suspicious profit level
            } else {
                profitFactor = 0.1;  // Extremely suspicious profit level
            }
            
            // Combine standard score with profit factor
            double finalScore = standardWeightedScore * (1 - profitWeight) + profitFactor * profitWeight;
            
            // Ensure the score is in the valid range
            return Math.max(0.0, Math.min(1.0, finalScore));
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating overall risk score with profit: " + e.getMessage(), e);
            return 0.5; // Default medium risk
        }
    }
    
    // Helper methods
    
    private double getExchangeReliability(String exchange) {
        if (exchange == null) return 0.5;
        
        switch (exchange.toLowerCase()) {
            case "binance": return 0.9;  // Most reliable
            case "coinbase": return 0.85;
            case "kraken": return 0.8;
            case "bybit": return 0.75;
            case "kucoin": return 0.75;
            case "okx": return 0.75;
            case "gemini": return 0.7;
            case "bitfinex": return 0.65;
            case "huobi": return 0.7;
            case "gateio": return 0.7;
            case "ftx": return 0.5;      // Deprecated
            default: return 0.5;         // Unknown exchange
        }
    }
    
    private double getExchangeSpeed(String exchange) {
        if (exchange == null) return 0.5;
        
        switch (exchange.toLowerCase()) {
            case "binance": return 0.9;  // Fastest
            case "okx": return 0.85;
            case "bybit": return 0.8;
            case "kucoin": return 0.75;
            case "huobi": return 0.7;
            case "gateio": return 0.7;
            case "kraken": return 0.65;
            case "coinbase": return 0.6;
            case "gemini": return 0.6;
            case "bitfinex": return 0.5;
            default: return 0.5;         // Unknown exchange
        }
    }
    
    private boolean isHighLiquidityExchange(String exchange) {
        if (exchange == null) return false;
        
        switch (exchange.toLowerCase()) {
            case "binance":
            case "coinbase":
            case "kraken":
            case "bybit":
            case "okx":
                return true;
            default:
                return false;
        }
    }
} 