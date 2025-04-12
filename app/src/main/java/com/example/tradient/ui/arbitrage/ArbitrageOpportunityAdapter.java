package com.example.tradient.ui.arbitrage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradient.R;
import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.domain.risk.EnhancedRiskService;

import java.util.List;
import java.util.concurrent.Executors;

public class ArbitrageOpportunityAdapter extends RecyclerView.Adapter<ArbitrageOpportunityAdapter.ViewHolder> {
    private static final String TAG = "ArbitrageOpportunityAdapter";
    
    // Risk color constants
    private static final int COLOR_RISK_LOW = 0xFF4CAF50;     // Green
    private static final int COLOR_RISK_MEDIUM = 0xFFFF9800;  // Orange
    private static final int COLOR_RISK_HIGH = 0xFFFF5722;    // Deep Orange
    private static final int COLOR_RISK_EXTREME = 0xFFF44336; // Red
    private static final int COLOR_RISK_UNKNOWN = 0xFF9E9E9E; // Gray
    
    private final List<ArbitrageOpportunity> arbitrageOpportunities;
    private final OnItemClickListener listener;
    private final EnhancedRiskService riskService;
    
    public ArbitrageOpportunityAdapter(List<ArbitrageOpportunity> arbitrageOpportunities, OnItemClickListener listener) {
        this.arbitrageOpportunities = arbitrageOpportunities;
        this.listener = listener;
        this.riskService = EnhancedRiskService.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_arbitrage_opportunity, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArbitrageOpportunity opportunity = arbitrageOpportunities.get(position);
        
        // Set basic opportunity details
        if (holder.pairText != null) {
            // Get trading pair information
            if (opportunity.getPair() != null) {
                holder.pairText.setText(opportunity.getPair().getSymbol());
            } else if (opportunity.getNormalizedSymbol() != null) {
                holder.pairText.setText(opportunity.getNormalizedSymbol());
            } else {
                holder.pairText.setText(opportunity.getSymbolBuy());
            }
        }
        
        if (holder.profitText != null) {
            // Get profit percentage
            double profitPercent;
            try {
                profitPercent = opportunity.getProfitPercent();
            } catch (Exception e) {
                profitPercent = opportunity.getPotentialProfit();
            }
            holder.profitText.setText(String.format("%.2f%%", profitPercent));
        }
        
        if (holder.exchangeText != null) {
            // Set exchange information
            holder.exchangeText.setText(
                String.format("%s → %s", 
                opportunity.getExchangeBuy(), 
                opportunity.getExchangeSell())
            );
        }
        
        // Get risk assessment using enhanced risk service
        loadRiskAssessment(opportunity, holder);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(opportunity);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return arbitrageOpportunities != null ? arbitrageOpportunities.size() : 0;
    }
    
    /**
     * Load risk assessment for opportunity using the enhanced risk service
     */
    private void loadRiskAssessment(ArbitrageOpportunity opportunity, ViewHolder holder) {
        // Show loading state
        holder.riskProgress.setProgress(0);
        holder.riskText.setText("LOADING");
        holder.riskText.setTextColor(COLOR_RISK_UNKNOWN);
        holder.slippageText.setText("Slip: --");
        holder.timeText.setText("Time: --");
        holder.roiEfficiencyText.setText("ROI/h: --");
        
        Log.d(TAG, "Starting risk assessment calculation for " + getPairString(opportunity));
        
        // Calculate risk assessment using real-time data
        riskService.calculateRisk(opportunity)
            .thenAcceptAsync(riskAssessment -> {
                if (riskAssessment != null) {
                    Log.d(TAG, "Risk assessment completed for " + getPairString(opportunity) + 
                          ": Risk=" + riskAssessment.getRiskLevel() + 
                          ", Slip=" + String.format("%.2f%%", riskAssessment.getSlippageEstimate() * 100) +
                          ", Liq=" + String.format("%.2f", riskAssessment.getLiquidityScore()));
                    
                    // Update UI with risk assessment data
                    updateRiskDisplay(holder, riskAssessment);
                    
                    // Try to store risk assessment with opportunity using reflection
                    try {
                        // Store risk assessment using reflection
                        try {
                            java.lang.reflect.Field field = 
                                ArbitrageOpportunity.class.getDeclaredField("riskAssessment");
                            field.setAccessible(true);
                            field.set(opportunity, riskAssessment);
                            Log.d(TAG, "Successfully stored risk assessment in opportunity object");
                        } catch (NoSuchFieldException e) {
                            // Try with method
                            try {
                                java.lang.reflect.Method setMethod = 
                                    ArbitrageOpportunity.class.getMethod("setRiskAssessment", RiskAssessment.class);
                                setMethod.invoke(opportunity, riskAssessment);
                                Log.d(TAG, "Successfully stored risk assessment using setter method");
                            } catch (Exception ex) {
                                // Just log error - UI is already updated
                                Log.w(TAG, "Could not store risk assessment in object: " + ex.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error storing risk assessment: " + e.getMessage());
                    }
                } else {
                    Log.w(TAG, "Risk assessment returned null for " + getPairString(opportunity));
                    // Set default values for null risk assessment
                    holder.riskText.setText("UNKNOWN");
                    holder.riskText.setTextColor(COLOR_RISK_UNKNOWN);
                    holder.riskProgress.setProgress(0);
                }
            }, Executors.newSingleThreadExecutor());
    }
    
    /**
     * Get a string representation of the trading pair
     */
    private String getPairString(ArbitrageOpportunity opportunity) {
        if (opportunity == null) return "null";
        
        try {
            if (opportunity.getPair() != null) {
                return opportunity.getPair().getSymbol();
            } else if (opportunity.getNormalizedSymbol() != null) {
                return opportunity.getNormalizedSymbol();
            } else if (opportunity.getSymbolBuy() != null) {
                return opportunity.getSymbolBuy();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "unknown_pair";
    }
    
    /**
     * Update risk display with assessment data
     */
    private void updateRiskDisplay(ViewHolder holder, RiskAssessment riskAssessment) {
        // Set risk level display
        String riskLevel = riskAssessment.getRiskLevel();
        int riskColor;
        
        switch (riskLevel) {
            case RiskAssessment.RISK_LEVEL_LOW:
                riskColor = COLOR_RISK_LOW;
                break;
            case RiskAssessment.RISK_LEVEL_MEDIUM:
                riskColor = COLOR_RISK_MEDIUM;
                break;
            case RiskAssessment.RISK_LEVEL_HIGH:
                riskColor = COLOR_RISK_HIGH;
                break;
            case RiskAssessment.RISK_LEVEL_EXTREME:
                riskColor = COLOR_RISK_EXTREME;
                break;
            default:
                riskColor = COLOR_RISK_UNKNOWN;
                break;
        }
        
        holder.riskText.setText(riskLevel);
        holder.riskText.setTextColor(riskColor);
        
        // Set risk progress bar
        holder.riskProgress.setProgress(riskAssessment.getNormalizedRiskScore());
        
        // Set slippage display
        double slippagePercent = riskAssessment.getSlippageEstimate() * 100.0;
        holder.slippageText.setText(String.format("Slip: %.1f%%", slippagePercent));
        
        // Set time display
        holder.timeText.setText("Time: " + riskAssessment.getFormattedExecutionTime());
        
        // Set ROI efficiency
        double roiHourly = riskAssessment.getRoiEfficiency() * 100.0;
        holder.roiEfficiencyText.setText(String.format("ROI/h: %.1f%%", roiHourly));
    }
    
    /**
     * ViewHolder class for arbitrage opportunities
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView pairText;
        public final TextView profitText;
        public final TextView exchangeText;
        public final TextView riskText;
        public final ProgressBar riskProgress;
        public final TextView slippageText;
        public final TextView timeText;
        public final TextView roiEfficiencyText;
        
        public ViewHolder(View view) {
            super(view);
            pairText = view.findViewById(R.id.text_trading_pair);
            profitText = view.findViewById(R.id.text_profit_percentage);
            exchangeText = view.findViewById(R.id.text_exchanges);
            riskText = view.findViewById(R.id.text_risk_level);
            riskProgress = view.findViewById(R.id.progress_risk);
            slippageText = view.findViewById(R.id.text_slippage);
            timeText = view.findViewById(R.id.text_execution_time);
            roiEfficiencyText = view.findViewById(R.id.text_roi_efficiency);
        }
    }
    
    /**
     * Interface for item click events
     */
    public interface OnItemClickListener {
        void onItemClick(ArbitrageOpportunity opportunity);
    }
} 