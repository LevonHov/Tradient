package com.example.tradient.data.model;

import android.graphics.Color;
import android.util.Log;

import com.example.tradient.domain.risk.RiskCalculationService;

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
                Ticker buyTicker = opportunity.getBuyTicker();
                Ticker sellTicker = opportunity.getSellTicker();
                
                // Only attempt full calculation if we have ticker data
                if (buyTicker != null && sellTicker != null) {
                    Log.d(TAG, "Calculating fresh risk assessment with full ticker data");
                    // Use RiskCalculationService for accurate risk assessment
                    RiskCalculationService riskService = new RiskCalculationService();
                    
                    // Calculate fee estimates if not available
                    double buyFees = model.buyFee > 0 ? model.buyFee : getExchangeFee(model.buyExchange);
                    double sellFees = model.sellFee > 0 ? model.sellFee : getExchangeFee(model.sellExchange);
                    
                    assessment = riskService.calculateRiskAssessment(
                        buyTicker, 
                        sellTicker, 
                        buyFees, 
                        sellFees
                    );
                    
                    if (assessment != null) {
                        riskScore = assessment.getOverallRiskScore();
                        opportunity.setRiskAssessment(assessment); // Cache for future use
                        Log.d(TAG, "Calculated new risk score: " + riskScore);
                    }
                } else {
                    Log.d(TAG, "Using basic risk calculation (missing ticker data)");
                    // Improved calculation based on profit and exchange reliability
                    double profitPercent = opportunity.getProfitPercent();
                    Log.d(TAG, "Profit percent for risk calculation: " + profitPercent);
                    
                    // Properly handle very high profit percentages (over 100%)
                    double profitFactor;
                    if (profitPercent >= 1.0) {
                        // If profit is over 100%, use a logarithmic scale
                        profitFactor = 0.8 + (Math.log10(profitPercent) * 0.1);
                        profitFactor = Math.min(1.0, profitFactor); // Cap at 1.0
                        Log.d(TAG, "Using logarithmic profit factor for high profit: " + profitFactor);
                    } else {
                        // For lower profits, use linear scale
                        profitFactor = Math.min(0.9, profitPercent * 5);
                        Log.d(TAG, "Using linear profit factor: " + profitFactor);
                    }
                    
                    double exchangeFactor = getExchangeRiskFactor(model.buyExchange, model.sellExchange);
                    // Weighted average, with more weight to profit for high-profit opportunities
                    double profitWeight = Math.min(0.9, 0.6 + (profitPercent * 0.3));
                    riskScore = (profitFactor * profitWeight) + (exchangeFactor * (1.0 - profitWeight));
                    Log.d(TAG, "Basic risk calculation result: " + riskScore + " (profit weight: " + profitWeight + ")");
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
        
        // IMPORTANT: Invert the risk score to match the interpretation in getRiskLevelName and getRiskColor
        // This aligns our risk scale where 0.0 = lowest risk, 1.0 = highest risk
        // RiskUtils and RiskCalculationService use 0.0 = highest risk, 1.0 = lowest risk
        riskScore = 1.0 - riskScore;
        Log.d(TAG, "Inverted risk score: " + riskScore + " (higher = riskier)");
        
        // Assign the calculated risk score
        model.riskScore = riskScore;
        
        // Set the risk level text
        model.riskLevel = getRiskLevelName(riskScore);
        
        // Set the risk color
        model.riskColor = getRiskColor(riskScore);
        
        Log.d(TAG, "Final risk for " + model.tradingPair + ": " + model.riskScore + 
              " (" + model.riskLevel + ")" + (forceRecalculation ? " [FORCED]" : ""));
    }
    
    /**
     * Gets the appropriate risk level name for a risk score
     */
    private static String getRiskLevelName(double riskScore) {
        // Higher scores = higher risk (1.0 is highest risk, 0.0 is lowest risk)
        if (riskScore >= 0.9) return "Critical";
        if (riskScore >= 0.8) return "Extreme";
        if (riskScore >= 0.7) return "Very High";
        if (riskScore >= 0.6) return "High";
        if (riskScore >= 0.5) return "Medium-High";
        if (riskScore >= 0.4) return "Medium";
        if (riskScore >= 0.3) return "Low-Medium";
        if (riskScore >= 0.2) return "Low";
        if (riskScore >= 0.1) return "Very Low";
        return "Minimal";
    }
    
    /**
     * Gets the appropriate color for a risk score
     */
    private static int getRiskColor(double riskScore) {
        // Higher scores = higher risk (red), lower scores = lower risk (green)
        if (riskScore >= 0.9) return Color.parseColor("#B71C1C"); // Dark Red
        if (riskScore >= 0.8) return Color.parseColor("#F44336"); // Red
        if (riskScore >= 0.7) return Color.parseColor("#FF5722"); // Deep Orange
        if (riskScore >= 0.6) return Color.parseColor("#FF9800"); // Orange
        if (riskScore >= 0.5) return Color.parseColor("#FFC107"); // Amber
        if (riskScore >= 0.4) return Color.parseColor("#FFEB3B"); // Yellow
        if (riskScore >= 0.3) return Color.parseColor("#CDDC39"); // Lime
        if (riskScore >= 0.2) return Color.parseColor("#8BC34A"); // Light Green
        if (riskScore >= 0.1) return Color.parseColor("#4CAF50"); // Green
        return Color.parseColor("#00C853"); // Bright Green
    }
    
    /**
     * Gets fee rate for a specific exchange
     */
    private static double getExchangeFee(String exchange) {
        if (exchange == null) return 0.002; // Default 0.2%
        
        switch (exchange.toLowerCase()) {
            case "binance": return 0.001;  // 0.1%
            case "coinbase": return 0.005; // 0.5%
            case "kraken": return 0.0026;  // 0.26%
            case "kucoin": return 0.001;   // 0.1%
            case "bybit": return 0.001;    // 0.1%
            case "okx": return 0.0008;     // 0.08%
            case "gemini": return 0.0035;  // 0.35%
            case "bitfinex": return 0.002; // 0.2%
            default: return 0.002;         // Default 0.2%
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