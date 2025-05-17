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
import com.example.tradient.util.ArbitrageProcessing;
import com.example.tradient.util.RiskAssessmentAdapter;

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
        try {
            Log.i(TAG, "Finding common symbols across exchanges...");
            statusMessage.postValue("Finding common symbols across exchanges...");
            
            // Use ConcurrentHashMap's merge operations for thread safety
            final Set<String> commonSymbols = ConcurrentHashMap.newKeySet();
            final Map<String, AtomicInteger> symbolCount = new ConcurrentHashMap<>();
            
            // First exchange adds all its symbols
            if (!exchangeSymbolMap.isEmpty()) {
                ExchangeService firstExchange = exchangeSymbolMap.keySet().iterator().next();
                Map<String, String> firstSymbolMap = exchangeSymbolMap.get(firstExchange);
                
                if (firstSymbolMap != null) {
                    // Add all normalized symbols from first exchange
                    for (String normalizedSymbol : firstSymbolMap.keySet()) {
                        symbolCount.put(normalizedSymbol, new AtomicInteger(1));
                        commonSymbols.add(normalizedSymbol);
                    }
                    
                    // Compare with all other exchanges
                    for (Map.Entry<ExchangeService, Map<String, String>> entry : exchangeSymbolMap.entrySet()) {
                        if (entry.getKey().equals(firstExchange)) {
                            continue;
                        }
                        
                        Map<String, String> otherSymbolMap = entry.getValue();
                        if (otherSymbolMap == null) {
                            continue;
                        }
                        
                        // Update counts for symbols that exist in this exchange
                        for (String normalizedSymbol : otherSymbolMap.keySet()) {
                            symbolCount.computeIfAbsent(normalizedSymbol, k -> new AtomicInteger(0)).incrementAndGet();
                        }
                    }
                    
                    // Keep only symbols that exist in all exchanges
                    int requiredCount = exchangeSymbolMap.size();
                    commonSymbols.removeIf(symbol -> symbolCount.getOrDefault(symbol, new AtomicInteger(0)).get() < requiredCount);
                    
                    // Update tradable symbols with thread-safe method
                    tradableSymbols.clear();
                    tradableSymbols.addAll(commonSymbols);
                    
                    Log.i(TAG, "Found " + tradableSymbols.size() + " common symbols across all exchanges");
                    statusMessage.postValue("Found " + tradableSymbols.size() + " common tradable symbols");
                    
                    // Mark initialization as complete
                    initialScanComplete = true;
                    updateInitializationProgress("initialScanComplete", true);
                    updateInitializationProgress("tradableSymbolsCount", tradableSymbols.size());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding common symbols", e);
            errorMessage.postValue("Error finding common symbols: " + e.getMessage());
        }
    }
    
    public String normalizeSymbol(String originalSymbol) {
        if (originalSymbol == null || originalSymbol.isEmpty()) {
            return "";
        }
        
        // Check cache first using ConcurrentHashMap's thread-safe get method
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
                    // Use putIfAbsent for thread safety
                    normalizedSymbolCache.putIfAbsent(originalSymbol, normalized);
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
        
        // Cache the result with putIfAbsent for thread safety
        normalizedSymbolCache.putIfAbsent(originalSymbol, normalized);
        return normalized;
    }

    private void updateInitializationProgress(String key, Object value) {
        // Create a new HashMap each time to avoid concurrent modification issues
        Map<String, Object> currentProgress = new HashMap<>();
        Map<String, Object> previousProgress = initializationProgress.getValue();
        
        if (previousProgress != null) {
            synchronized (previousProgress) {
                // Copy existing values to the new map with synchronization
                currentProgress.putAll(previousProgress);
            }
        }
        
        // Add the new key-value pair
        currentProgress.put(key, value);
        
        // Update the LiveData value
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
    
    private void processExchangeTickers(ExchangeService exchange) throws Exception {
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
            // Get all exchanges locally to avoid concurrent modification
            List<ExchangeService> currentExchanges;
            synchronized (exchanges) {
                currentExchanges = new ArrayList<>(exchanges);
            }
            
            // Compare with other exchanges
            for (ExchangeService otherExchange : currentExchanges) {
                if (otherExchange.equals(currentExchange)) {
                    continue;
                }
                
                // Get the symbol map atomically
                Map<String, String> otherSymbolMap = exchangeSymbolMap.get(otherExchange);
                if (otherSymbolMap == null) {
                    continue;
                }
                
                // Get the symbol safely
                String otherSymbol = otherSymbolMap.get(normalizedSymbol);
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
                        // Use appropriate caching strategy
                        String cacheKey = normalizedSymbol + ":" + otherExchange.getExchangeName();
                        cachedTickers.put(cacheKey, otherTicker);
                        tickerTimestamps.put(cacheKey, System.currentTimeMillis());
                    } catch (Exception e) {
                        Log.e(TAG, "Error fetching ticker for " + otherSymbol + " on " + otherExchange.getExchangeName(), e);
                        continue;
                    }
                }
                
                // Determine buy/sell direction - buy on the exchange with lower price, sell on higher price
                boolean isBuyOnCurrent = currentTicker.getLastPrice() < otherTicker.getLastPrice();
                ExchangeService buyExchange = isBuyOnCurrent ? currentExchange : otherExchange;
                ExchangeService sellExchange = isBuyOnCurrent ? otherExchange : currentExchange;
                
                // Get prices
                double buyPrice = isBuyOnCurrent ? currentTicker.getLastPrice() : otherTicker.getLastPrice();
                double sellPrice = isBuyOnCurrent ? otherTicker.getLastPrice() : currentTicker.getLastPrice();
                
                // Skip if prices are invalid
                if (buyPrice <= 0 || sellPrice <= 0) {
                    Log.w(TAG, "Invalid prices for " + normalizedSymbol + ": buy=" + buyPrice + ", sell=" + sellPrice);
                    continue;
                }
                
                // Get trading pair base asset (e.g., "BTC" from "BTC/USDT")
                String baseAsset = normalizedSymbol.split("/")[0];
                
                // Calculate profit with comprehensive fee model
                double buyFee = exchangeConfig.getFeePercentage(buyExchange.getExchangeName(), true); // Maker fee for buy
                double sellFee = exchangeConfig.getFeePercentage(sellExchange.getExchangeName(), false); // Taker fee for sell
                
                // Default trade amount for calculation (this would be calculated based on available balance in real trading)
                double initialAmount = 1000.0; // 1000 USD or equivalent in quote currency
                
                // Use comprehensive profit calculation that accounts for all fees
                double profitPercent = 
                        ArbitrageProcessing.calculateComprehensiveProfitPercentage(
                    initialAmount,
                    buyPrice,
                    sellPrice,
                    buyExchange.getExchangeName(),
                    sellExchange.getExchangeName(),
                    baseAsset,
                    buyFee,
                    sellFee
                );
                
                // Log the calculation for debugging
                Log.d(TAG, String.format(
                    "Comprehensive profit calculation for %s: buy=%f on %s, sell=%f on %s, profit=%.2f%% (includes all fees)",
                    normalizedSymbol, buyPrice, buyExchange.getExchangeName(), 
                    sellPrice, sellExchange.getExchangeName(), profitPercent));
                
                // Check if price difference exceeds minimum profit threshold
                if (profitPercent >= minProfitPercent) {
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
                        buyPrice,
                        sellPrice,
                        profitPercent  // Pass the comprehensive profit percentage
                    );
                    
                    // Set ticker data
                    opportunity.setBuyTicker(buyTicker);
                    opportunity.setSellTicker(sellTicker);
                    
                    // Store fee percentages for later calculation
                    opportunity.setBuyFeePercentage(buyFee);
                    opportunity.setSellFeePercentage(sellFee);
                    
                    // Set empty risk assessment
                    RiskAssessmentAdapter.setRiskAssessment(opportunity, new RiskAssessment());
                    
                    // Set net profit after all fees (already calculated by comprehensive method)
                    opportunity.setNetProfitPercentage(profitPercent);
                    
                    // Set viability based on net profit
                    opportunity.setViable(profitPercent > minProfitPercent);
                    
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
                        if (existingOpp.getOpportunityKey().equals(opportunity.getOpportunityKey())) {
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
                    Log.i(TAG, String.format("Found arbitrage opportunity: %s - Buy on %s at %.8f, Sell on %s at %.8f, Comprehensive Profit: %.2f%%",
                        normalizedSymbol,
                        buyExchange.getExchangeName(),
                        opportunity.getBuyPrice(),
                        sellExchange.getExchangeName(),
                        opportunity.getSellPrice(),
                        profitPercent
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
        RiskAssessment risk = RiskAssessmentAdapter.getRiskAssessment(opportunity);
        if (opportunity == null || risk == null) {
            return 0.0;
        }

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
        
        // Use atomic reads from the ConcurrentHashMap
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

    /**
     * Fix incorrectly calculated profit percentages
     * This verifies and corrects potential errors in profit calculation
     * @param buyPrice The buy price
     * @param sellPrice The sell price
     * @param reportedProfit The profit percentage reported by the system
     * @return Corrected profit percentage
     */
    private double validateProfitPercentage(double buyPrice, double sellPrice, double reportedProfit) {
        // Calculate the expected profit percentage based on raw price difference
        // Profit % should be ((sellPrice - buyPrice) / buyPrice) * 100
        double expectedProfit = ((sellPrice - buyPrice) / buyPrice) * 100;
        
        // If the reported profit is dramatically different (more than 3x), use the calculated value
        if (Math.abs(reportedProfit) > Math.abs(expectedProfit) * 3) {
            Log.e(TAG, "Detected incorrect profit: reported=" + reportedProfit + 
                  "%, calculated=" + expectedProfit + "% (buy: " + buyPrice + 
                  ", sell: " + sellPrice + ")");
            return expectedProfit;
        }
        
        return reportedProfit;
    }

    /**
     * Create an arbitrage opportunity with validated profit values
     */
    private ArbitrageOpportunity createOpportunity(
            String normalizedSymbol,
            String buyExchangeSymbol,
            String sellExchangeSymbol,
            String buyExchangeName,
            String sellExchangeName,
            double buyPrice,
            double sellPrice,
            double reportedProfitPercent) {
        
        // Validate and correct the profit percentage if necessary
        double correctedProfitPercent = validateProfitPercentage(buyPrice, sellPrice, reportedProfitPercent);
        
        return new ArbitrageOpportunity(
            normalizedSymbol,
            buyExchangeSymbol,
            sellExchangeSymbol,
            buyExchangeName,
            sellExchangeName,
            buyPrice,
            sellPrice,
            correctedProfitPercent
        );
    }
} 