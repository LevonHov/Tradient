package com.example.tradient.domain.engine;

import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;
import com.example.tradient.data.model.RiskAssessment;

/**
 * Domain model representing an arbitrage opportunity between two exchanges.
 * Contains all relevant information about the opportunity including pricing,
 * fees, risk metrics, and potential profitability.
 */
public class ArbitrageOpportunity {
    private String symbol;
    private String buyExchange;
    private String sellExchange;
    private double buyPrice;
    private double sellPrice;
    private double priceDiffPercentage;
    private double buyFeePercentage;
    private double sellFeePercentage;
    private double totalFeePercentage;
    private double netProfitPercentage;
    private double buySlippagePercentage;
    private double sellSlippagePercentage;
    private double totalSlippagePercentage;
    private double riskScore;
    private double successRate;
    private double liquidityScore = 1.0; // Default value
    private double volatilityScore = 0.95; // Default value
    private boolean isViable = false;
    private double potentialProfit;
    private double profitPercent;
    private Ticker buyTicker;
    private Ticker sellTicker;
    private double slippage;
    private double volatility;
    private OrderBook buyOrderBook;
    private OrderBook sellOrderBook;
    private int estimatedExecutionTimeMinutes;
    private Object riskMetrics;
    private double recommendedPositionSize;
    private double liquidityFactor;
    
    private RiskAssessment riskAssessment;
    
    // Constructor with parameters
    public ArbitrageOpportunity(double potentialProfit, String buyExchange, String sellExchange, TradingPair tradingPair) {
        this.potentialProfit = potentialProfit;
        this.buyExchange = buyExchange;
        this.sellExchange = sellExchange;
        this.symbol = tradingPair != null ? tradingPair.getSymbol() : null;
    }
    
    // Default constructor
    public ArbitrageOpportunity() {
    }
    
    // Getters and setters
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getBuyExchange() {
        return buyExchange;
    }
    
    public void setBuyExchange(String buyExchange) {
        this.buyExchange = buyExchange;
    }
    
    public String getSellExchange() {
        return sellExchange;
    }
    
    public void setSellExchange(String sellExchange) {
        this.sellExchange = sellExchange;
    }
    
    public double getBuyPrice() {
        return buyPrice;
    }
    
    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }
    
    public double getSellPrice() {
        return sellPrice;
    }
    
    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }
    
    public double getPriceDiffPercentage() {
        return priceDiffPercentage;
    }
    
    public void setPriceDiffPercentage(double priceDiffPercentage) {
        this.priceDiffPercentage = priceDiffPercentage;
    }
    
    public double getBuyFeePercentage() {
        return buyFeePercentage;
    }
    
    public void setBuyFeePercentage(double buyFeePercentage) {
        this.buyFeePercentage = buyFeePercentage;
    }
    
    public double getSellFeePercentage() {
        return sellFeePercentage;
    }
    
    public void setSellFeePercentage(double sellFeePercentage) {
        this.sellFeePercentage = sellFeePercentage;
    }
    
    public double getTotalFeePercentage() {
        return totalFeePercentage;
    }
    
    public void setTotalFeePercentage(double totalFeePercentage) {
        this.totalFeePercentage = totalFeePercentage;
    }
    
    public double getNetProfitPercentage() {
        return netProfitPercentage;
    }
    
    public void setNetProfitPercentage(double netProfitPercentage) {
        this.netProfitPercentage = netProfitPercentage;
    }
    
    public double getBuySlippagePercentage() {
        return buySlippagePercentage;
    }
    
    public void setBuySlippagePercentage(double buySlippagePercentage) {
        this.buySlippagePercentage = buySlippagePercentage;
    }
    
    public double getSellSlippagePercentage() {
        return sellSlippagePercentage;
    }
    
    public void setSellSlippagePercentage(double sellSlippagePercentage) {
        this.sellSlippagePercentage = sellSlippagePercentage;
    }
    
    public double getTotalSlippagePercentage() {
        return totalSlippagePercentage;
    }
    
    public void setTotalSlippagePercentage(double totalSlippagePercentage) {
        this.totalSlippagePercentage = totalSlippagePercentage;
    }
    
    public double getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }
    
    public double getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
    
    public double getLiquidityScore() {
        return liquidityScore;
    }
    
    public void setLiquidityScore(double liquidityScore) {
        this.liquidityScore = liquidityScore;
    }
    
    public double getVolatilityScore() {
        return volatilityScore;
    }
    
    public void setVolatilityScore(double volatilityScore) {
        this.volatilityScore = volatilityScore;
    }
    
    public double getPotentialProfit() {
        return potentialProfit;
    }
    
    public void setPotentialProfit(double potentialProfit) {
        this.potentialProfit = potentialProfit;
    }
    
    public double getProfitPercent() {
        return profitPercent;
    }
    
    public void setProfitPercent(double profitPercent) {
        this.profitPercent = profitPercent;
    }
    
    public boolean isViable() {
        return isViable;
    }
    
    public void setViable(boolean viable) {
        this.isViable = viable;
    }
    
    public Ticker getBuyTicker() {
        return buyTicker;
    }
    
    public void setBuyTicker(Ticker buyTicker) {
        this.buyTicker = buyTicker;
    }
    
    public Ticker getSellTicker() {
        return sellTicker;
    }
    
    public void setSellTicker(Ticker sellTicker) {
        this.sellTicker = sellTicker;
    }
    
    public double getSlippage() {
        return slippage;
    }
    
    public void setSlippage(double slippage) {
        this.slippage = slippage;
    }
    
    public double getVolatility() {
        return volatility;
    }
    
    public void setVolatility(double volatility) {
        this.volatility = volatility;
    }
    
    public Object getRiskMetrics() {
        return riskMetrics;
    }
    
    public void setRiskMetrics(Object riskMetrics) {
        this.riskMetrics = riskMetrics;
    }
    
    public double getRecommendedPositionSize() {
        return recommendedPositionSize;
    }
    
    public void setRecommendedPositionSize(double recommendedPositionSize) {
        this.recommendedPositionSize = recommendedPositionSize;
    }
    
    public double getLiquidityFactor() {
        return liquidityFactor;
    }
    
    public void setLiquidityFactor(double liquidityFactor) {
        this.liquidityFactor = liquidityFactor;
    }
    
    public OrderBook getBuyOrderBook() {
        return buyOrderBook;
    }
    
    public void setBuyOrderBook(OrderBook buyOrderBook) {
        this.buyOrderBook = buyOrderBook;
    }
    
    public OrderBook getSellOrderBook() {
        return sellOrderBook;
    }
    
    public void setSellOrderBook(OrderBook sellOrderBook) {
        this.sellOrderBook = sellOrderBook;
    }
    
    public int getEstimatedExecutionTimeMinutes() {
        return estimatedExecutionTimeMinutes;
    }
    
    public void setEstimatedExecutionTimeMinutes(int estimatedExecutionTimeMinutes) {
        this.estimatedExecutionTimeMinutes = estimatedExecutionTimeMinutes;
    }
    
    /**
     * Gets the risk assessment for this opportunity
     * 
     * @return The risk assessment
     */
    public RiskAssessment getRiskAssessment() {
        return riskAssessment;
    }
    
    /**
     * Sets the risk assessment for this opportunity
     * 
     * @param riskAssessment The risk assessment to set
     */
    public void setRiskAssessment(RiskAssessment riskAssessment) {
        this.riskAssessment = riskAssessment;
        if (riskAssessment != null) {
            // Update risk score based on assessment
            this.riskScore = riskAssessment.getOverallRiskScore();
        }
    }
} 