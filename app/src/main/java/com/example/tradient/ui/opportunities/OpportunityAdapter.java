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
import android.content.Context;
import android.widget.Toast;
import android.content.Intent;

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
import com.example.tradient.domain.risk.UnifiedRiskCalculator;

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
    private final UnifiedRiskCalculator riskCalculator;
    
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
        riskCalculator = UnifiedRiskCalculator.getInstance();
        
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
                // Navigate to the detail activity with the opportunity data
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
     * Get the appropriate risk level text for a risk score
     */
    private String getRiskLevelText(double riskScore) {
        // Use UnifiedRiskCalculator directly without inverting the scale
        return riskCalculator.getRiskLevelText(riskScore);
    }

    /**
     * Get the appropriate color for a risk score
     */
    private int getRiskColor(double riskScore) {
        // Use UnifiedRiskCalculator directly without inverting the scale
        return riskCalculator.getRiskColor(riskScore);
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
     * Calculate slippage for an opportunity
     */
    private double calculateSlippage(ArbitrageCardModel opportunity, ArbitrageOpportunity originalOpportunity) {
        // Default slippage for safety
        double defaultSlippage = 0.005; // 0.5%
        
        // Try to get slippage from the original opportunity first
        if (originalOpportunity != null) {
            try {
                // Check if the opportunity has a valid risk assessment
                RiskAssessment riskAssessment = originalOpportunity.getRiskAssessment();
                if (riskAssessment != null && riskAssessment.isValid()) {
                    double slippage = riskAssessment.getSlippageEstimate();
                    if (slippage > 0) {
                        return slippage;
                    }
                }
                
                // If there's no valid assessment, calculate using UnifiedRiskCalculator
                RiskAssessment assessment = riskCalculator.calculateRisk(originalOpportunity);
                if (assessment != null) {
                    // Apply the assessment to the opportunity
                    riskCalculator.applyRiskAssessment(originalOpportunity, assessment);
                    return assessment.getSlippageEstimate();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating slippage: " + e.getMessage(), e);
            }
        }
        
        // Fallback to the slippage in the card model if it exists
        if (opportunity.getEstimatedSlippage() > 0) {
            return opportunity.getEstimatedSlippage();
        }
        
        return defaultSlippage;
    }
    
    /**
     * Calculate execution time for an opportunity
     */
    private double calculateExecutionTime(ArbitrageCardModel opportunity, ArbitrageOpportunity originalOpportunity) {
        // Default execution time
        double defaultTime = 3.0; // 3 minutes
        
        // Try to get execution time from the original opportunity first
        if (originalOpportunity != null) {
            try {
                // Check if the opportunity has a valid risk assessment
                RiskAssessment riskAssessment = originalOpportunity.getRiskAssessment();
                if (riskAssessment != null && riskAssessment.isValid()) {
                    double executionTime = riskAssessment.getExecutionTimeEstimate();
                    if (executionTime > 0) {
                        return executionTime;
                    }
                }
                
                // If there's no valid assessment, calculate using UnifiedRiskCalculator
                RiskAssessment assessment = riskCalculator.calculateRisk(originalOpportunity);
                if (assessment != null) {
                    // Apply the assessment to the opportunity
                    riskCalculator.applyRiskAssessment(originalOpportunity, assessment);
                    return assessment.getExecutionTimeEstimate();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating execution time: " + e.getMessage(), e);
            }
        }
        
        // Fallback to the execution time in the card model if it exists
        if (opportunity.getEstimatedExecutionTimeMin() > 0) {
            return opportunity.getEstimatedExecutionTimeMin();
        }
        
        return defaultTime;
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