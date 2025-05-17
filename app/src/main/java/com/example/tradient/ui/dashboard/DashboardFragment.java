package com.example.tradient.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradient.R;
import com.example.tradient.data.factory.ViewModelFactory;
import com.example.tradient.repository.ExchangeRepository;
import com.example.tradient.viewmodel.ArbitrageViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.example.tradient.ui.KeyMetricWidgetManager;

/**
 * Dashboard fragment that displays key trading metrics.
 */
public class DashboardFragment extends Fragment {

    private ArbitrageViewModel viewModel;
    
    // Widget views - reusing existing IDs where possible
    private TextView totalProfitText;
    private TextView opportunitiesCountText;
    private TextView profitChangeIndicator;
    private TextView volatilityValueText;
    private LineChart priceChart;
    private TextView lastUpdatedText;
    private View statusIndicatorBinance;
    private TextView statusTextBinance;
    private View statusIndicatorKraken;
    private TextView statusTextKraken;
    private View chartPlaceholder;

    private KeyMetricWidgetManager profitWidgetManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views - using existing IDs where possible
        totalProfitText = view.findViewById(R.id.total_profit);
        opportunitiesCountText = view.findViewById(R.id.opportunities_count);
        profitChangeIndicator = view.findViewById(R.id.profit_change_indicator);
        volatilityValueText = view.findViewById(R.id.volatility_value);
        priceChart = view.findViewById(R.id.price_chart);
        lastUpdatedText = view.findViewById(R.id.last_updated);
        statusIndicatorBinance = view.findViewById(R.id.status_indicator_binance);
        statusTextBinance = view.findViewById(R.id.status_text_binance);
        statusIndicatorKraken = view.findViewById(R.id.status_indicator_kraken);
        statusTextKraken = view.findViewById(R.id.status_text_kraken);
        chartPlaceholder = view.findViewById(R.id.chart_placeholder);

        // Initialize ViewModel
        ExchangeRepository repository = new ExchangeRepository(requireContext());
        ViewModelFactory factory = new ViewModelFactory(repository);
        viewModel = new ViewModelProvider(requireActivity(), factory).get(ArbitrageViewModel.class);

        // Setup widgets with initial data
        setupMarketPulseWidget();
        setupExchangeStatusWidget();

        // Observe ViewModel data
        observeViewModel();

        // Initialize the data
        viewModel.initialize();

        // Find and initialize the KeyMetricWidgetManager
        // For demonstration purposes, we'll use any MaterialCardView in the layout
        // In production, you would use a specific view
        // Here we're using the first card view we find in the dashboard layout
        View parentLayout = view.getRootView();
        // For demonstration, we'll defer widget initialization until a specific
        // card view component has been added to your layout
        // profitWidgetManager = new KeyMetricWidgetManager(parentLayout);
    }
    
    private void setupMarketPulseWidget() {
        // Setup chart with placeholder data
        if (priceChart != null) {
            // Create sample data
            List<Entry> entries = new ArrayList<>();
            Random random = new Random();
            float lastValue = 34500 + random.nextFloat() * 1000;
            
            for (int i = 0; i < 24; i++) {
                // Create random but slightly trending price movements
                lastValue = lastValue + (random.nextFloat() * 200 - 100);
                entries.add(new Entry(i, lastValue));
            }
            
            // Create dataset
            LineDataSet dataSet = new LineDataSet(entries, "BTC Price");
            dataSet.setColor(getResources().getColor(R.color.neon_blue));
            dataSet.setLineWidth(2f);
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(Color.parseColor("#330052FF"));
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            
            // Create LineData
            LineData lineData = new LineData(dataSet);
            
            // Style the chart
            priceChart.setData(lineData);
            priceChart.getDescription().setEnabled(false);
            priceChart.getLegend().setEnabled(false);
            priceChart.setDrawGridBackground(false);
            priceChart.setDrawBorders(false);
            priceChart.setAutoScaleMinMaxEnabled(true);
            priceChart.setScaleEnabled(false);
            priceChart.setPinchZoom(false);
            priceChart.setTouchEnabled(false);
            
            // Style axes
            XAxis xAxis = priceChart.getXAxis();
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(false);
            xAxis.setDrawLabels(false);
            
            YAxis leftAxis = priceChart.getAxisLeft();
            leftAxis.setDrawGridLines(false);
            leftAxis.setDrawAxisLine(false);
            leftAxis.setDrawLabels(false);
            
            YAxis rightAxis = priceChart.getAxisRight();
            rightAxis.setEnabled(false);
            
            // Refresh the chart
            priceChart.invalidate();
        }
    }
    
    private void setupExchangeStatusWidget() {
        // Set last updated text
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
        lastUpdatedText.setText("Updated " + sdf.format(new Date()));
    }

    private void observeViewModel() {
        viewModel.getInitializationProgress().observe(getViewLifecycleOwner(), this::updateStats);
        viewModel.getArbitrageOpportunities().observe(getViewLifecycleOwner(), opportunities -> {
            if (opportunities != null) {
                // Update opportunities count
                opportunitiesCountText.setText(String.valueOf(opportunities.size()));
                
                // Update profit display based on opportunities
                updateProfitDisplay(opportunities.size());
                
                // Update volatility based on market conditions
                updateVolatilityDisplay(opportunities.size());
            }
        });
    }
    
    private void updateProfitDisplay(int opportunitiesCount) {
        // This is a simplified example - in a real app, 
        // you would calculate actual profit from opportunities
        double profit = opportunitiesCount * 123.45;
        String profitStr = String.format(Locale.US, "$%.2f", profit);
        totalProfitText.setText(profitStr);
        
        // Set percentage change indicator
        String changeStr = opportunitiesCount > 0 ? "+12.3%" : "0.0%";
        profitChangeIndicator.setText(changeStr);
        
        // Set appropriate color
        int textColor = opportunitiesCount > 0 ? 
                        getResources().getColor(R.color.success_green) : 
                        getResources().getColor(R.color.secondary_text);
        profitChangeIndicator.setTextColor(textColor);
    }
    
    private void updateVolatilityDisplay(int opportunitiesCount) {
        // Sample logic - in real app, calculate from market data
        String volatility;
        int volatilityColor;
        
        if (opportunitiesCount > 10) {
            volatility = "HIGH";
            volatilityColor = getResources().getColor(R.color.error_red);
        } else if (opportunitiesCount > 5) {
            volatility = "MEDIUM";
            volatilityColor = getResources().getColor(R.color.warning_yellow);
        } else {
            volatility = "LOW";
            volatilityColor = getResources().getColor(R.color.success_green);
        }
        
        volatilityValueText.setText(volatility);
        volatilityValueText.setTextColor(volatilityColor);
    }

    private void updateStats(Map<String, Object> stats) {
        if (stats == null) return;
        
        // Update exchange status widgets
        if (stats.containsKey("activeExchanges")) {
            int activeExchanges = (int) stats.get("activeExchanges");
            updateExchangeStatusDisplay(activeExchanges);
        }
        
        // Update last updated timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
        lastUpdatedText.setText("Updated " + sdf.format(new Date()));
    }
    
    private void updateExchangeStatusDisplay(int activeExchanges) {
        if (activeExchanges >= 2) {
            // Both exchanges operational
            statusIndicatorBinance.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.success_green))
            );
            statusTextBinance.setText("Operational");
            statusTextBinance.setTextColor(getResources().getColor(R.color.success_green));
            
            statusIndicatorKraken.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.success_green))
            );
            statusTextKraken.setText("Operational");
            statusTextKraken.setTextColor(getResources().getColor(R.color.success_green));
        } else if (activeExchanges == 1) {
            // Binance operational, Kraken degraded
            statusIndicatorBinance.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.success_green))
            );
            statusTextBinance.setText("Operational");
            statusTextBinance.setTextColor(getResources().getColor(R.color.success_green));
            
            statusIndicatorKraken.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.alert_orange))
            );
            statusTextKraken.setText("Degraded");
            statusTextKraken.setTextColor(getResources().getColor(R.color.alert_orange));
        } else {
            // Both exchanges down
            statusIndicatorBinance.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.error_red))
            );
            statusTextBinance.setText("Offline");
            statusTextBinance.setTextColor(getResources().getColor(R.color.error_red));
            
            statusIndicatorKraken.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.error_red))
            );
            statusTextKraken.setText("Offline");
            statusTextKraken.setTextColor(getResources().getColor(R.color.error_red));
        }
    }

    /**
     * Updates all dashboard widgets with the latest data.
     */
    public void refreshDashboard() {
        // Update other widgets as needed first
        setupMarketPulseWidget();
        setupExchangeStatusWidget();
        
        // The manager will handle the animations internally if initialized
        if (profitWidgetManager != null) {
            profitWidgetManager.updateMetrics();
        }
    }
} 