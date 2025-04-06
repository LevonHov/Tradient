package com.example.tradient.ui.opportunities;

import android.util.Pair;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.Exchange;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.data.service.ExchangeServiceFactory;
import com.example.tradient.domain.profit.ProfitCalculator;
import com.example.tradient.domain.profit.ProfitResult;
import com.example.tradient.domain.risk.RiskCalculator;
import com.example.tradient.domain.risk.SlippageManagerService;
import com.example.tradient.domain.risk.VolatilityService;
import com.example.tradient.domain.market.ExchangeLiquidityService;
import com.example.tradient.domain.market.ExchangeLiquidityService.ArbitrageLiquidityMetrics;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;

/**
 * ViewModel for OpportunityDetailActivity that handles data loading and business logic.
 * Follows the MVVM pattern used in the rest of the app.
 */
public class OpportunityDetailViewModel extends ViewModel {
    private static final String TAG = "OpportunityDetailViewModel";
    
    // Services and calculators
    private final ExecutorService executorService;
    private final RiskCalculator riskCalculator;
    private final SlippageManagerService slippageManager;
    private final VolatilityService volatilityService;
    private final ExchangeLiquidityService liquidityService;
    
    // Formatters
    private final DecimalFormat priceFormat;
    private final DecimalFormat percentFormat;
    
    // UI state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> lastUpdateTime = new MutableLiveData<>();
    
    // Market data
    private final MutableLiveData<List<Ticker>> buyExchangeTickers = new MutableLiveData<>();
    private final MutableLiveData<List<Ticker>> sellExchangeTickers = new MutableLiveData<>();
    private final MutableLiveData<OrderBook> buyOrderBook = new MutableLiveData<>();
    private final MutableLiveData<OrderBook> sellOrderBook = new MutableLiveData<>();
    
    // Performance metrics
    private final MutableLiveData<Integer> timeEstimateSeconds = new MutableLiveData<>();
    private final MutableLiveData<Double> volatility = new MutableLiveData<>();
    private final MutableLiveData<Double> optimalTradeSize = new MutableLiveData<>();
    private final MutableLiveData<Double> roiEfficiency = new MutableLiveData<>();
    private final MutableLiveData<Double> riskScore = new MutableLiveData<>();
    
    // Current profit data
    private final MutableLiveData<Double> currentProfit = new MutableLiveData<>();
    private final MutableLiveData<Double> currentBuyPrice = new MutableLiveData<>();
    private final MutableLiveData<Double> currentSellPrice = new MutableLiveData<>();
    
    // Opportunity details
    private ArbitrageOpportunity opportunity;
    private ExchangeService buyExchangeService;
    private ExchangeService sellExchangeService;
    
    // Liquidity metrics
    private final MutableLiveData<Double> combinedLiquidity = new MutableLiveData<>();
    private final MutableLiveData<Double> bidLiquidity = new MutableLiveData<>();
    private final MutableLiveData<Double> askLiquidity = new MutableLiveData<>();
    private final MutableLiveData<Map<Double, Double>> slippageMap = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<ArbitrageLiquidityMetrics> liquidityMetrics = new MutableLiveData<>();
    
    /**
     * Initialize the ViewModel with an opportunity
     */
    public OpportunityDetailViewModel() {
        executorService = Executors.newCachedThreadPool();
        riskCalculator = new RiskCalculator();
        slippageManager = new SlippageManagerService();
        volatilityService = new VolatilityService();
        liquidityService = new ExchangeLiquidityService();
        
        // Initialize formatters
        priceFormat = new DecimalFormat("#,##0.00####");
        percentFormat = new DecimalFormat("0.00%");
    }
    
    @Override
    protected void onCleared() {
        // Shutdown executor when ViewModel is cleared
        executorService.shutdown();
        super.onCleared();
    }
    
    /**
     * Set the opportunity to be displayed and initialize data
     */
    public void setOpportunity(ArbitrageOpportunity opportunity) {
        this.opportunity = opportunity;
        
        // Initialize services
        buyExchangeService = ExchangeServiceFactory.getExchangeService(
                Exchange.valueOf(opportunity.getBuyExchangeName().toUpperCase()));
        sellExchangeService = ExchangeServiceFactory.getExchangeService(
                Exchange.valueOf(opportunity.getSellExchangeName().toUpperCase()));
        
        // Set initial values
        currentProfit.setValue(opportunity.getPercentageProfit() / 100.0);
        currentBuyPrice.setValue(opportunity.getBuyPrice());
        currentSellPrice.setValue(opportunity.getSellPrice());
        
        // Load data
        loadDetailedData();
    }
    
    /**
     * Load detailed data for the opportunity
     */
    public void loadDetailedData() {
        isLoading.setValue(true);
        
        executorService.execute(() -> {
            try {
                // Load order books
                OrderBook buyOrderBookData = buyExchangeService.getOrderBook(opportunity.getSymbol());
                OrderBook sellOrderBookData = sellExchangeService.getOrderBook(opportunity.getSymbol());
                
                // Load historical ticker data
                List<Ticker> buyExchangeTickerData = buyExchangeService.getHistoricalTickers(opportunity.getSymbol(), 24);
                List<Ticker> sellExchangeTickerData = sellExchangeService.getHistoricalTickers(opportunity.getSymbol(), 24);
                
                // Calculate metrics using real-time services
                int timeEstimate = calculateTimeEstimate(buyExchangeService, sellExchangeService);
                
                // Calculate volatility with enhanced methods and error handling
                double volatilityValue = 0.03; // Default moderate volatility
                try {
                    // Calculate base volatility
                    volatilityValue = volatilityService.calculateVolatility(
                            buyExchangeTickerData, 
                            sellExchangeTickerData, 
                            opportunity.getSymbol());
                    
                    // Enhance with market sentiment
                    volatilityValue = volatilityService.calculateVolatilityWithMarketSentiment(
                            opportunity.getSymbol(), 
                            volatilityValue);
                    
                    Log.d("OpportunityDetailVM", "Calculated volatility for " + 
                            opportunity.getSymbol() + ": " + volatilityValue);
                } catch (Exception e) {
                    Log.e("OpportunityDetailVM", "Error calculating volatility: " + e.getMessage(), e);
                    // We'll use the default value set above
                }
                
                // Calculate real liquidity metrics with proper error handling
                ArbitrageLiquidityMetrics liquidityData = null;
                try {
                    liquidityData = liquidityService.calculateArbitrageLiquidity(
                            buyExchangeService, 
                            sellExchangeService, 
                            opportunity.getSymbol());
                    
                    if (liquidityData == null) {
                        throw new Exception("Failed to fetch liquidity data");
                    }
                } catch (Exception e) {
                    Log.e("OpportunityDetailVM", "Error calculating liquidity: " + e.getMessage(), e);
                    // Don't rethrow - we'll use fallbacks instead
                }
                
                // Ensure we always have some liquidity data
                if (liquidityData == null) {
                    Log.w("OpportunityDetailVM", "Using default liquidity values for " + opportunity.getSymbol());
                    // Create fallback values
                    double defaultLiquidity = getDefaultLiquidity(opportunity.getSymbol());
                    combinedLiquidity.postValue(defaultLiquidity);
                    bidLiquidity.postValue(defaultLiquidity * 0.5);
                    askLiquidity.postValue(defaultLiquidity * 0.5);
                    
                    Map<Double, Double> defaultSlippage = new HashMap<>();
                    defaultSlippage.put(1000.0, 0.002);
                    defaultSlippage.put(5000.0, 0.005);
                    defaultSlippage.put(10000.0, 0.01);
                    defaultSlippage.put(25000.0, 0.015);
                    defaultSlippage.put(50000.0, 0.025);
                    slippageMap.postValue(defaultSlippage);
                    
                    // Use estimated optimal trade size
                    double optimalTradeSizeValue = Math.min(10000.0, defaultLiquidity * 0.1);
                    optimalTradeSize.postValue(optimalTradeSizeValue);
                    
                    // Calculate liquidity factor for risk score
                    double liquidityValue = defaultLiquidity / 1000000.0; // Normalize to 0-1 range
                    liquidityValue = Math.min(1.0, Math.max(0.1, liquidityValue));
                    
                    // Calculate risk score with estimated values
                    double riskScoreValue = calculateRiskScore(
                            volatilityValue, 
                            liquidityValue,
                            opportunity.getPercentageProfit() / 100.0);
                    riskScore.postValue(riskScoreValue);
                } else {
                    // Use real liquidity data
                    double optimalTradeSizeValue = liquidityData.getOptimalTradeSize();
                    double roiEfficiencyValue = calculateRoiEfficiency(
                            opportunity.getPercentageProfit() / 100.0, 
                            timeEstimate);
                    
                    // Use liquidity value from real exchange data
                    double liquidityValue = liquidityData.getCombinedLiquidity() / 1000000.0; // Normalize to 0-1 range
                    liquidityValue = Math.min(1.0, Math.max(0.1, liquidityValue));
                    
                    double riskScoreValue = calculateRiskScore(
                            volatilityValue, 
                            liquidityValue,
                            opportunity.getPercentageProfit() / 100.0);
                    
                    // Update LiveData for liquidity metrics
                    combinedLiquidity.postValue(liquidityData.getCombinedLiquidity());
                    bidLiquidity.postValue(liquidityData.getBuyExchangeMetrics().getAvailableLiquidity());
                    askLiquidity.postValue(liquidityData.getSellExchangeMetrics().getAvailableLiquidity());
                    slippageMap.postValue(liquidityData.getSlippageMap());
                    liquidityMetrics.postValue(liquidityData);
                    optimalTradeSize.postValue(optimalTradeSizeValue);
                    riskScore.postValue(riskScoreValue);
                }
                
                // Update other metrics that aren't liquidity dependent
                buyOrderBook.postValue(buyOrderBookData);
                sellOrderBook.postValue(sellOrderBookData);
                buyExchangeTickers.postValue(buyExchangeTickerData);
                sellExchangeTickers.postValue(sellExchangeTickerData);
                timeEstimateSeconds.postValue(timeEstimate);
                volatility.postValue(volatilityValue);
                roiEfficiency.postValue(calculateRoiEfficiency(
                        opportunity.getPercentageProfit() / 100.0, 
                        timeEstimate));
                
                updateLastUpdateTime();
                isLoading.postValue(false);
            } catch (Exception e) {
                errorMessage.postValue("Error loading data: " + e.getMessage());
                
                // Still try to provide some sensible defaults
                double defaultLiquidity = getDefaultLiquidity(opportunity.getSymbol());
                combinedLiquidity.postValue(defaultLiquidity);
                optimalTradeSize.postValue(Math.min(10000.0, defaultLiquidity * 0.1));
                
                // Provide default volatility based on asset type
                double defaultVolatility = 0.03; // Default moderate volatility
                volatility.postValue(defaultVolatility);
                
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Refresh current market data for real-time updates
     */
    public void refreshMarketData() {
        executorService.execute(() -> {
            try {
                // Get fresh ticker data
                Ticker buyTicker = buyExchangeService.getTickerData(opportunity.getSymbol());
                Ticker sellTicker = sellExchangeService.getTickerData(opportunity.getSymbol());
                
                if (buyTicker != null && sellTicker != null) {
                    // Calculate updated profit
                    double buyPrice = buyTicker.getAskPrice();
                    double sellPrice = sellTicker.getBidPrice();
                    double buyFee = buyExchangeService.getExchangeFee(opportunity.getSymbol(), false);
                    double sellFee = sellExchangeService.getExchangeFee(opportunity.getSymbol(), false);
                    
                    ProfitResult result = ProfitCalculator.calculateArbitrageProfit(
                            buyPrice, sellPrice, buyFee, sellFee, 0.0);
                    double profit = result.getPercentageProfit() / 100.0;
                    
                    // Update LiveData
                    currentProfit.postValue(profit);
                    currentBuyPrice.postValue(buyPrice);
                    currentSellPrice.postValue(sellPrice);
                    
                    // Update volatility with real-time data and market sentiment
                    double volatilityValue = 0.03; // Default fallback value
                    try {
                        // Get base volatility from real-time data
                        volatilityValue = volatilityService.calculateRealTimeVolatility(
                                buyExchangeService, 
                                sellExchangeService, 
                                opportunity.getSymbol());
                        
                        // Enhance with market sentiment
                        volatilityValue = volatilityService.calculateVolatilityWithMarketSentiment(
                                opportunity.getSymbol(), 
                                volatilityValue);
                        
                        Log.d("OpportunityDetailVM", "Refresh: Updated volatility for " + 
                                opportunity.getSymbol() + ": " + volatilityValue);
                    } catch (Exception e) {
                        Log.e("OpportunityDetailVM", "Error refreshing volatility: " + e.getMessage(), e);
                        // Keep existing volatility if available
                        Double currentVolatility = volatility.getValue();
                        if (currentVolatility != null && currentVolatility > 0) {
                            volatilityValue = currentVolatility;
                        }
                        // Otherwise use the default value set above
                    }
                    
                    // Update metrics in the ViewModel
                    volatility.postValue(volatilityValue);
                    
                    // Update liquidity with real-time data - with error handling
                    ArbitrageLiquidityMetrics liquidityData = null;
                    try {
                        liquidityData = liquidityService.calculateArbitrageLiquidity(
                                buyExchangeService, 
                                sellExchangeService, 
                                opportunity.getSymbol());
                        
                        if (liquidityData != null) {
                            // Update LiveData for liquidity
                            double liquidityValue = liquidityData.getCombinedLiquidity() / 1000000.0; // Normalize to 0-1 range
                            liquidityValue = Math.min(1.0, Math.max(0.1, liquidityValue));
                            
                            combinedLiquidity.postValue(liquidityData.getCombinedLiquidity());
                            bidLiquidity.postValue(liquidityData.getBuyExchangeMetrics().getAvailableLiquidity());
                            askLiquidity.postValue(liquidityData.getSellExchangeMetrics().getAvailableLiquidity());
                            optimalTradeSize.postValue(liquidityData.getOptimalTradeSize());
                            slippageMap.postValue(liquidityData.getSlippageMap());
                            liquidityMetrics.postValue(liquidityData);
                            
                            // Recalculate risk score with new data
                            double newRiskScore = calculateRiskScore(
                                    volatilityValue, 
                                    liquidityValue,
                                    profit);
                            riskScore.postValue(newRiskScore);
                        } else {
                            // Keep previous liquidity data
                            Log.w("OpportunityDetailVM", "Refresh: Failed to get liquidity data, keeping previous values");
                        }
                    } catch (Exception e) {
                        Log.e("OpportunityDetailVM", "Error refreshing liquidity: " + e.getMessage(), e);
                        // We'll keep the previous liquidity values rather than using fallbacks
                    }
                    
                    updateLastUpdateTime();
                } else {
                    // Log warning but don't update UI with error
                    Log.w("OpportunityDetailVM", "Failed to get fresh ticker data for refresh");
                }
            } catch (Exception e) {
                // Only show error message for critical failures
                String errorMsg = "Error refreshing data: " + e.getMessage();
                Log.e("OpportunityDetailVM", errorMsg, e);
                
                if (e instanceof NullPointerException || errorMsg.contains("Network")) {
                    errorMessage.postValue(errorMsg);
                }
            }
        });
    }
    
    /**
     * Simulate a trade with the given amount
     */
    public Pair<Double, Double> simulateTrade(double amount) {
        double buyPrice = currentBuyPrice.getValue() != null ? currentBuyPrice.getValue() : opportunity.getBuyPrice();
        double sellPrice = currentSellPrice.getValue() != null ? currentSellPrice.getValue() : opportunity.getSellPrice();
        double buyFee = opportunity.getBuyFee();
        double sellFee = opportunity.getSellFee();
        
        // Calculate coin amount purchased
        double coinAmount = amount / buyPrice;
        double buyFeeAmount = amount * buyFee;
        
        // Calculate sell proceeds
        double sellAmount = coinAmount * sellPrice;
        double sellFeeAmount = sellAmount * sellFee;
        
        // Calculate net profit
        double netProfit = sellAmount - sellFeeAmount - amount - buyFeeAmount;
        double profitPercentage = (netProfit / amount) * 100;
        
        return new Pair<>(netProfit, profitPercentage);
    }
    
    // Helper methods for calculations
    
    private int calculateTimeEstimate(ExchangeService buyExchange, ExchangeService sellExchange) {
        // Sum of the estimated response times for both exchanges, plus buffer
        return buyExchange.getEstimatedResponseTimeMs() + 
               sellExchange.getEstimatedResponseTimeMs() + 
               2000; // 2 seconds buffer
    }
    
    private double calculateRiskScore(double volatility, double liquidityFactor, double profit) {
        // Higher volatility = higher risk
        double volatilityRisk = volatility * 0.4;
        
        // Lower liquidity = higher risk
        double liquidityRisk = (1 - liquidityFactor) * 0.4;
        
        // Profit factor (higher profit = lower risk, but non-linear)
        // Math.max to prevent negative values if profit is negative
        double profitFactor = Math.max(0, 0.2 - (profit * 2));
        
        // Combine the factors (higher score = higher risk)
        double riskScore = volatilityRisk + liquidityRisk + profitFactor;
        
        // Ensure the score is between 0.0 and 1.0
        return Math.max(0.0, Math.min(1.0, riskScore));
    }
    
    private double calculateRoiEfficiency(double profit, int timeEstimateSeconds) {
        // Convert to hourly rate
        double secondsPerHour = 3600.0;
        return profit * (secondsPerHour / timeEstimateSeconds);
    }
    
    private double calculateOptimalTradeSize(OrderBook buyOrderBook, OrderBook sellOrderBook, double profit) {
        if (buyOrderBook == null || sellOrderBook == null) {
            return 1000.0; // Default $1000 if no data
        }
        
        // Get total volume at each price level from both order books
        Map<Double, Double> buyLevels = buyOrderBook.getAsksAsMap();
        Map<Double, Double> sellLevels = sellOrderBook.getBidsAsMap();
        
        // Find the best bid/ask prices
        double lowestAsk = Double.MAX_VALUE;
        for (Double price : buyLevels.keySet()) {
            if (price < lowestAsk) {
                lowestAsk = price;
            }
        }
        
        double highestBid = 0;
        for (Double price : sellLevels.keySet()) {
            if (price > highestBid) {
                highestBid = price;
            }
        }
        
        // Calculate initial spread and profit
        double spreadAmount = highestBid - lowestAsk;
        double initialSpreadPct = spreadAmount / lowestAsk;
        
        // Calculate fees
        double buyFee = buyExchangeService.getExchangeFee(opportunity.getSymbol(), false);
        double sellFee = sellExchangeService.getExchangeFee(opportunity.getSymbol(), false);
        double totalFees = buyFee + sellFee;
        
        // Calculate max trade size that maintains profitability
        double maxSize = 0;
        double cumulativeBuyVolume = 0;
        double cumulativeSellVolume = 0;
        double estimatedSlippage = 0;
        double targetProfit = profit * 0.8; // Target 80% of the quoted profit to account for market movement
        
        // Sort price levels for slippage calculation
        List<Double> askPrices = new ArrayList<>(buyLevels.keySet());
        Collections.sort(askPrices); // Ascending order for asks
        
        List<Double> bidPrices = new ArrayList<>(sellLevels.keySet());
        Collections.sort(bidPrices, Collections.reverseOrder()); // Descending order for bids
        
        // Calculate position size at which slippage would reduce profit below target
        for (int i = 0; i < Math.min(5, askPrices.size()); i++) {
            double askPrice = askPrices.get(i);
            double askVolume = buyLevels.get(askPrice);
            
            if (i > 0) {
                // Calculate price slippage from best price
                estimatedSlippage += (askPrice - lowestAsk) * (askVolume / lowestAsk);
            }
            
            cumulativeBuyVolume += askVolume * askPrice; // Convert to USD value
        }
        
        for (int i = 0; i < Math.min(5, bidPrices.size()); i++) {
            double bidPrice = bidPrices.get(i);
            double bidVolume = sellLevels.get(bidPrice);
            
            if (i > 0) {
                // Calculate price slippage from best price
                estimatedSlippage += (highestBid - bidPrice) * (bidVolume / highestBid);
            }
            
            cumulativeSellVolume += bidVolume * bidPrice; // Convert to USD value
        }
        
        // Maximum volume before slippage becomes problematic
        double totalLiquidity = Math.min(cumulativeBuyVolume, cumulativeSellVolume);
        
        // Adjust liquidity based on spread and fees
        if (initialSpreadPct > 0 && totalFees < initialSpreadPct) {
            // Calculate how much volume we can trade before slippage erodes our profit
            double slippageMargin = initialSpreadPct - totalFees - targetProfit;
            
            if (slippageMargin > 0 && estimatedSlippage > 0) {
                // Determine max size where slippage doesn't exceed our margin
                maxSize = Math.min(totalLiquidity, slippageMargin / estimatedSlippage * totalLiquidity);
            } else {
                // No margin for slippage, or no estimable slippage
                maxSize = totalLiquidity * 0.05; // Conservative 5% of liquidity
            }
        } else {
            // Spread is negative or smaller than fees, so arbitrage is not profitable
            // Return minimum size for UI display purposes
            maxSize = 100.0;
        }
        
        // Apply sensible constraints
        double minSize = 100.0; // Minimum $100 to be worth the effort
        double maxReasonableSize = 50000.0; // Cap at $50,000 for risk management
        
        return Math.max(minSize, Math.min(maxSize, maxReasonableSize));
    }
    
    private void updateLastUpdateTime() {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        lastUpdateTime.postValue(timestamp);
    }
    
    // LiveData getters
    
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public LiveData<Double> getCurrentProfit() {
        return currentProfit;
    }
    
    public LiveData<Double> getCurrentBuyPrice() {
        return currentBuyPrice;
    }
    
    public LiveData<Double> getCurrentSellPrice() {
        return currentSellPrice;
    }
    
    public LiveData<OrderBook> getBuyOrderBook() {
        return buyOrderBook;
    }
    
    public LiveData<OrderBook> getSellOrderBook() {
        return sellOrderBook;
    }
    
    public LiveData<List<Ticker>> getBuyExchangeTickers() {
        return buyExchangeTickers;
    }
    
    public LiveData<List<Ticker>> getSellExchangeTickers() {
        return sellExchangeTickers;
    }
    
    public LiveData<Integer> getTimeEstimateSeconds() {
        return timeEstimateSeconds;
    }
    
    public LiveData<Double> getVolatility() {
        return volatility;
    }
    
    public LiveData<Double> getOptimalTradeSize() {
        return optimalTradeSize;
    }
    
    public LiveData<Double> getRoiEfficiency() {
        return roiEfficiency;
    }
    
    public LiveData<Double> getRiskScore() {
        return riskScore;
    }
    
    public ArbitrageOpportunity getOpportunity() {
        return opportunity;
    }
    
    /**
     * Fetch and return raw exchange data with actual numerical values
     * This provides direct access to the numbers from exchange APIs
     * 
     * @return A formatted string with all the raw numerical data
     */
    public String getRawExchangeData() {
        StringBuilder data = new StringBuilder();
        data.append("RAW EXCHANGE API DATA\n");
        data.append("====================\n\n");
        
        try {
            // 1. Get current ticker data
            Ticker buyTicker = buyExchangeService.getTickerData(opportunity.getSymbol());
            Ticker sellTicker = sellExchangeService.getTickerData(opportunity.getSymbol());
            
            data.append("CURRENT PRICES:\n");
            data.append(opportunity.getBuyExchangeName()).append(" Bid: ").append(priceFormat.format(buyTicker.getBidPrice())).append("\n");
            data.append(opportunity.getBuyExchangeName()).append(" Ask: ").append(priceFormat.format(buyTicker.getAskPrice())).append("\n");
            data.append(opportunity.getSellExchangeName()).append(" Bid: ").append(priceFormat.format(sellTicker.getBidPrice())).append("\n");
            data.append(opportunity.getSellExchangeName()).append(" Ask: ").append(priceFormat.format(sellTicker.getAskPrice())).append("\n");
            data.append(opportunity.getBuyExchangeName()).append(" 24h Volume: ").append(priceFormat.format(buyTicker.getVolume())).append("\n");
            data.append(opportunity.getSellExchangeName()).append(" 24h Volume: ").append(priceFormat.format(sellTicker.getVolume())).append("\n\n");
            
            // 2. Get order book data
            OrderBook buyOrderBook = buyExchangeService.getOrderBook(opportunity.getSymbol());
            OrderBook sellOrderBook = sellExchangeService.getOrderBook(opportunity.getSymbol());
            
            data.append("ORDER BOOK DEPTH:\n");
            // Get top 5 levels from each side
            data.append(opportunity.getBuyExchangeName()).append(" Top 5 Asks:\n");
            Map<Double, Double> buyAsks = buyOrderBook.getAsksAsMap();
            List<Double> askPrices = new ArrayList<>(buyAsks.keySet());
            Collections.sort(askPrices);
            for (int i = 0; i < Math.min(5, askPrices.size()); i++) {
                Double price = askPrices.get(i);
                data.append("  Price: ").append(priceFormat.format(price))
                    .append(", Volume: ").append(priceFormat.format(buyAsks.get(price))).append("\n");
            }
            
            data.append(opportunity.getSellExchangeName()).append(" Top 5 Bids:\n");
            Map<Double, Double> sellBids = sellOrderBook.getBidsAsMap();
            List<Double> bidPrices = new ArrayList<>(sellBids.keySet());
            Collections.sort(bidPrices, Collections.reverseOrder());
            for (int i = 0; i < Math.min(5, bidPrices.size()); i++) {
                Double price = bidPrices.get(i);
                data.append("  Price: ").append(priceFormat.format(price))
                    .append(", Volume: ").append(priceFormat.format(sellBids.get(price))).append("\n");
            }
            
            // 3. Get historical data volatility
            List<Ticker> buyHistory = buyExchangeService.getHistoricalTickers(opportunity.getSymbol(), 24);
            List<Ticker> sellHistory = sellExchangeService.getHistoricalTickers(opportunity.getSymbol(), 24);
            
            data.append("\nHISTORICAL DATA (Last 6 hours):\n");
            data.append(opportunity.getBuyExchangeName()).append(" Prices:\n");
            for (int i = Math.max(0, buyHistory.size() - 6); i < buyHistory.size(); i++) {
                Ticker t = buyHistory.get(i);
                data.append("  ").append(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(t.getTimestamp()))
                    .append(": ").append(priceFormat.format(t.getLastPrice())).append("\n");
            }
            
            data.append(opportunity.getSellExchangeName()).append(" Prices:\n");
            for (int i = Math.max(0, sellHistory.size() - 6); i < sellHistory.size(); i++) {
                Ticker t = sellHistory.get(i);
                data.append("  ").append(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(t.getTimestamp()))
                    .append(": ").append(priceFormat.format(t.getLastPrice())).append("\n");
            }
            
            // 4. Show calculated metrics using real data
            double realVolatility = volatilityService.calculateVolatility(buyHistory, sellHistory, opportunity.getSymbol());
            data.append("\nCALCULATED METRICS:\n");
            data.append("Volatility (24h): ").append(percentFormat.format(realVolatility)).append("\n");
            
            double spreadAmount = Math.abs(sellTicker.getBidPrice() - buyTicker.getAskPrice());
            double spreadPercent = spreadAmount / buyTicker.getAskPrice();
            data.append("Current Spread: ").append(priceFormat.format(spreadAmount))
                 .append(" (").append(percentFormat.format(spreadPercent)).append(")\n");
            
            double buyFee = buyExchangeService.getExchangeFee(opportunity.getSymbol(), false);
            double sellFee = sellExchangeService.getExchangeFee(opportunity.getSymbol(), false);
            data.append("Buy Fee: ").append(percentFormat.format(buyFee)).append("\n");
            data.append("Sell Fee: ").append(percentFormat.format(sellFee)).append("\n");
            
            double profitAfterFees = spreadPercent - buyFee - sellFee;
            data.append("Net Profit: ").append(percentFormat.format(profitAfterFees)).append("\n");
        } catch (Exception e) {
            data.append("Error fetching real-time data: ").append(e.getMessage());
        }
        
        return data.toString();
    }
    
    /**
     * Calculate slippage for a specific trade size based on liquidity metrics
     * 
     * @param tradeSize the size of the trade in USD
     * @return estimated slippage as a decimal percentage (e.g., 0.01 = 1%)
     */
    public double calculateSlippageForTradeSize(double tradeSize) {
        ArbitrageLiquidityMetrics metrics = liquidityMetrics.getValue();
        if (metrics == null) {
            return 0.01; // Default 1% slippage if no data available
        }
        
        // Use slippage map if available
        Map<Double, Double> slippageMap = this.slippageMap.getValue();
        if (slippageMap != null && !slippageMap.isEmpty()) {
            // Find nearest key in the map
            Double closestKey = null;
            double minDiff = Double.MAX_VALUE;
            
            for (Double size : slippageMap.keySet()) {
                double diff = Math.abs(size - tradeSize);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestKey = size;
                }
            }
            
            if (closestKey != null) {
                return slippageMap.get(closestKey);
            }
        }
        
        // Fallback: Calculate slippage based on liquidity metrics
        // Higher trade size relative to available liquidity = higher slippage
        double bidLiquidity = metrics.getBuyExchangeMetrics().getAvailableLiquidity();
        double askLiquidity = metrics.getSellExchangeMetrics().getAvailableLiquidity();
        double avgLiquidity = (bidLiquidity + askLiquidity) / 2;
        
        if (avgLiquidity <= 0) {
            return 0.01; // Default 1% if no liquidity data
        }
        
        // Formula: Higher trade size relative to liquidity = higher slippage
        // Base slippage is 0.1% for small trades, scaling up with size
        double baseSlippage = 0.001; // 0.1% base slippage
        double liquidityRatio = tradeSize / avgLiquidity;
        
        // Slippage increases more rapidly as liquidityRatio approaches 0.2 (20% of available liquidity)
        if (liquidityRatio > 0.2) {
            // Exponential increase for trade sizes above 20% of available liquidity
            return baseSlippage + Math.pow(liquidityRatio - 0.2, 1.5) * 0.1;
        } else {
            // Linear increase for smaller trade sizes
            return baseSlippage + (liquidityRatio * 0.025);
        }
    }
    
    /**
     * Calculate net profit percentage after accounting for slippage
     * 
     * @param tradeSize the trade size in USD
     * @return profit percentage as a decimal (e.g., 0.01 = 1%)
     */
    public double calculateNetProfitAfterSlippage(double tradeSize) {
        // Get base profit percentage without slippage
        double baseProfitPercentage = 0;
        Double currentProfit = getCurrentProfit().getValue();
        if (currentProfit != null) {
            baseProfitPercentage = currentProfit;
        } else if (opportunity != null) {
            baseProfitPercentage = opportunity.getPercentageProfit() / 100.0;
        }
        
        // Calculate slippage for this trade size
        double slippage = calculateSlippageForTradeSize(tradeSize);
        
        // Apply slippage factor to both buy and sell
        // For simplicity, we reduce profit by 2x slippage (affects both buy and sell)
        double netProfit = baseProfitPercentage - (2 * slippage);
        
        // Apply exchange fees
        if (opportunity != null) {
            double totalFees = opportunity.getBuyFee() + opportunity.getSellFee();
            netProfit -= totalFees;
        }
        
        return Math.max(netProfit, -0.1); // Cap loss at 10%
    }
    
    /**
     * Get liquidity metrics for UI display
     */
    public LiveData<Double> getCombinedLiquidity() {
        return combinedLiquidity;
    }
    
    public LiveData<Double> getBidLiquidity() {
        return bidLiquidity;
    }
    
    public LiveData<Double> getAskLiquidity() {
        return askLiquidity;
    }
    
    public LiveData<Map<Double, Double>> getSlippageMap() {
        return slippageMap;
    }
    
    public LiveData<ArbitrageLiquidityMetrics> getLiquidityMetrics() {
        return liquidityMetrics;
    }
    
    /**
     * Provide default liquidity values based on the asset type
     */
    private double getDefaultLiquidity(String symbol) {
        // Extract the base asset from the symbol
        String baseAsset = symbol.split("/")[0];
        if (baseAsset == null) {
            baseAsset = symbol.length() > 3 ? symbol.substring(0, 3) : symbol;
        }
        
        // Assign default values based on asset type
        switch (baseAsset.toUpperCase()) {
            case "BTC": return 500000.0; // $500K for BTC
            case "ETH": return 250000.0; // $250K for ETH
            case "BNB": return 200000.0; // $200K for BNB
            case "SOL": return 150000.0; // $150K for SOL
            default:
                if (baseAsset.toUpperCase().contains("USD")) {
                    return 1000000.0; // $1M for stablecoins
                } else {
                    return 100000.0; // $100K for other altcoins
                }
        }
    }
} 