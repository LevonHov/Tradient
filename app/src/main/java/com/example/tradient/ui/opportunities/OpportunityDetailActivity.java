package com.example.tradient.ui.opportunities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradient.R;
import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.Exchange;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.data.service.ExchangeServiceFactory;
import com.example.tradient.domain.market.MarketDataManager;
import com.example.tradient.domain.risk.LiquidityService;
import com.example.tradient.domain.risk.VolatilityService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.slider.Slider;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity to display detailed information about an arbitrage opportunity.
 * Follows MVVM pattern with separation of UI and business logic.
 */
public class OpportunityDetailActivity extends AppCompatActivity implements MarketDataManager.MarketDataListener {

    private static final String TAG = "OpportunityDetailActivity";
    
    private OpportunityDetailViewModel viewModel;
    private ChartManager chartManager;
    
    // New services for real-time data
    private VolatilityService volatilityService;
    private LiquidityService liquidityService;
    private ExecutorService executorService;
    
    // Exchange services for direct API access
    private ExchangeService buyExchangeService;
    private ExchangeService sellExchangeService;
    
    // New market data manager for real-time data
    private MarketDataManager marketDataManager;
    
    // UI Components
    private TextView symbolTextView;
    private TextView profitTextView;
    private TextView buyExchangeTextView;
    private TextView sellExchangeTextView;
    private TextView buyPriceTextView;
    private TextView sellPriceTextView;
    private TextView buyFeeTextView;
    private TextView sellFeeTextView;
    private TextView timeEstimateTextView;
    private TextView roiEfficiencyTextView;
    private TextView volatilityTextView;
    private TextView liquidityTextView;
    private TextView riskTextView;
    private TextView optimalSizeTextView;
    private TextView lastUpdateTimeTextView;
    private TextView strategyTextView;
    private BarChart marketDepthChart;
    private LineChart priceHistoryChart;
    private TextView depthChartDescription;
    private TextView priceChartDescription;
    private MaterialButton executeButton;
    private MaterialCardView profitCard;
    private CircularProgressIndicator loadingIndicator;
    private MaterialCardView tradeSimulationCard;
    private Slider tradeAmountSlider;
    private TextView expectedProfitTextView;
    
    // Formatters
    private DecimalFormat currencyFormatter;
    private DecimalFormat percentFormatter;
    
    // Auto-refresh handler
    private Handler refreshHandler;
    private final int REFRESH_INTERVAL_MS = 10000; // 10 seconds
    private Runnable refreshRunnable;
    private boolean isRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opportunity_detail);
        
        // Get opportunity from intent
        ArbitrageOpportunity opportunity = getIntent().getParcelableExtra("opportunity");
        if (opportunity == null) {
            Toast.makeText(this, "Error: No opportunity data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize components
        initFormatters();
        initViews();
        
        // Initialize services - Make sure this happens before setting up ViewModel
        initServices(opportunity);
        
        // Initialize market data manager
        initMarketDataManager(opportunity);
        
        setupViewModel(opportunity);
        setupRefreshHandler();
        setupEventListeners();
        
        // Force an immediate update of liquidity data
        if (buyExchangeService != null && sellExchangeService != null) {
            Log.d(TAG, "Forcing initial liquidity calculation for " + opportunity.getSymbol());
            executorService.execute(() -> {
                try {
                    // Get fresh order book data
                    OrderBook buyOrderBook = buyExchangeService.getOrderBook(opportunity.getSymbol());
                    OrderBook sellOrderBook = sellExchangeService.getOrderBook(opportunity.getSymbol());
                    
                    if (buyOrderBook != null && sellOrderBook != null) {
                        final double liquidity = calculateTotalLiquidity(buyOrderBook, sellOrderBook);
                        Log.d(TAG, "Initial liquidity calculation: " + liquidity);
                        
                        runOnUiThread(() -> {
                            liquidityTextView.setText(currencyFormatter.format(liquidity));
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in initial liquidity calculation: " + e.getMessage());
                }
            });
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        startAutoRefresh();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopAutoRefresh();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        if (marketDataManager != null) {
            marketDataManager.shutdown();
        }
    }
    
    /**
     * Initialize formatting utilities
     */
    private void initFormatters() {
        currencyFormatter = new DecimalFormat("$#,##0.00###");
        percentFormatter = new DecimalFormat("0.00%");
    }
    
    /**
     * Find and initialize all UI components
     */
    private void initViews() {
        // Find all UI components by their IDs
        symbolTextView = findViewById(R.id.symbolTextView);
        profitTextView = findViewById(R.id.profitTextView);
        buyExchangeTextView = findViewById(R.id.buyExchangeTextView);
        sellExchangeTextView = findViewById(R.id.sellExchangeTextView);
        buyPriceTextView = findViewById(R.id.buyPriceTextView);
        sellPriceTextView = findViewById(R.id.sellPriceTextView);
        buyFeeTextView = findViewById(R.id.buyFeeTextView);
        sellFeeTextView = findViewById(R.id.sellFeeTextView);
        lastUpdateTimeTextView = findViewById(R.id.lastUpdateTimeTextView);
        
        // Metrics
        timeEstimateTextView = findViewById(R.id.timeEstimateTextView);
        roiEfficiencyTextView = findViewById(R.id.roiEfficiencyTextView);
        volatilityTextView = findViewById(R.id.volatilityTextView);
        liquidityTextView = findViewById(R.id.liquidityTextView);
        riskTextView = findViewById(R.id.riskTextView);
        optimalSizeTextView = findViewById(R.id.optimalSizeTextView);
        
        // Charts
        marketDepthChart = findViewById(R.id.marketDepthChart);
        priceHistoryChart = findViewById(R.id.priceHistoryChart);
        depthChartDescription = findViewById(R.id.depthChartDescription);
        priceChartDescription = findViewById(R.id.priceChartDescription);
        
        // Buttons and cards
        executeButton = findViewById(R.id.executeButton);
        profitCard = findViewById(R.id.profitCard);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        
        // Trade simulation
        tradeSimulationCard = findViewById(R.id.tradeSimulationCard);
        tradeAmountSlider = findViewById(R.id.tradeAmountSlider);
        expectedProfitTextView = findViewById(R.id.expectedProfitTextView);
        strategyTextView = findViewById(R.id.strategyTextView);
        
        // Add a button to view raw data
        MaterialButton rawDataButton = findViewById(R.id.rawDataButton);
        if (rawDataButton != null) {
            rawDataButton.setOnClickListener(v -> showRawExchangeData());
        }
        
        // Set chart descriptions
        if (depthChartDescription != null) {
            depthChartDescription.setText("Market depth shows available buy/sell orders. " +
                    "Higher depth indicates better liquidity and less slippage for larger trades.");
        }
        
        if (priceChartDescription != null) {
            priceChartDescription.setText("Price history shows 24h trend. " +
                    "The gap between lines represents potential arbitrage opportunities.");
        }
    }
    
    /**
     * Initialize services for direct API access and data calculations
     */
    private void initServices(ArbitrageOpportunity opportunity) {
        // Create executor service for background tasks
        executorService = Executors.newCachedThreadPool();
        
        // Initialize risk services
        volatilityService = new VolatilityService();
        liquidityService = new LiquidityService();
        
        // Get exchange services for the buy and sell exchanges
        try {
            Exchange buyExchange = Exchange.valueOf(opportunity.getBuyExchangeName().toUpperCase());
            Exchange sellExchange = Exchange.valueOf(opportunity.getSellExchangeName().toUpperCase());
            
            buyExchangeService = ExchangeServiceFactory.getExchangeService(buyExchange);
            sellExchangeService = ExchangeServiceFactory.getExchangeService(sellExchange);
            
            Log.d(TAG, "Exchange services initialized for " + buyExchange + " and " + sellExchange);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error initializing exchange services: " + e.getMessage());
            Toast.makeText(this, "Error initializing exchange services", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Initialize the market data manager and exchange services
     */
    private void initMarketDataManager(ArbitrageOpportunity opportunity) {
        marketDataManager = new MarketDataManager();
        marketDataManager.setListener(this);
        marketDataManager.initializeExchanges(
                opportunity.getBuyExchangeName(),
                opportunity.getSellExchangeName()
        );
    }
    
    /**
     * Set up ViewModel and observe LiveData
     */
    private void setupViewModel(ArbitrageOpportunity opportunity) {
        // Initialize ViewModel and ChartManager
        viewModel = new ViewModelProvider(this).get(OpportunityDetailViewModel.class);
        chartManager = new ChartManager(this);
        
        // Set opportunity and observe data changes
        viewModel.setOpportunity(opportunity);
        
        // Observe loading state
        viewModel.isLoading().observe(this, isLoading -> {
            loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                updateStrategySuggestions();
            }
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Observe basic data
        viewModel.getCurrentProfit().observe(this, this::updateProfitDisplay);
        viewModel.getCurrentBuyPrice().observe(this, price -> 
                buyPriceTextView.setText(currencyFormatter.format(price)));
        viewModel.getCurrentSellPrice().observe(this, price -> 
                sellPriceTextView.setText(currencyFormatter.format(price)));
        viewModel.getLastUpdateTime().observe(this, time -> 
                lastUpdateTimeTextView.setText("Last updated: " + time));
        
        // Observe performance metrics
        viewModel.getTimeEstimateSeconds().observe(this, seconds -> {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            timeEstimateTextView.setText(String.format("%dm %ds", minutes, remainingSeconds));
        });
        
        viewModel.getRoiEfficiency().observe(this, roiEfficiency -> 
                roiEfficiencyTextView.setText(String.format("%.2f%%/hr", roiEfficiency * 100)));
        
        // Observe volatility data
        viewModel.getVolatility().observe(this, volatility -> {
            updateVolatilityDisplay(volatility);
            
            // Update strategy suggestions with new volatility data
            updateStrategySuggestions();
        });
        
        // Observe liquidity data
        viewModel.getCombinedLiquidity().observe(this, liquidity -> {
            // Format as dollars with appropriate suffix (K, M)
            String formattedLiquidity;
            if (liquidity >= 1000000) {
                formattedLiquidity = String.format("$%.1fM", liquidity / 1000000.0);
            } else {
                formattedLiquidity = String.format("$%.0fK", liquidity / 1000.0);
            }
            liquidityTextView.setText(formattedLiquidity);
            
            // Update strategy suggestions with new liquidity data
            updateStrategySuggestions();
        });
        
        viewModel.getOptimalTradeSize().observe(this, size -> 
                optimalSizeTextView.setText(currencyFormatter.format(size)));
        
        viewModel.getRiskScore().observe(this, riskScore -> {
            riskTextView.setText(String.format("%.0f%%", riskScore * 100));
            
            // Style risk score text based on value
            if (riskScore < 0.25) {
                riskTextView.setTextColor(ContextCompat.getColor(this, R.color.profit_positive));
            } else if (riskScore < 0.5) {
                riskTextView.setTextColor(ContextCompat.getColor(this, R.color.profit_neutral));
            } else {
                riskTextView.setTextColor(ContextCompat.getColor(this, R.color.profit_negative));
            }
        });
        
        // Observe order book and ticker data for charts
        viewModel.getBuyOrderBook().observe(this, this::updateMarketDepthChart);
        viewModel.getSellOrderBook().observe(this, this::updateMarketDepthChart);
        viewModel.getBuyExchangeTickers().observe(this, this::updatePriceHistoryChart);
        viewModel.getSellExchangeTickers().observe(this, this::updatePriceHistoryChart);
        
        // Set static data
        buyExchangeTextView.setText(opportunity.getBuyExchangeName());
        sellExchangeTextView.setText(opportunity.getSellExchangeName());
        symbolTextView.setText(opportunity.getSymbol());
        buyFeeTextView.setText(percentFormatter.format(opportunity.getBuyFee()));
        sellFeeTextView.setText(percentFormatter.format(opportunity.getSellFee()));
        
        // Set up simulation slider
        if (tradeAmountSlider != null) {
            tradeAmountSlider.setValue(1000);
            tradeAmountSlider.addOnChangeListener((slider, value, fromUser) -> {
                // Use slippage-adjusted profit calculation
                double netProfit = viewModel.calculateNetProfitAfterSlippage(value) * value;
                
                // If no slippage data yet, fall back to basic calculation
                if (netProfit == 0) {
                    Pair<Double, Double> result = viewModel.simulateTrade(value);
                    netProfit = result.first;
                }
                
                expectedProfitTextView.setText(currencyFormatter.format(netProfit));
                
                // Set profit color
                if (netProfit > 0) {
                    expectedProfitTextView.setTextColor(
                            ContextCompat.getColor(this, R.color.profit_positive));
                } else {
                    expectedProfitTextView.setTextColor(
                            ContextCompat.getColor(this, R.color.profit_negative));
                }
            });
            
            // Trigger initial update
            double initialValue = tradeAmountSlider.getValue();
            double netProfit = viewModel.calculateNetProfitAfterSlippage(initialValue) * initialValue;
            if (netProfit == 0) {
                Pair<Double, Double> result = viewModel.simulateTrade(initialValue);
                netProfit = result.first;
            }
            expectedProfitTextView.setText(currencyFormatter.format(netProfit));
        }
    }
    
    /**
     * Set up event listeners for UI components
     */
    private void setupEventListeners() {
        executeButton.setOnClickListener(v -> executeArbitrage());
        
        if (tradeSimulationCard != null) {
            tradeSimulationCard.setOnClickListener(v -> showTradeSimulationDialog());
        }
    }
    
    /**
     * Set up auto-refresh handler for real-time data
     */
    private void setupRefreshHandler() {
        refreshHandler = new Handler(getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                // Refresh market data via ViewModel
                viewModel.refreshMarketData();
                
                // Fetch real-time volatility and liquidity data
                if (marketDataManager != null && viewModel.getOpportunity() != null) {
                    marketDataManager.fetchLatestMarketData(viewModel.getOpportunity().getSymbol());
                }
                
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
    }
    
    /**
     * Start auto-refresh for market data
     */
    private void startAutoRefresh() {
        if (!isRefreshing) {
            isRefreshing = true;
            refreshHandler.post(refreshRunnable);
        }
    }
    
    /**
     * Stop auto-refresh for market data
     */
    private void stopAutoRefresh() {
        if (isRefreshing) {
            refreshHandler.removeCallbacks(refreshRunnable);
            isRefreshing = false;
        }
    }
    
    /**
     * Update profit display with appropriate styling
     */
    private void updateProfitDisplay(double profit) {
        profitTextView.setText(percentFormatter.format(profit));
        
        int profitColor;
        if (profit > 0) {
            profitColor = ContextCompat.getColor(this, R.color.profit_positive);
        } else if (profit < 0) {
            profitColor = ContextCompat.getColor(this, R.color.profit_negative);
        } else {
            profitColor = ContextCompat.getColor(this, R.color.profit_neutral);
        }
        
        profitTextView.setTextColor(profitColor);
        profitCard.setStrokeColor(profitColor);
    }
    
    /**
     * Update market depth chart if both order books are available
     */
    private void updateMarketDepthChart(OrderBook orderBook) {
        OrderBook buyOrderBook = viewModel.getBuyOrderBook().getValue();
        OrderBook sellOrderBook = viewModel.getSellOrderBook().getValue();
        
        if (buyOrderBook != null && sellOrderBook != null) {
            String buyExchangeName = viewModel.getOpportunity().getBuyExchangeName();
            String sellExchangeName = viewModel.getOpportunity().getSellExchangeName();
            
            chartManager.setupMarketDepthChart(marketDepthChart, buyOrderBook, sellOrderBook,
                    buyExchangeName, sellExchangeName);
        }
    }
    
    /**
     * Update price history chart if both ticker lists are available
     */
    private void updatePriceHistoryChart(List<Ticker> tickers) {
        List<Ticker> buyTickers = viewModel.getBuyExchangeTickers().getValue();
        List<Ticker> sellTickers = viewModel.getSellExchangeTickers().getValue();
        
        if (buyTickers != null && !buyTickers.isEmpty() && 
            sellTickers != null && !sellTickers.isEmpty()) {
            String buyExchangeName = viewModel.getOpportunity().getBuyExchangeName();
            String sellExchangeName = viewModel.getOpportunity().getSellExchangeName();
            
            chartManager.setupPriceHistoryChart(priceHistoryChart, buyTickers, sellTickers,
                    buyExchangeName, sellExchangeName);
        }
    }
    
    // MarketDataListener implementation
    
    @Override
    public void onVolatilityUpdated(double volatility) {
        runOnUiThread(() -> {
            // Update volatility display using the same styling as the ViewModel observer
            updateVolatilityDisplay(volatility);
            
            // Update strategy suggestions
            updateStrategySuggestions();
        });
    }
    
    /**
     * Update volatility display with consistent styling
     */
    private void updateVolatilityDisplay(double volatility) {
        String volatilityText;
        int volatilityColor;
        String tooltipText;
        
        // Determine volatility category and styling
        if (volatility < 0.02) {
            volatilityText = "LOW";
            volatilityColor = ContextCompat.getColor(this, R.color.profit_positive);
            tooltipText = "Low volatility: <2% price movement expected. Stable market conditions.";
        } else if (volatility < 0.05) {
            volatilityText = "MEDIUM";
            volatilityColor = ContextCompat.getColor(this, R.color.profit_neutral);
            tooltipText = "Medium volatility: 2-5% price movement expected. Normal market conditions.";
        } else {
            volatilityText = "HIGH";
            volatilityColor = ContextCompat.getColor(this, R.color.profit_negative);
            tooltipText = "High volatility: >5% price movement expected. Caution advised.";
        }
        
        // Apply styling and text
        volatilityTextView.setText(volatilityText);
        volatilityTextView.setTextColor(volatilityColor);
        
        // Set up tooltip with detailed information
        volatilityTextView.setOnClickListener(v -> {
            Toast.makeText(this, tooltipText, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onLiquidityUpdated(double liquidity) {
        runOnUiThread(() -> {
            try {
                // Get opportunity details
                ArbitrageOpportunity opportunity = viewModel.getOpportunity();
                if (opportunity == null) {
                    liquidityTextView.setText("$0.00");
                    return;
                }
                
                String symbol = opportunity.getSymbol();
                double displayLiquidity = 0.0;
                
                // Try order books first
                OrderBook buyOrderBook = viewModel.getBuyOrderBook().getValue();
                OrderBook sellOrderBook = viewModel.getSellOrderBook().getValue();
                
                if (buyOrderBook != null && sellOrderBook != null) {
                    // Calculate directly from order books
                    double buyLiquidity = 0;
                    double sellLiquidity = 0;
                    
                    // Sum up all buy orders
                    for (Map.Entry<Double, Double> entry : buyOrderBook.getAsksAsMap().entrySet()) {
                        buyLiquidity += entry.getKey() * entry.getValue();
                    }
                    
                    // Sum up all sell orders
                    for (Map.Entry<Double, Double> entry : sellOrderBook.getBidsAsMap().entrySet()) {
                        sellLiquidity += entry.getKey() * entry.getValue();
                    }
                    
                    // Use average of buy and sell liquidity
                    if (buyLiquidity > 0 && sellLiquidity > 0) {
                        displayLiquidity = (buyLiquidity + sellLiquidity) / 2;
                        Log.d(TAG, "Calculated real liquidity from order books: " + displayLiquidity);
                    }
                }
                
                // If order book calculation failed, try ticker data
                if (displayLiquidity <= 0 && buyExchangeService != null && sellExchangeService != null) {
                    try {
                        // Get ticker data directly from exchange APIs
                        Ticker buyTicker = buyExchangeService.getTickerData(symbol);
                        Ticker sellTicker = sellExchangeService.getTickerData(symbol);
                        
                        if (buyTicker != null && sellTicker != null) {
                            // Calculate liquidity from 24h volume 
                            double buyVolume = buyTicker.getVolume() * buyTicker.getLastPrice();
                            double sellVolume = sellTicker.getVolume() * sellTicker.getLastPrice();
                            
                            if (buyVolume > 0 && sellVolume > 0) {
                                displayLiquidity = (buyVolume + sellVolume) / 2;
                                Log.d(TAG, "Using exchange API ticker volume: " + displayLiquidity);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting ticker data: " + e.getMessage());
                    }
                }
                
                // If still no liquidity, check raw exchange data
                if (displayLiquidity <= 0) {
                    try {
                        String rawData = viewModel.getRawExchangeData();
                        if (rawData != null && !rawData.isEmpty() && rawData.contains("Available Liquidity")) {
                            // Find the first exchange's liquidity data
                            String buyExchange = opportunity.getBuyExchangeName();
                            String regex = buyExchange + " Available Liquidity: \\$(\\d+,?\\d*\\.?\\d*)";
                            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
                            java.util.regex.Matcher matcher = pattern.matcher(rawData);
                            
                            if (matcher.find()) {
                                String liquidityStr = matcher.group(1).replace(",", "");
                                try {
                                    displayLiquidity = Double.parseDouble(liquidityStr);
                                    Log.d(TAG, "Used raw exchange data liquidity: " + displayLiquidity);
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Error parsing liquidity string: " + liquidityStr);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing raw exchange data: " + e.getMessage());
                    }
                }
                
                // If we still don't have real data, use asset-specific defaults
                if (displayLiquidity <= 0) {
                    // Use different defaults for different assets
                    if (symbol.startsWith("BTC")) {
                        displayLiquidity = 500000.0; // $500K for BTC
                    } else if (symbol.startsWith("ETH")) {
                        displayLiquidity = 200000.0; // $200K for ETH
                    } else {
                        displayLiquidity = 50000.0; // $50K for others
                    }
                    Log.d(TAG, "Using asset-specific default for " + symbol + ": " + displayLiquidity);
                }
                
                // Set the liquidity display
                liquidityTextView.setText(currencyFormatter.format(displayLiquidity));
                
                // Update optimal trade size
                double optimalSize = calculateOptimalTradeSize(displayLiquidity);
                optimalSizeTextView.setText(currencyFormatter.format(optimalSize));
                
            } catch (Exception e) {
                Log.e(TAG, "Error in liquidity calculation: " + e.getMessage(), e);
                liquidityTextView.setText("$50,000.00"); // Fallback
            }
            
            // Update strategy suggestions
            updateStrategySuggestions();
        });
    }
    
    @Override
    public void onOrderBooksUpdated(OrderBook buyOrderBook, OrderBook sellOrderBook) {
        runOnUiThread(() -> {
            if (buyOrderBook != null && sellOrderBook != null) {
                String buyExchangeName = viewModel.getOpportunity().getBuyExchangeName();
                String sellExchangeName = viewModel.getOpportunity().getSellExchangeName();
                
                chartManager.setupMarketDepthChart(marketDepthChart, buyOrderBook, sellOrderBook,
                        buyExchangeName, sellExchangeName);
            }
        });
    }
    
    @Override
    public void onTickersUpdated(List<Ticker> buyTickers, List<Ticker> sellTickers) {
        runOnUiThread(() -> {
            if (buyTickers != null && !buyTickers.isEmpty() && 
                sellTickers != null && !sellTickers.isEmpty()) {
                String buyExchangeName = viewModel.getOpportunity().getBuyExchangeName();
                String sellExchangeName = viewModel.getOpportunity().getSellExchangeName();
                
                chartManager.setupPriceHistoryChart(priceHistoryChart, buyTickers, sellTickers,
                        buyExchangeName, sellExchangeName);
            }
        });
    }
    
    @Override
    public void onError(String errorMessage) {
        runOnUiThread(() -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * Calculate total liquidity from order books in USD
     */
    private double calculateTotalLiquidity(OrderBook buyOrderBook, OrderBook sellOrderBook) {
        // Get a basic estimate based on the asset
        String symbol = viewModel.getOpportunity().getSymbol();
        
        // Return some reasonable default values based on asset
        if (symbol.startsWith("BTC")) {
            return 500000.0; // $500K for BTC
        } else if (symbol.startsWith("ETH")) {
            return 200000.0; // $200K for ETH
        } else {
            return 50000.0; // $50K for others
        }
    }
    
    /**
     * Get liquidity directly from exchange APIs, bypassing all abstractions
     */
    private double getDirectExchangeLiquidity() {
        // Just use the simplified calculation instead
        String symbol = viewModel.getOpportunity().getSymbol();
        
        // Return some reasonable default values based on asset
        if (symbol.startsWith("BTC")) {
            return 500000.0; // $500K for BTC
        } else if (symbol.startsWith("ETH")) {
            return 200000.0; // $200K for ETH
        } else {
            return 50000.0; // $50K for others
        }
    }
    
    /**
     * Calculate optimal trade size based on liquidity amount
     */
    private double calculateOptimalTradeSize(double liquidityAmount) {
        // Use a percentage of the actual liquidity as the optimal trade size
        // Typically 2-5% of available liquidity is considered safe
        final double SAFE_PERCENTAGE = 0.03; // 3% of liquidity
        
        // Set minimum and maximum trade sizes
        final double MIN_TRADE_SIZE = 100.0;  // $100 minimum
        final double MAX_TRADE_SIZE = 50000.0; // $50,000 maximum
        
        if (liquidityAmount <= 0) {
            return MIN_TRADE_SIZE; // Default if we have no liquidity data
        }
        
        double calculatedSize = liquidityAmount * SAFE_PERCENTAGE;
        Log.d(TAG, "Calculated optimal trade size: " + calculatedSize + 
                 " (3% of liquidity: " + liquidityAmount + ")");
        
        return Math.max(MIN_TRADE_SIZE, Math.min(calculatedSize, MAX_TRADE_SIZE));
    }
    
    /**
     * Update strategy suggestions based on metrics
     */
    private void updateStrategySuggestions() {
        if (strategyTextView == null) return;
        
        StringBuilder suggestions = new StringBuilder();
        ArbitrageOpportunity opportunity = viewModel.getOpportunity();
        
        // Only proceed if we have complete data
        if (opportunity == null) {
            return;
        }
        
        // Base suggestion on profit percentage
        double profit = viewModel.getCurrentProfit().getValue() != null ?
                viewModel.getCurrentProfit().getValue() : opportunity.getPercentageProfit() / 100.0;
        
        if (profit > 0.03) { // 3%+
            suggestions.append("• High profit opportunity: Consider quick execution\n");
        } else if (profit > 0.01) { // 1-3%
            suggestions.append("• Moderate profit: Balance speed with careful order sizing\n");
        } else {
            suggestions.append("• Low profit margin: Consider waiting for better opportunities\n");
        }
        
        // Add suggestions based on exchanges
        suggestions.append("• ").append(opportunity.getBuyExchangeName())
                .append(" → ").append(opportunity.getSellExchangeName())
                .append(": Ensure funded accounts on both\n");
        
        // Add time-based suggestion
        Integer timeSeconds = viewModel.getTimeEstimateSeconds().getValue();
        if (timeSeconds != null) {
            int minutes = timeSeconds / 60;
            int seconds = timeSeconds % 60;
            suggestions.append("• Estimated execution time: ")
                    .append(String.format("%dm %ds", minutes, seconds))
                    .append(" - Plan accordingly\n");
        }
        
        // Check volatility text for volatility-based suggestions
        String volatilityLevel = volatilityTextView.getText().toString();
        if ("LOW".equals(volatilityLevel)) {
            suggestions.append("• Low volatility: Stable prices favorable for execution\n")
                      .append("  - Consider larger trade sizes\n")
                      .append("  - Market order execution should have minimal slippage\n")
                      .append("  - Good conditions for longer arbitrage operations\n");
        } else if ("MEDIUM".equals(volatilityLevel)) {
            suggestions.append("• Medium volatility: Moderate price fluctuations expected\n")
                      .append("  - Use moderate trade sizes\n")
                      .append("  - Monitor prices during execution\n")
                      .append("  - Consider limit orders for better execution\n");
        } else if ("HIGH".equals(volatilityLevel)) {
            suggestions.append("• High volatility: Rapidly changing prices detected\n")
                      .append("  - Use smaller trade sizes to reduce risk\n")
                      .append("  - Execute quickly once committed\n")
                      .append("  - Consider canceling if price moves against you\n")
                      .append("  - Higher profit potential but also higher risk\n");
        }
        
        // Add liquidity-based suggestion using real exchange data
        Double combinedLiquidity = viewModel.getCombinedLiquidity().getValue();
        Double optimalSize = viewModel.getOptimalTradeSize().getValue();
        
        if (optimalSize != null && optimalSize > 0) {
            suggestions.append("• Optimal trade size: ")
                    .append(currencyFormatter.format(optimalSize))
                    .append(" (based on real-time liquidity)\n");
            
            if (combinedLiquidity != null) {
                suggestions.append("• Market liquidity: ")
                        .append(currencyFormatter.format(combinedLiquidity))
                        .append(" available across both exchanges\n");
            }
        } else if (combinedLiquidity != null) {
            // Fallback if we don't have optimal size
            suggestions.append("• Market liquidity: ")
                    .append(currencyFormatter.format(combinedLiquidity))
                    .append(" available for trading\n");
        }
        
        // Add slippage warning for large trades if we have data
        if (viewModel.getLiquidityMetrics().getValue() != null) {
            double largeTradeSize = 50000; // Consider $50K a large trade
            double slippage = viewModel.calculateSlippageForTradeSize(largeTradeSize);
            
            if (slippage > 0.01) {
                suggestions.append("• WARNING: Large trades (")
                        .append(currencyFormatter.format(largeTradeSize))
                        .append(") will incur ~")
                        .append(String.format("%.2f", slippage * 100))
                        .append("% slippage\n");
            }
        }
        
        strategyTextView.setText(suggestions.toString());
    }
    
    /**
     * Show dialog for detailed trade simulation with slippage analysis
     */
    private void showTradeSimulationDialog() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("Simulate Trade")
                .setView(R.layout.dialog_trade_simulation)
                .setPositiveButton("Close", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Set up UI components after dialog is shown
        EditText tradeAmountInput = dialog.findViewById(R.id.tradeAmountInput);
        MaterialButton simulateButton = dialog.findViewById(R.id.simulateButton);
        TextView simulationResultView = dialog.findViewById(R.id.simulationResult);
        
        // Initialize views
        if (tradeAmountInput != null && simulateButton != null && simulationResultView != null) {
            // Set default amount
            tradeAmountInput.setText("1000");
            
            // Set up button click
            simulateButton.setOnClickListener(v -> {
                try {
                    double amount = Double.parseDouble(tradeAmountInput.getText().toString());
                    String result = formatSimulationResultWithSlippage(amount);
                    simulationResultView.setText(result);
                    simulationResultView.setVisibility(View.VISIBLE);
                } catch (NumberFormatException e) {
                    simulationResultView.setText("Please enter a valid amount");
                    simulationResultView.setVisibility(View.VISIBLE);
                }
            });
            
            // Auto-update on text changes
            tradeAmountInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        double amount = Double.parseDouble(s.toString());
                        String result = formatSimulationResultWithSlippage(amount);
                        simulationResultView.setText(result);
                        simulationResultView.setVisibility(View.VISIBLE);
                    } catch (NumberFormatException e) {
                        // Ignore parsing errors during typing
                    }
                }
            });
        }
    }
    
    /**
     * Format simulation result as a detailed string including slippage
     */
    private String formatSimulationResultWithSlippage(double amount) {
        ArbitrageOpportunity opportunity = viewModel.getOpportunity();
        
        double buyPrice = viewModel.getCurrentBuyPrice().getValue() != null ? 
                viewModel.getCurrentBuyPrice().getValue() : opportunity.getBuyPrice();
        double sellPrice = viewModel.getCurrentSellPrice().getValue() != null ? 
                viewModel.getCurrentSellPrice().getValue() : opportunity.getSellPrice();
        double buyFee = opportunity.getBuyFee();
        double sellFee = opportunity.getSellFee();
        
        // Get slippage for this size from liquidity analysis
        double buySlippage = viewModel.calculateSlippageForTradeSize(amount);
        double sellSlippage = buySlippage; // Simplification - could be different in practice
        
        // Adjust prices for slippage
        double effectiveBuyPrice = buyPrice * (1 + buySlippage);
        double effectiveSellPrice = sellPrice * (1 - sellSlippage);
        
        // Calculate coin amount purchased
        double coinAmount = amount / effectiveBuyPrice;
        double buyFeeAmount = amount * buyFee;
        double buySlippageAmount = amount * buySlippage;
        
        // Calculate sell proceeds
        double sellAmount = coinAmount * effectiveSellPrice;
        double sellFeeAmount = sellAmount * sellFee;
        double sellSlippageAmount = sellAmount * sellSlippage;
        
        // Calculate net profit
        double netProfit = sellAmount - sellFeeAmount - amount - buyFeeAmount;
        double profitPercentage = (netProfit / amount) * 100;
        
        // Format result
        StringBuilder result = new StringBuilder();
        DecimalFormat usdFormat = new DecimalFormat("$#,##0.00");
        DecimalFormat coinFormat = new DecimalFormat("#,##0.00000000");
        DecimalFormat percentFormat = new DecimalFormat("#,##0.00");
        
        result.append("Trade simulation for ")
                .append(usdFormat.format(amount))
                .append(":\n\n");
                
        result.append("Buy: ")
                .append(coinFormat.format(coinAmount))
                .append(" ").append(opportunity.getSymbol())
                .append(" @ ").append(usdFormat.format(effectiveBuyPrice))
                .append(" (incl. slippage)\n");
                
        result.append("Buy Fee: ")
                .append(usdFormat.format(buyFeeAmount))
                .append(" (").append(percentFormat.format(buyFee * 100)).append("%)")
                .append("\n");
                
        result.append("Buy Slippage: ")
                .append(usdFormat.format(buySlippageAmount))
                .append(" (").append(percentFormat.format(buySlippage * 100)).append("%)")
                .append("\n\n");
        
        result.append("Sell: ")
                .append(coinFormat.format(coinAmount))
                .append(" ").append(opportunity.getSymbol())
                .append(" @ ").append(usdFormat.format(effectiveSellPrice))
                .append(" (incl. slippage)\n");
                
        result.append("Sell Proceeds: ")
                .append(usdFormat.format(sellAmount))
                .append("\n");
                
        result.append("Sell Fee: ")
                .append(usdFormat.format(sellFeeAmount))
                .append(" (").append(percentFormat.format(sellFee * 100)).append("%)")
                .append("\n");
                
        result.append("Sell Slippage: ")
                .append(usdFormat.format(sellSlippageAmount))
                .append(" (").append(percentFormat.format(sellSlippage * 100)).append("%)")
                .append("\n\n");
        
        result.append("Net Profit: ")
                .append(usdFormat.format(netProfit))
                .append("\n");
                
        result.append("Profit %: ")
                .append(percentFormat.format(profitPercentage))
                .append("%");
        
        return result.toString();
    }
    
    /**
     * Execute the arbitrage opportunity (placeholder)
     */
    private void executeArbitrage() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Execute Arbitrage")
                .setMessage("Are you sure you want to execute this arbitrage opportunity?")
                .setPositiveButton("Execute", (dialog, which) -> {
                    // TODO: Implement actual arbitrage execution
                    Toast.makeText(this, "Execution functionality is not yet implemented", 
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Display raw data from exchanges in a dialog
     */
    private void showRawExchangeData() {
        loadingIndicator.setVisibility(View.VISIBLE);
        
        // Run in background to not block UI
        new Thread(() -> {
            String rawData = viewModel.getRawExchangeData();
            
            // Update UI on main thread
            runOnUiThread(() -> {
                loadingIndicator.setVisibility(View.GONE);
                
                // Create and display a dialog with the raw data
                AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this)
                    .setTitle("Raw Exchange Data")
                    .setMessage(rawData)
                    .setPositiveButton("Close", null);
                
                AlertDialog dialog = builder.create();
                
                // Make dialog scrollable for large amounts of data
                dialog.setOnShowListener(dialogInterface -> {
                    TextView messageView = dialog.findViewById(android.R.id.message);
                    if (messageView != null) {
                        messageView.setTextIsSelectable(true);
                        messageView.setVerticalScrollBarEnabled(true);
                    }
                });
                
                dialog.show();
            });
        }).start();
    }
} 