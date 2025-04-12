package com.example.tradient.domain.risk;

import android.util.Log;

import com.example.tradient.api.BinanceApiAdapter;
import com.example.tradient.api.ExchangeApiAdapter;
import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.Candle;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.data.model.Ticker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Enhanced risk calculator that uses real-time market data from exchanges
 * to calculate accurate risk assessments for arbitrage opportunities.
 */
public class RealTimeRiskCalculator {
    private static final String TAG = "RealTimeRiskCalculator";
    
    // Exchange adapters for API access
    private final Map<String, ExchangeApiAdapter> exchangeAdapters = new HashMap<>();
    
    // Default trade size for slippage calculations
    private static final double DEFAULT_TRADE_SIZE = 1000.0; // $1000 USD
    
    public RealTimeRiskCalculator() {
        // Initialize exchange adapters
        exchangeAdapters.put("binance", new BinanceApiAdapter());
        // Add other exchange adapters as needed
    }
    
    /**
     * Calculate risk directly from an ArbitrageOpportunity object, 
     * using tickers and order books if they're already available
     */
    public CompletableFuture<RiskAssessment> calculateRiskFromOpportunity(ArbitrageOpportunity opportunity) {
        if (opportunity == null) {
            CompletableFuture<RiskAssessment> future = new CompletableFuture<>();
            future.complete(createDefaultRiskAssessment(0.3)); // Medium-high risk
            return future;
        }
        
        String symbol = "";
        String buyExchange = "";
        String sellExchange = "";
        double tradeSize = DEFAULT_TRADE_SIZE;
        
        // Extract information from opportunity
        try {
            // Get symbol
            if (opportunity.getPair() != null) {
                symbol = opportunity.getPair().getSymbol();
            } else if (opportunity.getNormalizedSymbol() != null && !opportunity.getNormalizedSymbol().isEmpty()) {
                symbol = opportunity.getNormalizedSymbol();
            } else if (opportunity.getSymbolBuy() != null && !opportunity.getSymbolBuy().isEmpty()) {
                symbol = opportunity.getSymbolBuy();
            }
            
            // Get exchange names
            buyExchange = opportunity.getExchangeBuy();
            sellExchange = opportunity.getExchangeSell();
            
            // Try to get profit percentage for trade size calculation
            try {
                double profit = opportunity.getProfitPercent();
                tradeSize = determineTradeSize(profit);
            } catch (Exception e) {
                Log.d(TAG, "Could not get profit percentage, using default trade size");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting opportunity details: " + e.getMessage());
        }
        
        // Check if opportunity already has ticker data
        Ticker buyTicker = null;
        Ticker sellTicker = null;
        
        try {
            buyTicker = opportunity.getBuyTicker();
            sellTicker = opportunity.getSellTicker();
        } catch (Exception e) {
            Log.d(TAG, "Tickers not available in opportunity object: " + e.getMessage());
        }
        
        // Check if we need to fetch API data or use existing data
        if (buyTicker != null && sellTicker != null) {
            Log.d(TAG, "Using existing ticker data for risk calculation");
            
            // Create final references for lambda capture
            final Ticker finalBuyTicker = buyTicker;
            final Ticker finalSellTicker = sellTicker;
            final String finalSymbol = symbol;
            final double finalTradeSize = tradeSize;
            final ArbitrageOpportunity finalOpportunity = opportunity;
            
            // Use existing ticker data and calculate risk
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // Create containers for order books
                    OrderBook buyOrderBook = null;
                    OrderBook sellOrderBook = null;
                    
                    // Try to get order books if they exist
                    try {
                        // Check if order books are accessible
                        try {
                            // Try direct field access first via reflection
                            java.lang.reflect.Field buyOrderBookField = finalOpportunity.getClass().getDeclaredField("buyOrderBook");
                            buyOrderBookField.setAccessible(true);
                            buyOrderBook = (OrderBook) buyOrderBookField.get(finalOpportunity);
                            
                            java.lang.reflect.Field sellOrderBookField = finalOpportunity.getClass().getDeclaredField("sellOrderBook");
                            sellOrderBookField.setAccessible(true);
                            sellOrderBook = (OrderBook) sellOrderBookField.get(finalOpportunity);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            // Try getters via reflection if direct field access fails
                            try {
                                java.lang.reflect.Method getBuyOrderBookMethod = finalOpportunity.getClass().getMethod("getBuyOrderBook");
                                buyOrderBook = (OrderBook) getBuyOrderBookMethod.invoke(finalOpportunity);
                                
                                java.lang.reflect.Method getSellOrderBookMethod = finalOpportunity.getClass().getMethod("getSellOrderBook");
                                sellOrderBook = (OrderBook) getSellOrderBookMethod.invoke(finalOpportunity);
                            } catch (Exception ex) {
                                Log.d(TAG, "No order book methods available: " + ex.getMessage());
                                buyOrderBook = null;
                                sellOrderBook = null;
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "Order books not available: " + e.getMessage());
                        buyOrderBook = null;
                        sellOrderBook = null;
                    }
                    
                    return calculateRiskWithExistingData(
                        finalSymbol,
                        finalBuyTicker,
                        finalSellTicker,
                        buyOrderBook,
                        sellOrderBook,
                        finalTradeSize
                    );
                } catch (Exception e) {
                    Log.e(TAG, "Error calculating risk with existing data: " + e.getMessage());
                    return createDefaultRiskAssessment(0.4);
                }
            });
        } else {
            // Fetch data from APIs
            return calculateRisk(symbol, buyExchange, sellExchange, tradeSize);
        }
    }
    
    /**
     * Calculate risk from existing data without making API calls
     */
    private RiskAssessment calculateRiskWithExistingData(
            String symbol,
            Ticker buyTicker,
            Ticker sellTicker,
            OrderBook buyOrderBook,
            OrderBook sellOrderBook,
            double tradeSize) {
        
        // Calculate individual risk components from available data
        double liquidityScore = calculateLiquidityScore(buyTicker, sellTicker);
        double volatilityScore = 0.6; // Default medium-low volatility without historical data
        double marketDepthScore = 0.5; // Default medium depth
        
        // If order books are available, use them for better calculations
        if (buyOrderBook != null && sellOrderBook != null) {
            marketDepthScore = calculateMarketDepthScore(buyOrderBook, sellOrderBook);
            
            // Calculate more detailed slippage with real order book data
            double buySlippageEst = buyOrderBook.calculateSlippage(tradeSize, true);
            double sellSlippageEst = sellOrderBook.calculateSlippage(tradeSize, false);
            double totalSlippage = (buySlippageEst + sellSlippageEst) / 100.0; // Convert from % to decimal
            
            // If slippage is too small, use a minimum value
            if (totalSlippage < 0.001) {
                totalSlippage = 0.001; // Minimum 0.1% slippage
            }
            
            // Calculate risk metrics
            double exchangeRiskScore = calculateExchangeRiskScore(buyTicker.getExchangeName(), sellTicker.getExchangeName());
            double executionTimeEstimate = estimateExecutionTime(buyTicker.getExchangeName(), sellTicker.getExchangeName(), volatilityScore);
            double roiEfficiency = calculateRoiEfficiency(0.01, executionTimeEstimate); // Assume 1% profit
            
            // Calculate overall risk
            double overallRisk = calculateOverallRiskScore(liquidityScore, volatilityScore, marketDepthScore, exchangeRiskScore, totalSlippage);
            
            return new RiskAssessment(
                overallRisk,
                liquidityScore,
                volatilityScore,
                exchangeRiskScore,
                1.0 - totalSlippage,
                totalSlippage,
                executionTimeEstimate,
                roiEfficiency,
                tradeSize
            );
        } else {
            // Without order books, create a rough estimate
            double exchangeRiskScore = 0.7; // Default good exchange risk
            double slippageEstimate = 0.003; // Default 0.3% slippage
            double executionTimeEstimate = 2.0; // Default 2 minutes
            double roiEfficiency = 0.01 / (executionTimeEstimate / 60.0); // 1% profit per estimate time
            
            // Overall risk (weighted average of components)
            double overallRisk = (liquidityScore * 0.3) + (volatilityScore * 0.3) + (exchangeRiskScore * 0.2) + (0.2); // 0.2 fixed baseline
            
            return new RiskAssessment(
                overallRisk,
                liquidityScore,
                volatilityScore,
                exchangeRiskScore,
                1.0 - slippageEstimate,
                slippageEstimate,
                executionTimeEstimate,
                roiEfficiency,
                tradeSize
            );
        }
    }

    /**
     * Calculate a comprehensive risk assessment using real-time market data
     * @param symbol The trading pair in normalized format (e.g., "BTC/USDT")
     * @param buyExchange The name of the buy exchange
     * @param sellExchange The name of the sell exchange
     * @param tradeSize The size of the trade in USD
     * @return A CompletableFuture that resolves to a RiskAssessment
     */
    public CompletableFuture<RiskAssessment> calculateRisk(
            String symbol, 
            String buyExchange, 
            String sellExchange, 
            double tradeSize) {
        
        if (symbol == null || symbol.isEmpty() || 
            buyExchange == null || buyExchange.isEmpty() ||
            sellExchange == null || sellExchange.isEmpty()) {
            Log.e(TAG, "Invalid parameters for risk calculation");
            CompletableFuture<RiskAssessment> future = new CompletableFuture<>();
            future.complete(createDefaultRiskAssessment(0.3));
            return future;
        }
        
        Log.d(TAG, String.format("Calculating risk for %s: %s → %s", symbol, buyExchange, sellExchange));
        
        // Get exchange adapters
        ExchangeApiAdapter buyAdapter = getExchangeAdapter(buyExchange);
        ExchangeApiAdapter sellAdapter = getExchangeAdapter(sellExchange);
        
        if (buyAdapter == null || sellAdapter == null) {
            Log.e(TAG, "Exchange adapter not found for " + buyExchange + " or " + sellExchange);
            CompletableFuture<RiskAssessment> future = new CompletableFuture<>();
            future.complete(createDefaultRiskAssessment(0.4));
            return future;
        }
        
        // Convert symbol to exchange formats
        String buySymbol = buyAdapter.convertSymbolToExchangeFormat(symbol);
        String sellSymbol = sellAdapter.convertSymbolToExchangeFormat(symbol);
        
        // Create futures for all data we need
        CompletableFuture<Ticker> buyTickerFuture = buyAdapter.getTicker(buySymbol);
        CompletableFuture<Ticker> sellTickerFuture = sellAdapter.getTicker(sellSymbol);
        CompletableFuture<OrderBook> buyOrderBookFuture = buyAdapter.getOrderBook(buySymbol, 100);
        CompletableFuture<OrderBook> sellOrderBookFuture = sellAdapter.getOrderBook(sellSymbol, 100);
        CompletableFuture<List<Candle>> buyHistoryFuture = buyAdapter.getHistoricalData(buySymbol, "1h", 24);
        CompletableFuture<List<Candle>> sellHistoryFuture = sellAdapter.getHistoricalData(sellSymbol, "1h", 24);
        CompletableFuture<Double> buyFeeFuture = buyAdapter.getTradingFee(buySymbol);
        CompletableFuture<Double> sellFeeFuture = sellAdapter.getTradingFee(sellSymbol);
        
        // Combine all futures
        return CompletableFuture.allOf(
                buyTickerFuture, sellTickerFuture,
                buyOrderBookFuture, sellOrderBookFuture,
                buyHistoryFuture, sellHistoryFuture,
                buyFeeFuture, sellFeeFuture
        ).thenApply(v -> {
            try {
                // Get all data from futures
                Ticker buyTicker = buyTickerFuture.get(5, TimeUnit.SECONDS);
                Ticker sellTicker = sellTickerFuture.get(5, TimeUnit.SECONDS);
                OrderBook buyOrderBook = buyOrderBookFuture.get(5, TimeUnit.SECONDS);
                OrderBook sellOrderBook = sellOrderBookFuture.get(5, TimeUnit.SECONDS);
                List<Candle> buyHistory = buyHistoryFuture.get(5, TimeUnit.SECONDS);
                List<Candle> sellHistory = sellHistoryFuture.get(5, TimeUnit.SECONDS);
                double buyFee = buyFeeFuture.get(5, TimeUnit.SECONDS);
                double sellFee = sellFeeFuture.get(5, TimeUnit.SECONDS);
                
                Log.d(TAG, String.format("Successfully fetched data for %s: Buy volume: %.2f, Sell volume: %.2f", 
                     symbol, buyTicker.getVolume(), sellTicker.getVolume()));
                
                // Now calculate the risk metrics
                return calculateRiskAssessment(
                        symbol, buyTicker, sellTicker, buyOrderBook, sellOrderBook,
                        buyHistory, sellHistory, buyFee, sellFee, tradeSize
                );
                
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Log.e(TAG, "Error getting risk data: " + e.getMessage(), e);
                // Return a default assessment with medium-high risk if we can't get data
                return createDefaultRiskAssessment(0.3); // 0.3 = medium-high risk
            }
        });
    }
    
    /**
     * Determine appropriate trade size based on profit percentage
     */
    private double determineTradeSize(double profitPercent) {
        if (profitPercent <= 0.3) { // Less than 0.3% profit
            return DEFAULT_TRADE_SIZE * 0.5; // Half the default size
        } else if (profitPercent >= 1.0) { // 1% or more profit
            return DEFAULT_TRADE_SIZE * 2.0; // Double the default size
        } else {
            return DEFAULT_TRADE_SIZE;
        }
    }
    
    /**
     * Calculate a risk assessment from all available market data
     */
    private RiskAssessment calculateRiskAssessment(
            String symbol,
            Ticker buyTicker, 
            Ticker sellTicker,
            OrderBook buyOrderBook,
            OrderBook sellOrderBook,
            List<Candle> buyHistory,
            List<Candle> sellHistory,
            double buyFee,
            double sellFee,
            double tradeSize) {
        
        // Calculate individual risk components
        double liquidityScore = calculateLiquidityScore(buyTicker, sellTicker);
        double volatilityScore = calculateVolatilityScore(buyHistory, sellHistory);
        double marketDepthScore = calculateMarketDepthScore(buyOrderBook, sellOrderBook);
        double exchangeRiskScore = calculateExchangeRiskScore(buyTicker.getExchangeName(), sellTicker.getExchangeName());
        double slippageEstimate = calculateSlippageEstimate(buyOrderBook, sellOrderBook, tradeSize);
        
        // Calculate execution time estimate
        double executionTimeEstimate = estimateExecutionTime(
                buyTicker.getExchangeName(), 
                sellTicker.getExchangeName(),
                volatilityScore
        );
        
        // Calculate ROI efficiency based on a 1% profit
        double assumedProfit = 0.01; // 1% profit
        double roiEfficiency = calculateRoiEfficiency(assumedProfit, executionTimeEstimate);
        
        // Calculate optimal trade size based on market depth
        double optimalTradeSize = calculateOptimalTradeSize(buyOrderBook, sellOrderBook);
        
        // Calculate overall risk score (higher = less risky)
        double overallRiskScore = calculateOverallRiskScore(
                liquidityScore,
                volatilityScore,
                marketDepthScore,
                exchangeRiskScore,
                slippageEstimate
        );
        
        // Create clear risk level classification
        String riskLevel = getRiskLevelDescription(overallRiskScore);
        
        // Enhanced logging with more detailed breakdown
        Log.d(TAG, "┌──────────────────────────────────────────────");
        Log.d(TAG, String.format("│ RISK ASSESSMENT for %s", symbol));
        Log.d(TAG, "├──────────────────────────────────────────────");
        Log.d(TAG, String.format("│ OVERALL RISK SCORE: %.2f (%s)", overallRiskScore, riskLevel));
        Log.d(TAG, "├──────────────────────────────────────────────");
        Log.d(TAG, String.format("│ Liquidity:     %.2f", liquidityScore));
        Log.d(TAG, String.format("│ Volatility:    %.2f", volatilityScore));
        Log.d(TAG, String.format("│ Market Depth:  %.2f", marketDepthScore));
        Log.d(TAG, String.format("│ Exchange Risk: %.2f", exchangeRiskScore));
        Log.d(TAG, String.format("│ Slippage:      %.4f (%.2f%%)", slippageEstimate, slippageEstimate * 100));
        Log.d(TAG, "├──────────────────────────────────────────────");
        Log.d(TAG, String.format("│ Execution Time: %.1f min", executionTimeEstimate));
        Log.d(TAG, String.format("│ ROI Efficiency: %.2f%% per hour", roiEfficiency * 100));
        Log.d(TAG, String.format("│ Optimal Size:   $%.2f", optimalTradeSize));
        Log.d(TAG, "└──────────────────────────────────────────────");
        
        // Create and return the risk assessment
        RiskAssessment assessment = new RiskAssessment(
                overallRiskScore,
                liquidityScore,
                volatilityScore,
                exchangeRiskScore,
                1.0 - slippageEstimate, // Transaction risk is inverse of slippage
                slippageEstimate,
                executionTimeEstimate,
                roiEfficiency,
                optimalTradeSize
        );
        
        // Set the readable risk level
        assessment.setRiskLevel(riskLevel);
        
        return assessment;
    }
    
    /**
     * Get a human-readable risk level description from score
     */
    private String getRiskLevelDescription(double riskScore) {
        if (riskScore >= 0.8) {
            return "VERY LOW RISK";
        } else if (riskScore >= 0.7) {
            return "LOW RISK";
        } else if (riskScore >= 0.6) {
            return "MODERATE RISK";
        } else if (riskScore >= 0.4) {
            return "MEDIUM RISK";
        } else if (riskScore >= 0.3) {
            return "HIGH RISK";
        } else if (riskScore >= 0.2) {
            return "VERY HIGH RISK";
        } else {
            return "EXTREME RISK";
        }
    }
    
    /**
     * Calculate liquidity score based on ticker volumes
     */
    private double calculateLiquidityScore(Ticker buyTicker, Ticker sellTicker) {
        double buyVolume = buyTicker.getVolume() * buyTicker.getLastPrice(); // Convert to USD
        double sellVolume = sellTicker.getVolume() * sellTicker.getLastPrice(); // Convert to USD
        double avgVolume = (buyVolume + sellVolume) / 2.0;
        
        // Normalize volume to a 0-1 scale
        // $1M+ daily volume = 1.0 score
        // $10k daily volume = 0.1 score
        double normalizedVolume = Math.min(1.0, Math.max(0.1, avgVolume / 1000000.0));
        
        return normalizedVolume;
    }
    
    /**
     * Calculate volatility score based on price history
     * Higher score = lower volatility = less risky
     */
    private double calculateVolatilityScore(List<Candle> buyHistory, List<Candle> sellHistory) {
        if (buyHistory.isEmpty() || sellHistory.isEmpty()) {
            return 0.5; // Medium volatility if no history
        }
        
        // Calculate average price range for both exchanges
        double buyAvgRange = calculateAveragePriceRange(buyHistory);
        double sellAvgRange = calculateAveragePriceRange(sellHistory);
        double avgRange = (buyAvgRange + sellAvgRange) / 2.0;
        
        // Normalize volatility to a 0-1 scale (invert it so higher = less volatile = better)
        // 5%+ range = 0.1 score (high volatility)
        // 0.1% range = 0.9 score (low volatility)
        double normalizedVolatility = 1.0 - Math.min(0.9, Math.max(0.1, avgRange / 5.0));
        
        return normalizedVolatility;
    }
    
    /**
     * Calculate average price range from candles
     */
    private double calculateAveragePriceRange(List<Candle> candles) {
        if (candles.isEmpty()) return 0;
        
        double totalRange = 0;
        for (Candle candle : candles) {
            totalRange += candle.getPriceRangePercent();
        }
        
        return totalRange / candles.size();
    }
    
    /**
     * Calculate market depth score based on order books
     */
    private double calculateMarketDepthScore(OrderBook buyOrderBook, OrderBook sellOrderBook) {
        if (buyOrderBook == null || sellOrderBook == null) {
            return 0.5; // Medium depth if no data
        }
        
        // Calculate depth up to 2% away from mid price
        double buyDepth = buyOrderBook.getDepth(2.0);
        double sellDepth = sellOrderBook.getDepth(2.0);
        double avgDepth = (buyDepth + sellDepth) / 2.0;
        
        // Normalize depth to a 0-1 scale
        // $1M+ depth = 1.0 score
        // $10k depth = 0.1 score
        double normalizedDepth = Math.min(1.0, Math.max(0.1, avgDepth / 1000000.0));
        
        return normalizedDepth;
    }
    
    /**
     * Calculate exchange risk score based on exchange reliability
     */
    private double calculateExchangeRiskScore(String buyExchange, String sellExchange) {
        // In a real implementation, this would use historical exchange reliability data
        // For now, use hard-coded values based on exchange reputation
        Map<String, Double> exchangeScores = new HashMap<>();
        exchangeScores.put("binance", 0.9);   // Very reliable
        exchangeScores.put("coinbase", 0.9);  // Very reliable
        exchangeScores.put("kraken", 0.85);   // Reliable
        exchangeScores.put("okx", 0.8);       // Good
        exchangeScores.put("bybit", 0.75);    // Decent
        
        // Default to medium reliability for unknown exchanges
        double buyScore = exchangeScores.getOrDefault(buyExchange.toLowerCase(), 0.5);
        double sellScore = exchangeScores.getOrDefault(sellExchange.toLowerCase(), 0.5);
        
        // Return average of both exchanges
        return (buyScore + sellScore) / 2.0;
    }
    
    /**
     * Calculate slippage estimate based on order books
     */
    private double calculateSlippageEstimate(OrderBook buyOrderBook, OrderBook sellOrderBook, double tradeSize) {
        if (buyOrderBook == null || sellOrderBook == null || tradeSize <= 0) {
            return 0.005; // 0.5% default slippage
        }
        
        // Calculate buy and sell slippage
        double buySlippage = buyOrderBook.calculateSlippage(tradeSize, true);
        double sellSlippage = sellOrderBook.calculateSlippage(tradeSize, false);
        
        // Total slippage is the sum of both sides
        double totalSlippage = (buySlippage + sellSlippage) / 100.0; // Convert from percentage to decimal
        
        // Log actual calculated slippage before any normalization
        Log.d(TAG, String.format("Raw slippage calculation: buy=%.4f%%, sell=%.4f%%, total=%.4f%%", 
            buySlippage, sellSlippage, totalSlippage * 100));
        
        // Use wider range for more realistic values (0.05% to 10%)
        // Don't artificially constrain real-world data too much
        return Math.min(0.10, Math.max(0.0005, totalSlippage));
    }
    
    /**
     * Estimate execution time in minutes
     */
    private double estimateExecutionTime(String buyExchange, String sellExchange, double volatilityScore) {
        // Base execution time varies more based on market conditions
        double baseTime = 1.5; // Default baseline is 1.5 minutes
        
        // Adjust for exchange speed - more extreme differences
        Map<String, Double> exchangeSpeedFactors = new HashMap<>();
        exchangeSpeedFactors.put("binance", 0.7);  // Faster
        exchangeSpeedFactors.put("coinbase", 1.1); // Slightly slower
        exchangeSpeedFactors.put("kraken", 1.3);   // Slower
        exchangeSpeedFactors.put("kucoin", 1.2);   // Slower
        exchangeSpeedFactors.put("okx", 0.85);     // Above average
        exchangeSpeedFactors.put("bybit", 0.8);    // Fast
        exchangeSpeedFactors.put("huobi", 0.9);    // Above average
        
        double buyFactor = exchangeSpeedFactors.getOrDefault(buyExchange.toLowerCase(), 1.0);
        double sellFactor = exchangeSpeedFactors.getOrDefault(sellExchange.toLowerCase(), 1.0);
        double avgFactor = (buyFactor + sellFactor) / 2.0;
        
        // Higher volatility increases execution time more significantly
        // Low volatility (volatilityScore=0.9) → volatilityFactor≈1.3
        // High volatility (volatilityScore=0.2) → volatilityFactor≈4.0
        double volatilityFactor = 1.0 + (1.0 - volatilityScore) * 3.75;
        
        // Calculate time estimate
        double timeEstimate = baseTime * avgFactor * volatilityFactor;
        
        // Log calculation components
        Log.d(TAG, String.format("Time estimate calculation: base=%.1f, exchange=%.2f, volatility=%.2f → %.2f min", 
            baseTime, avgFactor, volatilityFactor, timeEstimate));
            
        // Allow wider range of execution times (0.5 min to 15 min)
        return Math.min(15.0, Math.max(0.5, timeEstimate));
    }
    
    /**
     * Calculate ROI efficiency (profit per hour)
     */
    private double calculateRoiEfficiency(double profitPercent, double executionTimeMinutes) {
        if (executionTimeMinutes <= 0) return 0;
        
        // Convert profit to decimal
        double profit = profitPercent;
        
        // Calculate profit per hour
        double hourlyRoi = profit * (60.0 / executionTimeMinutes);
        
        // Log ROI efficiency calculation
        Log.d(TAG, String.format("ROI efficiency: %.2f%% profit in %.2f min → %.4f%% per hour", 
            profit * 100, executionTimeMinutes, hourlyRoi * 100));
        
        return hourlyRoi;
    }
    
    /**
     * Calculate optimal trade size based on order book depth
     */
    private double calculateOptimalTradeSize(OrderBook buyOrderBook, OrderBook sellOrderBook) {
        if (buyOrderBook == null || sellOrderBook == null) {
            return DEFAULT_TRADE_SIZE; // Default $1000 if no data
        }
        
        // Calculate depth at different price levels
        double depth1Percent = Math.min(
                buyOrderBook.getDepth(1.0),
                sellOrderBook.getDepth(1.0)
        ) / 2.0; // Average and halve to be conservative
        
        // Optimal size is 10% of available depth at 1% price impact
        double optimalSize = depth1Percent * 0.1;
        
        // Ensure optimal size is reasonable ($100 to $10,000)
        return Math.min(10000.0, Math.max(100.0, optimalSize));
    }
    
    /**
     * Calculate overall risk score as weighted average of components
     */
    private double calculateOverallRiskScore(
            double liquidityScore,
            double volatilityScore,
            double marketDepthScore,
            double exchangeRiskScore,
            double slippageEstimate) {
        
        // Normalize slippage to a 0-1 score (higher = better = less slippage)
        // More extreme curve: 
        // - 0.5% slippage → 0.9 score
        // - 2% slippage → 0.6 score  
        // - 5% slippage → 0.2 score
        // - 10% slippage → 0 score
        double slippageScore = 1.0 - Math.min(1.0, slippageEstimate * 10.0); 
        
        // Log all risk components for debugging
        Log.d(TAG, String.format(
            "Risk components: liquidity=%.2f, volatility=%.2f, depth=%.2f, exchange=%.2f, slippage=%.2f%%→%.2f", 
            liquidityScore, volatilityScore, marketDepthScore, exchangeRiskScore, 
            slippageEstimate * 100, slippageScore));
        
        // Weight each component 
        double liquidityWeight = 0.25;
        double volatilityWeight = 0.25;
        double marketDepthWeight = 0.15;
        double exchangeRiskWeight = 0.15;
        double slippageWeight = 0.20;
        
        // Calculate weighted score
        double weightedScore = 
                (liquidityScore * liquidityWeight) +
                (volatilityScore * volatilityWeight) +
                (marketDepthScore * marketDepthWeight) +
                (exchangeRiskScore * exchangeRiskWeight) +
                (slippageScore * slippageWeight);
        
        return weightedScore;
    }
    
    /**
     * Create a default risk assessment for when API data can't be fetched
     * @param riskLevel 0.0-1.0 where higher is less risky
     */
    private RiskAssessment createDefaultRiskAssessment(double riskLevel) {
        double inverseRisk = 1.0 - riskLevel;
        
        // Generate risk components based on overall risk level
        double liquidityScore = riskLevel * 0.7 + 0.2;
        double volatilityScore = riskLevel * 0.8 + 0.1;
        double exchangeRiskScore = riskLevel * 0.6 + 0.3;
        double slippageEstimate = 0.001 + (inverseRisk * 0.049); // 0.1% to 5%
        
        // Calculate derived metrics
        double executionTimeEstimate = 1.0 + (inverseRisk * 9.0); // 1 to 10 minutes
        double roiEfficiency = 0.01 * (60.0 / executionTimeEstimate); // 1% profit 
        double optimalTradeSize = 100.0 + (riskLevel * 900.0); // $100 to $1000
        
        // Set up the risk level based on score
        String riskLevelDesc = getRiskLevelDescription(riskLevel);
        
        // Log the default assessment values
        Log.d(TAG, "┌──────────────────────────────────────────────");
        Log.d(TAG, "│ DEFAULT RISK ASSESSMENT");
        Log.d(TAG, "├──────────────────────────────────────────────");
        Log.d(TAG, String.format("│ OVERALL RISK SCORE: %.2f (%s)", riskLevel, riskLevelDesc));
        Log.d(TAG, "├──────────────────────────────────────────────");
        Log.d(TAG, String.format("│ Liquidity:     %.2f", liquidityScore));
        Log.d(TAG, String.format("│ Volatility:    %.2f", volatilityScore));
        Log.d(TAG, String.format("│ Exchange Risk: %.2f", exchangeRiskScore));
        Log.d(TAG, String.format("│ Slippage:      %.4f (%.2f%%)", slippageEstimate, slippageEstimate * 100));
        Log.d(TAG, "├──────────────────────────────────────────────");
        Log.d(TAG, String.format("│ Execution Time: %.1f min", executionTimeEstimate));
        Log.d(TAG, String.format("│ ROI Efficiency: %.2f%% per hour", roiEfficiency * 100));
        Log.d(TAG, String.format("│ Optimal Size:   $%.2f", optimalTradeSize));
        Log.d(TAG, "└──────────────────────────────────────────────");
        
        // Return assembled assessment
        RiskAssessment assessment = new RiskAssessment(
                riskLevel,
                liquidityScore,
                volatilityScore,
                exchangeRiskScore,
                1.0 - slippageEstimate,
                slippageEstimate,
                executionTimeEstimate,
                roiEfficiency,
                optimalTradeSize
        );
        
        // Set the human-readable risk level
        assessment.setRiskLevel(riskLevelDesc);
        
        return assessment;
    }
    
    /**
     * Format a risk assessment for the UI
     */
    public String formatRiskAssessmentForDisplay(RiskAssessment assessment) {
        if (assessment == null) {
            return "No risk assessment available";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Risk Level: ").append(assessment.getRiskLevel()).append("\n\n");
        
        // Format slippage for display
        double slippagePercent = assessment.getSlippageEstimate() * 100;
        String slippageText = String.format("%.2f%%", slippagePercent);
        
        // Format execution time
        String executionTimeText;
        double executionTime = assessment.getExecutionTimeEstimate();
        if (executionTime < 1.0) {
            executionTimeText = String.format("%.0f seconds", executionTime * 60);
        } else {
            executionTimeText = String.format("%.1f minutes", executionTime);
        }
        
        // Format ROI efficiency 
        double hourlyRoi = assessment.getRoiEfficiency() * 100;
        String roiText = String.format("%.2f%% per hour", hourlyRoi);
        
        // Add main metrics
        sb.append("Expected Slippage: ").append(slippageText).append("\n");
        sb.append("Execution Time: ").append(executionTimeText).append("\n");
        sb.append("ROI Efficiency: ").append(roiText).append("\n\n");
        
        // Add component scores
        sb.append("Liquidity: ").append(formatScoreWithStars(assessment.getLiquidityRiskScore())).append("\n");
        sb.append("Volatility: ").append(formatScoreWithStars(assessment.getVolatilityRiskScore())).append("\n");
        sb.append("Exchange: ").append(formatScoreWithStars(assessment.getExchangeRiskScore())).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Format a score as star rating (★★★☆☆)
     */
    private String formatScoreWithStars(double score) {
        // Convert 0-1 score to 0-5 stars
        int stars = (int) Math.round(score * 5);
        StringBuilder sb = new StringBuilder();
        
        // Add filled stars
        for (int i = 0; i < stars; i++) {
            sb.append("★");
        }
        
        // Add empty stars
        for (int i = stars; i < 5; i++) {
            sb.append("☆");
        }
        
        return sb.toString();
    }
    
    /**
     * Get the appropriate exchange adapter
     */
    private ExchangeApiAdapter getExchangeAdapter(String exchangeName) {
        if (exchangeName == null) return null;
        
        String normalizedName = exchangeName.toLowerCase().trim();
        return exchangeAdapters.get(normalizedName);
    }
    
    /**
     * Add or update an exchange adapter
     */
    public void addExchangeAdapter(String exchangeName, ExchangeApiAdapter adapter) {
        if (exchangeName != null && adapter != null) {
            exchangeAdapters.put(exchangeName.toLowerCase().trim(), adapter);
        }
    }
} 