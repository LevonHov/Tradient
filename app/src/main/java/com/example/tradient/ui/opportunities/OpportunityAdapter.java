package com.example.tradient.ui.opportunities;

import android.graphics.Color;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;
import android.util.Pair;
import android.content.Intent;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.example.tradient.R;
import com.example.tradient.config.ConfigurationFactory;
import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.domain.profit.ProfitCalculator;
import com.example.tradient.domain.profit.ProfitResult;
import com.example.tradient.domain.risk.RiskCalculator;
import com.example.tradient.domain.risk.SlippageManagerService;
import com.example.tradient.domain.risk.SlippageAnalyticsBuilder;
import com.example.tradient.util.TimeEstimationUtil;
import com.example.tradient.domain.risk.AssetRiskCalculator;
import com.example.tradient.data.model.OrderBook;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.android.material.card.MaterialCardView;

public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.OpportunityViewHolder> {

    private static final String TAG = "OpportunityAdapter";
    private List<ArbitrageOpportunity> opportunities;
    private final NumberFormat currencyFormatter;
    private final NumberFormat percentFormatter;
    private final SlippageManagerService slippageManager;
    private final RiskCalculator riskCalculator;
    private final AssetRiskCalculator assetRiskCalculator;
    
    // Configuration values from ArbitrageProcessMain
    private final double minProfitPercent;
    private final double availableCapital;
    private final double maxPositionPercent;
    private final double maxSlippagePercent;

    // Define color values here since they're missing from resources
    private static final int COLOR_PROFIT_GREEN = Color.parseColor("#00C087");
    private static final int COLOR_PROFIT_YELLOW = Color.parseColor("#FF9332");
    private static final int COLOR_PROFIT_RED = Color.parseColor("#FF3B30");

    public OpportunityAdapter(List<ArbitrageOpportunity> opportunities) {
        this.opportunities = opportunities;
        
        // Initialize formatters
        currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        currencyFormatter.setMinimumFractionDigits(2);
        currencyFormatter.setMaximumFractionDigits(5);
        percentFormatter = NumberFormat.getPercentInstance(Locale.US);
        percentFormatter.setMinimumFractionDigits(2);
        percentFormatter.setMaximumFractionDigits(2);
        
        // Initialize risk and slippage calculators
        slippageManager = new SlippageManagerService();
        riskCalculator = new RiskCalculator();
        assetRiskCalculator = new AssetRiskCalculator();
        
        // Load configuration values
        minProfitPercent = ConfigurationFactory.getArbitrageConfig().getMinProfitPercent();
        availableCapital = ConfigurationFactory.getArbitrageConfig().getAvailableCapital();
        maxPositionPercent = ConfigurationFactory.getArbitrageConfig().getMaxPositionPercent();
        maxSlippagePercent = ConfigurationFactory.getRiskConfig().getMaxSlippagePercent();
    }

    @NonNull
    @Override
    public OpportunityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opportunity, parent, false);
        return new OpportunityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OpportunityViewHolder holder, int position) {
        ArbitrageOpportunity opportunity = opportunities.get(position);
        
        // Set item click listener to open detail screen
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, OpportunityDetailActivity.class);
            intent.putExtra("opportunity", opportunity);
            context.startActivity(intent);
        });
        
        // Set symbol and profit
        holder.symbolText.setText(opportunity.getNormalizedSymbol());
        
        // Get fee percentages (they are stored in decimal format, e.g., 0.001 for 0.1%)
        double buyFeePercent = opportunity.getBuyFeePercentage();
        double sellFeePercent = opportunity.getSellFeePercentage();
        
        // Get buy and sell prices from opportunity
        double buyPrice = opportunity.getBuyPrice();
        double sellPrice = opportunity.getSellPrice();
        
        // Calculate profit using our new profit calculator
        // Assume 1.0 as amount for percentage calculation
        ProfitResult profitResult = ProfitCalculator.calculateArbitrageProfit(
                buyPrice, 
                sellPrice, 
                buyFeePercent, // Fees are already in decimal format (e.g., 0.001 for 0.1%)
                sellFeePercent, // Fees are already in decimal format (e.g., 0.001 for 0.1%)
                1.0);
        
        // Get the profit percentage
        double profitPercentage = profitResult.getPercentageProfit();
        
        // Format and display profit with angle bracket indicator
        String profitDisplay;
        int profitColor;
        int profitBadgeBackground;
        
        if (profitPercentage > 1.0) {
            profitDisplay = String.format("▲ %.2f%%", profitPercentage);
            profitColor = COLOR_PROFIT_GREEN;
            profitBadgeBackground = R.drawable.profit_badge_background;
        } else if (profitPercentage > 0.1) {
            profitDisplay = String.format("△ %.2f%%", profitPercentage);
            profitColor = COLOR_PROFIT_GREEN;
            profitBadgeBackground = R.drawable.profit_badge_background;
        } else if (profitPercentage > 0) {
            profitDisplay = String.format("△ %.2f%%", profitPercentage);
            profitColor = COLOR_PROFIT_YELLOW;
            profitBadgeBackground = R.drawable.neutral_badge_background;
        } else {
            profitDisplay = String.format("▽ %.2f%%", profitPercentage);
            profitColor = COLOR_PROFIT_RED;
            profitBadgeBackground = R.drawable.loss_badge_background;
        }
        
        // Set profit text and color
        holder.profitText.setText(profitDisplay);
        holder.profitText.setTextColor(profitColor);
        
        // Set the profit badge background
        MaterialCardView profitBadge = holder.itemView.findViewById(R.id.profit_badge);
        profitBadge.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), profitBadgeBackground));
        
        // Store the calculated profit for potential later use
        opportunity.setNetProfitPercentage(profitPercentage);
        
        // Display basic information
        Log.d("OpportunityAdapter", "Arbitrage Opportunity - Symbol: " + opportunity.getNormalizedSymbol());
        Log.d("OpportunityAdapter", "Buy Price: " + buyPrice + ", Sell Price: " + sellPrice);
        Log.d("OpportunityAdapter", "Buy Fee: " + (buyFeePercent * 100) + "%, Sell Fee: " + (sellFeePercent * 100) + "%");
        Log.d("OpportunityAdapter", "Calculated Profit: " + profitPercentage + "%");
        
        // Set exchange info
        holder.buyExchangeName.setText(opportunity.getExchangeBuy());
        holder.sellExchangeName.setText(opportunity.getExchangeSell());
        
        // Set exchange logos
        setExchangeLogo(holder.buyExchangeLogo, opportunity.getExchangeBuy());
        setExchangeLogo(holder.sellExchangeLogo, opportunity.getExchangeSell());
        
        // Set prices
        holder.buyPrice.setText(currencyFormatter.format(opportunity.getBuyPrice()));
        holder.sellPrice.setText(currencyFormatter.format(opportunity.getSellPrice()));
        
        // Display fees (convert from decimal to percentage for display)
        holder.buyFeeText.setText(String.format("%.2f%%", buyFeePercent * 100));
        holder.sellFeeText.setText(String.format("%.2f%%", sellFeePercent * 100));
        
        // Calculate optimal position size based on ArbitrageProcessMain algorithm
        double optimalPosition = calculateOptimalPositionSize(opportunity, availableCapital, maxPositionPercent);
        
        // Calculate slippage using the advanced SlippageAnalyticsBuilder
        double tradeSize = optimalPosition / opportunity.getBuyPrice();
        double totalSlippage = calculateAdvancedSlippage(opportunity, tradeSize);
        
        // Calculate asset-specific risk instead of opportunity risk
        String assetSymbol = opportunity.getNormalizedSymbol();
        calculateAssetRisk(opportunity);
        int riskScore = assetRiskCalculator.getAssetRiskPercentage(assetSymbol);
        
        // Set risk display with actual percentage and better visualization
        int progressColor;
        String riskText;
        
        // Use 10 color gradations from green to red based on risk score
        if (riskScore < 10) {
            riskText = "Minimal";
            progressColor = Color.parseColor("#00E676"); // Bright green
        } else if (riskScore < 20) {
            riskText = "Minor";
            progressColor = Color.parseColor("#00C853"); // Green
        } else if (riskScore < 30) {
            riskText = "Low";
            progressColor = Color.parseColor("#64DD17"); // Light green
        } else if (riskScore < 40) {
            riskText = "Moderate";
            progressColor = Color.parseColor("#9CCC65"); // Pale green
        } else if (riskScore < 50) {
            riskText = "Balanced";
            progressColor = Color.parseColor("#CDDC39"); // Lime green
        } else if (riskScore < 60) {
            riskText = "Medium";
            progressColor = Color.parseColor("#FFEB3B"); // Yellow
        } else if (riskScore < 70) {
            riskText = "Elevated";
            progressColor = Color.parseColor("#FFC107"); // Amber
        } else if (riskScore < 80) {
            riskText = "Significant";
            progressColor = Color.parseColor("#FF9800"); // Orange
        } else if (riskScore < 90) {
            riskText = "High";
            progressColor = Color.parseColor("#FF5722"); // Deep orange
        } else {
            riskText = "Extreme";
            progressColor = Color.parseColor("#F44336"); // Red
        }
        
        holder.riskText.setText(riskText);
        holder.riskText.setTextColor(progressColor);
        
        // Set risk indicator color
        View riskIndicator = holder.itemView.findViewById(R.id.risk_indicator);
        if (riskIndicator != null) {
            riskIndicator.setBackgroundTintList(ColorStateList.valueOf(progressColor));
        }
        
        // Set progress to show the actual risk percentage with animation
        if (holder.riskProgress.getProgress() != riskScore) {
            holder.riskProgress.setProgress(0);
            holder.riskProgress.setProgressTintList(ColorStateList.valueOf(progressColor));
            holder.riskProgress.setProgress(riskScore, true); // Animate the progress change
        } else {
            holder.riskProgress.setProgressTintList(ColorStateList.valueOf(progressColor));
        }
        
        // Set slippage text using the advanced calculation
        holder.slippageText.setText(String.format("%.2f%%", totalSlippage * 100));
        
        // Calculate estimated execution time using real market data
        Ticker buyTicker = opportunity.getBuyTicker();
        Ticker sellTicker = opportunity.getSellTicker();
        OrderBook buyOrderBook = null; // opportunity.getBuyOrderBook() doesn't exist
        OrderBook sellOrderBook = null; // opportunity.getSellOrderBook() doesn't exist
        String baseAsset = opportunity.getNormalizedSymbol().split("/")[0];
        double tradeAmount = calculateOptimalTradeSize(opportunity);
        
        // Use a volatility estimate based on ticker data
        TimeEstimationUtil.MarketVolatility volatility = estimateVolatilityFromTickers(buyTicker, sellTicker);
        
        // Get a dynamic, data-driven time estimate
        Pair<Double, Double> timeEstimate = TimeEstimationUtil.estimateArbitrageTimeMinutes(
                opportunity.getExchangeBuy(), 
                opportunity.getExchangeSell(),
                buyTicker,
                sellTicker,
                buyOrderBook,
                sellOrderBook,
                tradeAmount,
                volatility,
                opportunity.getNormalizedSymbol());
                
        double estimatedTimeMinutes = timeEstimate.first;
        double timeUncertainty = timeEstimate.second;
        
        // Calculate ROI efficiency (profit per hour)
        double roiEfficiency = TimeEstimationUtil.calculateROIEfficiency(
                profitPercentage, estimatedTimeMinutes);
        
        // Format time display with confidence interval
        String timeDisplay = TimeEstimationUtil.formatTimeString(estimatedTimeMinutes);
        
        // Update UI with time information
        holder.timeText.setText(timeDisplay);
        
        // Get dynamic time thresholds based on current market conditions
        Double[] timeThresholds = TimeEstimationUtil.getTimeThresholds(assetSymbol, buyTicker);
        double goodThreshold = timeThresholds[0];
        double mediumThreshold = timeThresholds[1];
        
        // Set color based on dynamic time thresholds
        if (estimatedTimeMinutes < goodThreshold) {
            holder.timeText.setTextColor(COLOR_PROFIT_GREEN);
        } else if (estimatedTimeMinutes < mediumThreshold) {
            holder.timeText.setTextColor(COLOR_PROFIT_YELLOW);
        } else {
            holder.timeText.setTextColor(COLOR_PROFIT_RED);
        }
        
        // Display ROI efficiency
        String roiDisplay = String.format("%.2f%%/h", roiEfficiency);
        holder.roiEfficiencyText.setText(roiDisplay);
        
        // Set color based on ROI efficiency (higher is better)
        if (roiEfficiency > 5.0) {
            holder.roiEfficiencyText.setTextColor(COLOR_PROFIT_GREEN);
        } else if (roiEfficiency > 1.0) {
            holder.roiEfficiencyText.setTextColor(COLOR_PROFIT_YELLOW);
        } else {
            holder.roiEfficiencyText.setTextColor(COLOR_PROFIT_RED);
        }
    }

    @Override
    public int getItemCount() {
        return opportunities.size();
    }

    public void updateOpportunities(List<ArbitrageOpportunity> newOpportunities) {
        this.opportunities = newOpportunities;
        notifyDataSetChanged();
    }
    
    /**
     * Calculate a comprehensive risk score from 0 (lowest risk) to 100 (highest risk)
     * Using the advanced RiskCalculator with all factors considered
     */
    private int calculateRiskScore(ArbitrageOpportunity opportunity) {
        try {
            // If opportunity has no risk assessment, calculate it
            if (opportunity == null) {
                return 50; // Default to moderate risk if missing opportunity
            }
            
            RiskAssessment assessment;
            if (opportunity.getRiskAssessment() == null) {
                // Calculate risk directly using the RiskCalculator
                Ticker buyTicker = opportunity.getBuyTicker();
                Ticker sellTicker = opportunity.getSellTicker();
                double buyFees = opportunity.getBuyFeePercentage();
                double sellFees = opportunity.getSellFeePercentage();
                
                if (buyTicker == null || sellTicker == null) {
                    return 50; // Default to moderate risk if missing tickers
                }
                
                assessment = riskCalculator.calculateRisk(buyTicker, sellTicker, buyFees, sellFees);
                opportunity.setRiskAssessment(assessment); // Cache for future use
            } else {
                assessment = opportunity.getRiskAssessment();
            }
            
            // Extract risk factors directly from the assessment
            double overallRisk = assessment.getOverallRiskScore();
            double slippageRisk = assessment.getSlippageRisk();
            double liquidityScore = assessment.getLiquidityScore();
            double volatilityScore = assessment.getVolatilityScore();
            double depthScore = assessment.getDepthScore();
            double executionRisk = assessment.getExecutionRisk();
            
            // Calculate a weighted composite risk (lower scores for liquidity/depth/volatility indicate higher risk)
            double compositeRisk = riskCalculator.calculateOverallRisk(assessment);
            
            // Convert to percentage (0-100)
            int riskScore = (int)(compositeRisk * 100);
            
            // Log risk factors for debugging
            Log.d(TAG, String.format("Risk factors: overall=%.2f, slippage=%.2f, liquidity=%.2f, volatility=%.2f, " +
                    "depth=%.2f, execution=%.2f, composite=%.2f", 
                    overallRisk, slippageRisk, liquidityScore, volatilityScore, depthScore, executionRisk, compositeRisk));
            
            // Ensure score is within valid range
            return Math.max(0, Math.min(100, riskScore));
        } catch (Exception e) {
            Log.e(TAG, "Error calculating risk score: " + e.getMessage());
            // If risk calculation fails, provide a moderate risk value
            return 50;
        }
    }
    
    /**
     * Calculate the optimal position size for an arbitrage opportunity.
     * This method has been removed and will be reimplemented.
     * 
     * @param opportunity The arbitrage opportunity
     * @param availableCapital Total capital available
     * @param maxPositionPct Maximum position size as percentage of capital
     * @return Placeholder value, always returns 0.0
     */
    private double calculateOptimalPositionSize(ArbitrageOpportunity opportunity, 
                                              double availableCapital, 
                                              double maxPositionPct) {
        // *** PROFIT CALCULATION REMOVED - TO BE REIMPLEMENTED ***
            return 0.0;
    }
    
    /**
     * Calculates advanced slippage using the SlippageAnalyticsBuilder
     * 
     * @param opportunity The arbitrage opportunity
     * @param tradeSize The size of the trade
     * @return Total slippage as a decimal (e.g. 0.005 for 0.5%)
     */
    private double calculateAdvancedSlippage(ArbitrageOpportunity opportunity, double tradeSize) {
        try {
            String symbol = opportunity.getNormalizedSymbol();
            
            // Create a SlippageAnalyticsBuilder for advanced calculations
            SlippageAnalyticsBuilder slippageAnalytics = new SlippageAnalyticsBuilder();
            
            // Calculate buy-side slippage
            double buySlippage = slippageAnalytics.calculateSlippage(
                opportunity.getBuyTicker(),
                tradeSize,
                true, // isBuy
                symbol
            );
            
            // Calculate sell-side slippage
            double sellSlippage = slippageAnalytics.calculateSlippage(
                opportunity.getSellTicker(),
                tradeSize,
                false, // isBuy (false for sell)
                symbol
            );
            
            // Log slippage details for debugging
            Log.d(TAG, String.format("Slippage for %s: buy=%.4f%%, sell=%.4f%%, total=%.4f%%", 
                    symbol, buySlippage*100, sellSlippage*100, (buySlippage+sellSlippage)*100));
            
            return buySlippage + sellSlippage;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating advanced slippage: " + e.getMessage(), e);
            // Fallback to basic calculation
            return calculateBasicSlippage(opportunity.getBuyTicker(), true, tradeSize) + 
                   calculateBasicSlippage(opportunity.getSellTicker(), false, tradeSize);
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
    
    /**
     * Calculate liquidity factor based on opportunity details
     */
    private double calculateLiquidityFactor(ArbitrageOpportunity opportunity) {
        // Base factor on volume available for the opportunity
        double buyDepth = opportunity.getBuyExchangeLiquidity();
        double sellDepth = opportunity.getSellExchangeLiquidity();
        
        // Combine with trading pair liquidity factors
        String baseAsset = opportunity.getNormalizedSymbol().split("/")[0];
        double assetFactor = getAssetLiquidityFactor(baseAsset);
        
        // Calculate and normalize factor (0.1-1.0)
        double rawFactor = (buyDepth + sellDepth) / 2.0 * assetFactor;
        return Math.max(0.1, Math.min(1.0, rawFactor));
    }

    /**
     * Get asset-specific liquidity factor
     */
    private double getAssetLiquidityFactor(String asset) {
        String normalized = asset.toUpperCase();
        
        switch (normalized) {
            case "BTC": return 1.0;
            case "ETH": return 0.95;
            case "SOL": return 0.9;
            case "BNB": return 0.9;
            case "XRP": return 0.85;
            case "ADA": return 0.8;
            case "USDT": return 1.0;
            case "USDC": return 0.98;
            default: return 0.75;
        }
    }

    /**
     * Estimate current market volatility (this would be updated periodically)
     */
    private TimeEstimationUtil.MarketVolatility estimateCurrentVolatility() {
        // In a production app, this would use real-time market data
        // For now, use a default medium volatility
        return TimeEstimationUtil.MarketVolatility.MEDIUM;
    }
    
    /**
     * Set the appropriate exchange logo based on exchange name
     */
    private void setExchangeLogo(ImageView imageView, String exchangeName) {
        // Use the actual exchange logos from drawable resources
        switch (exchangeName.toLowerCase()) {
            case "binance":
                imageView.setImageResource(R.drawable.binance_logo);
                break;
            case "coinbase":
                imageView.setImageResource(R.drawable.coinbase_logo);
                break;
            case "kraken":
                imageView.setImageResource(R.drawable.kraken_logo);
                break;
            case "bybit":
                imageView.setImageResource(R.drawable.bybit_logo);
                break;
            case "okx":
                imageView.setImageResource(R.drawable.okx_logo);
                break;
            default:
                // Fallback for unknown exchanges
                imageView.setImageResource(R.drawable.ic_opportunities);
                break;
        }
    }

    /**
     * Calculate asset-specific risk using all available exchange data
     * 
     * @param opportunity The arbitrage opportunity containing the asset
     */
    private void calculateAssetRisk(ArbitrageOpportunity opportunity) {
        try {
            String symbol = opportunity.getNormalizedSymbol();
            Ticker buyTicker = opportunity.getBuyTicker();
            Ticker sellTicker = opportunity.getSellTicker();
            double buyFee = opportunity.getBuyFeePercentage();
            double sellFee = opportunity.getSellFeePercentage();
            
            // Only proceed if we have valid ticker data
            if (buyTicker == null || sellTicker == null) {
                Log.w(TAG, "Missing ticker data for " + symbol + ", skipping risk calculation");
                return;
            }
            
            // Calculate risk using both tickers for more accuracy
            RiskAssessment assessment = assetRiskCalculator.calculateAssetRisk(
                    symbol, buyTicker, sellTicker, buyFee, sellFee);
            
            // Store the risk assessment in the opportunity for later use
            opportunity.setRiskAssessment(assessment);
            
            // Log the risk factors for debugging
            Map<String, Integer> factors = assetRiskCalculator.getDetailedRiskFactors(symbol);
            Log.d(TAG, String.format("Asset risk for %s: overall=%d%%, liquidity=%d%%, volatility=%d%%, depth=%d%%, slippage=%d%%",
                    symbol, 
                    assetRiskCalculator.getAssetRiskPercentage(symbol),
                    factors.get("Liquidity"),
                    factors.get("Volatility"),
                    factors.get("Market Depth"),
                    factors.get("Slippage")));
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating asset risk: " + e.getMessage());
        }
    }

    /**
     * Estimate volatility level from ticker data
     */
    private TimeEstimationUtil.MarketVolatility estimateVolatilityFromTickers(Ticker buyTicker, Ticker sellTicker) {
        if (buyTicker == null || sellTicker == null) {
            return TimeEstimationUtil.MarketVolatility.MEDIUM; // Default
        }
        
        // Calculate price ranges as percentages
        double buyRange = 0;
        double sellRange = 0;
        
        if (buyTicker.getLastPrice() > 0 && buyTicker.getHighPrice() > 0 && buyTicker.getLowPrice() > 0) {
            buyRange = (buyTicker.getHighPrice() - buyTicker.getLowPrice()) / buyTicker.getLastPrice();
        }
        
        if (sellTicker.getLastPrice() > 0 && sellTicker.getHighPrice() > 0 && sellTicker.getLowPrice() > 0) {
            sellRange = (sellTicker.getHighPrice() - sellTicker.getLowPrice()) / sellTicker.getLastPrice();
        }
        
        // Use the average volatility from both exchanges
        double avgVolatility = (buyRange + sellRange) / 2.0;
        
        // Map to volatility levels
        if (avgVolatility < 0.01) {
            return TimeEstimationUtil.MarketVolatility.VERY_LOW;
        } else if (avgVolatility < 0.02) {
            return TimeEstimationUtil.MarketVolatility.LOW;
        } else if (avgVolatility < 0.04) {
            return TimeEstimationUtil.MarketVolatility.MEDIUM;
        } else if (avgVolatility < 0.07) {
            return TimeEstimationUtil.MarketVolatility.HIGH;
        } else {
            return TimeEstimationUtil.MarketVolatility.VERY_HIGH;
        }
    }
    
    /**
     * Calculate optimal trade size based on order book depth and available capital
     */
    private double calculateOptimalTradeSize(ArbitrageOpportunity opportunity) {
        // Start with a reasonable default based on available capital
        double defaultSize = availableCapital * 0.1; // 10% of available capital
        
        // Fall back to using ticker data since OrderBook isn't available
        Ticker buyTicker = opportunity.getBuyTicker();
        if (buyTicker != null && buyTicker.getVolume() > 0) {
            // Use a small percentage of 24h volume as alternative to order book depth
            double volumeBased = buyTicker.getVolume() * 0.001; // 0.1% of 24h volume
            return Math.min(defaultSize, volumeBased);
        }
        
        return defaultSize;
    }

    static class OpportunityViewHolder extends RecyclerView.ViewHolder {
        TextView symbolText;
        TextView profitText;
        ImageView buyExchangeLogo;
        TextView buyExchangeName;
        ImageView sellExchangeLogo;
        TextView sellExchangeName;
        TextView buyPrice;
        TextView sellPrice;
        TextView buyFeeText;
        TextView sellFeeText;
        ProgressBar riskProgress;
        TextView riskText;
        TextView slippageText;
        TextView timeText;
        TextView roiEfficiencyText;

        OpportunityViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.symbol_text);
            profitText = itemView.findViewById(R.id.profit_text);
            buyExchangeLogo = itemView.findViewById(R.id.buy_exchange_logo);
            buyExchangeName = itemView.findViewById(R.id.buy_exchange_name);
            sellExchangeLogo = itemView.findViewById(R.id.sell_exchange_logo);
            sellExchangeName = itemView.findViewById(R.id.sell_exchange_name);
            buyPrice = itemView.findViewById(R.id.buy_price);
            sellPrice = itemView.findViewById(R.id.sell_price);
            buyFeeText = itemView.findViewById(R.id.buy_fee_text);
            sellFeeText = itemView.findViewById(R.id.sell_fee_text);
            riskProgress = itemView.findViewById(R.id.risk_progress);
            riskText = itemView.findViewById(R.id.risk_text);
            slippageText = itemView.findViewById(R.id.slippage_text);
            timeText = itemView.findViewById(R.id.time_text);
            roiEfficiencyText = itemView.findViewById(R.id.roi_efficiency_text);
        }
    }
} 