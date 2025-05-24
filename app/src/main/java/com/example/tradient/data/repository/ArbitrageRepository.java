package com.example.tradient.data.repository;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.domain.engine.MarketData;
import com.example.tradient.domain.profit.ProfitCalculator;
import com.example.tradient.domain.profit.ProfitResult;
import com.example.tradient.domain.model.Exchange;
import com.example.tradient.data.model.TradingPair;
import com.example.tradient.data.service.ExchangeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repository responsible for identifying arbitrage opportunities across exchanges
 */
public class ArbitrageRepository {

    // Minimum profit threshold percentage
    private static final double MIN_PROFIT_THRESHOLD = 0.1;

    /**
     * Process raw market data to identify arbitrage opportunities
     * @param allMarketData Map of exchange to trading pair to market data
     * @return List of identified arbitrage opportunities
     */
    public List<ArbitrageOpportunity> processMarketData(Map<String, Map<String, MarketData>> allMarketData) {
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();
        
        // Iterate through exchanges and trading pairs to find opportunities
        for (Map.Entry<String, Map<String, MarketData>> buyExchangeEntry : allMarketData.entrySet()) {
            String buyExchangeName = buyExchangeEntry.getKey();
            
            for (Map.Entry<String, Map<String, MarketData>> sellExchangeEntry : allMarketData.entrySet()) {
                String sellExchangeName = sellExchangeEntry.getKey();
                
                // Skip same exchange comparison (or implement intra-exchange arbitrage later)
                if (buyExchangeName.equals(sellExchangeName)) {
                    continue;
                }
                
                // Find common trading pairs between the two exchanges
                for (String tradingPair : buyExchangeEntry.getValue().keySet()) {
                    if (!sellExchangeEntry.getValue().containsKey(tradingPair)) {
                        continue; // Trading pair not available on both exchanges
                    }
                    
                    // Get market data for this pair on both exchanges
                    MarketData buyMarketData = buyExchangeEntry.getValue().get(tradingPair);
                    MarketData sellMarketData = sellExchangeEntry.getValue().get(tradingPair);
                    
                    // Extract necessary data for calculations
                    double buyPrice = buyMarketData.getAskPrice();
                    double sellPrice = sellMarketData.getBidPrice();
                    double buyFee = getExchangeFee(buyExchangeName);
                    double sellFee = getExchangeFee(sellExchangeName);
                    double amount = calculateOptimalTradeAmount(buyMarketData, sellMarketData);
                    
                    // Skip if no profitable opportunity (sell price must be higher than buy price)
                    if (sellPrice <= buyPrice) {
                        continue;
                    }
                    
                    // Calculate profit using the correct formula
                    ProfitResult profitResult = ProfitCalculator.calculateArbitrageProfit(
                            buyPrice, sellPrice, buyFee, sellFee, amount);
                    
                    // Skip if not profitable
                    if (!profitResult.isProfitable()) {
                        continue;
                    }
                    
                    // Apply minimum profit threshold (e.g., 0.1%)
                    if (!profitResult.isViable(MIN_PROFIT_THRESHOLD)) {
                        continue;
                    }
                    
                    // Get exchange objects for advanced calculations
                    Exchange buyExchange = getExchangeByName(buyExchangeName);
                    Exchange sellExchange = getExchangeByName(sellExchangeName);
                    
                    // Get order books for slippage calculation
                    OrderBook buyOrderBook = buyExchange.getOrderBook(tradingPair);
                    OrderBook sellOrderBook = sellExchange.getOrderBook(tradingPair);
                    
                    // Extract base asset from trading pair
                    String baseAsset = tradingPair.split("/")[0];
                    
                    // Get exchange services
                    ExchangeService buyExchangeService = buyExchange.getExchangeService();
                    ExchangeService sellExchangeService = sellExchange.getExchangeService();
                    
                    // Calculate advanced metrics
                    ProfitCalculator.ArbitrageMetrics metrics = ProfitCalculator.calculateFullyAdjustedMetrics(
                            buyExchangeService, sellExchangeService, tradingPair, amount, buyPrice, sellPrice);
                    
                    // Skip if not viable after full calculation
                    if (!metrics.isViable()) {
                        continue;
                    }
                    
                    // Create and populate the opportunity with enhanced data
                    ArbitrageOpportunity opportunity = new ArbitrageOpportunity();
                    opportunity.setPair(new TradingPair(tradingPair));
                    opportunity.setExchangeBuy(buyExchangeName);
                    opportunity.setExchangeSell(sellExchangeName);
                    
                    // Create domain model opportunity with profit data
                    com.example.tradient.domain.engine.ArbitrageOpportunity domainOpp = 
                            new com.example.tradient.domain.engine.ArbitrageOpportunity();
                    domainOpp.setSymbol(tradingPair);
                    domainOpp.setBuyExchange(buyExchangeName);
                    domainOpp.setSellExchange(sellExchangeName);
                    domainOpp.setBuyPrice(buyPrice);
                    domainOpp.setSellPrice(sellPrice);
                    domainOpp.setNetProfitPercentage(profitResult.getPercentageProfit());
                    domainOpp.setBuyFeePercentage(buyFee);
                    domainOpp.setSellFeePercentage(sellFee);
                    
                    // Set profit values
                    opportunity.setPotentialProfit(profitResult.getAbsoluteProfit());
                    opportunity.setNetProfitPercentage(profitResult.getPercentageProfit());
                    
                    // Set additional data
                    double buyLiquidity = calculateExchangeLiquidity(buyOrderBook);
                    double sellLiquidity = calculateExchangeLiquidity(sellOrderBook);
                    opportunity.setBuyExchangeLiquidity(buyLiquidity);
                    opportunity.setSellExchangeLiquidity(sellLiquidity);
                    
                    // Set viability based on profit threshold
                    opportunity.setViable(profitResult.isViable(MIN_PROFIT_THRESHOLD));
                    
                    // Update the arbitrage time sensitivity check
                    boolean isTimeSensitive = false;
                    // We'll use a simple default time estimate since we removed the complex metrics
                    double estimatedTimeMinutes = 30.0;
                    if (estimatedTimeMinutes < 30 && profitResult.getPercentageProfit() > 1.0) {
                        isTimeSensitive = true;
                    }
                    opportunity.setTimeSensitive(isTimeSensitive);
                    
                    // Add to list of opportunities
                    opportunities.add(opportunity);
                }
            }
        }
        
        return opportunities;
    }

    /**
     * Calculate exchange liquidity factor based on order book
     */
    private double calculateExchangeLiquidity(OrderBook orderBook) {
        if (orderBook == null) {
            return 0.5; // Default if no data
        }
        
        double liquidity = 0.0;
        
        // Sum volumes in both sides of the book
        if (orderBook.getBids() != null && !orderBook.getBids().isEmpty()) {
            for (int i = 0; i < Math.min(5, orderBook.getBids().size()); i++) {
                liquidity += orderBook.getBids().get(i).getVolume();
            }
        }
        
        if (orderBook.getAsks() != null && !orderBook.getAsks().isEmpty()) {
            for (int i = 0; i < Math.min(5, orderBook.getAsks().size()); i++) {
                liquidity += orderBook.getAsks().get(i).getVolume();
            }
        }
        
        // Normalize to 0-1 range with logarithmic scale
        return Math.min(1.0, Math.max(0.1, Math.log10(1 + liquidity) / 6.0));
    }

    /**
     * Calculate potential profit amount
     */
    private double calculatePotentialProfit(double amount, double profitPercentage) {
        return amount * profitPercentage / 100.0;
    }

    /**
     * Calculate basic profit percentage before adjustments
     * This is now just a wrapper around the new profit calculator
     */
    private double calculateBasicProfit(double buyPrice, double sellPrice, double buyFee, double sellFee) {
        ProfitResult result = ProfitCalculator.calculateArbitrageProfit(
                buyPrice, sellPrice, buyFee, sellFee, 1.0);
        return result.getPercentageProfit();
    }
    
    /**
     * Get exchange fee percentage for a specific exchange
     * @return Fee as a decimal (e.g., 0.001 for 0.1%)
     */
    private double getExchangeFee(String exchangeName) {
        // In a real implementation, this would look up fees from a configuration or database
        // For now, return default values based on exchange name
        switch (exchangeName.toLowerCase()) {
            case "binance":
                return 0.0004; // 0.04% taker fee (spot)
            case "coinbase":
                return 0.0060; // 0.60% taker fee
            case "kraken":
                return 0.0026; // 0.26% taker fee
            case "bybit":
                return 0.0010; // 0.10% taker fee
            case "okx":
                return 0.0010; // 0.10% taker fee
            default:
                return 0.0010; // Default fee 0.10%
        }
    }
    
    /**
     * Calculate optimal trade amount based on available liquidity
     */
    private double calculateOptimalTradeAmount(MarketData buyMarketData, MarketData sellMarketData) {
        // In a real implementation, this would analyze order books and balance constraints
        // For now, return a simple estimation based on volumes
        double buyVolume = buyMarketData.getVolume24h();
        double sellVolume = sellMarketData.getVolume24h();
        
        // Use 2% of the smaller volume as a conservative amount
        return Math.min(buyVolume, sellVolume) * 0.02;
    }
    
    /**
     * Get exchange object by name
     */
    private Exchange getExchangeByName(String exchangeName) {
        // In a real implementation, this would return the actual exchange instance
        // For now, return a new instance (assuming Exchange has a constructor that takes a name)
        return new Exchange(exchangeName);
    }
} 