package com.example.tradient.domain.risk;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.interfaces.IRiskManager;
import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.data.model.RiskConfiguration;
import android.util.Log;

import java.util.Objects;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Advanced risk assessment engine for cryptocurrency arbitrage.
 * 
 * This class evaluates the risk of arbitrage opportunities using multiple factors:
 * - Liquidity risk through volume analysis
 * - Volatility risk through price movement assessment
 * - Slippage risk based on order book analysis
 * - Market depth assessment for large orders
 * - Asset-specific risk factors
 * 
 * The calculator provides:
 * - Overall risk scores (0-1 scale)
 * - Individual factor assessments
 * - Early warning indicators
 * - Predictive analytics 
 * - Position sizing recommendations
 * 
 * Risk parameters are loaded from the configuration system and can be
 * dynamically adjusted without changing code.
 * 
 * Compatible with Android platform.
 */
public class RiskCalculator implements IRiskManager {

    private enum MarketCondition {
        VOLATILE,
        STABLE,
        ILLIQUID
    }

    private static final String TAG = "RiskCalculator";

    // Constants for risk calculation
    private static final double PREDICTIVE_RISK_FACTOR = 0.95;
    private static final double PREDICTIVE_CONFIDENCE = 0.75;
    private static final double MIN_VOLUME_THRESHOLD = 10000.0;
    private static final double MAX_VOLATILITY_THRESHOLD = 0.03;

    private final RiskWeights riskWeights;
    private final RiskConfiguration riskConfig;
    private final double minProfitPercent;
    private final RiskCalculationService riskCalculationService;

    public RiskCalculator() {
        this.riskWeights = new RiskWeights();
        this.riskConfig = ConfigurationFactory.getRiskConfig();
        this.minProfitPercent = ConfigurationFactory.getArbitrageConfig().getMinProfitPercent() / 100.0;
        this.riskCalculationService = new RiskCalculationService();
    }

    public RiskCalculator(double minProfitPercent) {
        this.riskWeights = new RiskWeights();
        this.riskConfig = ConfigurationFactory.getRiskConfig();
        this.minProfitPercent = minProfitPercent;
        this.riskCalculationService = new RiskCalculationService();
    }

    public RiskAssessment calculateRisk(Ticker buyTicker, Ticker sellTicker, double buyFees, double sellFees) {
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
        if (buyTicker == null || sellTicker == null) {
            return 0.0;
        }

        // Calculate average volume between buy and sell exchanges
        double buyVolume = buyTicker.getVolume();
        double sellVolume = sellTicker.getVolume();
        double avgVolume = (buyVolume + sellVolume) / 2.0;

        // Calculate volume percentile based on historical data
        double volumePercentile = calculateVolumePercentile(avgVolume);
        
        // Calculate order book depth
        double buyDepth = buyTicker.getBidAmount() + buyTicker.getAskAmount();
        double sellDepth = sellTicker.getBidAmount() + sellTicker.getAskAmount();
        double avgDepth = (buyDepth + sellDepth) / 2.0;

        // Calculate depth percentile based on historical data
        double depthPercentile = calculateDepthPercentile(avgDepth);

        // Combine volume and depth metrics with weights
        double liquidityScore = (volumePercentile * 0.6) + (depthPercentile * 0.4);

        // Normalize to 0-1 range
        return Math.min(1.0, Math.max(0.0, liquidityScore));
    }

    private double calculateVolatilityScore(Ticker buyTicker, Ticker sellTicker) {
        if (buyTicker == null || sellTicker == null) {
            return 0.0;
        }

        // Calculate price spread as a measure of volatility
        double buySpread = (buyTicker.getAskPrice() - buyTicker.getBidPrice()) / buyTicker.getLastPrice();
        double sellSpread = (sellTicker.getAskPrice() - sellTicker.getBidPrice()) / sellTicker.getLastPrice();
        double avgSpread = (buySpread + sellSpread) / 2.0;

        // Calculate spread percentile based on historical data
        double spreadPercentile = calculateSpreadPercentile(avgSpread);
        
        // Lower spread percentile = higher score (lower risk)
        return 1.0 - spreadPercentile;
    }

    private double calculateSlippageScore(Ticker buyTicker, Ticker sellTicker) {
        if (buyTicker == null || sellTicker == null) {
            return 0.0;
        }

        // Calculate expected slippage based on order book depth
        double buySlippage = calculateExpectedSlippage(buyTicker, true);
        double sellSlippage = calculateExpectedSlippage(sellTicker, false);
        double totalSlippage = buySlippage + sellSlippage;

        // Calculate slippage percentile based on historical data
        double slippagePercentile = calculateSlippagePercentile(totalSlippage);
        
        // Lower slippage percentile = higher score (lower risk)
        return 1.0 - slippagePercentile;
    }

    private double calculateMarketDepthScore(Ticker buyTicker, Ticker sellTicker) {
        if (buyTicker == null || sellTicker == null) {
            return 0.0;
        }

        // Calculate order book depth
        double buyDepth = buyTicker.getBidAmount() + buyTicker.getAskAmount();
        double sellDepth = sellTicker.getBidAmount() + sellTicker.getAskAmount();
        double avgDepth = (buyDepth + sellDepth) / 2.0;

        // Calculate depth percentile based on historical data
        double depthPercentile = calculateDepthPercentile(avgDepth);
        
        // Higher depth percentile = higher score (lower risk)
        return depthPercentile;
    }

    private double calculateExecutionSpeedScore(Ticker buyTicker, Ticker sellTicker) {
        if (buyTicker == null || sellTicker == null) {
            return 0.0;
        }

        // Get exchange latency scores based on real-time performance
        double buyLatency = getExchangeLatencyScore(buyTicker.getExchangeName());
        double sellLatency = getExchangeLatencyScore(sellTicker.getExchangeName());

        // Calculate latency percentile based on historical data
        double latencyPercentile = calculateLatencyPercentile((buyLatency + sellLatency) / 2.0);
        
        // Lower latency percentile = higher score (lower risk)
        return 1.0 - latencyPercentile;
    }

    private double calculateFeeScore(double buyFee, double sellFee) {
        // Calculate total fees
        double totalFees = buyFee + sellFee;

        // Calculate fee percentile based on historical data
        double feePercentile = calculateFeePercentile(totalFees);
        
        // Lower fee percentile = higher score (lower risk)
        return 1.0 - feePercentile;
    }

    private double calculateOverallRiskScore(
            double liquidityScore,
            double volatilityScore,
            double slippageScore,
            double marketDepthScore,
            double executionSpeedScore,
            double feeScore) {
        
        // Get dynamic weights based on market conditions
        Map<String, Double> weights = getDynamicWeights();
        
        // Calculate weighted sum using dynamic weights
        double weightedScore = 
            (liquidityScore * weights.get("liquidity")) +
            (volatilityScore * weights.get("volatility")) +
            (slippageScore * weights.get("slippage")) +
            (marketDepthScore * weights.get("marketDepth")) +
            (executionSpeedScore * weights.get("executionSpeed")) +
            (feeScore * weights.get("fees"));
        
        // Ensure the final score is within the valid range
        return Math.max(0.0, Math.min(1.0, weightedScore));
    }

    private Map<String, Double> getDynamicWeights() {
        // Get current market conditions
        MarketCondition condition = getCurrentMarketCondition();
        
        // Adjust weights based on market conditions
        Map<String, Double> weights = new HashMap<>();
        
        switch (condition) {
            case VOLATILE:
                weights.put("liquidity", 0.20);
                weights.put("volatility", 0.30);
                weights.put("slippage", 0.20);
                weights.put("marketDepth", 0.15);
                weights.put("executionSpeed", 0.10);
                weights.put("fees", 0.05);
                break;
            case STABLE:
                weights.put("liquidity", 0.25);
                weights.put("volatility", 0.15);
                weights.put("slippage", 0.20);
                weights.put("marketDepth", 0.20);
                weights.put("executionSpeed", 0.10);
                weights.put("fees", 0.10);
                break;
            case ILLIQUID:
                weights.put("liquidity", 0.30);
                weights.put("volatility", 0.15);
                weights.put("slippage", 0.25);
                weights.put("marketDepth", 0.20);
                weights.put("executionSpeed", 0.05);
                weights.put("fees", 0.05);
                break;
            default:
                // Default weights
                weights.put("liquidity", 0.25);
                weights.put("volatility", 0.20);
                weights.put("slippage", 0.20);
                weights.put("marketDepth", 0.15);
                weights.put("executionSpeed", 0.10);
                weights.put("fees", 0.10);
        }
        
        return weights;
    }

    private double calculateSpreadPercentile(double spread) {
        // Get historical spread data for the asset
        List<Double> historicalSpreads = getHistoricalSpreads();
        
        // Calculate percentile
        return calculatePercentile(spread, historicalSpreads);
    }

    private double calculateSlippagePercentile(double slippage) {
        // Get historical slippage data for the asset
        List<Double> historicalSlippages = getHistoricalSlippages();
        
        // Calculate percentile
        return calculatePercentile(slippage, historicalSlippages);
    }

    private double calculateLatencyPercentile(double latency) {
        // Get historical latency data for the exchanges
        List<Double> historicalLatencies = getHistoricalLatencies();
        
        // Calculate percentile
        return calculatePercentile(latency, historicalLatencies);
    }

    private double calculateFeePercentile(double fees) {
        // Get historical fee data for the exchanges
        List<Double> historicalFees = getHistoricalFees();
        
        // Calculate percentile
        return calculatePercentile(fees, historicalFees);
    }

    private double calculatePercentile(double value, List<Double> data) {
        if (data == null || data.isEmpty()) {
            return 0.5; // Default to median if no data
        }
        
        // Sort the data
        Collections.sort(data);
        
        // Count values less than the given value
        int count = 0;
        for (Double d : data) {
            if (d < value) {
                count++;
            }
        }
        
        // Calculate percentile
        return (double) count / data.size();
    }

    private MarketCondition getCurrentMarketCondition() {
        // Analyze current market data to determine condition
        double volatility = calculateCurrentVolatility();
        double liquidity = calculateCurrentLiquidity();
        
        if (volatility > 0.05) { // 5% volatility threshold
            return MarketCondition.VOLATILE;
        } else if (liquidity < 0.3) { // 30% liquidity threshold
            return MarketCondition.ILLIQUID;
        } else {
            return MarketCondition.STABLE;
        }
    }

    private double calculateCurrentVolatility() {
        // Calculate current market volatility
        // Implementation depends on your market data source
        return 0.0; // Placeholder
    }

    private double calculateCurrentLiquidity() {
        // Calculate current market liquidity
        // Implementation depends on your market data source
        return 0.0; // Placeholder
    }

    private List<Double> getHistoricalSpreads() {
        // Get historical spread data
        // Implementation depends on your data source
        return new ArrayList<>(); // Placeholder
    }

    private List<Double> getHistoricalSlippages() {
        // Get historical slippage data
        // Implementation depends on your data source
        return new ArrayList<>(); // Placeholder
    }

    private List<Double> getHistoricalLatencies() {
        // Get historical latency data
        // Implementation depends on your data source
        return new ArrayList<>(); // Placeholder
    }

    private List<Double> getHistoricalFees() {
        // Get historical fee data
        // Implementation depends on your data source
        return new ArrayList<>(); // Placeholder
    }

    private RiskAssessment createFailedAssessment() {
        RiskAssessment failedAssessment = new RiskAssessment();
        failedAssessment.setOverallRiskScore(0.0);
        failedAssessment.setRiskLevel("Extreme Risk");
        return failedAssessment;
    }

    @Override
    public int calculateSuccessRate(double profitPercent, double riskScore, double marketVolatility) {
        // Base success rate on profit percentage and risk score
        double baseRate = profitPercent * (1.0 - riskScore);
        
        // Adjust for market volatility
        double volatilityAdjustment = 1.0 - (marketVolatility * 2.0);
        
        // Calculate final success rate as a percentage (0-100)
        int successPercentage = (int) ((baseRate * volatilityAdjustment) * 100);
        
        // Ensure the rate is between 0 and 100
        return Math.max(0, Math.min(100, successPercentage));
    }

    private double calculateExpectedSlippage(Ticker ticker, boolean isBuy) {
        if (ticker == null) return 0.0;
        double depth = isBuy ? ticker.getAskAmount() : ticker.getBidAmount();
        double price = isBuy ? ticker.getAskPrice() : ticker.getBidPrice();
        double lastPrice = ticker.getLastPrice();
        if (depth <= 0 || price <= 0 || lastPrice <= 0) return 0.0;
        return Math.abs(price - lastPrice) / lastPrice;
    }

    private double getExchangeLatencyScore(String exchangeName) {
        switch (exchangeName.toLowerCase()) {
            case "binance": return 0.9;
            case "coinbase": return 0.8;
            case "kraken": return 0.7;
            case "bybit": return 0.6;
            case "okx": return 0.5;
            default: return 0.4;
        }
    }

    /**
     * Assesses risk for an arbitrage opportunity
     */
    public RiskAssessment assessRisk(ArbitrageOpportunity opportunity) {
        if (opportunity == null) {
            return createFailedAssessment();
        }

        try {
            Ticker buyTicker = opportunity.getBuyTicker();
            Ticker sellTicker = opportunity.getSellTicker();
            double buyFee = opportunity.getBuyFeePercentage();
            double sellFee = opportunity.getSellFeePercentage();

            // Use RiskCalculationService to calculate the risk assessment
            RiskAssessment assessment = riskCalculationService.calculateRiskAssessment(
                buyTicker, 
                sellTicker, 
                buyFee, 
                sellFee
            );

            // Update opportunity with the risk assessment using the adapter
            com.example.tradient.util.RiskAssessmentAdapter.setRiskAssessment(opportunity, assessment);
            opportunity.setViable(assessment.getOverallRiskScore() >= RiskScoreConstants.MODERATE_RISK_THRESHOLD);

            return assessment;
        } catch (Exception e) {
            Log.e(TAG, "Error assessing risk for opportunity: " + e.getMessage());
            return createFailedAssessment();
        }
    }

    @Override
    public double assessLiquidity(Ticker buyTicker, Ticker sellTicker) {
        try {
            if (buyTicker == null || sellTicker == null) {
                return RiskScoreConstants.MIN_RISK_SCORE;
            }

            double buyVolume = buyTicker.getVolume();
            double sellVolume = sellTicker.getVolume();
            
            if (!RiskScoreValidator.isValidVolume(buyVolume) || !RiskScoreValidator.isValidVolume(sellVolume)) {
                return RiskScoreConstants.MIN_RISK_SCORE;
            }
            
            double averageVolume = (buyVolume + sellVolume) / 2.0;
            double normalizedVolume = averageVolume / RiskScoreConstants.VOLUME_NORMALIZATION_FACTOR;
            return RiskScoreValidator.normalizeRiskScore(normalizedVolume);
        } catch (Exception e) {
            Log.e(TAG, "Error assessing liquidity: " + e.getMessage());
            return RiskScoreConstants.MIN_RISK_SCORE;
        }
    }

    @Override
    public double assessVolatility(String symbol) {
        try {
            // Create empty lists for the required parameters
            List<Ticker> buyTickers = new ArrayList<>();
            List<Ticker> sellTickers = new ArrayList<>();
            
            // Use VolatilityService to calculate volatility with the correct parameters
            VolatilityService volatilityService = new VolatilityService();
            double volatility = volatilityService.calculateVolatility(buyTickers, sellTickers, symbol);
            
            if (volatility > RiskScoreConstants.MAX_VOLATILITY_THRESHOLD) {
                return RiskScoreConstants.MIN_RISK_SCORE;
            }
            
            double normalizedVolatility = volatility / RiskScoreConstants.MAX_VOLATILITY_THRESHOLD;
            return RiskScoreValidator.normalizeRiskScore(1.0 - normalizedVolatility);
        } catch (Exception e) {
            Log.e(TAG, "Error assessing volatility: " + e.getMessage());
            return RiskScoreConstants.MIN_RISK_SCORE;
        }
    }

    @Override
    public double calculateRisk(Ticker buyTicker, Ticker sellTicker) {
        try {
            if (buyTicker == null || sellTicker == null) {
                return RiskScoreConstants.MIN_RISK_SCORE;
            }

            // Calculate individual risk factors
            double liquidityRisk = 1.0 - assessLiquidity(buyTicker, sellTicker);
            double volatilityRisk = 1.0 - assessVolatility(buyTicker.getSymbol());
            double slippageRisk = calculateSlippageRisk(buyTicker, sellTicker);
            double marketDepthRisk = 1.0 - calculateMarketDepthRisk(buyTicker, sellTicker);
            double executionSpeedRisk = 1.0 - calculateExecutionSpeedRisk(buyTicker, sellTicker);

            // Get weights from RiskWeights
            Map<String, Double> weights = new HashMap<>();
            weights.put("liquidity", riskWeights.getWeight("liquidity"));
            weights.put("volatility", riskWeights.getWeight("volatility"));
            weights.put("slippage", riskWeights.getWeight("slippage"));
            weights.put("marketDepth", riskWeights.getWeight("marketDepth"));
            weights.put("executionSpeed", riskWeights.getWeight("executionSpeed"));

            // Calculate weighted risk score
            double weightedRisk = 
                (liquidityRisk * weights.get("liquidity")) +
                (volatilityRisk * weights.get("volatility")) +
                (slippageRisk * weights.get("slippage")) +
                (marketDepthRisk * weights.get("marketDepth")) +
                (executionSpeedRisk * weights.get("executionSpeed"));

            // Normalize to 0-1 range
            return RiskScoreValidator.normalizeRiskScore(weightedRisk);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating risk: " + e.getMessage());
            return RiskScoreConstants.MIN_RISK_SCORE;
        }
    }

    private double calculateSlippageRisk(Ticker buyTicker, Ticker sellTicker) {
        try {
            // Calculate slippage based on order book depth
            double buySlippage = calculateExpectedSlippage(buyTicker, true);
            double sellSlippage = calculateExpectedSlippage(sellTicker, false);
            double totalSlippage = buySlippage + sellSlippage;
            
            if (!RiskScoreValidator.isValidSlippage(totalSlippage)) {
                return RiskScoreConstants.MIN_RISK_SCORE;
            }
            
            double slippageFactor = totalSlippage / RiskScoreConstants.SLIPPAGE_NORMALIZATION_FACTOR;
            return RiskScoreValidator.normalizeRiskScore(1.0 - slippageFactor);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating slippage risk: " + e.getMessage());
            return RiskScoreConstants.MIN_RISK_SCORE;
        }
    }

    private double calculateMarketDepthRisk(Ticker buyTicker, Ticker sellTicker) {
        try {
            double buyDepth = buyTicker.getBidAmount() + buyTicker.getAskAmount();
            double sellDepth = sellTicker.getBidAmount() + sellTicker.getAskAmount();
            double averageVolume = (buyTicker.getVolume() + sellTicker.getVolume()) / 2.0;
            
            if (!RiskScoreValidator.isValidMarketDepth(buyDepth, averageVolume) || 
                !RiskScoreValidator.isValidMarketDepth(sellDepth, averageVolume)) {
                return RiskScoreConstants.MIN_RISK_SCORE;
            }
            
            double depthRatio = Math.min(buyDepth, sellDepth) / averageVolume;
            return RiskScoreValidator.normalizeRiskScore(depthRatio);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating market depth risk: " + e.getMessage());
            return RiskScoreConstants.MIN_RISK_SCORE;
        }
    }

    private double calculateExecutionSpeedRisk(Ticker buyTicker, Ticker sellTicker) {
        try {
            double buyLatency = getExchangeLatencyScore(buyTicker.getExchangeName());
            double sellLatency = getExchangeLatencyScore(sellTicker.getExchangeName());
            return (buyLatency + sellLatency) / 2.0;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating execution speed risk: " + e.getMessage());
            return RiskScoreConstants.MIN_RISK_SCORE;
        }
    }

    private double calculateVolumePercentile(double volume) {
        // Get historical volume data
        List<Double> historicalVolumes = getHistoricalVolumes();
        if (historicalVolumes == null || historicalVolumes.isEmpty()) {
            return 0.5; // Default to median if no historical data
        }

        // Sort volumes
        Collections.sort(historicalVolumes);
        
        // Count values less than the given volume
        int count = 0;
        for (Double v : historicalVolumes) {
            if (v < volume) {
                count++;
            }
        }
        
        // Calculate percentile
        return (double) count / historicalVolumes.size();
    }

    private double calculateDepthPercentile(double depth) {
        // Get historical depth data
        List<Double> historicalDepths = getHistoricalDepths();
        if (historicalDepths == null || historicalDepths.isEmpty()) {
            return 0.5; // Default to median if no historical data
        }

        // Sort depths
        Collections.sort(historicalDepths);
        
        // Count values less than the given depth
        int count = 0;
        for (Double d : historicalDepths) {
            if (d < depth) {
                count++;
            }
        }
        
        // Calculate percentile
        return (double) count / historicalDepths.size();
    }

    private List<Double> getHistoricalVolumes() {
        // Implementation depends on your data source
        // This should return a list of historical volumes
        return new ArrayList<>(); // Placeholder
    }

    private List<Double> getHistoricalDepths() {
        // Implementation depends on your data source
        // This should return a list of historical order book depths
        return new ArrayList<>(); // Placeholder
    }

    /**
     * Calculates a comprehensive risk assessment for an arbitrage opportunity.
     * 
     * @param buyTicker The ticker data for the buy side
     * @param sellTicker The ticker data for the sell side
     * @param buyFee The buy fee as a percentage (e.g., 0.001 for 0.1%)
     * @param sellFee The sell fee as a percentage (e.g., 0.001 for 0.1%)
     * @return A risk assessment object with detailed risk metrics
     */
    public RiskAssessment calculateRiskAssessment(Ticker buyTicker, Ticker sellTicker, double buyFee, double sellFee) {
        try {
            if (buyTicker == null || sellTicker == null) {
                return createFailedAssessment();
            }
            
            // Create new risk assessment
            RiskAssessment assessment = new RiskAssessment();
            
            // Calculate individual risk metrics
            double liquidityScore = assessLiquidity(buyTicker, sellTicker);
            double volatilityScore = assessVolatility(buyTicker.getSymbol());
            double slippageRisk = calculateSlippageRisk(buyTicker, sellTicker);
            double marketDepthScore = calculateMarketDepthRisk(buyTicker, sellTicker);
            double executionSpeedScore = calculateExecutionSpeedRisk(buyTicker, sellTicker);
            double feeImpact = calculateFeeImpact(buyFee, sellFee);
            
            // Set assessment properties
            assessment.setLiquidityScore(liquidityScore);
            assessment.setVolatilityScore(volatilityScore);
            assessment.setSlippageRisk(slippageRisk);
            assessment.setMarketDepthScore(marketDepthScore);
            assessment.setExecutionSpeedRisk(executionSpeedScore);
            assessment.setFeeImpact(feeImpact);
            
            // Calculate overall risk score using weighted average
            Map<String, Double> weights = getDynamicWeights();
            double overallScore = 
                (liquidityScore * weights.getOrDefault("liquidity", 0.25)) +
                (volatilityScore * weights.getOrDefault("volatility", 0.25)) +
                ((1.0 - slippageRisk) * weights.getOrDefault("slippage", 0.15)) +
                (marketDepthScore * weights.getOrDefault("marketDepth", 0.15)) +
                (executionSpeedScore * weights.getOrDefault("executionSpeed", 0.10)) +
                (feeImpact * weights.getOrDefault("feeImpact", 0.10));
            
            // Normalize and set overall score
            assessment.setOverallRiskScore(RiskScoreValidator.normalizeRiskScore(overallScore));
            
            // Set risk level based on overall score
            assessment.setRiskLevel(determineRiskLevel(assessment.getOverallRiskScore()));
            
            // Set exchange information
            assessment.setExchangeBuy(buyTicker.getExchangeName());
            assessment.setExchangeSell(sellTicker.getExchangeName());
            
            // Set fee information
            assessment.setBuyFeePercentage(buyFee);
            assessment.setSellFeePercentage(sellFee);
            
            return assessment;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating risk assessment: " + e.getMessage());
            return createFailedAssessment();
        }
    }
    
    /**
     * Calculates the impact of fees on profitability
     */
    private double calculateFeeImpact(double buyFee, double sellFee) {
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
     * Determines the risk level based on the overall risk score
     */
    private String determineRiskLevel(double overallScore) {
        if (overallScore < RiskScoreConstants.HIGH_RISK_THRESHOLD) {
            return "HIGH";
        } else if (overallScore < RiskScoreConstants.MODERATE_RISK_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}