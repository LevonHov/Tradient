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
import com.example.tradient.data.model.ArbitrageCardModel;
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
import com.example.tradient.ui.opportunities.RiskUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.android.material.card.MaterialCardView;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;

public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.ViewHolder> {

    private static final String TAG = "OpportunityAdapter";
    private final List<ArbitrageCardModel> opportunities = new ArrayList<>();
    private final List<ArbitrageOpportunity> originalOpportunities = new ArrayList<>();
    private final Context context;
    private static final DecimalFormat df = new DecimalFormat("0.00");
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

    public OpportunityAdapter(Context context) {
        this.context = context;
        
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opportunity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArbitrageCardModel opportunity = opportunities.get(position);
        
        // Set the trading pair
        holder.symbolText.setText(opportunity.getDisplaySymbol());
        
        // Set the profit percentage
        double profitPercent = opportunity.getProfitPercent();
        holder.profitText.setText("△ " + String.format(Locale.US, "%.2f%%", profitPercent));
        
        // Set profit text color based on profit percentage
        if (profitPercent >= 0.01) {
            holder.profitText.setTextColor(COLOR_PROFIT_GREEN);
        } else if (profitPercent >= 0.005) {
            holder.profitText.setTextColor(COLOR_PROFIT_YELLOW);
        } else {
            holder.profitText.setTextColor(COLOR_PROFIT_RED);
        }
        
        // Set exchange information
        String buyExchange = opportunity.getBuyExchange();
        String sellExchange = opportunity.getSellExchange();
        holder.buyExchangeName.setText(buyExchange);
        holder.sellExchangeName.setText(sellExchange);
        
        // Set exchange logos
        setExchangeLogo(holder.buyExchangeLogo, buyExchange);
        setExchangeLogo(holder.sellExchangeLogo, sellExchange);
        
        // Set price information
        holder.buyPrice.setText(opportunity.getFormattedBuyPrice());
        holder.sellPrice.setText(opportunity.getFormattedSellPrice());
        
        // Set fee information
        holder.buyFeeText.setText(String.format(Locale.US, "%.2f%%", opportunity.getBuyFee() * 100));
        holder.sellFeeText.setText(String.format(Locale.US, "%.2f%%", opportunity.getSellFee() * 100));
        
        // Set risk information
        double riskScore = opportunity.getRiskScore();
        int riskProgress = (int)(riskScore * 100);
        holder.riskText.setText(getRiskLevelText(riskScore));
        holder.riskIndicator.setBackgroundTintList(ColorStateList.valueOf(getRiskColor(riskScore)));
        holder.riskText.setTextColor(getRiskColor(riskScore));
        holder.riskProgress.setProgress(riskProgress);
        
        // Get the original opportunity for advanced calculations
        int originalIndex = findOriginalOpportunityIndex(opportunity.getOpportunityId());
        ArbitrageOpportunity originalOpportunity = originalIndex >= 0 ? originalOpportunities.get(originalIndex) : null;
        
        // Calculate and set slippage information
        double slippage = calculateSlippage(opportunity, originalOpportunity);
        holder.slippageText.setText(String.format(Locale.US, "%.2f%%", slippage * 100));
        
        // Calculate and set execution time and ROI
        double executionTimeMin = calculateExecutionTime(opportunity, originalOpportunity);
        
        if (executionTimeMin > 0) {
            int timeMinutes = (int) executionTimeMin;
            holder.timeText.setText(timeMinutes + "m");
            
            // Calculate ROI per hour
            double hourlyROI = (profitPercent / timeMinutes) * 60;
            String roiText = String.format(Locale.US, "%.2f%%/h", hourlyROI);
            holder.roiEfficiencyText.setText(roiText);
            
            if (hourlyROI >= 0.01) {
                holder.roiEfficiencyText.setTextColor(COLOR_PROFIT_GREEN);
            } else if (hourlyROI >= 0.005) {
                holder.roiEfficiencyText.setTextColor(COLOR_PROFIT_YELLOW);
            } else {
                holder.roiEfficiencyText.setTextColor(COLOR_PROFIT_RED);
            }
        } else {
            holder.timeText.setText("~");
            holder.roiEfficiencyText.setText("");
        }
        
        // Set card click listener to open details
        holder.cardView.setOnClickListener(v -> {
            if (originalIndex >= 0) {
                Intent intent = new Intent(context, OpportunityDetailActivity.class);
                intent.putExtra("opportunity", originalOpportunities.get(originalIndex));
                context.startActivity(intent);
            } else {
                Log.e(TAG, "Could not find original opportunity with ID: " + opportunity.getOpportunityId());
            }
        });
    }
    
    /**
     * Find the index of the original opportunity by ID
     */
    private int findOriginalOpportunityIndex(String opportunityId) {
        for (int i = 0; i < originalOpportunities.size(); i++) {
            if (originalOpportunities.get(i).getOpportunityKey().equals(opportunityId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return opportunities.size();
    }

    /**
     * Update the adapter with a new list of opportunities
     */
    public void updateOpportunities(List<ArbitrageOpportunity> newOpportunities) {
        originalOpportunities.clear();
        originalOpportunities.addAll(newOpportunities);
        
        // Convert ArbitrageOpportunity to ArbitrageCardModel with forced risk recalculation
        opportunities.clear();
        for (ArbitrageOpportunity opportunity : newOpportunities) {
            Log.d(TAG, "Processing opportunity: " + opportunity.getNormalizedSymbol() + ", profit: " + opportunity.getProfitPercent());
            ArbitrageCardModel cardModel = ArbitrageCardModel.fromOpportunity(opportunity, true);
            if (cardModel != null) {
                opportunities.add(cardModel);
                Log.d(TAG, "Added card model with risk: " + cardModel.getRiskScore() + " (" + cardModel.getRiskLevel() + ")");
            }
        }
        
        // Sort by profit percentage (descending)
        Collections.sort(opportunities, (o1, o2) -> 
            Double.compare(o2.getProfitPercent(), o1.getProfitPercent()));
        
        notifyDataSetChanged();
    }
    
    /**
     * Filter opportunities based on risk level
     */
    public void filterByRiskLevel(String riskLevel) {
        opportunities.clear();
        
        for (ArbitrageOpportunity opportunity : originalOpportunities) {
            ArbitrageCardModel cardModel = ArbitrageCardModel.fromOpportunity(opportunity, true);
            if (cardModel != null) {
                if (riskLevel.equals("ALL") || cardModel.getRiskLevel().equals(riskLevel)) {
                    opportunities.add(cardModel);
                }
            }
        }
        
        // Sort by profit percentage (descending)
        Collections.sort(opportunities, (o1, o2) -> 
            Double.compare(o2.getProfitPercent(), o1.getProfitPercent()));
        
        notifyDataSetChanged();
    }

    /**
     * Format volume to a readable string with K, M, B suffixes
     */
    private String formatVolume(double volume) {
        if (volume < 1000) {
            return String.format(Locale.US, "$%.0f", volume);
        } else if (volume < 1000000) {
            return String.format(Locale.US, "$%.1fK", volume / 1000);
        } else if (volume < 1000000000) {
            return String.format(Locale.US, "$%.1fM", volume / 1000000);
        } else {
            return String.format(Locale.US, "$%.1fB", volume / 1000000000);
        }
    }

    /**
     * Get risk level text based on risk score
     */
    private String getRiskLevelText(double riskScore) {
        // Higher scores = higher risk (1.0 is highest risk, 0.0 is lowest risk)
        if (riskScore >= 0.9) {
            return "Critical Risk";
        } else if (riskScore >= 0.8) {
            return "Extreme Risk";
        } else if (riskScore >= 0.7) {
            return "Very High Risk";
        } else if (riskScore >= 0.6) {
            return "High Risk";
        } else if (riskScore >= 0.5) {
            return "Medium-High Risk";
        } else if (riskScore >= 0.4) {
            return "Medium Risk";
        } else if (riskScore >= 0.3) {
            return "Low-Medium Risk";
        } else if (riskScore >= 0.2) {
            return "Low Risk";
        } else if (riskScore >= 0.1) {
            return "Very Low Risk";
        } else {
            return "Minimal Risk";
        }
    }

    /**
     * Get color for risk level based on risk score
     */
    private int getRiskColor(double riskScore) {
        // Higher scores = higher risk (red), lower scores = lower risk (green)
        if (riskScore >= 0.9) {
            return COLOR_RISK_EXTREME;         // Dark Red
        } else if (riskScore >= 0.8) {
            return COLOR_RISK_VERY_HIGH;       // Red
        } else if (riskScore >= 0.7) {
            return COLOR_RISK_HIGH;            // Deep Orange
        } else if (riskScore >= 0.6) {
            return COLOR_RISK_MODERATE_HIGH;   // Orange
        } else if (riskScore >= 0.5) {
            return COLOR_RISK_BALANCED;        // Amber
        } else if (riskScore >= 0.4) {
            return COLOR_RISK_MODERATE;        // Yellow
        } else if (riskScore >= 0.3) {
            return COLOR_RISK_LOW;             // Lime
        } else if (riskScore >= 0.2) {
            return COLOR_RISK_LOW;             // Lime (same as Low)
        } else if (riskScore >= 0.1) {
            return COLOR_RISK_VERY_LOW;        // Light Green
        } else {
            return COLOR_RISK_MINIMAL;         // Green
        }
    }
    
    /**
     * Set the appropriate logo for an exchange
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
     * Calculate estimated slippage based on market conditions
     * @param opportunity The card model containing basic information
     * @param originalOpportunity The original opportunity with more detailed data (can be null)
     * @return Estimated slippage as a percentage (0.0 to 1.0)
     */
    private double calculateSlippage(ArbitrageCardModel opportunity, ArbitrageOpportunity originalOpportunity) {
        // First try to use the pre-calculated value from the model
        double slippage = opportunity.getEstimatedSlippage();
        
        // If we have a valid slippage already, just use it
        if (slippage > 0 && slippage <= maxSlippagePercent) {
            Log.d(TAG, "Using pre-calculated slippage: " + slippage);
            return slippage;
        }
        
        // If we have the original opportunity, calculate a more accurate slippage
        if (originalOpportunity != null) {
            try {
                // Get liquidity information from the opportunity
                double buyVolume = opportunity.getBuyVolume();
                double sellVolume = opportunity.getSellVolume();
                
                // Get ticker data if available
                Ticker buyTicker = originalOpportunity.getBuyTicker();
                Ticker sellTicker = originalOpportunity.getSellTicker();
                
                if (buyTicker != null && sellTicker != null) {
                    // Use direct slippage calculation
                    // Estimate slippage based on volume and liquidity
                    double buyLiquidity = opportunity.getBuyExchangeLiquidity();
                    double sellLiquidity = opportunity.getSellExchangeLiquidity();
                    
                    // Calculate volume to liquidity ratio (higher means more slippage)
                    double volumeToLiquidityRatio = 0.01; // Default value
                    
                    if (buyLiquidity > 0 && sellLiquidity > 0) {
                        // Calculate weighted average of buy/sell ratios
                        double buyRatio = Math.min(1.0, (availableCapital * maxPositionPercent) / buyLiquidity);
                        double sellRatio = Math.min(1.0, (availableCapital * maxPositionPercent) / sellLiquidity);
                        volumeToLiquidityRatio = (buyRatio + sellRatio) / 2.0;
                    }
                    
                    // Base slippage calculation - higher volume/liquidity ratio means higher slippage
                    slippage = 0.002 + (volumeToLiquidityRatio * 0.01);
                    
                    // Adjust for exchange reliability
                    double exchangeReliability = (getExchangeReliability(opportunity.getBuyExchange()) + 
                                              getExchangeReliability(opportunity.getSellExchange())) / 2.0;
                    
                    // More reliable exchanges have lower slippage
                    slippage = slippage * (1.0 - (exchangeReliability * 0.5));
                    
                    Log.d(TAG, "Calculated fresh slippage: " + slippage);
                    
                    // Ensure slippage is within reasonable bounds
                    slippage = Math.min(slippage, maxSlippagePercent);
                    slippage = Math.max(slippage, 0.0001); // Minimum 0.01%
                    
                    return slippage;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating slippage: " + e.getMessage());
            }
        }
        
        // Fallback: calculate a basic slippage based on available data
        double profitPercent = opportunity.getProfitPercent();
        double exchangeFactor = (getExchangeReliability(opportunity.getBuyExchange()) + 
                               getExchangeReliability(opportunity.getSellExchange())) / 2.0;
        
        // Higher profits and better exchanges = lower slippage
        slippage = 0.005 - (Math.min(profitPercent, 0.05) * 0.05) - (exchangeFactor * 0.002);
        slippage = Math.max(0.001, slippage); // At least 0.1%
        
        Log.d(TAG, "Using fallback slippage calculation: " + slippage);
        return slippage;
    }
    
    /**
     * Calculate estimated execution time based on market conditions
     * @param opportunity The card model containing basic information
     * @param originalOpportunity The original opportunity with more detailed data (can be null)
     * @return Estimated execution time in minutes
     */
    private double calculateExecutionTime(ArbitrageCardModel opportunity, ArbitrageOpportunity originalOpportunity) {
        // First try to use the pre-calculated value from the model
        double executionTimeMin = opportunity.getEstimatedExecutionTimeMin();
        
        // If we have a valid execution time already, just use it
        if (executionTimeMin > 0) {
            Log.d(TAG, "Using pre-calculated execution time: " + executionTimeMin);
            return executionTimeMin;
        }
        
        // If we have the original opportunity, calculate a more accurate execution time
        if (originalOpportunity != null) {
            try {
                // Calculate execution time based on exchange speed, profit, and trading pair
                String buyExchange = opportunity.getBuyExchange();
                String sellExchange = opportunity.getSellExchange();
                double profitPercent = opportunity.getProfitPercent();
                
                // Base time depends on the exchanges involved
                double exchangeSpeedFactor = (getExchangeSpeed(buyExchange) + getExchangeSpeed(sellExchange)) / 2.0;
                
                // Base execution time - faster exchanges take less time
                double baseTime = 30.0 * (1.0 - exchangeSpeedFactor);
                
                // Adjust for profit - higher profit often means slower execution due to market depth
                double profitFactor = Math.min(1.5, 1.0 + (profitPercent * 10.0));
                
                executionTimeMin = baseTime * profitFactor;
                
                // Add random variation to make estimates more realistic (±15%)
                double randomFactor = 0.85 + (Math.random() * 0.3);
                executionTimeMin *= randomFactor;
                
                // Round to nearest minute
                executionTimeMin = Math.round(executionTimeMin);
                
                Log.d(TAG, "Calculated fresh execution time: " + executionTimeMin);
                
                // Ensure execution time is within reasonable bounds
                executionTimeMin = Math.max(5.0, executionTimeMin); // At least 5 minutes
                executionTimeMin = Math.min(120.0, executionTimeMin); // At most 2 hours
                
                return executionTimeMin;
            } catch (Exception e) {
                Log.e(TAG, "Error calculating execution time: " + e.getMessage());
            }
        }
        
        // Fallback: calculate a basic execution time based on exchange reliability
        double exchangeSpeed = (getExchangeSpeed(opportunity.getBuyExchange()) + 
                             getExchangeSpeed(opportunity.getSellExchange())) / 2.0;
        
        // Base execution time = 20 minutes, adjusted by exchange speed
        executionTimeMin = 20.0 * (1.0 - exchangeSpeed);
        executionTimeMin = Math.max(5.0, executionTimeMin); // At least 5 minutes
        
        Log.d(TAG, "Using fallback execution time calculation: " + executionTimeMin);
        return executionTimeMin;
    }
    
    /**
     * Gets a reliability factor for a specific exchange (for slippage calculation)
     */
    private double getExchangeReliability(String exchange) {
        if (exchange == null) return 0.3;
        
        switch (exchange.toLowerCase()) {
            case "binance": return 0.9;  // Most reliable
            case "coinbase": return 0.85;
            case "kraken": return 0.8;
            case "kucoin": return 0.75;
            case "bybit": return 0.7;
            case "okx": return 0.75;
            case "gemini": return 0.7;
            case "bitfinex": return 0.6;
            default: return 0.5;
        }
    }
    
    /**
     * Gets a speed factor for a specific exchange (for execution time calculation)
     */
    private double getExchangeSpeed(String exchange) {
        if (exchange == null) return 0.5;
        
        switch (exchange.toLowerCase()) {
            case "binance": return 0.8;  // Fastest
            case "okx": return 0.75;
            case "bybit": return 0.7;
            case "kucoin": return 0.65;
            case "kraken": return 0.6;
            case "coinbase": return 0.55;
            case "gemini": return 0.5;
            case "bitfinex": return 0.45;
            default: return 0.5;
        }
    }

    /**
     * View holder for opportunity items
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final TextView symbolText;
        final TextView profitText;
        final TextView buyExchangeName;
        final TextView sellExchangeName;
        final ImageView buyExchangeLogo;
        final ImageView sellExchangeLogo;
        final TextView buyPrice;
        final TextView sellPrice;
        final TextView buyFeeText;
        final TextView sellFeeText;
        final TextView riskText;
        final View riskIndicator;
        final ProgressBar riskProgress;
        final TextView slippageText;
        final TextView timeText;
        final TextView roiEfficiencyText;

        ViewHolder(View view) {
            super(view);
            cardView = (CardView) view;
            symbolText = view.findViewById(R.id.symbol_text);
            profitText = view.findViewById(R.id.profit_text);
            buyExchangeName = view.findViewById(R.id.buy_exchange_name);
            sellExchangeName = view.findViewById(R.id.sell_exchange_name);
            buyExchangeLogo = view.findViewById(R.id.buy_exchange_logo);
            sellExchangeLogo = view.findViewById(R.id.sell_exchange_logo);
            buyPrice = view.findViewById(R.id.buy_price);
            sellPrice = view.findViewById(R.id.sell_price);
            buyFeeText = view.findViewById(R.id.buy_fee_text);
            sellFeeText = view.findViewById(R.id.sell_fee_text);
            riskText = view.findViewById(R.id.risk_text);
            riskIndicator = view.findViewById(R.id.risk_indicator);
            riskProgress = view.findViewById(R.id.risk_progress);
            slippageText = view.findViewById(R.id.slippage_text);
            timeText = view.findViewById(R.id.time_text);
            roiEfficiencyText = view.findViewById(R.id.roi_efficiency_text);
        }
    }
} 