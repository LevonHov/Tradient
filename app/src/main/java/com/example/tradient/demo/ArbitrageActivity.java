package com.example.tradient.demo;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradient.R;
import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.model.ArbitrageConfiguration;
import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.ExchangeConfiguration;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.data.model.RiskConfiguration;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;
import com.example.tradient.data.service.BinanceExchangeService;
import com.example.tradient.data.service.BybitV5ExchangeService;
import com.example.tradient.data.service.CoinbaseExchangeService;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.data.service.KrakenExchangeService;
import com.example.tradient.data.service.OkxExchangeService;
import com.example.tradient.domain.engine.ExchangeToExchangeArbitrage;
import com.example.tradient.domain.risk.RiskCalculator;
import com.example.tradient.domain.risk.SlippageAnalyticsBuilder;
import com.example.tradient.domain.risk.SlippageManagerService;
import com.example.tradient.domain.risk.SlippageStressTester;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ArbitrageActivity - Android implementation of the arbitrage process
 * Adapts the console-based ArbitrageProcessMain to run in an Android environment
 */
public class ArbitrageActivity extends AppCompatActivity implements INotificationService {
    private static final String TAG = "ArbitrageActivity";

    // Replace hard-coded values with configuration
    private double MIN_PROFIT_PERCENT;
    private double AVAILABLE_CAPITAL;
    private double MAX_POSITION_PERCENT;
    private double MAX_SLIPPAGE_PERCENT;
    private boolean ENABLE_FEE_REPORTS;

    // Store exchange symbol mappings
    private Map<ExchangeService, Map<String, String>> exchangeSymbolMap = new HashMap<>();

    // Add slippage manager service
    private SlippageManagerService slippageManager;
    private SlippageAnalyticsBuilder slippageAnalytics;
    
    // UI components
    private TextView statusTextView;
    private TextView opportunitiesTextView;
    
    // Background task executor
    private ScheduledExecutorService scheduler;
    
    // List of exchanges
    private List<ExchangeService> exchanges = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arbitrage);
        
        // Initialize UI components
        statusTextView = findViewById(R.id.statusTextView);
        opportunitiesTextView = findViewById(R.id.opportunitiesTextView);
        
        updateStatus("Initializing arbitrage system...");
        
        // Start initialization in background thread
        new Thread(this::initializeArbitrageSystem).start();
    }
    
    /**
     * Initialize the arbitrage system components
     */
    private void initializeArbitrageSystem() {
        try {
            // Load configuration
            updateStatus("Loading configuration...");
            loadConfiguration();
            
            // Initialize analytics
            slippageAnalytics = SlippageAnalyticsBuilder.create();
            slippageManager = slippageAnalytics.getSlippageManager();
            
            // Initialize exchanges
            updateStatus("Initializing exchange services...");
            initializeExchanges();
            
            // Fetch trading pairs
            updateStatus("Fetching trading pairs...");
            fetchTradingPairs();
            
            // Find common symbols
            updateStatus("Finding common trading pairs...");
            List<String> tradableSymbols = findCommonSymbols(exchanges);
            
            // Limit symbols if needed but use a higher limit to catch more opportunities
            int maxSymbolLimit = ConfigurationFactory.getInteger("system.performance.maxSymbolLimit", 20);
            if (tradableSymbols.size() > maxSymbolLimit) {
                // Sort by popularity (first) and then alphabetically
                List<String> sortedSymbols = new ArrayList<>(tradableSymbols);
                // We could add custom sorting by popularity here if needed
                sortedSymbols.sort(String::compareTo);
                tradableSymbols = new ArrayList<>(sortedSymbols.subList(0, maxSymbolLimit));
                logInfo("Limited to " + maxSymbolLimit + " tradable symbols for better performance");
            } else {
                logInfo("Processing all " + tradableSymbols.size() + " common trading pairs");
            }
            
            if (tradableSymbols.isEmpty()) {
                updateStatus("No common trading pairs found. Cannot proceed with arbitrage.");
                return;
            }
            
            // Initialize WebSockets
            updateStatus("Initializing WebSocket connections for " + tradableSymbols.size() + " symbols...");
            initializeWebSockets(new HashSet<>(tradableSymbols));
            
            // Schedule periodic arbitrage scan
            updateStatus("Starting periodic arbitrage scanning...");
            final Set<String> finalSymbols = new HashSet<>(tradableSymbols);
            schedulePeriodicScans(exchanges, finalSymbols);
            
        } catch (Exception e) {
            logError("Error initializing arbitrage system", e);
            updateStatus("Error: " + e.getMessage());
        }
    }
    
    /**
     * Load configuration values from configuration service
     */
    private void loadConfiguration() {
        // Load arbitrage configuration
        ArbitrageConfiguration arbitrageConfig = ConfigurationFactory.getArbitrageConfig();
        MIN_PROFIT_PERCENT = arbitrageConfig.getMinProfitPercent();
        AVAILABLE_CAPITAL = arbitrageConfig.getAvailableCapital();
        MAX_POSITION_PERCENT = arbitrageConfig.getMaxPositionPercent();

        // Load risk configuration
        RiskConfiguration riskConfig = ConfigurationFactory.getRiskConfig();
        MAX_SLIPPAGE_PERCENT = riskConfig.getMaxSlippagePercent();

        // Load other settings
        ENABLE_FEE_REPORTS = ConfigurationFactory.getBoolean("system.logging.feeReporting", true);

        logInfo("Configuration loaded successfully:");
        logInfo("- Min Profit %: " + MIN_PROFIT_PERCENT);
        logInfo("- Available Capital: $" + AVAILABLE_CAPITAL);
        logInfo("- Max Position %: " + (MAX_POSITION_PERCENT * 100) + "%");
        logInfo("- Max Slippage %: " + (MAX_SLIPPAGE_PERCENT * 100) + "%");
    }
    
    /**
     * Initialize all exchange services
     */
    private void initializeExchanges() {
        // Use configuration for exchange fees
        ExchangeConfiguration exchangeConfig = ConfigurationFactory.getExchangeConfig();
        
        // Create exchange instances
        BinanceExchangeService binance = new BinanceExchangeService(exchangeConfig.getExchangeFee("binance"));
        binance.setNotificationService(this);
        
        CoinbaseExchangeService coinbase = new CoinbaseExchangeService(exchangeConfig.getExchangeFee("coinbase"));
        coinbase.setNotificationService(this);
        
        KrakenExchangeService kraken = new KrakenExchangeService(exchangeConfig.getExchangeFee("kraken"));
        kraken.setNotificationService(this);
        
        BybitV5ExchangeService bybit = new BybitV5ExchangeService(exchangeConfig.getExchangeFee("bybit"));
        bybit.setNotificationService(this);
        
        OkxExchangeService okx = new OkxExchangeService(exchangeConfig.getExchangeFee("okx"));
        okx.setNotificationService(this);

        // Initialize exchanges - don't reset fees to zero
        binance.setLogoResource("binance_logo");
        coinbase.setLogoResource("coinbase_logo");
        kraken.setLogoResource("kraken_logo");
        bybit.setLogoResource("bybit_logo");
        okx.setLogoResource("okx_logo");
        
        // Don't reset fee tiers to zero
        // binance.updateFeesTiers(0.0);
        // coinbase.updateFeesTiers(0.0);
        // kraken.updateFeesTiers(0.0);
        // bybit.updateFeesTiers(0.0);
        // okx.updateFeesTiers(0.0);

        // Add only enabled exchanges from configuration
        exchanges.clear();
        if (exchangeConfig.isExchangeEnabled("binance")) {
            exchanges.add(binance);
            logInfo("Added Binance exchange");
        }
        if (exchangeConfig.isExchangeEnabled("coinbase")) {
            exchanges.add(coinbase);
            logInfo("Added Coinbase exchange");
        }
        if (exchangeConfig.isExchangeEnabled("kraken")) {
            exchanges.add(kraken);
            logInfo("Added Kraken exchange");
        }
        if (exchangeConfig.isExchangeEnabled("bybit")) {
            exchanges.add(bybit);
            logInfo("Added Bybit exchange");
        }
        if (exchangeConfig.isExchangeEnabled("okx")) {
            exchanges.add(okx);
            logInfo("Added OKX exchange");
        }
        
        logInfo("Initialized " + exchanges.size() + " exchanges");
    }
    
    /**
     * Fetch trading pairs from all exchanges
     */
    private void fetchTradingPairs() {
        for (ExchangeService exchange : exchanges) {
            try {
                String name = exchange.getExchangeName();
                logInfo("Fetching trading pairs from " + name);
                List<TradingPair> pairs = exchange.fetchTradingPairs();
                logInfo("Fetched " + pairs.size() + " trading pairs from " + name);
            } catch (Exception e) {
                logError("Error fetching trading pairs from " + exchange.getExchangeName(), e);
            }
        }
    }

    /**
     * Find trading symbols available on at least two exchanges using symbol normalization.
     * This enhances arbitrage opportunities by considering any pair traded on at least two exchanges.
     */
    private List<String> findCommonSymbols(List<ExchangeService> exchangeServices) {
        if (exchangeServices.isEmpty()) {
            return new ArrayList<>();
        }

        // Create maps to store normalized symbol -> original symbol for each exchange
        Map<ExchangeService, Map<String, String>> exchangeSymbolMaps = new HashMap<>();

        // Track which exchanges support each normalized symbol
        Map<String, Set<ExchangeService>> symbolExchangeMap = new HashMap<>();

        // Populate maps with normalized symbols for each exchange
        for (ExchangeService exchange : exchangeServices) {
            Map<String, String> normalizedMap = new HashMap<>();
            List<TradingPair> pairs = exchange.getTradingPairs();

            // Skip exchanges with no trading pairs
            if (pairs == null || pairs.isEmpty()) {
                logWarning("No trading pairs available for " + exchange.getExchangeName());
                continue;
            }

            for (TradingPair pair : pairs) {
                String originalSymbol = pair.getSymbol();
                String normalizedSymbol = normalizeSymbol(originalSymbol);
                normalizedMap.put(normalizedSymbol, originalSymbol);

                // Track this exchange as supporting this symbol
                symbolExchangeMap.computeIfAbsent(normalizedSymbol, k -> new HashSet<>()).add(exchange);
            }

            exchangeSymbolMaps.put(exchange, normalizedMap);
            logInfo("Found " + normalizedMap.size() + " trading pairs for " + exchange.getExchangeName());
        }

        // Find symbols available on at least two exchanges
        Set<String> validArbitrageSymbols = new HashSet<>();

        for (Map.Entry<String, Set<ExchangeService>> entry : symbolExchangeMap.entrySet()) {
            String symbol = entry.getKey();
            Set<ExchangeService> supportingExchanges = entry.getValue();

            if (supportingExchanges.size() >= 2) {
                validArbitrageSymbols.add(symbol);
            }
        }

        if (validArbitrageSymbols.isEmpty()) {
            logWarning("No symbols found available on at least two exchanges after normalization");
            return new ArrayList<>();
        }

        logInfo("Found " + validArbitrageSymbols.size() + " symbols available on at least two exchanges");

        // Create mapping of normalized symbols to original symbols for each exchange
        // This will help us later when we need to query data for specific exchange formats
        exchangeSymbolMap.clear(); // Clear any previous mappings

        for (String normalizedSymbol : validArbitrageSymbols) {
            StringBuilder sb = new StringBuilder("Found tradable symbol: " + normalizedSymbol + " on exchanges: (");
            boolean first = true;
            Set<ExchangeService> exchanges = symbolExchangeMap.get(normalizedSymbol);

            for (ExchangeService exchange : exchanges) {
                Map<String, String> symbolMap = exchangeSymbolMaps.get(exchange);
                String originalSymbol = symbolMap.get(normalizedSymbol);

                if (originalSymbol != null) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(exchange.getExchangeName()).append(": ").append(originalSymbol);
                    first = false;

                    // Store the mapping for later use
                    if (!exchangeSymbolMap.containsKey(exchange)) {
                        exchangeSymbolMap.put(exchange, new HashMap<>());
                    }
                    exchangeSymbolMap.get(exchange).put(normalizedSymbol, originalSymbol);
                }
            }
            sb.append(")");
            logInfo(sb.toString());
        }

        return new ArrayList<>(validArbitrageSymbols);
    }
    
    /**
     * Normalizes a symbol to a standard format.
     */
    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return "";
        }
        
        // Convert to uppercase first
        String normalized = symbol.toUpperCase().trim();
        
        // Replace common separators with standard slash
        normalized = normalized.replace("-", "/")
                              .replace("_", "/")
                              .replace(".", "/");
        
        // Common normalization for popular symbols
        // This ensures consistency across exchanges with different naming conventions
        if (normalized.equals("XBTUSDT") || normalized.equals("XBTUSD")) {
            normalized = "BTC/USDT";
        } else if (normalized.equals("XETHUSDT") || normalized.equals("ETHUSDT")) {
            normalized = "ETH/USDT";
        }
        
        // Add more common normalizations if needed
        
        return normalized;
    }
    
    /**
     * Initialize WebSocket connections for real-time data
     */
    private void initializeWebSockets(Set<String> tradableSymbols) {
        if (tradableSymbols.isEmpty()) {
            updateStatus("No tradable symbols found. Cannot initialize WebSockets.");
            return;
        }
        
        Map<String, Boolean> results = new HashMap<>();
        
        // Process each exchange separately
        for (ExchangeService exchange : exchanges) {
            try {
                String name = exchange.getExchangeName().toLowerCase();
                
                // Get exchange-specific symbols
                if (!exchangeSymbolMap.containsKey(exchange)) {
                    logWarning("No symbol mapping for " + name);
                    results.put(name, false);
                    continue;
                }
                
                List<String> exchangeSpecificSymbols = new ArrayList<>();
                Map<String, String> symbolMap = exchangeSymbolMap.get(exchange);
                
                for (String normalizedSymbol : tradableSymbols) {
                    String exchangeSymbol = symbolMap.get(normalizedSymbol);
                    if (exchangeSymbol != null) {
                        exchangeSpecificSymbols.add(exchangeSymbol);
                    }
                }
                
                if (exchangeSpecificSymbols.isEmpty()) {
                    logWarning("No supported symbols for " + name);
                    results.put(name, false);
                    continue;
                }
                
                // Initialize WebSocket
                boolean success = exchange.initializeWebSocket(exchangeSpecificSymbols);
                results.put(name, success);
                logInfo("WebSocket connection for " + name + ": " + (success ? "Success" : "Failed"));
                
            } catch (Exception e) {
                String name = exchange.getExchangeName().toLowerCase();
                logError("Error initializing WebSocket for " + name, e);
                results.put(name, false);
            }
        }
        
        // Report connection status
        StringBuilder status = new StringBuilder("WebSocket Status:\n");
        int connectedCount = 0;
        int failedCount = 0;
        
        for (Map.Entry<String, Boolean> entry : results.entrySet()) {
            boolean connected = entry.getValue();
            status.append("- ").append(entry.getKey()).append(": ")
                  .append(connected ? "Connected" : "Failed").append("\n");
            
            if (connected) {
                connectedCount++;
            } else {
                failedCount++;
            }
        }
        
        if (failedCount > 0) {
            status.append("\n").append(failedCount).append(" of ").append(results.size())
                  .append(" exchanges failed to connect. Check logs for details.");
            status.append("\nThe app will continue with ").append(connectedCount)
                  .append(" connected exchanges.");
        }
        
        updateStatus(status.toString());
    }

    /**
     * Schedule periodic arbitrage scans
     */
    private void schedulePeriodicScans(List<ExchangeService> exchanges, Set<String> tradableSymbols) {
        // Cancel any existing scheduler
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        
        // Create a new scheduler
        scheduler = Executors.newScheduledThreadPool(1);
        
        // Get scan interval from configuration (default: 5 seconds)
        int scanInterval = ConfigurationFactory.getInteger("system.scheduling.arbitrageScanInterval", 5000);
        
        // Create the scanning task
        Runnable task = () -> {
            try {
                runArbitrageComparison(exchanges, tradableSymbols);
            } catch (Exception e) {
                logError("Error during arbitrage scan", e);
            }
        };
        
        // Schedule the task to run periodically
        scheduler.scheduleAtFixedRate(task, 1000, scanInterval, TimeUnit.MILLISECONDS);
        
        logInfo("Scheduled arbitrage scans every " + (scanInterval / 1000) + " seconds");
    }
    
    /**
     * Run arbitrage comparison between exchanges
     */
    private void runArbitrageComparison(List<ExchangeService> exchanges, Set<String> tradableSymbols) {
        if (tradableSymbols.isEmpty() || exchanges.size() < 2) {
            updateStatus("Cannot scan for arbitrage: Not enough exchanges or symbols");
            return;
        }
        
        final Date scanTime = new Date();
        final List<ArbitrageOpportunity> opportunities = new ArrayList<>();
        final StringBuilder displayText = new StringBuilder();
        
        displayText.append("Scanning ").append(tradableSymbols.size()).append(" symbols across ")
                 .append(exchanges.size()).append(" exchanges...\n\n");
        
        // Track processed combinations to avoid duplicates
        Set<String> processedCombinations = new HashSet<>();
        
        // Create risk calculator
        RiskCalculator riskCalculator = new RiskCalculator(MIN_PROFIT_PERCENT / 100);
        
        // Log details about exchanges being compared
        StringBuilder exchangeInfo = new StringBuilder("Checking arbitrage between exchanges: ");
        for (ExchangeService ex : exchanges) {
            exchangeInfo.append(ex.getExchangeName()).append(", ");
        }
        logInfo(exchangeInfo.toString());

        // Compare each exchange with every other exchange
        for (int i = 0; i < exchanges.size(); i++) {
            for (int j = 0; j < exchanges.size(); j++) {
                // Skip comparing exchange with itself
                if (i == j) continue;
                
                ExchangeService exA = exchanges.get(i);
                ExchangeService exB = exchanges.get(j);
                
                // Create unique key for this exchange pair to avoid duplicates
                String exchangePairKey = exA.getExchangeName() + "-" + exB.getExchangeName();
                if (processedCombinations.contains(exchangePairKey)) {
                    continue;
                }
                processedCombinations.add(exchangePairKey);
                
                logInfo("Comparing " + exA.getExchangeName() + " with " + exB.getExchangeName());
                int opportunityCount = 0;
                
                for (String symbol : tradableSymbols) {
                    try {
                        // Check if both exchanges support this symbol
                        if (!exchangeSymbolMap.containsKey(exA) || !exchangeSymbolMap.containsKey(exB)) {
                            continue;
                        }
                        
                        String symbolA = exchangeSymbolMap.get(exA).get(symbol);
                        String symbolB = exchangeSymbolMap.get(exB).get(symbol);
                        
                        // Skip if symbol not available on both exchanges
                        if (symbolA == null || symbolB == null) {
                            continue;
                        }
                        
                        // Calculate arbitrage opportunity
                        TradingPair pair = new TradingPair(symbol);
                        ExchangeToExchangeArbitrage arbitrageCalc = new ExchangeToExchangeArbitrage(exA, exB);
                        ArbitrageOpportunity opportunity = arbitrageCalc.calculateArbitrage(pair);
                        
                        // Process opportunity if profitable
                        if (opportunity != null && opportunity.getProfitPercent() > MIN_PROFIT_PERCENT) {
                            processArbitrageOpportunity(opportunity, exA, exB, riskCalculator);
                            opportunities.add(opportunity);
                            opportunityCount++;
                            
                            // Format opportunity for display
                            displayText.append(formatOpportunity(opportunity)).append("\n\n");
                        }
                    } catch (Exception e) {
                        logError("Error calculating arbitrage for " + symbol + " between " + 
                                exA.getExchangeName() + " and " + exB.getExchangeName(), e);
                    }
                }
                
                logInfo("Found " + opportunityCount + " opportunities between " + 
                        exA.getExchangeName() + " and " + exB.getExchangeName());
            }
        }
        
        // Update results display
        if (opportunities.isEmpty()) {
            displayText.append("No arbitrage opportunities found above ")
                     .append(MIN_PROFIT_PERCENT).append("% profit threshold.");
        } else {
            // Sort opportunities by profit percentage
            opportunities.sort((o1, o2) -> Double.compare(o2.getProfitPercent(), o1.getProfitPercent()));
            
            // Show exchange distribution
            Map<String, Integer> exchangePairCount = new HashMap<>();
            for (ArbitrageOpportunity opp : opportunities) {
                String pair = opp.getExchangeBuy() + "-" + opp.getExchangeSell();
                exchangePairCount.put(pair, exchangePairCount.getOrDefault(pair, 0) + 1);
            }
            
            displayText.append("Found ").append(opportunities.size())
                     .append(" arbitrage opportunities!\n\n");
            
            displayText.append("Opportunities by exchange pairs:\n");
            for (Map.Entry<String, Integer> entry : exchangePairCount.entrySet()) {
                displayText.append("- ").append(entry.getKey()).append(": ")
                          .append(entry.getValue()).append(" opportunities\n");
            }
            displayText.append("\n");
        }
        
        displayText.append("\nLast updated: ").append(scanTime);
        
        // Update the UI with our findings
        runOnUiThread(() -> {
            updateStatus("Last scan: " + scanTime + " - Found " + opportunities.size() + " opportunities");
            if (opportunitiesTextView != null) {
                opportunitiesTextView.setText(displayText.toString());
            }
        });
    }
    
    /**
     * Process a single arbitrage opportunity with risk assessment
     */
    private void processArbitrageOpportunity(ArbitrageOpportunity opportunity, 
                                            ExchangeService exA, ExchangeService exB,
                                            RiskCalculator riskCalculator) {
        // Get buy/sell exchange information
        String buyExchange = opportunity.getExchangeBuy();
        String sellExchange = opportunity.getExchangeSell();
        double buyPrice = opportunity.getBuyPrice();
        double sellPrice = opportunity.getSellPrice();
        
        // Determine which exchange service is buy/sell
        ExchangeService buyExchangeService = null;
        ExchangeService sellExchangeService = null;
        
        if (buyExchange.equals(exA.getExchangeName())) {
            buyExchangeService = exA;
            sellExchangeService = exB;
        } else {
            buyExchangeService = exB;
            sellExchangeService = exA;
        }
        
        if (buyExchangeService != null && sellExchangeService != null) {
            // Get fees
            // Use calculateFee method with a sample amount to derive the percentage
            double sampleAmount = 1000.0; // Use 1000 as base for easier percentage calculation
            double buyFeeAmount = buyExchangeService.getMakerFee().calculateFee(sampleAmount);
            double sellFeeAmount = sellExchangeService.getTakerFee().calculateFee(sampleAmount);
            
            // Calculate fee percentages
            double buyFeePercent = (buyFeeAmount / sampleAmount) * 100.0;
            double sellFeePercent = (sellFeeAmount / sampleAmount) * 100.0;
            
            // Calculate dynamic quantity based on price
            double quantity = calculateDynamicQuantity(buyPrice);
            
            // Get tickers for risk assessment
            String buySymbol = exchangeSymbolMap.get(buyExchangeService).get(opportunity.getNormalizedSymbol());
            String sellSymbol = exchangeSymbolMap.get(sellExchangeService).get(opportunity.getNormalizedSymbol());
            
            Ticker buyTicker = buyExchangeService.getTickerData(buySymbol);
            Ticker sellTicker = sellExchangeService.getTickerData(sellSymbol);
            
            // Calculate risk assessment
            RiskAssessment risk = riskCalculator.calculateRisk(
                buyTicker, 
                sellTicker,
                buyFeePercent / 100, 
                sellFeePercent / 100
            );
            
            // Set data in opportunity
            opportunity.setRiskAssessment(risk);
            opportunity.setBuyTicker(buyTicker);
            opportunity.setSellTicker(sellTicker);
            
            // Calculate optimal position size
            double optimalPositionSize = calculateOptimalPositionSize(
                opportunity,
                AVAILABLE_CAPITAL,
                MAX_POSITION_PERCENT
            );
            
            // Calculate expected slippage
            double tradeSize = optimalPositionSize / buyPrice;
            double buySlippage = calculateExpectedSlippage(buyTicker, true, tradeSize, buySymbol);
            double sellSlippage = calculateExpectedSlippage(sellTicker, false, tradeSize, sellSymbol);
            
            // Store slippage data
            opportunity.setBuySlippage(buySlippage);
            opportunity.setSellSlippage(sellSlippage);
        }
    }
    
    /**
     * Format an arbitrage opportunity for display
     */
    private String formatOpportunity(ArbitrageOpportunity opportunity) {
        if (opportunity == null) {
            return "Invalid opportunity";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(">>> %s: Buy on %s at %s, Sell on %s at %s\n",
            opportunity.getNormalizedSymbol(),
            opportunity.getExchangeBuy(),
            formatPrice(opportunity.getBuyPrice()),
            opportunity.getExchangeSell(),
            formatPrice(opportunity.getSellPrice())));

        sb.append(String.format("    Profit: %.4f%% | Success Rate: %.2f%%\n",
            opportunity.getProfitPercent(),
            opportunity.getSuccessfulArbitragePercent()));
            
        // Add risk assessment if available
        RiskAssessment risk = opportunity.getRiskAssessment();
        if (risk != null) {
            sb.append(String.format("    Risk Score: %.2f | Liquidity: %.2f | Volatility: %.2f\n",
                risk.getOverallRiskScore(),
                risk.getLiquidityScore(),
                risk.getVolatilityScore()));

            // Add slippage information
            sb.append(String.format("    Slippage: Buy: %.4f%% | Sell: %.4f%% | Total: %.4f%%\n",
                opportunity.getBuySlippage() * 100,
                opportunity.getSellSlippage() * 100,
                (opportunity.getBuySlippage() + opportunity.getSellSlippage()) * 100));
        }
        
        return sb.toString();
    }

    /**
     * Calculates the optimal position size for an arbitrage opportunity based on risk assessment.
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
        double potentialProfit = opportunity.getProfitPercent() / 100.0;
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
     * This version uses the advanced slippage calculator with dynamic calibration.
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
            logError("Error calculating slippage", e);
            // Fallback to basic calculation
            return calculateBasicSlippage(ticker, isBuy, tradeAmount);
        }
    }

    /**
     * Fallback method for basic slippage calculation when advanced analytics are unavailable.
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

    /**
     * Formats price values with appropriate scientific notation for small values
     */
    private String formatPrice(double price) {
        if (price < 0.001) {
            return String.format("%.4E", price);
        } else {
            return String.format("%.8f", price);
        }
    }
    
    /**
     * Update the status text on the UI thread
     */
    private void updateStatus(final String message) {
        runOnUiThread(() -> {
            if (statusTextView != null) {
                statusTextView.setText(message);
            }
            logInfo(message);
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Shutdown the scheduler
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        
        // Close WebSocket connections
        for (ExchangeService exchange : exchanges) {
            try {
                if (exchange.isWebSocketConnected()) {
                    exchange.closeWebSocket();
                }
            } catch (Exception e) {
                logError("Error closing WebSocket for " + exchange.getExchangeName(), e);
            }
        }
        
        logInfo("ArbitrageActivity destroyed, all resources released");
    }
    
    // INotificationService implementation
    
    @Override
    public void logInfo(String message) {
        Log.i(TAG, message);
    }
    
    @Override
    public void logWarning(String message) {
        Log.w(TAG, message);
    }
    
    @Override
    public void logError(String message, Throwable error) {
        Log.e(TAG, message, error);
    }
    
    @Override
    public void logDebug(String message) {
        Log.d(TAG, message);
    }
    
    @Override
    public void notify(String title, String message, String type) {
        // Simple implementation - log the notification
        Log.i(TAG, "Notification (" + type + "): " + title + " - " + message);
        // In a real app, this would show a proper Android notification
    }
    
    @Override
    public void notifyArbitrageOpportunity(com.example.tradient.data.interfaces.ArbitrageResult opportunity) {
        // Log opportunity details
        Log.i(TAG, "Arbitrage opportunity found: " + opportunity);
        // In a real app, this would show a notification or update UI
    }
    
    @Override
    public void notifyArbitrageError(Throwable error) {
        // Log the arbitrage error
        Log.e(TAG, "Arbitrage error", error);
        // In a real app, this would show an error notification
    }

    /**
     * Calculate a dynamic quantity based on price to ensure reasonable trade volumes
     * @param price The price of the asset
     * @return An appropriate quantity to trade
     */
    private double calculateDynamicQuantity(double price) {
        if (price < 0.001) {
            return 1000000; // 1 million units for micro-priced tokens like SHIB
        } else if (price < 1.0) {
            return 1000; // 1,000 units for tokens under $1
        } else if (price < 100.0) {
            return 10; // 10 units for tokens under $100
        } else {
            return 0.01; // 0.01 units for expensive tokens like BTC
        }
    }
} 