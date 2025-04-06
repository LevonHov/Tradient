package com.example.tradient.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.ExchangeConfiguration;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.domain.risk.SlippageAnalyticsBuilder;
import com.example.tradient.domain.risk.SlippageManagerService;
import com.example.tradient.repository.ExchangeRepository;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.domain.risk.RiskCalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class ArbitrageViewModel extends ViewModel {
    private static final String TAG = "ArbitrageViewModel";

    // Configuration values
    private double minProfitPercent = 0.1;
    private double availableCapital = 1000.0;
    private double maxPositionPercent = 10.0;
    private double maxSlippagePercent = 0.5;
    
    // Exchange configuration
    private ExchangeConfiguration exchangeConfig;
    
    // Map to store exchange symbol mappings
    private Map<ExchangeService, Map<String, String>> exchangeSymbolMap = new ConcurrentHashMap<>();
    
    // Repositories
    private final ExchangeRepository exchangeRepository;
    
    // Service instances
    private SlippageManagerService slippageManager;
    
    // Cache for ticker data
    private Map<String, Ticker> cachedTickers = new ConcurrentHashMap<>();
    private Map<String, Long> tickerTimestamps = new ConcurrentHashMap<>();
    private static final long TICKER_CACHE_TTL = 2000; // 2 seconds
    
    // Background task executors
    private ExecutorService exchangeInitExecutor;
    private ExecutorService arbitrageProcessExecutor;
    private ScheduledExecutorService scheduler;
    
    // List of exchanges
    private List<ExchangeService> exchanges = Collections.synchronizedList(new ArrayList<>());
    
    // Set of tradable symbols
    private Set<String> tradableSymbols = Collections.synchronizedSet(new HashSet<>());
    
    // Stats tracking
    private Map<String, Integer> exchangePairStats = new ConcurrentHashMap<>();
    private AtomicInteger opportunitiesFound = new AtomicInteger(0);
    private AtomicInteger symbolsWithoutData = new AtomicInteger(0);
    private AtomicInteger totalOpportunitiesFound = new AtomicInteger(0);
    
    // Flags to track initialization progress
    private boolean configLoaded = false;
    private boolean initialScanComplete = false;
    private int exchangesInitialized = 0;
    private int exchangesWithWebSockets = 0;
    
    // Cache for normalized symbol calculations
    private final Map<String, String> normalizedSymbolCache = new ConcurrentHashMap<>();
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[-_.]");
    
    // Observer pattern implementation with LiveData
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<List<ArbitrageOpportunity>> arbitrageOpportunities = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> initializationProgress = new MutableLiveData<>();
    
    // Symbol prioritization data structure
    private SymbolPrioritizationManager symbolPrioritizationManager;
    
    public ArbitrageViewModel(ExchangeRepository exchangeRepository) {
        this.exchangeRepository = exchangeRepository;
        this.exchangeConfig = ConfigurationFactory.getExchangeConfig();
        initExecutors();
    }
    
    private void initExecutors() {
        exchangeInitExecutor = Executors.newFixedThreadPool(3);
        arbitrageProcessExecutor = Executors.newWorkStealingPool();
        scheduler = Executors.newScheduledThreadPool(1);
    }
    
    /**
     * Initializes the ViewModel and starts the continuous scanning process.
     */
    public void initialize() {
        loadConfiguration();
        startContinuousScan();
    }
    
    /**
     * Start a continuous scanning process that will run indefinitely
     */
    private void startContinuousScan() {
        try {
            Log.i(TAG, "Starting continuous arbitrage scanning...");
            statusMessage.postValue("Starting continuous arbitrage scanning...");
            
            // Initialize exchanges if not already done
            if (exchanges.isEmpty()) {
                initializeExchanges();
            }
            
            // Cancel any existing scanning task
            if (scheduler != null) {
                scheduler.shutdownNow();
            }
            
            // Create a new scheduler
            scheduler = Executors.newScheduledThreadPool(1);
            
            // Schedule repeated scans at fixed rate
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    runArbitrageScan();
                    updateStats();
                } catch (Exception e) {
                    Log.e(TAG, "Error in scheduled arbitrage scan", e);
                    errorMessage.postValue("Error in scan: " + e.getMessage());
                    // Do not stop scanning - we'll try again next interval
                }
            }, 0, 5, TimeUnit.SECONDS); // Run every 5 seconds
            
            Log.i(TAG, "Continuous arbitrage scanning started");
            statusMessage.postValue("Continuous scanning active - watching for opportunities");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start continuous scanning", e);
            errorMessage.postValue("Failed to start continuous scanning: " + e.getMessage());
        }
    }
    
    /**
     * Updates statistics about the scanning process
     */
    private void updateStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("opportunitiesFound", opportunitiesFound.get());
        stats.put("totalOpportunitiesFound", totalOpportunitiesFound.get());
        stats.put("symbolsWithoutData", symbolsWithoutData.get());
        stats.put("exchangesInitialized", exchangesInitialized);
        stats.put("activeExchanges", exchanges.size());
        
        // Reset the opportunity counter for this cycle, but keep the total
        opportunitiesFound.set(0);
        symbolsWithoutData.set(0);
        
        // Update the UI
        initializationProgress.postValue(stats);
    }
    
    private void loadConfiguration() {
        try {
            Log.i(TAG, "Loading configuration...");
            
            // Set values from configuration
            this.minProfitPercent = exchangeConfig.getMinProfitPercent();
            this.availableCapital = exchangeConfig.getAvailableCapital();
            this.maxPositionPercent = exchangeConfig.getMaxPositionPercent();
            this.maxSlippagePercent = exchangeConfig.getMaxSlippagePercent();
            
            // Set default values if configuration returns zeroes
            if (this.minProfitPercent <= 0) this.minProfitPercent = 0.1;
            if (this.availableCapital <= 0) this.availableCapital = 1000.0;
            if (this.maxPositionPercent <= 0) this.maxPositionPercent = 10.0;
            if (this.maxSlippagePercent <= 0) this.maxSlippagePercent = 0.5;
            
            Log.i(TAG, "Configuration loaded with MIN_PROFIT_PERCENT=" + minProfitPercent + "%");
            
            // Initialize the slippage manager
            SlippageAnalyticsBuilder slippageAnalytics = SlippageAnalyticsBuilder.create();
            slippageManager = slippageAnalytics.getSlippageManager();
            
            // Initialize the symbol prioritization manager
            symbolPrioritizationManager = new SymbolPrioritizationManager();
            
            configLoaded = true;
            updateInitializationProgress("configLoaded", true);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading configuration", e);
            errorMessage.postValue("Error loading configuration: " + e.getMessage());
        }
    }
    
    private void initializeExchanges() {
        exchangeRepository.getEnabledExchanges().thenAccept(result -> {
            exchanges.addAll(result);
            exchangesInitialized = exchanges.size();
            updateInitializationProgress("exchangesInitialized", exchangesInitialized);
            
            if (!exchanges.isEmpty()) {
                fetchTradingPairsForAllExchanges();
            } else {
                errorMessage.postValue("No exchanges enabled. Please enable at least one exchange in settings.");
            }
        }).exceptionally(ex -> {
            Log.e(TAG, "Error initializing exchanges", ex);
            errorMessage.postValue("Error initializing exchanges: " + ex.getMessage());
            return null;
        });
    }
    
    private void fetchTradingPairsForAllExchanges() {
        for (ExchangeService exchange : exchanges) {
            exchangeInitExecutor.submit(() -> fetchTradingPairsForExchange(exchange));
        }
    }
    
    private void fetchTradingPairsForExchange(ExchangeService exchange) {
        try {
            statusMessage.postValue("Fetching pairs from " + exchange.getExchangeName() + "...");
            
            exchangeRepository.getTradingPairs(exchange).thenAccept(result -> {
                Map<String, String> symbolMap = new HashMap<>();
                
                // Process trading pairs
                for (var pair : result) {
                    String originalSymbol = pair.getSymbol();
                    String normalizedSymbol = normalizeSymbol(originalSymbol);
                    symbolMap.put(normalizedSymbol, originalSymbol);
                }
                
                // Store the mapping
                exchangeSymbolMap.put(exchange, symbolMap);
                
                // Update initialization progress
                Map<String, Object> exchangeProgress = new HashMap<>();
                exchangeProgress.put("exchange", exchange.getExchangeName());
                exchangeProgress.put("pairsCount", symbolMap.size());
                updateInitializationProgress("tradingPairs", exchangeProgress);
                
                // Proceed to find common symbols if all exchanges have been processed
                synchronized (exchanges) {
                    if (exchangeSymbolMap.size() >= exchanges.size()) {
                        findCommonSymbols();
                    }
                }
                
            }).exceptionally(ex -> {
                Log.e(TAG, "Error fetching trading pairs for " + exchange.getExchangeName(), ex);
                errorMessage.postValue("Error fetching trading pairs: " + ex.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error in fetchTradingPairsForExchange", e);
            errorMessage.postValue("Error: " + e.getMessage());
        }
    }
    
    private void findCommonSymbols() {
        // Implementation for finding common symbols across exchanges
        // ... (will be implemented in next edit)
    }
    
    public String normalizeSymbol(String originalSymbol) {
        if (originalSymbol == null || originalSymbol.isEmpty()) {
            return "";
        }
        
        // Check cache first
        String cached = normalizedSymbolCache.get(originalSymbol);
        if (cached != null) {
            return cached;
        }
        
        // Initialize with default value
        String normalized = originalSymbol.toUpperCase();
        
        // Handle common formats
        if (originalSymbol.contains("/")) {
            // Already in standard format like BTC/USDT
            normalized = originalSymbol.toUpperCase();
        } else {
            // Handle formats like BTCUSDT, BTC-USDT, BTC_USDT
            String clean = SEPARATOR_PATTERN.matcher(originalSymbol).replaceAll("");
            
            // Special case for Coinbase, which uses format like BTC-USDT
            if (originalSymbol.contains("-")) {
                String[] parts = originalSymbol.split("-");
                if (parts.length == 2) {
                    normalized = parts[0].toUpperCase() + "/" + parts[1].toUpperCase();
                    normalizedSymbolCache.put(originalSymbol, normalized);
                    return normalized;
                }
            }
            
            // Standardize common base/quote pairs
            if (clean.endsWith("USDT")) {
                normalized = clean.substring(0, clean.length() - 4) + "/USDT";
            } else if (clean.endsWith("USD")) {
                normalized = clean.substring(0, clean.length() - 3) + "/USD";
            } else if (clean.endsWith("BTC")) {
                normalized = clean.substring(0, clean.length() - 3) + "/BTC";
            } else if (clean.endsWith("ETH")) {
                normalized = clean.substring(0, clean.length() - 3) + "/ETH";
            } else if (clean.length() > 3) {
                // Try to intelligently split between base and quote
                String[] commonQuotes = {"USDT", "USD", "BTC", "ETH", "BNB", "BUSD"};
                boolean found = false;
                
                for (String quote : commonQuotes) {
                    if (clean.endsWith(quote)) {
                        normalized = clean.substring(0, clean.length() - quote.length()) + "/" + quote;
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    // Default assumption: first 3-4 chars are the base currency
                    normalized = clean.substring(0, Math.min(4, clean.length() / 2)) + "/" + 
                               clean.substring(Math.min(4, clean.length() / 2));
                }
            }
        }
        
        // Cache the result
        normalizedSymbolCache.put(originalSymbol, normalized);
        return normalized;
    }

    private void updateInitializationProgress(String key, Object value) {
        Map<String, Object> currentProgress = initializationProgress.getValue();
        if (currentProgress == null) {
            currentProgress = new HashMap<>();
        }
        currentProgress.put(key, value);
        initializationProgress.postValue(currentProgress);
    }
    
    private void runArbitrageScan() {
        try {
            Log.i(TAG, "Starting arbitrage scan...");
            statusMessage.postValue("Scanning for arbitrage opportunities...");
            
            if (exchanges.size() < 2) {
                Log.w(TAG, "Not enough exchanges for arbitrage scanning");
                errorMessage.postValue("Need at least 2 exchanges to scan for arbitrage");
                return;
            }
            
            // Get current ticker data for all exchanges
            for (ExchangeService exchange : exchanges) {
                arbitrageProcessExecutor.submit(() -> {
                    try {
                        processExchangeTickers(exchange);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing tickers for " + exchange.getExchangeName(), e);
                        errorMessage.postValue("Error processing " + exchange.getExchangeName() + ": " + e.getMessage());
                    }
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in arbitrage scan", e);
            errorMessage.postValue("Error in arbitrage scan: " + e.getMessage());
        }
    }
    
    private void processExchangeTickers(ExchangeService exchange) {
        try {
            // Get trading pairs for this exchange
            Map<String, String> symbolMap = exchangeSymbolMap.get(exchange);
            if (symbolMap == null || symbolMap.isEmpty()) {
                Log.w(TAG, "No symbol mapping found for " + exchange.getExchangeName());
                return;
            }
            
            // Process each symbol
            for (Map.Entry<String, String> entry : symbolMap.entrySet()) {
                String normalizedSymbol = entry.getKey();
                String exchangeSymbol = entry.getValue();
                
                try {
                    // Get ticker data
                    Ticker ticker = exchangeRepository.getTicker(exchange, exchangeSymbol).get();
                    if (ticker == null) {
                        Log.w(TAG, "No ticker data for " + exchangeSymbol + " on " + exchange.getExchangeName());
                        symbolsWithoutData.incrementAndGet();
                        continue;
                    }
                    
                    // Update cache
                    cachedTickers.put(normalizedSymbol + ":" + exchange.getExchangeName(), ticker);
                    tickerTimestamps.put(normalizedSymbol + ":" + exchange.getExchangeName(), System.currentTimeMillis());
                    
                    // Check for arbitrage opportunities
                    checkArbitrageOpportunities(normalizedSymbol, exchange, ticker);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error processing ticker for " + exchangeSymbol + " on " + exchange.getExchangeName(), e);
                    continue;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing exchange tickers for " + exchange.getExchangeName(), e);
            throw e;
        }
    }
    
    private void checkArbitrageOpportunities(String normalizedSymbol, ExchangeService currentExchange, Ticker currentTicker) {
        try {
            // Compare with other exchanges
            for (ExchangeService otherExchange : exchanges) {
                if (otherExchange.equals(currentExchange)) {
                    continue;
                }
                
                String otherSymbol = exchangeSymbolMap.get(otherExchange).get(normalizedSymbol);
                if (otherSymbol == null) {
                    continue;
                }
                
                // Get ticker from cache or fetch new one
                Ticker otherTicker = getCachedTicker(normalizedSymbol, otherExchange);
                if (otherTicker == null) {
                    try {
                        otherTicker = exchangeRepository.getTicker(otherExchange, otherSymbol).get();
                        if (otherTicker == null) {
                            continue;
                        }
                        cachedTickers.put(normalizedSymbol + ":" + otherExchange.getExchangeName(), otherTicker);
                        tickerTimestamps.put(normalizedSymbol + ":" + otherExchange.getExchangeName(), System.currentTimeMillis());
                    } catch (Exception e) {
                        Log.e(TAG, "Error fetching ticker for " + otherSymbol + " on " + otherExchange.getExchangeName(), e);
                        continue;
                    }
                }
                
                // Calculate price difference
                double priceDiff = Math.abs(currentTicker.getLastPrice() - otherTicker.getLastPrice());
                double priceDiffPercent = (priceDiff / Math.min(currentTicker.getLastPrice(), otherTicker.getLastPrice())) * 100;
                
                // Check if price difference exceeds minimum profit threshold
                if (priceDiffPercent >= minProfitPercent) {
                    // Determine buy/sell direction
                    boolean isBuyOnCurrent = currentTicker.getLastPrice() < otherTicker.getLastPrice();
                    ExchangeService buyExchange = isBuyOnCurrent ? currentExchange : otherExchange;
                    ExchangeService sellExchange = isBuyOnCurrent ? otherExchange : currentExchange;
                    
                    // Get tickers for risk assessment
                    Ticker buyTicker = isBuyOnCurrent ? currentTicker : otherTicker;
                    Ticker sellTicker = isBuyOnCurrent ? otherTicker : currentTicker;
                    
                    // Create arbitrage opportunity
                    ArbitrageOpportunity opportunity = new ArbitrageOpportunity(
                        normalizedSymbol,
                        exchangeSymbolMap.get(buyExchange).get(normalizedSymbol),
                        exchangeSymbolMap.get(sellExchange).get(normalizedSymbol),
                        buyExchange.getExchangeName(),
                        sellExchange.getExchangeName(),
                        isBuyOnCurrent ? currentTicker.getLastPrice() : otherTicker.getLastPrice(),
                        isBuyOnCurrent ? otherTicker.getLastPrice() : currentTicker.getLastPrice(),
                        priceDiffPercent
                    );
                    
                    // Set ticker data
                    opportunity.setBuyTicker(buyTicker);
                    opportunity.setSellTicker(sellTicker);
                    
                    // Store fee percentages for later calculation
                    double buyFee = exchangeConfig.getFeePercentage(buyExchange.getExchangeName(), true); // Maker fee for buy
                    double sellFee = exchangeConfig.getFeePercentage(sellExchange.getExchangeName(), false); // Taker fee for sell
                    opportunity.setBuyFeePercentage(buyFee);
                    opportunity.setSellFeePercentage(sellFee);
                    
                    // Set a temporary placeholder for profit
                    opportunity.setNetProfitPercentage(0.0);
                    
                    // Set empty risk assessment
                    opportunity.setRiskAssessment(new RiskAssessment());
                    
                    // Set viability to false until profit calculation is reimplemented
                    opportunity.setViable(false);
                    
                    // Create a unique key for this opportunity
                    String opportunityKey = opportunity.getOpportunityKey();
                    
                    // Get current opportunities
                    List<ArbitrageOpportunity> currentOpportunities = arbitrageOpportunities.getValue();
                    if (currentOpportunities == null) {
                        currentOpportunities = new ArrayList<>();
                    }
                    
                    // Check if we already have this opportunity
                    boolean opportunityExists = false;
                    int existingIndex = -1;
                    
                    for (int i = 0; i < currentOpportunities.size(); i++) {
                        ArbitrageOpportunity existingOpp = currentOpportunities.get(i);
                        if (existingOpp.getOpportunityKey().equals(opportunityKey)) {
                            opportunityExists = true;
                            existingIndex = i;
                            break;
                        }
                    }
                    
                    // Update existing or add new opportunity
                    if (opportunityExists) {
                        // Update the existing opportunity with new data
                        currentOpportunities.set(existingIndex, opportunity);
                    } else {
                        // Add new opportunity and increment counter
                        currentOpportunities.add(opportunity);
                        opportunitiesFound.incrementAndGet();
                        totalOpportunitiesFound.incrementAndGet();
                    }
                    
                    // Post updated list back to UI
                    arbitrageOpportunities.postValue(currentOpportunities);
                    
                    // Log opportunity
                    Log.i(TAG, String.format("Found arbitrage opportunity: %s - Buy on %s at %.8f, Sell on %s at %.8f, Profit: %.2f%%, Risk: %.2f, Slippage: %.4f%%",
                        normalizedSymbol,
                        buyExchange.getExchangeName(),
                        opportunity.getBuyPrice(),
                        sellExchange.getExchangeName(),
                        opportunity.getSellPrice(),
                        priceDiffPercent,
                        0.0, // Placeholder for risk
                        0.0 // Placeholder for slippage
                    ));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking arbitrage opportunities for " + normalizedSymbol, e);
            // Don't throw the exception - we want to continue scanning other symbols
        }
    }
    
    /**
     * Calculates the optimal position size for an arbitrage opportunity based on risk assessment.
     * Direct implementation from ArbitrageProcessMain.
     * 
     * @param opportunity The arbitrage opportunity to size
     * @param availableCapital Total capital available for trading
     * @param maxPositionPct Maximum position size as percentage of capital
     * @return Optimal position size in base currency units
     */
    private double calculateOptimalPositionSize(ArbitrageOpportunity opportunity, double availableCapital, double maxPositionPct) {
        // Validate inputs
        if (opportunity == null || opportunity.getRiskAssessment() == null) {
            return 0.0;
        }

        RiskAssessment risk = opportunity.getRiskAssessment();

        // Extract key risk factors
        double overallRisk = risk.getOverallRiskScore();
        double slippageRisk = risk.getSlippageRisk();
        double liquidityScore = risk.getLiquidityScore();
        double volatilityScore = risk.getVolatilityScore();

        // Calculate win probability (using overall risk as a proxy)
        double winProbability = Math.min(0.95, overallRisk * 0.9 + 0.05);

        // Calculate potential profit and loss
        double potentialProfit = opportunity.getNetProfitPercentage() / 100.0; // Already in percent, convert to decimal
        double potentialLoss = 1.0 - slippageRisk; // Use slippage risk as a proxy for potential loss

        // Calculate Kelly fraction (optimal bet size as fraction of capital)
        double kellyFraction = 0.0;
        if (potentialLoss > 0) {
            kellyFraction = (winProbability * (1 + potentialProfit) - 1) / potentialLoss;
        }

        // Apply a safety factor (using half Kelly or less is common practice)
        double safetyFactor = 0.5;
        kellyFraction *= safetyFactor;

        // Cap the position size
        double cappedFraction = Math.min(kellyFraction, maxPositionPct);

        // Apply additional risk-based scaling factors
        double liquidityAdjustment = Math.pow(liquidityScore, 1.5); // Penalize low liquidity more aggressively
        double volatilityAdjustment = Math.pow(volatilityScore, 1.2); // Slightly reduce size for high volatility

        // Calculate final position size with all constraints
        double optimalFraction = cappedFraction * liquidityAdjustment * volatilityAdjustment;

        // Convert fraction to actual position size
        double positionSize = availableCapital * optimalFraction;

        // Implement minimum position size threshold (to avoid dust positions)
        double minimumPositionSize = 10.0; // Example minimum size in base currency
        if (positionSize < minimumPositionSize) {
            return 0.0; // Don't trade if optimal size is too small
        }

        return positionSize;
    }
    
    /**
     * Calculates expected slippage for a given trade size.
     * This version uses the advanced slippage calculator with dynamic calibration from ArbitrageProcessMain.
     *
     * @param ticker The ticker data
     * @param isBuy Whether this is a buy (true) or sell (false) operation
     * @param tradeAmount The size of the trade to execute
     * @param symbol The trading symbol
     * @return Expected slippage as a percentage of the trade value
     */
    private double calculateExpectedSlippage(Ticker ticker, boolean isBuy, double tradeAmount, String symbol) {
        try {
            if (slippageManager != null) {
                // Use the advanced slippage calculation
                return slippageManager.calculateSlippage(ticker, tradeAmount, isBuy, symbol);
            } else {
                // Fallback to basic calculation if the manager isn't initialized
                return calculateBasicSlippage(ticker, isBuy, tradeAmount);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating slippage: " + e.getMessage());
            // Fallback to basic calculation
            return calculateBasicSlippage(ticker, isBuy, tradeAmount);
        }
    }

    /**
     * Fallback method for basic slippage calculation when advanced analytics are unavailable.
     * From ArbitrageProcessMain.
     */
    private double calculateBasicSlippage(Ticker ticker, boolean isBuy, double tradeAmount) {
        if (ticker == null) {
            return 0.005; // Default 0.5% slippage if no market data
        }

        double spread = ticker.getAskPrice() - ticker.getBidPrice();
        if (spread <= 0 || ticker.getLastPrice() <= 0) {
            return 0.005; // Default to 0.5% if invalid prices
        }

        double relativeSpread = spread / ticker.getLastPrice();

        // Basic volume-based adjustment (higher volume = lower slippage)
        double volumeAdjustment = 1.0;
        if (ticker.getVolume() > 0) {
            // Normalize the trade amount relative to 24h volume
            double volumeRatio = tradeAmount / ticker.getVolume();
            volumeAdjustment = Math.min(1.0 + (volumeRatio * 10), 3.0); // Cap at 3x
        }

        // Calculate slippage based on spread and volume
        double baseSlippage = relativeSpread * 0.5 * volumeAdjustment;

        // Ensure slippage is within reasonable bounds (0.05% to 2%)
        return Math.max(0.0005, Math.min(baseSlippage, 0.02));
    }
    
    private Ticker getCachedTicker(String normalizedSymbol, ExchangeService exchange) {
        String cacheKey = normalizedSymbol + ":" + exchange.getExchangeName();
        Long timestamp = tickerTimestamps.get(cacheKey);
        
        if (timestamp != null && System.currentTimeMillis() - timestamp < TICKER_CACHE_TTL) {
            return cachedTickers.get(cacheKey);
        }
        
        return null;
    }
    
    // LiveData getters
    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<List<ArbitrageOpportunity>> getArbitrageOpportunities() {
        return arbitrageOpportunities;
    }
    
    public LiveData<Map<String, Object>> getInitializationProgress() {
        return initializationProgress;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        
        // Shutdown all executors when the ViewModel is cleared
        if (exchangeInitExecutor != null) {
            exchangeInitExecutor.shutdownNow();
        }
        
        if (arbitrageProcessExecutor != null) {
            arbitrageProcessExecutor.shutdownNow();
        }
        
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        
        Log.i(TAG, "ArbitrageViewModel cleared, all executors shutdown");
    }
    
    // Symbol prioritization manager inner class
    // This will be moved to a separate file in a future refactoring
    private static class SymbolPrioritizationManager {
        // Implementation will go here
    }
} 