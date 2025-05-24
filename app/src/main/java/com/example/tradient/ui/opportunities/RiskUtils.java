package com.example.tradient.ui.opportunities;

import android.util.Log;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.domain.risk.RiskCalculator;
import com.example.tradient.domain.risk.RiskCalculationService;

/**
 * Utility class for risk-related functionality.
 * Provides consistent risk calculation and interpretation.
 */
public class RiskUtils {
    private static final String TAG = "RiskUtils";

    // Risk level definitions
    public static class RiskLevel {
        public final String name;
        public final double minValue;
        public final double maxValue;

        public RiskLevel(String name, double minValue, double maxValue) {
            this.name = name;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        public boolean contains(double value) {
            return value >= minValue && value <= maxValue;
        }
    }

    // Risk levels from lowest risk to highest
    public static final RiskLevel[] RISK_LEVELS = {
        new RiskLevel("Minimal", 0.9, 1.0),
        new RiskLevel("Very Low", 0.8, 0.89),
        new RiskLevel("Low", 0.7, 0.79),
        new RiskLevel("Low-Medium", 0.6, 0.69),
        new RiskLevel("Medium", 0.5, 0.59),
        new RiskLevel("Medium-High", 0.4, 0.49),
        new RiskLevel("High", 0.3, 0.39),
        new RiskLevel("Very High", 0.2, 0.29),
        new RiskLevel("Extreme", 0.1, 0.19),
        new RiskLevel("Critical", 0.0, 0.09)
    };

    /**
     * Gets the risk score from an opportunity consistently
     * Risk scores are on a 0-1 scale where:
     * - 0.0 represents maximum risk (worst)
     * - 1.0 represents minimum risk (best)
     */
    public static double getRiskScore(ArbitrageOpportunity opportunity) {
        try {
            if (opportunity == null) {
                Log.w(TAG, "Null opportunity provided to getRiskScore");
                return 0.5; // Default to medium risk, but log warning
            }
            
            // Use the more robust ArbitrageCardModel for consistent risk calculation
            // Force recalculation to ensure we get the most accurate score
            com.example.tradient.data.model.ArbitrageCardModel cardModel = 
                com.example.tradient.data.model.ArbitrageCardModel.fromOpportunity(opportunity, true);
            
            if (cardModel != null) {
                double riskScore = cardModel.getRiskScore();
                Log.d(TAG, String.format("Risk for %s: %.2f (%s) via ArbitrageCardModel", 
                    opportunity.getNormalizedSymbol(), riskScore, cardModel.getRiskLevel()));
                return riskScore;
            }
            
            // If card model creation failed, use older approaches as fallback
            Log.w(TAG, "Failed to create card model for " + opportunity.getNormalizedSymbol() + 
                ", falling back to legacy method");
                
            // First try to get the already calculated risk score directly from the opportunity field
            // This is the legacy approach but can be more efficient in some cases
            double directRiskScore = opportunity.getRiskScore();
            if (directRiskScore > 0) {
                Log.d(TAG, "Using pre-calculated risk score: " + directRiskScore);
                return Math.max(0.0, Math.min(1.0, directRiskScore));
            }
            
            // Next try to get the risk assessment from the opportunity
            RiskAssessment assessment = opportunity.getRiskAssessment();
            
            // If no assessment exists yet, calculate a new one
            if (assessment == null) {
                // First check if we have the necessary ticker data
                Ticker buyTicker = opportunity.getBuyTicker();
                Ticker sellTicker = opportunity.getSellTicker();
                
                if (buyTicker == null || sellTicker == null) {
                    Log.w(TAG, "Missing ticker data for " + opportunity.getNormalizedSymbol() + 
                        ", cannot calculate accurate risk");
                    
                    // Calculate a basic risk score from what we have
                    double profitRiskFactor = Math.min(1.0, opportunity.getProfitPercent() / 10.0);
                    double exchangeRiskFactor = calculateExchangeRiskFactor(
                        opportunity.getExchangeBuy(), opportunity.getExchangeSell());
                    
                    // Combine factors with weightings
                    double combinedRiskScore = (profitRiskFactor * 0.7) + (exchangeRiskFactor * 0.3);
                    Log.d(TAG, "Calculated basic risk score for " + opportunity.getNormalizedSymbol() + 
                        ": " + combinedRiskScore);
                    
                    return combinedRiskScore;
                }
                
                // Check if we can use basic risk calculation instead of full assessment
                if (!isValidTickerData(buyTicker) || !isValidTickerData(sellTicker)) {
                    Log.w(TAG, "Ticker data is incomplete for " + opportunity.getNormalizedSymbol() + 
                        ", using basic risk calculation");
                    double basicRiskScore = calculateBasicRiskScore(buyTicker, sellTicker);
                    
                    // Blend with profit and exchange factors for better accuracy
                    double profitFactor = Math.min(1.0, opportunity.getProfitPercent() / 10.0);
                    double exchangeFactor = calculateExchangeRiskFactor(
                        opportunity.getExchangeBuy(), opportunity.getExchangeSell());
                    
                    // Calculate a weighted score
                    double finalScore = (basicRiskScore * 0.5) + (profitFactor * 0.3) + (exchangeFactor * 0.2);
                    Log.d(TAG, "Using blended basic risk score: " + finalScore);
                    return finalScore;
                }
                
                // Log that we're calculating a new assessment
                Log.d(TAG, "Calculating new risk assessment for " + opportunity.getNormalizedSymbol());
                
                try {
                    // Use the proper RiskCalculationService for accurate risk assessment
                    // Get the dependency from application context or service locator
                    RiskCalculationService riskService = new RiskCalculationService();
                    
                    // Extract fees or use default fees since opportunity doesn't have fee methods
                    double buyFees = 0.001;  // Default 0.1% fee
                    double sellFees = 0.001; // Default 0.1% fee
                    
                    // Try to extract fee information from exchange names if available
                    String buyExchange = opportunity.getExchangeBuy();
                    String sellExchange = opportunity.getExchangeSell();
                    
                    if (buyExchange != null) {
                        buyFees = getExchangeFee(buyExchange);
                    }
                    
                    if (sellExchange != null) {
                        sellFees = getExchangeFee(sellExchange);
                    }
                    
                    // Use the sophisticated risk calculation service with the correct parameters
                    assessment = riskService.calculateRiskAssessment(
                        buyTicker, 
                        sellTicker, 
                        buyFees, 
                        sellFees
                    );
                    
                    Log.d(TAG, "Successfully calculated risk assessment using RiskCalculationService");
                } catch (Exception e) {
                    Log.e(TAG, "Error using RiskCalculationService: " + e.getMessage(), e);
                    
                    // Fallback to the old RiskCalculator if the service fails
                    Log.d(TAG, "Falling back to basic RiskCalculator");
                    RiskCalculator riskCalculator = new RiskCalculator();
                    assessment = riskCalculator.assessRisk(opportunity);
                }
                
                if (assessment == null) {
                    Log.w(TAG, "Risk assessment calculation failed for " + 
                        opportunity.getNormalizedSymbol());
                    
                    // Calculate a simpler risk score
                    double profitRiskFactor = Math.min(1.0, opportunity.getProfitPercent() / 5.0);
                    Log.d(TAG, "Using profit-based risk score: " + profitRiskFactor);
                    return profitRiskFactor;
                }
                
                // Store the assessment directly in the opportunity
                opportunity.setRiskAssessment(assessment);
            }
            
            // Get the risk score from the assessment (0-1 scale)
            double riskScore = assessment.getOverallRiskScore();
            
            // Check for invalid values
            if (Double.isNaN(riskScore) || riskScore < 0.0 || riskScore > 1.0) {
                Log.w(TAG, "Invalid risk score for " + 
                    opportunity.getNormalizedSymbol() + ": " + riskScore);
                
                // Try to use individual risk components if available
                double liquidityScore = assessment.getLiquidityScore();
                double volatilityScore = assessment.getVolatilityScore();
                if (liquidityScore > 0 || volatilityScore > 0) {
                    // Use the average of available scores
                    double availableScores = 0;
                    double scoreSum = 0;
                    
                    if (liquidityScore > 0) {
                        scoreSum += liquidityScore;
                        availableScores++;
                    }
                    
                    if (volatilityScore > 0) {
                        scoreSum += volatilityScore;
                        availableScores++;
                    }
                    
                    if (availableScores > 0) {
                        riskScore = scoreSum / availableScores;
                        Log.d(TAG, "Using average of individual scores: " + riskScore);
                        return riskScore;
                    }
                }
                
                // Last resort: use profit as a rough approximation
                double profitPercent = opportunity.getProfitPercent();
                riskScore = Math.min(0.9, profitPercent / 10.0);
                Log.d(TAG, "Using profit-based risk score as fallback: " + riskScore);
            }
            
            // Log the final risk score
            Log.d(TAG, String.format("Final risk score for %s: %.2f (%s)",
                opportunity.getNormalizedSymbol(), riskScore, getRiskLevelName(riskScore)));
            
            return riskScore;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating risk score: " + e.getMessage(), e);
            
            // Try to recover by using available opportunity data
            if (opportunity != null) {
                double profitPercent = opportunity.getProfitPercent();
                if (profitPercent > 0) {
                    // Higher profit generally indicates lower risk, use a scaling factor
                    double profitBasedRisk = Math.min(0.8, profitPercent / 10.0);
                    Log.d(TAG, "Using emergency profit-based risk: " + profitBasedRisk);
                    return profitBasedRisk;
                }
            }
            
            return 0.5; // Default to medium risk on error
        }
    }
    
    /**
     * Calculates a risk factor based on the exchanges involved
     */
    private static double calculateExchangeRiskFactor(String buyExchange, String sellExchange) {
        double buyFactor = getExchangeRiskFactor(buyExchange);
        double sellFactor = getExchangeRiskFactor(sellExchange);
        return (buyFactor + sellFactor) / 2.0;
    }
    
    /**
     * Gets a risk factor for a specific exchange
     */
    private static double getExchangeRiskFactor(String exchange) {
        if (exchange == null) return 0.3;
        
        switch (exchange.toLowerCase()) {
            case "binance": return 0.9;  // Most reliable
            case "coinbase": return 0.85;
            case "kraken": return 0.8;
            case "kucoin": return 0.75;
            case "bybit": return 0.7;
            case "okx": return 0.65;
            case "gemini": return 0.7;
            case "bitfinex": return 0.6;
            default: return 0.5;
        }
    }

    /**
     * Gets a descriptive risk level name based on a risk score
     */
    public static String getRiskLevelName(double riskScore) {
        // Ensure risk score is in valid range
        riskScore = Math.max(0.0, Math.min(1.0, riskScore));
        
        // Find the corresponding risk level
        for (RiskLevel level : RISK_LEVELS) {
            if (level.contains(riskScore)) {
                return level.name;
            }
        }
        
        // Default fallback if no level matched (shouldn't happen with proper ranges)
        return "Medium";
    }

    /**
     * Checks if ticker data is valid for risk calculations
     */
    private static boolean isValidTickerData(Ticker ticker) {
        if (ticker == null) return false;
        
        // Check essential price data
        if (ticker.getLastPrice() <= 0 ||
            ticker.getBidPrice() <= 0 ||
            ticker.getAskPrice() <= 0) {
            return false;
        }
        
        // Check volume data
        if (ticker.getVolume() <= 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate a basic risk score from ticker data
     */
    public static double calculateBasicRiskScore(Ticker buyTicker, Ticker sellTicker) {
        double riskScore = 0.5; // Start with medium risk
        
        if (!isValidTickerData(buyTicker) || !isValidTickerData(sellTicker)) {
            return riskScore;
        }
        
        try {
            // Calculate price stability (lower spread = more stable = less risky)
            double buySpread = (buyTicker.getAskPrice() - buyTicker.getBidPrice()) / buyTicker.getLastPrice();
            double sellSpread = (sellTicker.getAskPrice() - sellTicker.getBidPrice()) / sellTicker.getLastPrice();
            double averageSpread = (buySpread + sellSpread) / 2.0;
            double spreadRiskFactor = 1.0 - Math.min(1.0, averageSpread * 100.0);
            
            // Calculate volume factor (higher volume = more liquidity = less risky)
            double buyVolume = buyTicker.getVolume();
            double sellVolume = sellTicker.getVolume();
            double minVolume = Math.min(buyVolume, sellVolume);
            double volumeFactor = Math.min(1.0, minVolume / 1000000.0); // Normalize to 1M volume
            
            // Calculate final risk score
            riskScore = (spreadRiskFactor * 0.6) + (volumeFactor * 0.4);
            
            // Ensure score is in valid range
            riskScore = Math.max(0.1, Math.min(0.9, riskScore));
            
            return riskScore;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating basic risk score: " + e.getMessage());
            return 0.5;
        }
    }

    /**
     * Gets fee rate for a specific exchange
     */
    private static double getExchangeFee(String exchange) {
        if (exchange == null) return 0.001; // Default 0.1%
        
        switch (exchange.toLowerCase()) {
            case "binance": return 0.0004;  // 0.04% taker fee (spot)
            case "coinbase": return 0.0060; // 0.60% taker fee
            case "kraken": return 0.0026;   // 0.26% taker fee
            case "kucoin": return 0.0010;   // 0.10% taker fee
            case "bybit": return 0.0010;    // 0.10% taker fee
            case "okx": return 0.0010;      // 0.10% taker fee (reduced from 0.20%)
            case "gemini": return 0.0035;   // 0.35% taker fee
            case "bitfinex": return 0.0020; // 0.20% taker fee
            default: return 0.0010;         // Default 0.10%
        }
    }
} 