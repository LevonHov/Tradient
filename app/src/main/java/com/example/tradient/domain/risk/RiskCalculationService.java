package com.example.tradient.domain.risk;

import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.RiskConfiguration;
import com.example.tradient.config.ConfigurationFactory;
import android.util.Log;

/**
 * Service for calculating risk scores using standardized components.
 */
public class RiskCalculationService {
    private static final String TAG = "RiskCalculationService";
    
    private final RiskWeights riskWeights;
    private final RiskConfiguration riskConfig;
    
    public RiskCalculationService() {
        this.riskWeights = new RiskWeights();
        this.riskConfig = ConfigurationFactory.getRiskConfig();
    }
    
    public RiskAssessment calculateRiskAssessment(Ticker buyTicker, Ticker sellTicker, double buyFees, double sellFees) {
        try {
            RiskAssessment assessment = new RiskAssessment();
            
            // Calculate individual risk factors
            double liquidityScore = calculateLiquidityScore(buyTicker, sellTicker);
            double volatilityScore = calculateVolatilityScore(buyTicker, sellTicker);
            double slippageScore = calculateSlippageScore(buyTicker, sellTicker);
            double marketDepthScore = calculateMarketDepthScore(buyTicker, sellTicker);
            double executionSpeedScore = calculateExecutionSpeedScore(buyTicker, sellTicker);
            double feeScore = calculateFeeScore(buyFees, sellFees);
            
            // Set individual scores
            assessment.setLiquidityScore(liquidityScore);
            assessment.setVolatilityScore(volatilityScore);
            assessment.setSlippageRisk(slippageScore);
            assessment.setMarketDepthScore(marketDepthScore);
            assessment.setExecutionSpeedRisk(executionSpeedScore);
            assessment.setFeeImpact(feeScore);
            
            // Calculate overall risk score
            double overallRisk = calculateOverallRiskScore(
                liquidityScore,
                volatilityScore,
                slippageScore,
                marketDepthScore,
                executionSpeedScore,
                feeScore
            );
            
            // Normalize and set overall risk score
            overallRisk = RiskScoreValidator.normalizeRiskScore(overallRisk);
            assessment.setOverallRiskScore(overallRisk);
            
            // Set risk level description
            assessment.setRiskLevel(RiskScoreValidator.getRiskLevelDescription(overallRisk));
            
            return assessment;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating risk assessment: " + e.getMessage());
            return createFailedAssessment();
        }
    }
    
    private double calculateLiquidityScore(Ticker buyTicker, Ticker sellTicker) {
        double buyVolume = buyTicker.getVolume();
        double sellVolume = sellTicker.getVolume();
        
        if (!RiskScoreValidator.isValidVolume(buyVolume) || !RiskScoreValidator.isValidVolume(sellVolume)) {
            return RiskScoreConstants.MIN_RISK_SCORE;
        }
        
        double averageVolume = (buyVolume + sellVolume) / 2.0;
        double normalizedVolume = averageVolume / RiskScoreConstants.VOLUME_NORMALIZATION_FACTOR;
        return RiskScoreValidator.normalizeRiskScore(normalizedVolume);
    }
    
    private double calculateVolatilityScore(Ticker buyTicker, Ticker sellTicker) {
        double buySpread = (buyTicker.getAskPrice() - buyTicker.getBidPrice()) / buyTicker.getLastPrice();
        double sellSpread = (sellTicker.getAskPrice() - sellTicker.getBidPrice()) / sellTicker.getLastPrice();
        double averageSpread = (buySpread + sellSpread) / 2.0;
        
        if (!RiskScoreValidator.isValidVolatility(averageSpread)) {
            return RiskScoreConstants.MIN_RISK_SCORE;
        }
        
        double spreadFactor = averageSpread / RiskScoreConstants.SPREAD_NORMALIZATION_FACTOR;
        return RiskScoreValidator.normalizeRiskScore(1.0 - spreadFactor);
    }
    
    private double calculateSlippageScore(Ticker buyTicker, Ticker sellTicker) {
        double buySlippage = calculateExpectedSlippage(buyTicker, true);
        double sellSlippage = calculateExpectedSlippage(sellTicker, false);
        double totalSlippage = buySlippage + sellSlippage;
        
        if (!RiskScoreValidator.isValidSlippage(totalSlippage)) {
            return RiskScoreConstants.MIN_RISK_SCORE;
        }
        
        double slippageFactor = totalSlippage / RiskScoreConstants.SLIPPAGE_NORMALIZATION_FACTOR;
        return RiskScoreValidator.normalizeRiskScore(1.0 - slippageFactor);
    }
    
    private double calculateMarketDepthScore(Ticker buyTicker, Ticker sellTicker) {
        double buyDepth = buyTicker.getBidAmount() + buyTicker.getAskAmount();
        double sellDepth = sellTicker.getBidAmount() + sellTicker.getAskAmount();
        double averageVolume = (buyTicker.getVolume() + sellTicker.getVolume()) / 2.0;
        
        if (!RiskScoreValidator.isValidMarketDepth(buyDepth, averageVolume) || 
            !RiskScoreValidator.isValidMarketDepth(sellDepth, averageVolume)) {
            return RiskScoreConstants.MIN_RISK_SCORE;
        }
        
        double depthRatio = Math.min(buyDepth, sellDepth) / averageVolume;
        return RiskScoreValidator.normalizeRiskScore(depthRatio);
    }
    
    private double calculateExecutionSpeedScore(Ticker buyTicker, Ticker sellTicker) {
        double buyLatency = getExchangeLatencyScore(buyTicker.getExchangeName());
        double sellLatency = getExchangeLatencyScore(sellTicker.getExchangeName());
        return (buyLatency + sellLatency) / 2.0;
    }
    
    private double calculateFeeScore(double buyFees, double sellFees) {
        double totalFees = buyFees + sellFees;
        double feeFactor = totalFees / RiskScoreConstants.FEE_NORMALIZATION_FACTOR;
        return RiskScoreValidator.normalizeRiskScore(1.0 - feeFactor);
    }
    
    private double calculateOverallRiskScore(
            double liquidityScore,
            double volatilityScore,
            double slippageScore,
            double marketDepthScore,
            double executionSpeedScore,
            double feeScore) {
        
        return (liquidityScore * riskWeights.getWeight("liquidity")) +
               (volatilityScore * riskWeights.getWeight("volatility")) +
               (slippageScore * riskWeights.getWeight("slippage")) +
               (marketDepthScore * riskWeights.getWeight("marketDepth")) +
               (executionSpeedScore * riskWeights.getWeight("executionSpeed")) +
               (feeScore * riskWeights.getWeight("fees"));
    }
    
    private double getExchangeLatencyScore(String exchange) {
        switch (exchange.toLowerCase()) {
            case "binance":
                return 0.9;
            case "coinbase":
                return 0.8;
            case "kraken":
                return 0.7;
            case "bybit":
                return 0.6;
            case "okx":
                return 0.5;
            default:
                return 0.4;
        }
    }
    
    private RiskAssessment createFailedAssessment() {
        RiskAssessment failedAssessment = new RiskAssessment();
        failedAssessment.setOverallRiskScore(RiskScoreConstants.MIN_RISK_SCORE);
        failedAssessment.setRiskLevel("Extreme Risk");
        return failedAssessment;
    }
    
    private double calculateExpectedSlippage(Ticker ticker, boolean isBuy) {
        if (ticker == null) {
            return RiskScoreConstants.MAX_SLIPPAGE_THRESHOLD; // Use max slippage if no ticker
        }
        
        // Calculate slippage based on order book depth and spread
        double price = isBuy ? ticker.getAskPrice() : ticker.getBidPrice();
        double lastPrice = ticker.getLastPrice();
        double volume = ticker.getVolume();
        
        if (price <= 0 || lastPrice <= 0) {
            return RiskScoreConstants.MAX_SLIPPAGE_THRESHOLD;
        }
        
        // Calculate base slippage from price spread
        double immediateSlippage = Math.abs(price - lastPrice) / lastPrice;
        
        // Adjust based on volume (lower volume = higher slippage)
        double volumeFactor = 1.0;
        if (volume > 0) {
            volumeFactor = Math.min(3.0, Math.max(0.5, 1000000.0 / volume));
        }
        
        // Calculate final slippage
        double finalSlippage = immediateSlippage * volumeFactor;
        
        // Constrain to reasonable bounds
        return Math.min(RiskScoreConstants.MAX_SLIPPAGE_THRESHOLD, 
                Math.max(0.0001, finalSlippage));
    }
} 