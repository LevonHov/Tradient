package com.example.tradient.data.model;

import android.graphics.Color;
import android.util.Log;

import com.example.tradient.domain.risk.RiskCalculationService;
import com.example.tradient.domain.risk.UnifiedRiskCalculator;

import java.text.DecimalFormat;
import java.util.Date;

/**
 * A simplified model specifically designed for displaying arbitrage opportunities in the UI.
 * This model pre-calculates and stores all required values to avoid recalculation in the UI layer.
 */
public class ArbitrageCardModel {
    private static final String TAG = "ArbitrageCardModel";
    
    // Basic opportunity info
    private String opportunityId;
    private String tradingPair;
    private String buyExchange;
    private String sellExchange;
    private String buySymbol;
    private String sellSymbol;
    
    // Price information
    private double buyPrice;
    private double sellPrice;
    private double profitPercent;
    private double netProfitPercent; // After fees and slippage
    
    // Risk information (pre-calculated)
    private double riskScore;        // 0.0 (lowest risk) to 1.0 (highest risk)
    private String riskLevel;        // Textual description: Very Low, Low, Moderate, High, etc.
    private int riskColor;           // Color representing risk level
    
    // Fee and execution details
    private double buyFee;
    private double sellFee;
    private double estimatedSlippage;
    private double estimatedExecutionTimeMin;
    
    // Liquidity and market data
    private double buyVolume;
    private double sellVolume;
    private double buyExchangeLiquidity;
    private double sellExchangeLiquidity;
    
    // Timestamps and execution status
    private Date discoveryTime;
    private boolean isExecuted;
    private boolean isViable;
    
    // Formatters for display
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00######");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.00%");
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("0.0");

    /**
     * Creates a new ArbitrageCardModel from an ArbitrageOpportunity, calculating all needed values.
     */
    public static ArbitrageCardModel fromOpportunity(ArbitrageOpportunity opportunity) {
        return fromOpportunity(opportunity, false);
    }
    
    /**
     * Creates a new ArbitrageCardModel from an ArbitrageOpportunity, with option to force risk recalculation.
     * @param opportunity The opportunity to convert
     * @param forceRiskRecalculation Whether to force a fresh risk calculation even if an assessment exists
     * @return A new ArbitrageCardModel with pre-calculated values
     */
    public static ArbitrageCardModel fromOpportunity(ArbitrageOpportunity opportunity, boolean forceRiskRecalculation) {
        if (opportunity == null) {
            Log.e(TAG, "Cannot create model from null opportunity");
            return null;
        }
        
        ArbitrageCardModel model = new ArbitrageCardModel();
        
        try {
            // Set basic opportunity info
            model.opportunityId = opportunity.getOpportunityKey();
            model.tradingPair = opportunity.getNormalizedSymbol();
            model.buyExchange = opportunity.getExchangeBuy();
            model.sellExchange = opportunity.getExchangeSell();
            model.buySymbol = opportunity.getSymbolBuy();
            model.sellSymbol = opportunity.getSymbolSell();
            
            // Set price information
            model.buyPrice = opportunity.getBuyPrice();
            model.sellPrice = opportunity.getSellPrice();
            model.profitPercent = opportunity.getProfitPercent();
            model.netProfitPercent = opportunity.getNetProfitPercentage();
            
            // Set fee and execution details
            model.buyFee = opportunity.getBuyFeePercentage();
            model.sellFee = opportunity.getSellFeePercentage();
            model.estimatedSlippage = opportunity.getTotalSlippagePercentage();
            model.estimatedExecutionTimeMin = opportunity.getEstimatedTimeMinutes();
            
            // Set liquidity and market data
            model.buyVolume = opportunity.getBuyTicker() != null ? opportunity.getBuyTicker().getVolume() : 0;
            model.sellVolume = opportunity.getSellTicker() != null ? opportunity.getSellTicker().getVolume() : 0;
            model.buyExchangeLiquidity = opportunity.getBuyExchangeLiquidity();
            model.sellExchangeLiquidity = opportunity.getSellExchangeLiquidity();
            
            // Set timestamps and execution status
            model.discoveryTime = opportunity.getTimestamp();
            model.isExecuted = opportunity.isExecuted();
            model.isViable = opportunity.isViable();
            
            // Calculate risk information, force recalculation if specified
            calculateRisk(model, opportunity, forceRiskRecalculation);
            
            return model;
        } catch (Exception e) {
            Log.e(TAG, "Error creating model from opportunity: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Pre-calculates all risk-related information for the model
     */
    private static void calculateRisk(ArbitrageCardModel model, ArbitrageOpportunity opportunity, boolean forceRecalculation) {
        // Default medium risk in case calculation fails
        double riskScore = 0.5;
        
        // Only use existing assessment if it's valid AND we're not forcing recalculation
        RiskAssessment assessment = opportunity.getRiskAssessment();
        if (!forceRecalculation && assessment != null && assessment.isValid()) {
            // Use the pre-calculated assessment
            riskScore = assessment.getOverallRiskScore();
            Log.d(TAG, "Using pre-calculated risk assessment: " + riskScore);
        } else {
            // Always calculate a new risk assessment on force recalculation
            try {
                // Use UnifiedRiskCalculator for consistency across the app
                UnifiedRiskCalculator riskCalculator = UnifiedRiskCalculator.getInstance();
                
                // Calculate fresh risk assessment
                assessment = riskCalculator.calculateRisk(opportunity);
                
                // Apply the assessment to the opportunity to ensure all fields are updated
                riskCalculator.applyRiskAssessment(opportunity, assessment);
                
                if (assessment != null) {
                    riskScore = assessment.getOverallRiskScore();
                    Log.d(TAG, "Calculated new risk score using UnifiedRiskCalculator: " + riskScore);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating risk for opportunity " + model.tradingPair + ": " + e.getMessage());
                // Use a fallback formula based on profit
                double profitPercent = opportunity.getProfitPercent();
                if (profitPercent >= 1.0) {
                    // If profit is over 100%, use a logarithmic scale for the fallback too
                    riskScore = 0.8 + (Math.log10(profitPercent) * 0.1);
                } else {
                    riskScore = Math.min(0.9, profitPercent * 5);
                }
                Log.d(TAG, "Fallback risk calculation result: " + riskScore + " (from profit: " + profitPercent + ")");
            }
        }
        
        // IMPORTANT: Use the risk score directly without inversion
        // UnifiedRiskCalculator already uses the scale where 0.0 = highest risk, 1.0 = lowest risk
        model.riskScore = riskScore;
        
        // Set the risk level text using UnifiedRiskCalculator for consistency
        UnifiedRiskCalculator riskCalculator = UnifiedRiskCalculator.getInstance();
        model.riskLevel = riskCalculator.getRiskLevelText(riskScore);
        
        // Set the risk color using UnifiedRiskCalculator for consistency
        model.riskColor = riskCalculator.getRiskColor(riskScore);
        
        Log.d(TAG, "Final risk for " + model.tradingPair + ": " + model.riskScore + 
              " (" + model.riskLevel + ")" + (forceRecalculation ? " [FORCED]" : ""));
    }
    
    /**
     * Gets the appropriate risk level name for a risk score
     * This method is deprecated and only kept for backward compatibility
     * Use UnifiedRiskCalculator.getRiskLevelText() instead
     */
    private static String getRiskLevelName(double riskScore) {
        // Use UnifiedRiskCalculator for consistent risk level names
        return UnifiedRiskCalculator.getInstance().getRiskLevelText(riskScore);
    }
    
    /**
     * Gets the appropriate color for a risk score
     * This method is deprecated and only kept for backward compatibility
     * Use UnifiedRiskCalculator.getRiskColor() instead
     */
    private static int getRiskColor(double riskScore) {
        // Use UnifiedRiskCalculator for consistent risk colors
        return UnifiedRiskCalculator.getInstance().getRiskColor(riskScore);
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
    
    /**
     * Calculates a risk factor based on the exchanges involved
     */
    private static double getExchangeRiskFactor(String buyExchange, String sellExchange) {
        double buyFactor = getExchangeReliability(buyExchange);
        double sellFactor = getExchangeReliability(sellExchange);
        return (buyFactor + sellFactor) / 2.0;
    }
    
    /**
     * Gets a reliability factor for a specific exchange
     */
    private static double getExchangeReliability(String exchange) {
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
    
    // Getters
    
    public String getOpportunityId() {
        return opportunityId;
    }
    
    public String getTradingPair() {
        return tradingPair;
    }
    
    public String getBuyExchange() {
        return buyExchange;
    }
    
    public String getSellExchange() {
        return sellExchange;
    }
    
    public String getBuySymbol() {
        return buySymbol;
    }
    
    public String getSellSymbol() {
        return sellSymbol;
    }
    
    public double getBuyPrice() {
        return buyPrice;
    }
    
    public double getSellPrice() {
        return sellPrice;
    }
    
    public double getProfitPercent() {
        return profitPercent;
    }
    
    public double getNetProfitPercent() {
        return netProfitPercent;
    }
    
    public double getRiskScore() {
        return riskScore;
    }
    
    public String getRiskLevel() {
        return riskLevel;
    }
    
    public int getRiskColor() {
        return riskColor;
    }
    
    public double getBuyFee() {
        return buyFee;
    }
    
    public double getSellFee() {
        return sellFee;
    }
    
    public double getEstimatedSlippage() {
        return estimatedSlippage;
    }
    
    public double getEstimatedExecutionTimeMin() {
        return estimatedExecutionTimeMin;
    }
    
    public double getBuyVolume() {
        return buyVolume;
    }
    
    public double getSellVolume() {
        return sellVolume;
    }
    
    public double getBuyExchangeLiquidity() {
        return buyExchangeLiquidity;
    }
    
    public double getSellExchangeLiquidity() {
        return sellExchangeLiquidity;
    }
    
    public Date getDiscoveryTime() {
        return discoveryTime;
    }
    
    public boolean isExecuted() {
        return isExecuted;
    }
    
    public boolean isViable() {
        return isViable;
    }
    
    // Formatted display methods
    
    /**
     * Get formatted profit percentage string for display
     */
    public String getFormattedProfitPercent() {
        // FIXED: The profitPercent is already stored as a percentage value (e.g., 42.15 for 42.15%)
        // and the PERCENT_FORMAT already includes the % symbol which multiplies by 100
        return PERCENT_FORMAT.format(profitPercent);
    }
    
    /**
     * Get formatted net profit percentage string for display
     */
    public String getFormattedNetProfitPercent() {
        // FIXED: The netProfitPercent is already stored as a percentage value (e.g., 42.15 for 42.15%)
        // and the PERCENT_FORMAT already includes the % symbol which multiplies by 100
        return PERCENT_FORMAT.format(netProfitPercent);
    }
    
    public String getFormattedBuyPrice() {
        return PRICE_FORMAT.format(buyPrice);
    }
    
    public String getFormattedSellPrice() {
        return PRICE_FORMAT.format(sellPrice);
    }
    
    public String getFormattedExecutionTime() {
        if (estimatedExecutionTimeMin < 1.0) {
            return "< 1 min";
        } else if (estimatedExecutionTimeMin < 60.0) {
            return TIME_FORMAT.format(estimatedExecutionTimeMin) + " min";
        } else {
            return TIME_FORMAT.format(estimatedExecutionTimeMin / 60.0) + " hrs";
        }
    }
    
    public String getDisplaySymbol() {
        return tradingPair;
    }
    
    public String getRiskLevelWithLabel() {
        return riskLevel + " Risk";
    }
} 