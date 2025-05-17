package com.example.tradient.ui.opportunities;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.tradient.R;
import com.example.tradient.data.interfaces.ArbitrageResult;
import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.domain.risk.RiskCalculator;
import com.example.tradient.domain.risk.UnifiedRiskCalculator;
import com.example.tradient.domain.risk.RiskEnsurer;
import com.example.tradient.infrastructure.ExchangeRegistry;
import com.example.tradient.repository.ExchangeRepository;
import com.example.tradient.util.RiskAssessmentAdapter;
import com.example.tradient.util.TimeEstimationUtil;
import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A simplified activity to display details about arbitrage opportunities.
 * This version focuses on essential information without complex charts.
 */
public class OpportunityDetailActivity extends AppCompatActivity {
    private static final String TAG = "OpportunityDetailActivity";
    
    // UI Components - Basic Info
    private TextView symbolText;
    private TextView buyExchangeText;
    private TextView sellExchangeText;
    private TextView buyPriceText;
    private TextView sellPriceText;
    private TextView profitText;
    private ImageView buyExchangeLogo;
    private ImageView sellExchangeLogo;
    
    // UI Components - Financial Details
    private TextView buyFeeText;
    private TextView sellFeeText;
    private TextView netProfitText;
    private TextView slippageText;
    private TextView totalCostText;
    
    // UI Components - Risk Assessment
    private TextView riskLevelText;
    private TextView liquidityText;
    private TextView volatilityText;
    private TextView executionTimeText;
    private View riskIndicator;
    private ProgressBar riskProgressBar;
    
    // UI Components - Action Buttons
    private Button refreshButton;
    private Button simulateButton;
    private ProgressBar loadingProgress;
    
    // Data
    private ArbitrageOpportunity opportunity;
    private UnifiedRiskCalculator unifiedRiskCalculator;
    private ExchangeRepository exchangeRepository;
    private ExecutorService executorService;
    
    // Formatters
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final DecimalFormat percentFormat = new DecimalFormat("0.00%");
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00####");
    
    // Refresh handling
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final int AUTO_REFRESH_INTERVAL = 10000; // 10 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opportunity_detail);
        
        // Initialize components
        initializeComponents();
        
        // Get opportunity from intent
        if (getIntent() != null && getIntent().hasExtra("opportunity")) {
            opportunity = getIntent().getParcelableExtra("opportunity");
            if (opportunity != null) {
                // Set up services
                unifiedRiskCalculator = UnifiedRiskCalculator.getInstance();
                exchangeRepository = new ExchangeRepository(this);
                executorService = Executors.newCachedThreadPool();
                
                // Ensure consistent risk values before display
                opportunity = RiskEnsurer.ensureRiskValues(opportunity, true);
                
                // Display data from the Parcelable first
                displayOpportunityData();
                
                // Then queue up a refresh to get real-time data
                Log.d(TAG, "Queueing initial data refresh");
                new Handler().postDelayed(() -> refreshData(), 500);
            } else {
                showError("Invalid opportunity data received");
            }
        } else {
            showError("No opportunity data found");
        }
        
        // Set up action listeners
        setupActionListeners();
        
        // Configure toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Opportunity Details");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Start auto-refresh
        startAutoRefresh();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop auto-refresh
        stopAutoRefresh();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor service
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Initialize UI components
     */
    private void initializeComponents() {
        // Basic Info
        symbolText = findViewById(R.id.symbolText);
        buyExchangeText = findViewById(R.id.buyExchangeText);
        sellExchangeText = findViewById(R.id.sellExchangeText);
        buyPriceText = findViewById(R.id.buyPriceText);
        sellPriceText = findViewById(R.id.sellPriceText);
        profitText = findViewById(R.id.profitText);
        buyExchangeLogo = findViewById(R.id.buyExchangeLogo);
        sellExchangeLogo = findViewById(R.id.sellExchangeLogo);
        
        // Financial Details
        buyFeeText = findViewById(R.id.buyFeeText);
        sellFeeText = findViewById(R.id.sellFeeText);
        netProfitText = findViewById(R.id.netProfitText);
        slippageText = findViewById(R.id.slippageText);
        totalCostText = findViewById(R.id.totalCostText);
        
        // Risk Assessment
        riskLevelText = findViewById(R.id.riskLevelText);
        liquidityText = findViewById(R.id.liquidityText);
        volatilityText = findViewById(R.id.volatilityText);
        executionTimeText = findViewById(R.id.executionTimeText);
        riskIndicator = findViewById(R.id.riskIndicator);
        riskProgressBar = findViewById(R.id.riskProgressBar);
        
        // Action Buttons
        refreshButton = findViewById(R.id.refreshButton);
        simulateButton = findViewById(R.id.simulateButton);
        loadingProgress = findViewById(R.id.loadingProgress);
    }
    
    /**
     * Display opportunity data in the UI
     */
    private void displayOpportunityData() {
        if (opportunity == null) {
            showError("Invalid opportunity data");
            return;
        }
        
        try {
            // Set loading state
            setLoadingState(true);
            
            // Basic Info - with null checks
            String symbol = opportunity.getNormalizedSymbol();
            symbolText.setText(symbol != null ? symbol : "Unknown");
            
            String buyExchange = opportunity.getBuyExchangeName();
            buyExchangeText.setText(buyExchange != null ? buyExchange : "Unknown");
            
            String sellExchange = opportunity.getSellExchangeName();
            sellExchangeText.setText(sellExchange != null ? sellExchange : "Unknown");
            
            // Set exchange logos - with null checks
            setExchangeLogo(buyExchangeLogo, buyExchange);
            setExchangeLogo(sellExchangeLogo, sellExchange);
            
            // Format and display prices - with safety checks
            double buyPrice = opportunity.getBuyPrice();
            double sellPrice = opportunity.getSellPrice();
            
            // Verify prices are valid
            if (buyPrice <= 0) buyPrice = 0.0001;
            if (sellPrice <= 0) sellPrice = 0.0001;
            
            String buyPriceFormatted = formatPrice(buyPrice);
            String sellPriceFormatted = formatPrice(sellPrice);
            buyPriceText.setText(buyPriceFormatted);
            sellPriceText.setText(sellPriceFormatted);
            
            // Profit percentage - with safety checks
            double profitPercent;
            try {
                profitPercent = opportunity.getProfitPercent();
                if (Double.isNaN(profitPercent) || Double.isInfinite(profitPercent)) {
                    profitPercent = 0.0;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting profit percent: " + e.getMessage());
                profitPercent = 0.0;
            }
            
            profitText.setText(String.format(Locale.US, "%.2f%%", profitPercent));
            
            // Set profit text color based on percentage
            if (profitPercent >= 1.0) {
                profitText.setTextColor(Color.parseColor("#00C087")); // Green
            } else if (profitPercent >= 0.5) {
                profitText.setTextColor(Color.parseColor("#FF9800")); // Orange
            } else {
                profitText.setTextColor(Color.parseColor("#FF3B30")); // Red
            }
            
            // Financial details - with safety checks
            double buyFee = 0.1;  // Default 0.1%
            double sellFee = 0.1; // Default 0.1%
            
            try {
                buyFee = opportunity.getBuyFeePercentage();
                if (buyFee <= 0 || buyFee > 100 || Double.isNaN(buyFee)) {
                    buyFee = 0.1; // Default to 0.1% if invalid
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting buy fee: " + e.getMessage());
            }
            
            try {
                sellFee = opportunity.getSellFeePercentage();
                if (sellFee <= 0 || sellFee > 100 || Double.isNaN(sellFee)) {
                    sellFee = 0.1; // Default to 0.1% if invalid
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting sell fee: " + e.getMessage());
            }
            
            buyFeeText.setText(String.format(Locale.US, "%.2f%%", buyFee));
            sellFeeText.setText(String.format(Locale.US, "%.2f%%", sellFee));
            
            // Net profit (profit - fees) - with safety checks
            double netProfit = profitPercent - buyFee - sellFee;
            if (netProfit < -100) netProfit = -100; // Limit extremely negative values
            netProfitText.setText(String.format(Locale.US, "%.2f%%", netProfit));
            netProfitText.setTextColor(profitText.getCurrentTextColor());
            
            // Ensure risk assessment exists
            RiskAssessment risk = null;
            try {
                risk = RiskAssessmentAdapter.getRiskAssessment(opportunity);
            } catch (Exception e) {
                Log.e(TAG, "Error getting risk assessment: " + e.getMessage());
            }
            
            // Estimated slippage (get from risk assessment or calculate)
            double slippage = 0.5; // Default 0.5%
            if (risk != null) {
                try {
                    slippage = risk.getSlippageRisk() * 100;
                    if (Double.isNaN(slippage) || slippage <= 0) {
                        slippage = 0.5;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting slippage: " + e.getMessage());
                }
            }
            slippageText.setText(String.format(Locale.US, "%.2f%%", slippage));
            
            // Total cost - example value based on standard position size
            double standardPositionSize = 1000.0; // $1000 USD position
            totalCostText.setText(currencyFormat.format(standardPositionSize));
            
            // Risk assessment
            displayRiskAssessment(risk);
            
            // Finish loading
            setLoadingState(false);
            
        } catch (Exception e) {
            Log.e(TAG, "Fatal error displaying opportunity data", e);
            Toast.makeText(this, "Error displaying opportunity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            setLoadingState(false);
        }
    }
    
    /**
     * Display risk assessment data
     * @param risk The risk assessment object
     * 
     * NOTE: In our risk system:
     * - Higher risk scores (0.8-1.0) = LOWER risk = BETTER/SAFER
     * - Lower risk scores (0.0-0.2) = HIGHER risk = WORSE/RISKIER
     * This is why higher progress bar values are green (good) and lower values are red (bad).
     */
    private void displayRiskAssessment(RiskAssessment risk) {
        try {
            if (risk == null) {
                Log.w(TAG, "Risk assessment is null, creating default");
                // Create a default risk assessment if none exists
                risk = new RiskAssessment();
                risk.setOverallRiskScore(0.5);
                risk.setLiquidityScore(0.5);
                risk.setVolatilityScore(0.5);
                risk.setSlippageRisk(0.01); // Default 1% slippage
                risk.setBuyFeePercentage(0.1); // Default 0.1% fee
                risk.setSellFeePercentage(0.1); // Default 0.1% fee
                
                // Register this default risk on the opportunity
                if (opportunity != null) {
                    try {
                        RiskAssessmentAdapter.setRiskAssessment(opportunity, risk);
                        Log.d(TAG, "Set default risk assessment on opportunity");
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting default risk assessment: " + e.getMessage());
                    }
                }
            }
            
            // Get risk level - with safety checks
            double riskScore = 0.5; // Default to medium risk
            try {
                riskScore = risk.getOverallRiskScore();
                if (Double.isNaN(riskScore) || riskScore < 0 || riskScore > 1) {
                    riskScore = 0.5; // Default to 0.5 if invalid
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting risk score: " + e.getMessage());
            }
            
            String riskLevel = "Medium Risk"; // Default risk level
            int riskColor = Color.YELLOW;     // Default risk color
            
            try {
                riskLevel = unifiedRiskCalculator.getRiskLevelText(riskScore);
                riskColor = unifiedRiskCalculator.getRiskColor(riskScore);
            } catch (Exception e) {
                Log.e(TAG, "Error getting risk level or color: " + e.getMessage());
            }
            
            Log.d(TAG, "Displaying risk assessment - Score: " + riskScore + 
                  ", Level: " + riskLevel + 
                  ", Liquidity: " + risk.getLiquidityScore() + 
                  ", Volatility: " + risk.getVolatilityScore());
            
            // Set risk level text and color
            riskLevelText.setText(riskLevel);
            riskLevelText.setTextColor(riskColor);
            
            // Set risk indicator color
            riskIndicator.setBackgroundColor(riskColor);
            
            // Set risk progress (0-100)
            int riskProgress = (int)(riskScore * 100);
            riskProgressBar.setProgress(riskProgress);
            
            // Set liquidity score - with safety checks
            double liquidityScore = 0.5; // Default to medium liquidity
            try {
                liquidityScore = risk.getLiquidityScore();
                if (Double.isNaN(liquidityScore) || liquidityScore < 0 || liquidityScore > 1) {
                    liquidityScore = 0.5; // Default if invalid
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting liquidity score: " + e.getMessage());
            }
            String liquidityLevel = getQualityLevel(liquidityScore);
            liquidityText.setText(liquidityLevel);
            
            // Set volatility score - with safety checks
            double volatilityScore = 0.5; // Default to medium volatility
            try {
                volatilityScore = risk.getVolatilityScore();
                if (Double.isNaN(volatilityScore) || volatilityScore < 0 || volatilityScore > 1) {
                    volatilityScore = 0.5; // Default if invalid
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting volatility score: " + e.getMessage());
            }
            // Display volatility level without inverting (use consistent scale)
            String volatilityLevel = getQualityLevel(volatilityScore);
            volatilityText.setText(volatilityLevel);
            
            // Estimated execution time (minutes) - with safety checks
            double executionTime = 3.0; // Default to 3 minutes
            try {
                executionTime = risk.getExecutionTimeEstimate();
                if (Double.isNaN(executionTime) || executionTime <= 0) {
                    executionTime = estimateExecutionTime(opportunity, risk);
                    if (Double.isNaN(executionTime) || executionTime <= 0) {
                        executionTime = 3.0; // Default if all else fails
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting execution time: " + e.getMessage());
            }
            
            executionTimeText.setText(formatExecutionTime(executionTime));
            Log.d(TAG, "Risk display complete - Execution time: " + formatExecutionTime(executionTime));
            
        } catch (Exception e) {
            Log.e(TAG, "Error displaying risk assessment", e);
            // Set default values for UI in case of error
            riskLevelText.setText("Medium Risk");
            liquidityText.setText("Average");
            volatilityText.setText("Average");
            executionTimeText.setText("3.0 min");
            riskIndicator.setBackgroundColor(Color.YELLOW);
            riskProgressBar.setProgress(50);
        }
    }
    
    /**
     * Format execution time nicely
     * @param minutes Execution time in minutes
     * @return Formatted time string
     */
    private String formatExecutionTime(double minutes) {
        // Safety check for invalid values
        if (Double.isNaN(minutes) || Double.isInfinite(minutes) || minutes <= 0) {
            return "3.0 min"; // Default value
        }
        
        try {
            if (minutes < 1.0) {
                // Show as seconds for very short times
                int seconds = (int)(minutes * 60.0);
                return seconds + " sec";
            } else if (minutes < 60.0) {
                // Show as minutes for medium times
                return String.format(Locale.US, "%.1f min", minutes);
            } else {
                // Show as hours for long times
                return String.format(Locale.US, "%.1f hrs", minutes / 60.0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting execution time: " + e.getMessage());
            return "3.0 min"; // Default in case of error
        }
    }
    
    /**
     * Estimate execution time based on opportunity and risk assessment
     * @param opportunity The arbitrage opportunity
     * @param risk The risk assessment
     * @return Estimated execution time in minutes
     */
    private int estimateExecutionTime(ArbitrageOpportunity opportunity, RiskAssessment risk) {
        if (opportunity == null) {
            return 5; // Default 5 minutes
        }
        
        // Base execution time of 3 minutes
        int baseTime = 3;
        
        // Add time based on exchange names
        String buyExchange = opportunity.getBuyExchangeName().toLowerCase();
        String sellExchange = opportunity.getSellExchangeName().toLowerCase();
        
        // Slower exchanges take longer
        int exchangeTimeFactor = 0;
        if (buyExchange.contains("kraken") || sellExchange.contains("kraken")) {
            exchangeTimeFactor += 3;
        }
        if (buyExchange.contains("coinbase") || sellExchange.contains("coinbase")) {
            exchangeTimeFactor += 2;
        }
        
        // Lower risk scores (higher risk) mean longer execution time
        // Use the risk score directly: 0.0 = highest risk (longest time), 1.0 = lowest risk (shortest time)
        int riskTimeFactor = (int)((1.0 - (risk != null ? risk.getOverallRiskScore() : 0.5)) * 10);
        
        // Calculate total time
        int totalTime = baseTime + exchangeTimeFactor + riskTimeFactor;
        
        // Ensure minimum of 1 minute
        return Math.max(1, totalTime);
    }
    
    /**
     * Refresh opportunity data
     */
    private void refreshData() {
        if (opportunity == null) return;
        
        // Set loading state
        setLoadingState(true);
        
        // Use executor service to perform refresh in background
        executorService.submit(() -> {
            try {
                Log.d(TAG, "Starting data refresh for " + opportunity.getSymbol());
                
                // Get the exchange services from the registry
                ExchangeRegistry registry = ExchangeRegistry.getInstance(new LoggingNotificationService());
                ExchangeService buyExchangeService = registry.getExchange(opportunity.getBuyExchangeName().toLowerCase());
                ExchangeService sellExchangeService = registry.getExchange(opportunity.getSellExchangeName().toLowerCase());
                
                if (buyExchangeService == null || sellExchangeService == null) {
                    runOnUiThread(() -> {
                        Snackbar.make(findViewById(android.R.id.content), 
                                "Could not find exchange services", Snackbar.LENGTH_SHORT).show();
                        setLoadingState(false);
                    });
                    return;
                }
                
                // Get tickers for both exchanges
                Ticker buyTicker = buyExchangeService.getTickerData(opportunity.getSymbolBuy());
                Ticker sellTicker = sellExchangeService.getTickerData(opportunity.getSymbolSell());
                
                // Update prices if tickers are available
                if (buyTicker != null && sellTicker != null) {
                    double newBuyPrice = buyTicker.getLastPrice();
                    double newSellPrice = sellTicker.getLastPrice();
                    
                    Log.d(TAG, String.format("Fetched prices - Buy: %.8f, Sell: %.8f", newBuyPrice, newSellPrice));
                    
                    // Calculate new profit percentage
                    double newProfitPercent = ((newSellPrice - newBuyPrice) / newBuyPrice) * 100;
                    
                    // Get proper fee information from exchange services
                    double buyFee = buyExchangeService.getFeePercentage(opportunity.getSymbolBuy(), false) * 100; // Convert to percentage
                    double sellFee = sellExchangeService.getFeePercentage(opportunity.getSymbolSell(), true) * 100; // Convert to percentage
                    
                    Log.d(TAG, String.format("Fetched fees - Buy: %.4f%%, Sell: %.4f%%", buyFee, sellFee));
                    
                    // Register correct fees in the opportunity
                    opportunity.setBuyFeePercentage(buyFee);
                    opportunity.setSellFeePercentage(sellFee);
                    
                    // Update the opportunity with new data
                    opportunity.setBuyTicker(buyTicker);
                    opportunity.setSellTicker(sellTicker);
                    opportunity.setTimestamp(new Date());
                    
                    // Ensure consistent risk values with the new data
                    opportunity = RiskEnsurer.ensureRiskValues(opportunity, true);
                    
                    // Update UI on main thread
                    runOnUiThread(() -> {
                        displayOpportunityData();
                        Snackbar.make(findViewById(android.R.id.content), 
                                "Data refreshed with accurate risk values", Snackbar.LENGTH_SHORT).show();
                    });
                } else {
                    Log.e(TAG, "Failed to get ticker data - Buy ticker: " + 
                          (buyTicker == null ? "null" : "ok") + ", Sell ticker: " + 
                          (sellTicker == null ? "null" : "ok"));
                    
                    runOnUiThread(() -> {
                        Snackbar.make(findViewById(android.R.id.content), 
                                "Could not get latest prices", Snackbar.LENGTH_SHORT).show();
                        setLoadingState(false);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing data", e);
                runOnUiThread(() -> {
                    Snackbar.make(findViewById(android.R.id.content), 
                            "Error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    setLoadingState(false);
                });
            }
        });
    }
    
    /**
     * Simulate a trade for demonstration purposes
     */
    private void simulateTrade() {
        // Show a dialog with trade simulation details
        // This is just a placeholder - in a real app this would execute the trade
        Toast.makeText(this, "Trade simulation not implemented in this demo", 
                Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Set up button click listeners
     */
    private void setupActionListeners() {
        refreshButton.setOnClickListener(v -> refreshData());
        simulateButton.setOnClickListener(v -> simulateTrade());
    }
    
    /**
     * Set loading state
     * @param isLoading Whether the view is in loading state
     */
    private void setLoadingState(boolean isLoading) {
        loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        refreshButton.setEnabled(!isLoading);
        simulateButton.setEnabled(!isLoading);
    }
    
    /**
     * Start auto-refresh
     */
    private void startAutoRefresh() {
        refreshHandler.postDelayed(new Runnable() {
    @Override
            public void run() {
                refreshData();
                refreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL);
            }
        }, AUTO_REFRESH_INTERVAL);
    }
    
    /**
     * Stop auto-refresh
     */
    private void stopAutoRefresh() {
        refreshHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Format price based on value
     * @param price The price to format
     * @return Formatted price string
     */
    private String formatPrice(double price) {
        // Check for invalid values
        if (Double.isNaN(price) || Double.isInfinite(price)) {
            return "0.00";
        }
        
        // Negative prices don't make sense in this context
        if (price < 0) {
            price = 0;
        }
        
        try {
            if (price < 0.01) {
                return String.format(Locale.US, "%.8f", price);
            } else if (price < 1.0) {
                return String.format(Locale.US, "%.6f", price);
            } else if (price < 1000.0) {
                return String.format(Locale.US, "%.4f", price);
            } else {
                return String.format(Locale.US, "%.2f", price);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting price: " + e.getMessage());
            return "0.00";
        }
    }
    
    /**
     * Set exchange logo based on exchange name
     * @param logoView ImageView to set logo in
     * @param exchangeName Name of exchange
     */
    private void setExchangeLogo(ImageView logoView, String exchangeName) {
        if (exchangeName == null || exchangeName.isEmpty()) {
            logoView.setImageResource(R.drawable.exchange_icon_placeholder);
            return;
        }
        
        // Convert to lowercase for case-insensitive matching
        String exchange = exchangeName.toLowerCase();
        
        // Set logo based on exchange name
        int logoResource;
        switch (exchange) {
            case "binance":
                logoResource = R.drawable.binance_logo;
                break;
            case "coinbase":
                logoResource = R.drawable.coinbase_logo;
                break;
            case "kraken":
                logoResource = R.drawable.kraken_logo;
                break;
            case "okx":
                logoResource = R.drawable.okx_logo;
                break;
            case "bybit":
                logoResource = R.drawable.bybit_logo;
                break;
            default:
                logoResource = R.drawable.exchange_icon_placeholder;
                break;
        }
        
        logoView.setImageResource(logoResource);
    }
    
    /**
     * Get quality level text (for liquidity, volatility)
     * @param score Quality score (0-1, higher is better/lower risk)
     * @return Quality level text
     */
    private String getQualityLevel(double score) {
        if (score >= 0.8) {
            return "Excellent (Low Risk)";
        } else if (score >= 0.6) {
            return "Good";
        } else if (score >= 0.4) {
            return "Average";
        } else if (score >= 0.2) {
            return "Poor";
        } else {
            return "Very Poor (High Risk)";
        }
    }
    
    /**
     * Estimate execution time based on exchange services
     * @param buyExchange Buy exchange service
     * @param sellExchange Sell exchange service
     * @return Estimated execution time in minutes
     */
    private double estimateExecutionTime(ExchangeService buyExchange, ExchangeService sellExchange) {
        // Base time of 2 minutes
        double baseTime = 2.0;
        
        // Add time based on exchange API response times
        double buyResponseTime = buyExchange.getEstimatedResponseTimeMs() / 1000.0 / 60.0; // Convert ms to minutes
        double sellResponseTime = sellExchange.getEstimatedResponseTimeMs() / 1000.0 / 60.0;
        
        // Calculate total time (consider parallelization)
        double totalResponseTime = Math.max(buyResponseTime, sellResponseTime);
        
        // Add time for potential retries and network delays
        double networkDelayFactor = 1.5;
        
        return baseTime + (totalResponseTime * networkDelayFactor);
    }
    
    /**
     * Show error message and finish activity
     * @param message Error message
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Simple implementation of INotificationService that logs messages
     */
    private class LoggingNotificationService implements INotificationService {
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
            Log.i(TAG, "Notification: " + title + " - " + message);
        }
        
        @Override
        public void notifyArbitrageOpportunity(ArbitrageResult opportunity) {
            Log.i(TAG, "Arbitrage opportunity found: " + opportunity);
        }
        
        @Override
        public void notifyArbitrageError(Throwable error) {
            Log.e(TAG, "Arbitrage error", error);
        }
    }
} 