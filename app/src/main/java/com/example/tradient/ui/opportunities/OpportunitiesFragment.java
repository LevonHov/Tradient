package com.example.tradient.ui.opportunities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradient.R;
import com.example.tradient.data.factory.ViewModelFactory;
import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.RiskAssessment;
import com.example.tradient.domain.risk.RiskCalculator;
import com.example.tradient.repository.ExchangeRepository;
import com.example.tradient.util.RiskAssessmentAdapter;
import com.example.tradient.viewmodel.ArbitrageViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class OpportunitiesFragment extends Fragment {

    private ArbitrageViewModel viewModel;
    private RecyclerView opportunitiesRecyclerView;
    private OpportunityAdapter opportunityAdapter;
    private TextView emptyStateText;
    
    // Search-related fields
    private EditText searchEditText;
    private ImageButton clearButton;
    private ImageButton filterButton;
    private ChipGroup filterChipGroup;
    private HorizontalScrollView filterChipsScrollView;
    
    // Advanced filter fields
    private FloatingActionButton advancedFilterFab;
    private CardView filterPanel;
    private RangeSlider profitRangeSlider;
    private TextView profitMinValue;
    private TextView profitMaxValue;
    private ChipGroup exchangeChipGroup;
    private Slider riskLevelSlider;
    private Spinner executionTimeSpinner;
    private Slider volumeSlider;
    private Button resetFiltersButton;
    private Button applyFiltersButton;
    private TextView matchingOpportunitiesText;
    
    private List<ArbitrageOpportunity> allOpportunities = new ArrayList<>();
    private List<ArbitrageOpportunity> filteredOpportunities = new ArrayList<>();
    
    // Filter state
    private String currentSearchQuery = "";
    private String currentChipFilter = "";
    private float minProfitPercent = 0f;
    private float maxProfitPercent = 50f;
    private Set<String> selectedExchanges = new HashSet<>();
    private float maxRiskLevel = 5f;
    private int maxExecutionTimeMinutes = -1; // -1 means any time
    private float minVolume = 1f;
    private boolean isFilterPanelVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_opportunities, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        opportunitiesRecyclerView = view.findViewById(R.id.opportunities_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        
        // Initialize search views
        searchEditText = view.findViewById(R.id.searchEditText);
        clearButton = view.findViewById(R.id.clearButton);
        filterButton = view.findViewById(R.id.filterButton);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        filterChipsScrollView = view.findViewById(R.id.filterChipsScrollView);
        
        // Initialize advanced filter views
        advancedFilterFab = view.findViewById(R.id.advancedFilterFab);
        filterPanel = view.findViewById(R.id.filterPanel);
        profitRangeSlider = view.findViewById(R.id.profitRangeSlider);
        profitMinValue = view.findViewById(R.id.profitMinValue);
        profitMaxValue = view.findViewById(R.id.profitMaxValue);
        exchangeChipGroup = view.findViewById(R.id.exchangeChipGroup);
        riskLevelSlider = view.findViewById(R.id.riskLevelSlider);
        executionTimeSpinner = view.findViewById(R.id.executionTimeSpinner);
        volumeSlider = view.findViewById(R.id.volumeSlider);
        resetFiltersButton = view.findViewById(R.id.resetFiltersButton);
        applyFiltersButton = view.findViewById(R.id.applyFiltersButton);
        matchingOpportunitiesText = view.findViewById(R.id.matchingOpportunitiesText);

        // Set up RecyclerView
        opportunitiesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        opportunityAdapter = new OpportunityAdapter(new ArrayList<>());
        opportunitiesRecyclerView.setAdapter(opportunityAdapter);
        
        // Debug message at startup
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText("Loading opportunities...");

        // Set up search functionality
        setupSearchFunctionality();
        
        // Set up filter chips
        setupFilterChips();
        
        // Set up advanced filters
        setupAdvancedFilters();

        // Initialize ViewModel
        ExchangeRepository repository = new ExchangeRepository(requireContext());
        ViewModelFactory factory = new ViewModelFactory(repository);
        viewModel = new ViewModelProvider(requireActivity(), factory).get(ArbitrageViewModel.class);

        // Observe ViewModel data
        observeViewModel();

        // Initialize the data
        viewModel.initialize();
    }
    
    private void setupSearchFunctionality() {
        // Configure search text change listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().toLowerCase(Locale.getDefault()).trim();
                clearButton.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                filterOpportunities();
            }
        });
        
        // Configure clear button
        clearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearButton.setVisibility(View.GONE);
        });
        
        // Configure filter button
        filterButton.setOnClickListener(v -> {
            // Toggle the filter chips visibility for a simple implementation
            boolean isVisible = filterChipsScrollView.getVisibility() == View.VISIBLE;
            filterChipsScrollView.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        });
    }
    
    private void setupFilterChips() {
        // Set up click listener for each chip
        for (int i = 0; i < filterChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) filterChipGroup.getChildAt(i);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Get the selected filter
                    currentChipFilter = buttonView.getText().toString();
                    if (currentChipFilter.equals("All")) {
                        currentChipFilter = "";
                    }
                    filterOpportunities();
                }
            });
        }
    }
    
    private void setupAdvancedFilters() {
        // Set up the FAB to show/hide the filter panel
        advancedFilterFab.setOnClickListener(v -> toggleFilterPanel());
        
        // Set up the profit range slider
        profitRangeSlider.setValues(0f, 50f);
        profitRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            minProfitPercent = values.get(0);
            maxProfitPercent = values.get(1);
            profitMinValue.setText(String.format(Locale.US, "%.1f%%", minProfitPercent));
            profitMaxValue.setText(String.format(Locale.US, "%.1f%%+", maxProfitPercent));
            updateMatchingCount();
        });
        
        // Set up the exchange chips
        initializeExchangeChips();
        
        // Set up the risk level slider
        TextView riskLevelValue = requireView().findViewById(R.id.riskLevelValue);
        TextView slippageValueText = requireView().findViewById(R.id.slippageValueText);
        TextView riskScoreValueText = requireView().findViewById(R.id.riskScoreValueText);
        Slider riskLevelSlider = requireView().findViewById(R.id.riskLevelSlider);
        
        if (riskLevelValue != null && riskLevelSlider != null) {
            // Set initial value
            riskLevelValue.setText(getRiskLevelName(convertRiskSliderToScore(maxRiskLevel)));
            updateSlippageInfo(maxRiskLevel, slippageValueText, riskScoreValueText);
            
            // Set up slider listener
            riskLevelSlider.addOnChangeListener((slider, value, fromUser) -> {
                maxRiskLevel = value;
                double riskScore = convertRiskSliderToScore(value);
                
                // Update the displayed risk level name
                if (riskLevelValue != null) {
                    riskLevelValue.setText(getRiskLevelName(riskScore));
                }
                
                // Update slippage and risk score information
                updateSlippageInfo(value, slippageValueText, riskScoreValueText);
                
                // Update matching count
                updateMatchingCount();
                
                // Apply filter immediately if panel is visible
                if (isFilterPanelVisible) {
                    applyAllFilters();
                }
            });
        }
        
        // Set up the execution time spinner
        String[] executionTimeOptions = {
            "Any time", 
            "< 1 minute", 
            "< 5 minutes", 
            "< 15 minutes", 
            "< 30 minutes"
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
            requireContext(), 
            R.layout.spinner_dropdown_item, 
            executionTimeOptions
        );
        executionTimeSpinner.setAdapter(spinnerAdapter);
        executionTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: maxExecutionTimeMinutes = -1; break; // Any time
                    case 1: maxExecutionTimeMinutes = 1; break;  // < 1 minute
                    case 2: maxExecutionTimeMinutes = 5; break;  // < 5 minutes
                    case 3: maxExecutionTimeMinutes = 15; break; // < 15 minutes
                    case 4: maxExecutionTimeMinutes = 30; break; // < 30 minutes
                }
                updateMatchingCount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                maxExecutionTimeMinutes = -1; // Any time
            }
        });
        
        // Set up the volume slider
        volumeSlider.addOnChangeListener((slider, value, fromUser) -> {
            minVolume = value;
            updateMatchingCount();
        });
        
        // Set up the button actions
        resetFiltersButton.setOnClickListener(v -> {
            resetFilters();
            // Apply filter after reset
            applyAllFilters();
            // Show message that filters were reset
            matchingOpportunitiesText.setText("Filters reset");
        });
        
        applyFiltersButton.setOnClickListener(v -> {
            // Apply the filters
            applyAllFilters();
            // Close the filter panel
            toggleFilterPanel();
            // Show message that filters were applied
            matchingOpportunitiesText.setText("Filters applied");
        });
    }
    
    private void initializeExchangeChips() {
        // Define valid exchanges
        List<String> validExchanges = Arrays.asList("Binance", "OKX", "ByBit", "Kraken", "Coinbase");
        
        for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) exchangeChipGroup.getChildAt(i);
            String exchange = chip.getText().toString();
            
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (exchange.equals("All Exchanges")) {
                    if (isChecked) {
                        // Uncheck all other chips
                        for (int j = 0; j < exchangeChipGroup.getChildCount(); j++) {
                            Chip otherChip = (Chip) exchangeChipGroup.getChildAt(j);
                            if (!otherChip.equals(buttonView) && otherChip.isChecked()) {
                                otherChip.setChecked(false);
                            }
                        }
                        selectedExchanges.clear();
                    }
                } else {
                    // Uncheck "All Exchanges" chip
                    Chip allChip = exchangeChipGroup.findViewById(R.id.chip_exchange_all);
                    if (allChip.isChecked()) {
                        allChip.setChecked(false);
                    }
                    
                    if (isChecked) {
                        selectedExchanges.add(exchange);
                    } else {
                        selectedExchanges.remove(exchange);
                    }
                    
                    // If no exchanges selected, check "All Exchanges"
                    if (selectedExchanges.isEmpty()) {
                        allChip.setChecked(true);
                    }
                }
                updateMatchingCount();
            });
        }
    }
    
    private void toggleFilterPanel() {
        isFilterPanelVisible = !isFilterPanelVisible;
        filterPanel.setVisibility(isFilterPanelVisible ? View.VISIBLE : View.GONE);
        
        // Rotate the FAB icon to indicate open/closed state
        advancedFilterFab.setRotation(isFilterPanelVisible ? 45 : 0);
    }
    
    private void resetFilters() {
        // Reset profit range
        profitRangeSlider.setValues(0f, 50f);
        minProfitPercent = 0f;
        maxProfitPercent = 50f;
        profitMinValue.setText("0%");
        profitMaxValue.setText("50%+");
        
        // Reset exchanges
        selectedExchanges.clear();
        for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) exchangeChipGroup.getChildAt(i);
            chip.setChecked(chip.getId() == R.id.chip_exchange_all);
        }
        
        // Reset risk level
        riskLevelSlider.setValue(5f);
        maxRiskLevel = 5f;
        
        // Reset execution time
        executionTimeSpinner.setSelection(0); // "Any time"
        maxExecutionTimeMinutes = -1;
        
        // Reset volume
        volumeSlider.setValue(1f);
        minVolume = 1f;
        
        updateMatchingCount();
    }
    
    private void updateMatchingCount() {
        if (allOpportunities.isEmpty()) {
            matchingOpportunitiesText.setText("0 opportunities match");
            return;
        }
        
        int count = countMatchingOpportunities();
        matchingOpportunitiesText.setText(String.format(Locale.US, "%d opportunities match", count));
    }
    
    private int countMatchingOpportunities() {
        return (int) allOpportunities.stream()
            .filter(this::opportunityMatchesAdvancedFilters)
            .count();
    }

    private void observeViewModel() {
        viewModel.getArbitrageOpportunities().observe(getViewLifecycleOwner(), opportunities -> {
            // Debug log
            if (opportunities == null) {
                showError("Received null opportunities from viewModel");
                return;
            }
            
            // Show count even if empty
            String countMessage = "Received " + (opportunities != null ? opportunities.size() : 0) + " opportunities";
            if (opportunities == null || opportunities.isEmpty()) {
                showError(countMessage);
            } else {
                emptyStateText.setVisibility(View.GONE);
                opportunitiesRecyclerView.setVisibility(View.VISIBLE);
                matchingOpportunitiesText.setText(countMessage);
                updateOpportunities(opportunities);
            }
        });
        
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), this::updateStatus);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), this::showError);
    }

    private void updateOpportunities(List<ArbitrageOpportunity> opportunities) {
        if (opportunities != null && !opportunities.isEmpty()) {
            // Store all opportunities (after removing negative profits)
            allOpportunities = new ArrayList<>(opportunities);
            allOpportunities.removeIf(opportunity -> opportunity.getProfitPercent() <= 0);
            
            // Sort opportunities by profit percentage (highest first)
            sortOpportunitiesByProfit(allOpportunities, false);
            
            // Always apply current filters when data is loaded
            applyAllFilters();
            
            // Add summary info including risk levels
            updateRiskLevelSummary(allOpportunities);
        } else {
            allOpportunities.clear();
            showEmptyState();
        }
    }
    
    /**
     * Update the risk level summary in the fragment
     * Displays statistics about risk levels across all opportunities
     */
    private void updateRiskLevelSummary(List<ArbitrageOpportunity> opportunities) {
        // Only process if we have opportunities
        if (opportunities == null || opportunities.isEmpty()) {
            return;
        }
        
        int lowRiskCount = 0;
        int mediumRiskCount = 0;
        int highRiskCount = 0;
        
        // Count opportunities by risk level
        for (ArbitrageOpportunity opportunity : opportunities) {
            double riskScore = getRiskScoreForOpportunity(opportunity);
            
            if (riskScore >= 0.7) {
                lowRiskCount++;
            } else if (riskScore >= 0.4) {
                mediumRiskCount++;
            } else {
                highRiskCount++;
            }
        }
        
        // Create summary text
        String riskSummary = String.format("Risk Summary: %d Low, %d Medium, %d High", 
            lowRiskCount, mediumRiskCount, highRiskCount);
            
        // Display in the TextView
        TextView riskSummaryView = getView().findViewById(R.id.risk_summary_text);
        if (riskSummaryView != null) {
            riskSummaryView.setText(riskSummary);
            riskSummaryView.setVisibility(View.VISIBLE);
        }
        
        // Log for debugging
        Log.d("OpportunitiesFragment", riskSummary);
    }
    
    /**
     * Apply all filters and update the display with filtered opportunities
     */
    private void applyAllFilters() {
        // If we have no opportunities, don't attempt to filter
        if (allOpportunities == null || allOpportunities.isEmpty()) {
            matchingOpportunitiesText.setText("No opportunities available");
            opportunityAdapter.updateOpportunities(new ArrayList<>());
            return;
        }
        
        // Filter, sort and display the opportunities
        List<ArbitrageOpportunity> filteredList = filterOpportunities();
        sortOpportunitiesByProfit(filteredList, false);
        opportunityAdapter.updateOpportunities(filteredList);
        
        // Update the count display
        if (filteredList.isEmpty()) {
            matchingOpportunitiesText.setText("No opportunities match filters");
        } else {
            matchingOpportunitiesText.setText(String.format(Locale.US, "%d opportunities match", filteredList.size()));
        }
    }
    
    /**
     * Filter opportunities based on current filter settings
     * @return List of filtered opportunities
     */
    private List<ArbitrageOpportunity> filterOpportunities() {
        List<ArbitrageOpportunity> filteredList = new ArrayList<>();
        
        // Debug log for all opportunities before filtering
        Log.d("OpportunitiesFragment", "Total opportunities before filtering: " + allOpportunities.size());
        for (ArbitrageOpportunity opp : allOpportunities) {
            double riskScore = getRiskScoreForOpportunity(opp);
            String riskLevel = getRiskLevelName(riskScore);
            Log.d("OpportunitiesFragment", String.format("Pre-filter: %s - Risk: %.2f (%s)", 
                opp.getNormalizedSymbol(), riskScore, riskLevel));
        }
        
        for (ArbitrageOpportunity opportunity : allOpportunities) {
            // Skip negative profit opportunities
            if (opportunity.getProfitPercent() <= 0) {
                continue;
            }
            
            // Apply profit filter - only filter if maxProfitPercent is less than 100
            if (maxProfitPercent < 100) {
                double profitPercent = opportunity.getProfitPercent();
                if (profitPercent < minProfitPercent || profitPercent > maxProfitPercent) {
                    continue;
                }
            }
            
            // Apply exchange filter if exchanges are selected
            if (!selectedExchanges.isEmpty()) {
                String buyExchange = opportunity.getExchangeBuy();
                String sellExchange = opportunity.getExchangeSell();
                if (!selectedExchanges.contains(buyExchange) && !selectedExchanges.contains(sellExchange)) {
                    continue;
                }
            }
            
            // Apply risk filter if maxRiskLevel is set (converted to 0-1 scale)
            if (maxRiskLevel < 1.0) { // Only apply if not at maximum
                double riskScore = getRiskScoreForOpportunity(opportunity);
                String riskLevel = getRiskLevelName(riskScore);
                Log.d("OpportunitiesFragment", String.format("Checking opportunity %s - Risk: %.2f (%s) vs Max: %.2f",
                    opportunity.getNormalizedSymbol(), riskScore, riskLevel, maxRiskLevel));
                
                if (riskScore < maxRiskLevel) { // Lower risk score means higher risk
                    Log.d("OpportunitiesFragment", String.format("Filtered out %s due to high risk (%.2f < %.2f)",
                        opportunity.getNormalizedSymbol(), riskScore, maxRiskLevel));
                    continue;
                }
            }
            
            filteredList.add(opportunity);
        }
        
        // Log the filtered count and risk distribution
        Log.d("OpportunitiesFragment", String.format("Filtered to %d opportunities out of %d", 
            filteredList.size(), allOpportunities.size()));
        
        // Log risk distribution of filtered opportunities
        for (RiskLevel level : RISK_LEVELS) {
            int count = 0;
            for (ArbitrageOpportunity opp : filteredList) {
                double riskScore = getRiskScoreForOpportunity(opp);
                if (level.contains(riskScore)) {
                    count++;
                }
            }
            Log.d("OpportunitiesFragment", String.format("Risk level %s: %d opportunities", level.name, count));
        }
        
        return filteredList;
    }
    
    /**
     * Gets the risk score from an opportunity
     */
    private double getRiskScoreForOpportunity(ArbitrageOpportunity opportunity) {
        try {
            if (opportunity == null) {
                return 0.0; // Highest risk if opportunity is null
            }
            
            // Get the risk assessment using the adapter
            RiskAssessment assessment = RiskAssessmentAdapter.getRiskAssessment(opportunity);
            if (assessment == null) {
                // Calculate risk assessment using RiskCalculator
                RiskCalculator riskCalculator = new RiskCalculator();
                assessment = riskCalculator.assessRisk(opportunity);
                if (assessment == null) {
                    return 0.0; // Highest risk if assessment fails
                }
                // Store the assessment in the opportunity using the adapter
                RiskAssessmentAdapter.setRiskAssessment(opportunity, assessment);
            }
            
            // Get the risk score from the assessment (0-1 scale)
            double riskScore = assessment.getOverallRiskScore();
            
            // Log the risk score for debugging
            Log.d("OpportunitiesFragment", String.format("Risk score for %s: %.2f", 
                opportunity.getNormalizedSymbol(), riskScore));
            
            return riskScore;
        } catch (Exception e) {
            Log.e("OpportunitiesFragment", "Error calculating risk score: " + e.getMessage());
            return 0.0; // Highest risk on error
        }
    }
    
    /**
     * Sorts a list of arbitrage opportunities by profit percentage
     * @param opportunities The list to sort
     * @param ascending If true, sort in ascending order; if false, sort in descending order (highest profit first)
     */
    private void sortOpportunitiesByProfit(List<ArbitrageOpportunity> opportunities, boolean ascending) {
        opportunities.sort((o1, o2) -> {
            double profit1 = o1.getProfitPercent();
            double profit2 = o2.getProfitPercent();
            
            if (ascending) {
                return Double.compare(profit1, profit2);
            } else {
                return Double.compare(profit2, profit1); // Descending order
            }
        });
    }
    
    private boolean opportunityMatchesAdvancedFilters(ArbitrageOpportunity opportunity) {
        // Skip negative profit opportunities
        if (opportunity.getProfitPercent() <= 0) {
            return false;
        }

        // Apply profit filter - only filter if maxProfitPercent is less than 100
        if (maxProfitPercent < 100) {
            double profitPercent = opportunity.getProfitPercent();
            if (profitPercent < minProfitPercent || profitPercent > maxProfitPercent) {
                return false;
            }
        }

        // Check exchanges
        if (!selectedExchanges.isEmpty()) {
            String buyExchange = opportunity.getExchangeBuy();
            String sellExchange = opportunity.getExchangeSell();
            if (!selectedExchanges.contains(buyExchange) && !selectedExchanges.contains(sellExchange)) {
                return false;
            }
        }

        // Check risk level
        if (maxRiskLevel < 1.0) {
            double riskScore = getRiskScoreForOpportunity(opportunity);
            if (riskScore < maxRiskLevel) {
                return false;
            }
        }

        return true;
    }

    private void showEmptyState() {
        emptyStateText.setVisibility(View.VISIBLE);
        opportunitiesRecyclerView.setVisibility(View.GONE);
        emptyStateText.setText(R.string.no_opportunities_found);
    }

    private void updateStatus(String status) {
        // Could show a status bar or toast with status updates
        if (status != null && status.contains("scanning")) {
            emptyStateText.setText(R.string.scanning_for_opportunities);
        }
    }

    private void showError(String error) {
        // Show error in UI
        if (opportunityAdapter.getItemCount() == 0) {
            emptyStateText.setVisibility(View.VISIBLE);
            opportunitiesRecyclerView.setVisibility(View.GONE);
            emptyStateText.setText(error);
        }
    }

    private static class RiskLevel {
        final String name;
        final double minValue;
        final double maxValue;

        RiskLevel(String name, double minValue, double maxValue) {
            this.name = name;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        boolean contains(double value) {
            return value >= minValue && value <= maxValue;
        }
    }

    private static final RiskLevel[] RISK_LEVELS = {
        new RiskLevel("Very Low", 0.8, 1.0),    
        new RiskLevel("Low", 0.6, 0.79),
        new RiskLevel("Moderate", 0.4, 0.59),
        new RiskLevel("Balanced", 0.3, 0.39),
        new RiskLevel("Moderate High", 0.2, 0.29),
        new RiskLevel("High", 0.1, 0.19),
        new RiskLevel("Extreme", 0.0, 0.09)
    };

    private String getRiskLevelName(double riskScore) {
        // Round the risk score to one decimal place for more accurate matching
        riskScore = Math.round(riskScore * 10.0) / 10.0;
        
        for (RiskLevel level : RISK_LEVELS) {
            if (level.contains(riskScore)) {
                return level.name;
            }
        }
        return "Unknown";
    }

    /**
     * Convert the risk slider value (1-10) to a risk score (0-1)
     * Lower slider value = higher risk (lower risk score)
     */
    private double convertRiskSliderToScore(double sliderValue) {
        // Map slider value 1-10
        // to risk score 0.1-0.9 (reversed so higher slider = lower risk)
        // The slider is 1-10, but we want a normalized value of 0.1-0.9
        double normalizedScore = (11.0 - sliderValue) / 10.0;  // 10->0.1, 1->1.0
        
        // Ensure the result is in the valid range
        return Math.max(0.1, Math.min(1.0, normalizedScore));
    }
    
    /**
     * Update the slippage and risk score information based on the risk level slider
     */
    private void updateSlippageInfo(double sliderValue, TextView slippageText, TextView riskScoreText) {
        if (slippageText == null || riskScoreText == null) {
            return;
        }
        
        double riskScore = convertRiskSliderToScore(sliderValue);
        
        // Determine expected slippage range based on risk level
        String slippageRange;
        if (riskScore >= 0.8) {
            slippageRange = "0.05% - 0.1%";  // Very low slippage for low risk
        } else if (riskScore >= 0.6) {
            slippageRange = "0.1% - 0.2%";   // Low slippage
        } else if (riskScore >= 0.4) {
            slippageRange = "0.2% - 0.5%";   // Moderate slippage
        } else if (riskScore >= 0.2) {
            slippageRange = "0.5% - 1.0%";   // High slippage
        } else {
            slippageRange = "> 1.0%";        // Very high slippage
        }
        
        // Set slippage text
        slippageText.setText(slippageRange);
        
        // Set risk score text (converting to the 0-1 scale)
        String riskScoreDisplay = String.format(Locale.US, "≥ %.1f (%s)",
            riskScore, getRiskLevelName(riskScore));
        riskScoreText.setText(riskScoreDisplay);
        
        // Set colors based on risk level
        int color;
        if (riskScore >= 0.7) {
            color = ContextCompat.getColor(requireContext(), R.color.success_green);
        } else if (riskScore >= 0.4) {
            color = ContextCompat.getColor(requireContext(), R.color.warning_yellow);
        } else {
            color = ContextCompat.getColor(requireContext(), R.color.error_red);
        }
        
        slippageText.setTextColor(color);
        riskScoreText.setTextColor(color);
    }
} 