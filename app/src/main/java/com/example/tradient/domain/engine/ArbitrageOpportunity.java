package com.example.tradient.domain.engine;

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
} 