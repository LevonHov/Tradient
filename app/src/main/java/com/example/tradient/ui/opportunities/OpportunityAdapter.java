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
import com.example.tradient.util.AssetLogoMap;
import com.example.tradient.util.RiskAssessmentAdapter;

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
    private Context context;
    
    // Configuration values from ArbitrageProcessMain
    private final double minProfitPercent;
    private final double availableCapital;
    private final double maxPositionPercent;
    private final double maxSlippagePercent;

    // Define color values here since they're missing from resources
    private static final int COLOR_PROFIT_GREEN = Color.parseColor("#00C087");
    private static final int COLOR_PROFIT_YELLOW = Color.parseColor("#FF9332");
    private static final int COLOR_PROFIT_RED = Color.parseColor("#FF3B30");
    
    // Risk color constants
    private static final int COLOR_RISK_MINIMAL = Color.parseColor("#00C853");      // Green
    private static final int COLOR_RISK_VERY_LOW = Color.parseColor("#64DD17");     // Light Green
    private static final int COLOR_RISK_LOW = Color.parseColor("#AEEA00");          // Lime
    private static final int COLOR_RISK_MODERATE = Color.parseColor("#FFEB3B");     // Yellow
    private static final int COLOR_RISK_BALANCED = Color.parseColor("#FFC107");     // Amber
    private static final int COLOR_RISK_MODERATE_HIGH = Color.parseColor("#FF9800"); // Orange
    private static final int COLOR_RISK_HIGH = Color.parseColor("#FF5722");         // Deep Orange
    private static final int COLOR_RISK_VERY_HIGH = Color.parseColor("#F44336");    // Red
    private static final int COLOR_RISK_EXTREME = Color.parseColor("#B71C1C");      // Dark Red
    private static final int COLOR_RISK_UNKNOWN = Color.parseColor("#9E9E9E");      // Grey

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
        context = parent.getContext();
        View view = LayoutInflater.from(context)
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
        
        // Get profit percentage directly from opportunity
        double profitPercentage = opportunity.getProfitPercent();
        
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
        
        // Display basic information
        Log.d("OpportunityAdapter", "Arbitrage Opportunity - Symbol: " + opportunity.getNormalizedSymbol());
        Log.d("OpportunityAdapter", "Buy Price: " + opportunity.getBuyPrice() + ", Sell Price: " + opportunity.getSellPrice());
        Log.d("OpportunityAdapter", "Buy Fee: " + (opportunity.getBuyFeePercentage() * 100) + "%, Sell Fee: " + (opportunity.getSellFeePercentage() * 100) + "%");
        Log.d("OpportunityAdapter", "Profit: " + profitPercentage + "%");
        
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
        holder.buyFeeText.setText(String.format("%.2f%%", opportunity.getBuyFeePercentage() * 100));
        holder.sellFeeText.setText(String.format("%.2f%%", opportunity.getSellFeePercentage() * 100));
        
        // Get risk assessment from opportunity
        RiskAssessment riskAssessment = RiskAssessmentAdapter.getRiskAssessment(opportunity);
        
        // Display risk information
        if (riskAssessment != null) {
            // Get risk score from assessment
            double riskScore = riskAssessment.getRiskScore();
            
            // Set risk progress
            int progress = (int) (riskScore * 100);
            holder.riskProgress.setProgress(progress);
            
            // Get risk level text
            String riskLevelText = getRiskLevelText(riskScore);
            holder.riskText.setText(riskLevelText);
            
            // Set risk color based on risk score
            int riskColor = getRiskColor(riskScore);
            holder.riskText.setTextColor(riskColor);
            
            // Set progress color
            holder.riskProgress.setProgressTintList(ColorStateList.valueOf(riskColor));
            
            // Set slippage text if available
            double slippage = riskAssessment.getSlippageEstimate();
            if (slippage > 0) {
                holder.slippageText.setText(String.format("Slip: %.2f%%", slippage * 100));
            } else {
                holder.slippageText.setText("Slip: --");
            }
            
            // Set estimated execution time if available
            double timeEstimateMinutes = riskAssessment.getExecutionTimeEstimate();
            if (timeEstimateMinutes > 0) {
                if (timeEstimateMinutes < 1) {
                    holder.timeText.setText(String.format("Time: %.0fs", timeEstimateMinutes * 60));
                } else {
                    holder.timeText.setText(String.format("Time: %.1fm", timeEstimateMinutes));
                }
            } else {
                holder.timeText.setText("Time: --");
            }
            
            // Set ROI efficiency if available
            double roiEfficiency = riskAssessment.getRoiEfficiency();
            if (roiEfficiency > 0) {
                holder.roiEfficiencyText.setText(String.format("ROI/h: %.1f", roiEfficiency));
            } else {
                holder.roiEfficiencyText.setText("ROI/h: --");
            }
        } else {
            // No risk assessment available, show default values
            holder.riskProgress.setProgress(0);
            holder.riskText.setText("UNKNOWN");
            holder.riskText.setTextColor(COLOR_RISK_UNKNOWN);
            holder.slippageText.setText("Slip: --");
            holder.timeText.setText("Time: --");
            holder.roiEfficiencyText.setText("ROI/h: --");
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
     * Calculate a comprehensive risk score from 0 (highest risk) to 100 (lowest risk)
     * Using the advanced RiskCalculator with all factors considered
     */
    private int calculateRiskScore(ArbitrageOpportunity opportunity) {
        try {
            if (opportunity == null) {
                return 0; // Return highest risk if opportunity is null
            }
            
            // Get the risk assessment using the adapter
            RiskAssessment assessment = RiskAssessmentAdapter.getRiskAssessment(opportunity);
            if (assessment == null) {
                // Calculate risk assessment using RiskCalculator
                RiskCalculator riskCalculator = new RiskCalculator();
                assessment = riskCalculator.assessRisk(opportunity);
                if (assessment == null) {
                    return 0; // Return highest risk if assessment fails
                }
                // Store the assessment in the opportunity using the adapter
                RiskAssessmentAdapter.setRiskAssessment(opportunity, assessment);
            }
            
            // Get the risk score from the assessment (0-1 scale)
            double riskScore = assessment.getOverallRiskScore();
            
            // Convert to percentage (0-100) for display
            // Higher risk score means lower risk, so we keep it as is
            int displayScore = (int) (riskScore * 100);
            
            // Log the risk score for debugging
            Log.d("OpportunityAdapter", String.format("Risk score for %s: %.2f (display: %d)",
                opportunity.getNormalizedSymbol(), riskScore, displayScore));
            
            return displayScore;
        } catch (Exception e) {
            Log.e("OpportunityAdapter", "Error calculating risk score: " + e.getMessage());
            return 0; // Return highest risk on error
        }
    }

    private void bindRiskLevel(TextView riskLevelView, ArbitrageOpportunity opportunity) {
        RiskAssessment riskAssessment = RiskAssessmentAdapter.getRiskAssessment(opportunity);
        if (riskAssessment != null) {
            double riskScore = riskAssessment.getOverallRiskScore();
            String riskLevel = getRiskLevelText(riskScore);
            int riskColor = getRiskColor(riskScore);
            
            riskLevelView.setText(riskLevel);
            riskLevelView.setTextColor(riskColor);
            
            // Log the risk score for debugging
            Log.d(TAG, "Risk Score for " + opportunity.getNormalizedSymbol() + 
                  ": " + riskScore + " (" + riskLevel + ")");
        } else {
            riskLevelView.setText("Unknown Risk");
            riskLevelView.setTextColor(COLOR_RISK_UNKNOWN);
        }
    }

    private String getRiskLevelText(double riskScore) {
        if (riskScore >= 0.8) return "Minimal Risk";
        if (riskScore >= 0.7) return "Very Low Risk";
        if (riskScore >= 0.6) return "Low Risk";
        if (riskScore >= 0.5) return "Moderate Risk";
        if (riskScore >= 0.4) return "Balanced Risk";
        if (riskScore >= 0.3) return "Moderate High Risk";
        if (riskScore >= 0.2) return "High Risk";
        if (riskScore >= 0.1) return "Very High Risk";
        return "Extreme Risk";
    }

    private int getRiskColor(double riskScore) {
        if (riskScore >= 0.8) return COLOR_RISK_MINIMAL;
        if (riskScore >= 0.7) return COLOR_RISK_VERY_LOW;
        if (riskScore >= 0.6) return COLOR_RISK_LOW;
        if (riskScore >= 0.5) return COLOR_RISK_MODERATE;
        if (riskScore >= 0.4) return COLOR_RISK_BALANCED;
        if (riskScore >= 0.3) return COLOR_RISK_MODERATE_HIGH;
        if (riskScore >= 0.2) return COLOR_RISK_HIGH;
        if (riskScore >= 0.1) return COLOR_RISK_VERY_HIGH;
        return COLOR_RISK_EXTREME;
    }
    
    private double calculateExecutionRisk(String buyExchange, String sellExchange) {
        // Assign execution risk based on exchange reliability
        if (buyExchange.equals("binance") && sellExchange.equals("binance")) return 0.2;
        if (buyExchange.equals("coinbase") && sellExchange.equals("coinbase")) return 0.3;
        if (buyExchange.equals("kraken") && sellExchange.equals("kraken")) return 0.4;
        if (buyExchange.equals("bybit") && sellExchange.equals("bybit")) return 0.5;
        return 0.7; // Higher risk for other exchanges
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
        // Use the AssetLogoMap utility to get the appropriate logo resource
        imageView.setImageResource(AssetLogoMap.getExchangeLogo(exchangeName));
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
            
            // Store the risk assessment in the opportunity for later use using the adapter
            RiskAssessmentAdapter.setRiskAssessment(opportunity, assessment);
            
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