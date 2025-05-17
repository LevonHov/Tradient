package com.example.tradient.ui.arbitrage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradient.R;
import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.domain.risk.EnhancedRiskService;
import com.example.tradient.domain.risk.UnifiedRiskCalculator;
import com.example.tradient.domain.risk.RiskEnsurer;

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
        
        // Use RiskEnsurer to get consistent risk values
        try {
            // First check if opportunity already has a valid risk assessment
            RiskAssessment existingRisk = opportunity.getRiskAssessment();
            if (existingRisk != null && existingRisk.isValid()) {
                Log.d(TAG, "Using existing risk assessment for " + getPairString(opportunity));
                // Ensure consistent risk values
                RiskEnsurer.ensureRiskValues(opportunity, false);
                updateRiskDisplay(holder, existingRisk);
                return;
            }
            
            Log.d(TAG, "Starting risk assessment calculation for " + getPairString(opportunity));
            
            // Calculate fresh risk assessment asynchronously
            riskService.calculateRisk(opportunity)
                .thenAcceptAsync(riskAssessment -> {
                    if (riskAssessment != null) {
                        Log.d(TAG, "Risk assessment completed for " + getPairString(opportunity) + 
                              ": Score=" + riskAssessment.getOverallRiskScore() + 
                              ", Slip=" + String.format("%.2f%%", riskAssessment.getSlippageEstimate() * 100) +
                              ", Liq=" + String.format("%.2f", riskAssessment.getLiquidityScore()));
                        
                        // Ensure consistent risk values
                        RiskEnsurer.ensureRiskValues(opportunity, false);
                        
                        // Update UI with risk assessment data on the main thread
                        androidx.core.content.ContextCompat.getMainExecutor(holder.itemView.getContext()).execute(() -> {
                            updateRiskDisplay(holder, riskAssessment);
                        });
                    } else {
                        Log.w(TAG, "Risk assessment returned null for " + getPairString(opportunity));
                        
                        // Create an unknown state risk assessment
                        RiskAssessment unknownAssessment = RiskAssessment.createUnknownState();
                        opportunity.setRiskAssessment(unknownAssessment);
                        
                        // Update UI on the main thread
                        androidx.core.content.ContextCompat.getMainExecutor(holder.itemView.getContext()).execute(() -> {
                            updateRiskDisplay(holder, unknownAssessment);
                        });
                    }
                }, Executors.newSingleThreadExecutor())
                .exceptionally(ex -> {
                    // Log the full exception
                    Log.e(TAG, "Error calculating risk assessment for " + getPairString(opportunity), ex);
                    
                    // Create an error state risk assessment
                    RiskAssessment errorAssessment = RiskAssessment.createErrorState(ex);
                    opportunity.setRiskAssessment(errorAssessment);
                    
                    // Update UI on the main thread
                    androidx.core.content.ContextCompat.getMainExecutor(holder.itemView.getContext()).execute(() -> {
                        updateRiskDisplay(holder, errorAssessment);
                    });
                    
                    return null;
                });
        } catch (Exception e) {
            Log.e(TAG, "Error loading risk assessment: " + e.getMessage(), e);
            
            // Create an error state risk assessment
            RiskAssessment errorAssessment = RiskAssessment.createErrorState(e);
            opportunity.setRiskAssessment(errorAssessment);
            
            // Update UI on the main thread
            androidx.core.content.ContextCompat.getMainExecutor(holder.itemView.getContext()).execute(() -> {
                updateRiskDisplay(holder, errorAssessment);
            });
        }
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
        // Use UnifiedRiskCalculator for consistent risk level text and color
        UnifiedRiskCalculator riskCalculator = UnifiedRiskCalculator.getInstance();
        
        // Get risk score from assessment
        double riskScore = riskAssessment.getOverallRiskScore();
        
        // Get risk level and color from UnifiedRiskCalculator
        String riskLevel = riskCalculator.getRiskLevelText(riskScore);
        int riskColor = riskCalculator.getRiskColor(riskScore);
        
        // Update UI with consistent risk information
        holder.riskText.setText(riskLevel);
        holder.riskText.setTextColor(riskColor);
        
        // Set risk progress bar (0-100)
        holder.riskProgress.setProgress((int)(riskScore * 100));
        
        // Set slippage display
        double slippagePercent = riskAssessment.getSlippageEstimate() * 100.0;
        holder.slippageText.setText(String.format("Slip: %.1f%%", slippagePercent));
        
        // Set time display
        double executionTime = riskAssessment.getExecutionTimeEstimate();
        String timeDisplay = formatExecutionTime(executionTime);
        holder.timeText.setText("Time: " + timeDisplay);
        
        // Set ROI efficiency
        double roiHourly = riskAssessment.getRoiEfficiency();
        // Ensure ROI is displayed correctly
        if (Double.isNaN(roiHourly) || roiHourly <= 0) {
            // Calculate ROI per hour if not available
            if (executionTime > 0) {
                // Find the opportunity in the list that matches this assessment
                double profitPercent = 0;
                for (ArbitrageOpportunity opportunity : arbitrageOpportunities) {
                    // We'll use the first opportunity we find with this assessment
                    if (opportunity != null && opportunity.getRiskAssessment() == riskAssessment) {
                        try {
                            profitPercent = opportunity.getProfitPercent();
                            break;
                        } catch (Exception e) {
                            // Ignore and continue with 0
                        }
                    }
                }
                roiHourly = (profitPercent / executionTime) * 60.0;
            } else {
                roiHourly = 0;
            }
        }
        holder.roiEfficiencyText.setText(String.format("ROI/h: %.1f%%", roiHourly));
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
        
        if (minutes < 1.0) {
            // Show as seconds for very short times
            int seconds = (int)(minutes * 60.0);
            return seconds + " sec";
        } else if (minutes < 60.0) {
            // Show as minutes for medium times
            return String.format("%.1f min", minutes);
        } else {
            // Show as hours for long times
            return String.format("%.1f hrs", minutes / 60.0);
        }
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