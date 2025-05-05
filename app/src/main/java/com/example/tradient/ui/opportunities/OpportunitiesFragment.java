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
import com.example.tradient.data.model.ArbitrageCardModel;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        opportunityAdapter = new OpportunityAdapter(requireContext());
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
                    String chipText = buttonView.getText().toString();
                    if (chipText.equals("All")) {
                        currentChipFilter = "";
                    } else {
                        currentChipFilter = chipText;
                    }
                    
                    // Update the search text to match the chip selection
                    searchEditText.setText(currentChipFilter);
                    clearButton.setVisibility(currentChipFilter.isEmpty() ? View.GONE : View.VISIBLE);
                    
                    // Apply the filter
                    filterOpportunities();
                }
            });
        }
    }
    
    private void setupAdvancedFilters() {
        try {
            // Check for null views that could cause crashes
            if (advancedFilterFab == null || filterPanel == null || 
                profitRangeSlider == null || profitMinValue == null || 
                profitMaxValue == null || exchangeChipGroup == null || 
                riskLevelSlider == null || executionTimeSpinner == null || 
                volumeSlider == null || resetFiltersButton == null || 
                applyFiltersButton == null || matchingOpportunitiesText == null) {
                
                Log.e("OpportunitiesFragment", "One or more filter views are null - cannot setup advanced filters");
                return;
            }
            
            // Set up the FAB to show/hide the filter panel
            advancedFilterFab.setOnClickListener(v -> toggleFilterPanel());
            
            // Set up the profit range slider
            profitRangeSlider.setValues(0f, 50f);
            profitRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
                try {
                    List<Float> values = slider.getValues();
                    if (values != null && values.size() >= 2) {
                        minProfitPercent = values.get(0);
                        maxProfitPercent = values.get(1);
                        profitMinValue.setText(String.format(Locale.US, "%.1f%%", minProfitPercent));
                        profitMaxValue.setText(String.format(Locale.US, "%.1f%%+", maxProfitPercent));
                        updateMatchingCount();
                    }
                } catch (Exception e) {
                    Log.e("OpportunitiesFragment", "Error updating profit range: " + e.getMessage(), e);
                }
            });
            
            // Set up the exchange chips
            initializeExchangeChips();
            
            // Set up the risk level slider
            TextView riskLevelValue = null;
            TextView slippageValueText = null;
            TextView riskScoreValueText = null;
            
            try {
                View view = getView();
                if (view != null) {
                    riskLevelValue = view.findViewById(R.id.riskLevelValue);
                    slippageValueText = view.findViewById(R.id.slippageValueText);
                    riskScoreValueText = view.findViewById(R.id.riskScoreValueText);
                }
            } catch (Exception e) {
                Log.e("OpportunitiesFragment", "Error finding risk text views: " + e.getMessage(), e);
            }
            
            final TextView finalRiskLevelValue = riskLevelValue;
            final TextView finalSlippageValueText = slippageValueText;
            final TextView finalRiskScoreValueText = riskScoreValueText;
            
            if (finalRiskLevelValue != null && riskLevelSlider != null) {
                try {
                    // Set initial value
                    finalRiskLevelValue.setText(getRiskLevelName(convertRiskSliderToScore(maxRiskLevel)));
                    updateSlippageInfo(maxRiskLevel, finalSlippageValueText, finalRiskScoreValueText);
                } catch (Exception e) {
                    Log.e("OpportunitiesFragment", "Error setting initial risk values: " + e.getMessage(), e);
                }
                
                // Set up slider listener
                riskLevelSlider.addOnChangeListener((slider, value, fromUser) -> {
                    try {
                        maxRiskLevel = value;
                        double riskScore = convertRiskSliderToScore(value);
                        
                        // Update the displayed risk level name
                        if (finalRiskLevelValue != null) {
                            finalRiskLevelValue.setText(getRiskLevelName(riskScore));
                        }
                        
                        // Update slippage and risk score information
                        updateSlippageInfo(value, finalSlippageValueText, finalRiskScoreValueText);
                        
                        // Update matching count
                        updateMatchingCount();
                        
                        // Apply filter immediately if panel is visible
                        if (isFilterPanelVisible) {
                            applyAllFilters();
                        }
                    } catch (Exception e) {
                        Log.e("OpportunitiesFragment", "Error updating risk level: " + e.getMessage(), e);
                    }
                });
            }
            
            // Rest of the setup code...
            setupExecutionTimeSpinner();
            setupVolumeSlider();
            setupFilterButtons();
            
        } catch (Exception e) {
            Log.e("OpportunitiesFragment", "Critical error setting up advanced filters: " + e.getMessage(), e);
        }
    }
    
    private void setupExecutionTimeSpinner() {
        try {
            // Set up the execution time spinner
            String[] executionTimeOptions = {
                "Any time", 
                "< 1 minute", 
                "< 5 minutes", 
                "< 15 minutes", 
                "< 30 minutes"
            };
            
            if (executionTimeSpinner != null) {
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
            }
        } catch (Exception e) {
            Log.e("OpportunitiesFragment", "Error setting up execution time spinner: " + e.getMessage(), e);
        }
    }
    
    private void setupVolumeSlider() {
        try {
            // Set up the volume slider
            if (volumeSlider != null) {
                volumeSlider.addOnChangeListener((slider, value, fromUser) -> {
                    minVolume = value;
                    updateMatchingCount();
                });
            }
        } catch (Exception e) {
            Log.e("OpportunitiesFragment", "Error setting up volume slider: " + e.getMessage(), e);
        }
    }
    
    private void setupFilterButtons() {
        try {
            // Set up the button actions
            if (resetFiltersButton != null) {
                resetFiltersButton.setOnClickListener(v -> {
                    resetFilters();
                    // Apply filter after reset
                    applyAllFilters();
                    // Show message that filters were reset
                    if (matchingOpportunitiesText != null) {
                        matchingOpportunitiesText.setText("Filters reset");
                    }
                });
            }
            
            if (applyFiltersButton != null) {
                applyFiltersButton.setOnClickListener(v -> {
                    // Apply the filters
                    applyAllFilters();
                    // Close the filter panel
                    toggleFilterPanel();
                    // Show message that filters were applied
                    if (matchingOpportunitiesText != null) {
                        matchingOpportunitiesText.setText("Filters applied");
                    }
                });
            }
        } catch (Exception e) {
            Log.e("OpportunitiesFragment", "Error setting up filter buttons: " + e.getMessage(), e);
        }
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
        try {
            if (opportunities == null || opportunities.isEmpty()) {
                showEmptyState();
                return;
            }
    
            // Create a safe copy of the opportunities list
            allOpportunities = new ArrayList<>();
            for (ArbitrageOpportunity opportunity : opportunities) {
                if (opportunity != null && opportunity.getProfitPercent() > 0) {
                    allOpportunities.add(opportunity);
                }
            }
            
            if (allOpportunities.isEmpty()) {
                showEmptyState();
                return;
            }
            
            // Apply any existing filters
            try {
                filteredOpportunities = filterOpportunities();
            } catch (Exception e) {
                Log.e("OpportunitiesFragment", "Error filtering opportunities: " + e.getMessage(), e);
                filteredOpportunities = new ArrayList<>(allOpportunities); // Use all as fallback
            }
            
            // Update the adapter safely
            try {
                if (opportunityAdapter != null) {
                    opportunityAdapter.updateOpportunities(filteredOpportunities);
                    opportunityAdapter.notifyDataSetChanged();
                } else {
                    Log.e("OpportunitiesFragment", "Adapter is null, can't update opportunities");
                }
            } catch (Exception e) {
                Log.e("OpportunitiesFragment", "Error updating adapter: " + e.getMessage(), e);
            }
            
            // Hide empty state
            if (emptyStateText != null) {
                emptyStateText.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e("OpportunitiesFragment", "Critical error updating opportunities: " + e.getMessage(), e);
            if (emptyStateText != null) {
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("Error loading opportunities");
            }
        }
    }
    
    /**
     * Displays statistics about risk levels across all opportunities
     */
    private void updateRiskLevelSummary(List<ArbitrageOpportunity> opportunities) {
        // Only process if we have opportunities
        if (opportunities == null || opportunities.isEmpty()) {
            return;
        }
        
        Log.d("OpportunitiesFragment", "Calculating risk summary for " + opportunities.size() + " opportunities");
        
        // Initialize counters for each risk level
        Map<String, Integer> riskLevelCounts = new HashMap<>();
        for (RiskUtils.RiskLevel level : RiskUtils.RISK_LEVELS) {
            riskLevelCounts.put(level.name, 0);
        }
        
        // Convert opportunities to card models for pre-calculated risk values
        // Force recalculation of risk to ensure accurate counts
        Log.d("OpportunitiesFragment", "******************************************");
        Log.d("OpportunitiesFragment", "RISK DISTRIBUTION CALCULATION STARTING");
        Log.d("OpportunitiesFragment", "******************************************");
        
        for (ArbitrageOpportunity opportunity : opportunities) {
            // Create a card model to get the pre-calculated risk values - force recalculation
            com.example.tradient.data.model.ArbitrageCardModel cardModel = 
                com.example.tradient.data.model.ArbitrageCardModel.fromOpportunity(opportunity, true);
            
            if (cardModel != null) {
                String riskLevel = cardModel.getRiskLevel();
                
                // Log details for debugging
                Log.d("OpportunitiesFragment", String.format("Risk for %s: %.2f (%s) - Profit: %.2f%%, Buy: %s, Sell: %s",
                    cardModel.getTradingPair(), cardModel.getRiskScore(), riskLevel,
                    cardModel.getProfitPercent(), cardModel.getBuyExchange(), cardModel.getSellExchange()));
                
                // Increment the counter for this risk level
                Integer currentCount = riskLevelCounts.get(riskLevel);
                if (currentCount != null) {
                    riskLevelCounts.put(riskLevel, currentCount + 1);
                }
            } else {
                Log.w("OpportunitiesFragment", "Failed to create card model for opportunity: " + 
                    opportunity.getNormalizedSymbol());
            }
        }
        
        Log.d("OpportunitiesFragment", "******************************************");
        Log.d("OpportunitiesFragment", "RISK DISTRIBUTION CALCULATION COMPLETE");
        Log.d("OpportunitiesFragment", "******************************************");
        
        // Create summary text
        StringBuilder summary = new StringBuilder("Risk Summary: ");
        boolean addedAny = false;
        for (RiskUtils.RiskLevel level : RiskUtils.RISK_LEVELS) {
            Integer count = riskLevelCounts.get(level.name);
            if (count != null && count > 0) {
                if (addedAny) {
                    summary.append(", ");
                }
                summary.append(String.format("%d %s", count, level.name));
                addedAny = true;
            }
        }
            
        // Log the overall distribution
        Log.d("OpportunitiesFragment", "Risk distribution: " + riskLevelCounts);
        Log.d("OpportunitiesFragment", summary.toString());
    }
    
    /**
     * Gets the risk score from an opportunity
     * Risk scores are on a 0-1 scale where:
     * - 0.0 represents maximum risk (worst)
     * - 1.0 represents minimum risk (best)
     */
    private double getRiskScoreForOpportunity(ArbitrageOpportunity opportunity) {
        if (opportunity == null) {
            Log.e("OpportunitiesFragment", "Cannot get risk score for null opportunity");
            return 0.5; // Default medium risk
        }
        
        try {
            return RiskUtils.getRiskScore(opportunity);
        } catch (Exception e) {
            Log.e("OpportunitiesFragment", "Error calculating risk score: " + e.getMessage(), e);
            return 0.5; // Default to medium risk on error
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
        try {
            // Handle null opportunity
            if (opportunity == null) {
                Log.w("OpportunitiesFragment", "Null opportunity in advanced filters");
                return false;
            }
            
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
                
                // Skip if exchanges are null
                if (buyExchange == null || sellExchange == null) {
                    Log.w("OpportunitiesFragment", "Null exchange in " + opportunity.getNormalizedSymbol());
                    return false;
                }
                
                if (!selectedExchanges.contains(buyExchange) && !selectedExchanges.contains(sellExchange)) {
                    return false;
                }
            }
    
            // Apply risk filter if maxRiskLevel is not at maximum (which is 10)
            if (maxRiskLevel < 10.0) {
                try {
                    // Convert slider value (1-10) to risk score (0-1)
                    double maxRiskScore = convertRiskSliderToScore(maxRiskLevel);
                    
                    // Get the opportunity's risk score (already on 0-1 scale)
                    double opportunityRiskScore = 0.5; // Default to medium risk
                    try {
                        opportunityRiskScore = getRiskScoreForOpportunity(opportunity);
                    } catch (Exception e) {
                        Log.e("OpportunitiesFragment", "Error getting risk score: " + e.getMessage());
                        // Use default medium risk
                    }
                    
                    // Get the risk level name for logging
                    String riskLevel = "Medium"; // Default
                    try {
                        riskLevel = getRiskLevelName(opportunityRiskScore);
                    } catch (Exception e) {
                        Log.e("OpportunitiesFragment", "Error getting risk level name: " + e.getMessage());
                    }
                    
                    // Use safe string formatting to avoid crashes
                    String normalizedSymbol = opportunity.getNormalizedSymbol();
                    if (normalizedSymbol == null) normalizedSymbol = "unknown";
                    
                    try {
                        Log.d("OpportunitiesFragment", String.format("Checking opportunity %s - Risk Score: %.2f (%s) vs Max Risk Score: %.2f",
                            normalizedSymbol, opportunityRiskScore, riskLevel, maxRiskScore));
                    } catch (Exception e) {
                        Log.e("OpportunitiesFragment", "Error logging risk info: " + e.getMessage());
                    }
                    
                    // IMPORTANT: Higher risk score means LOWER risk (0 = highest risk, 1 = lowest risk)
                    // Only include opportunities with risk score >= maxRiskScore 
                    // (which means risk is lower than or equal to the maximum allowed risk)
                    if (opportunityRiskScore < maxRiskScore) {
                        try {
                            Log.d("OpportunitiesFragment", String.format("Filtered out %s due to high risk (%.2f < %.2f)",
                                normalizedSymbol, opportunityRiskScore, maxRiskScore));
                        } catch (Exception e) {
                            Log.e("OpportunitiesFragment", "Error logging filtered opportunity: " + e.getMessage());
                        }
                        return false;
                    }
                } catch (Exception e) {
                    Log.e("OpportunitiesFragment", "Error in risk filtering: " + e.getMessage());
                    // On error, include the opportunity (don't filter it out)
                    // This prevents crashes while still maintaining most filtering functionality
                }
            }
    
            return true;
        } catch (Exception e) {
            Log.e("OpportunitiesFragment", "Critical error in filter matching: " + e.getMessage(), e);
            return true; // On critical error, include the opportunity rather than crash
        }
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

    /**
     * Convert the risk slider value (1-10) to a risk score (0-1)
     * Lower slider value = higher risk (lower risk score)
     */
    private double convertRiskSliderToScore(double sliderValue) {
        try {
            // Ensure slider value is within valid range
            sliderValue = Math.max(1.0, Math.min(10.0, sliderValue));
            
            // Map slider value 1-10
            // to risk score 0.1-0.9 (reversed so higher slider = lower risk)
            // The slider is 1-10, but we want a normalized value of 0.1-0.9
            double normalizedScore = (11.0 - sliderValue) / 10.0;  // 10->0.1, 1->1.0
            
            // Ensure the result is in the valid range
            return Math.max(0.1, Math.min(1.0, normalizedScore));
        } catch (Exception e) {
            Log.e("OpportunitiesFragment", "Error converting slider value: " + e.getMessage(), e);
            return 0.5; // Default to medium risk on error
        }
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
        
        // Set colors based on consistent risk thresholds
        int color;
        if (riskScore >= 0.6) { // Low and Very Low risk
            color = ContextCompat.getColor(requireContext(), R.color.success_green);
        } else if (riskScore >= 0.3) { // Moderate, Balanced risk
            color = ContextCompat.getColor(requireContext(), R.color.warning_yellow);
        } else { // High, Very High, Extreme risk
            color = ContextCompat.getColor(requireContext(), R.color.error_red);
        }
        
        slippageText.setTextColor(color);
        riskScoreText.setTextColor(color);
    }

    /**
     * Gets the risk level name for a given risk score
     */
    private String getRiskLevelName(double riskScore) {
        try {
            // Ensure risk score is in valid range
            riskScore = Math.max(0.0, Math.min(1.0, riskScore));
            return RiskUtils.getRiskLevelName(riskScore);
        } catch (Exception e) {
            Log.e("OpportunitiesFragment", "Error getting risk level name: " + e.getMessage(), e);
            return "Medium"; // Default to medium on error
        }
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
     * Filter opportunities based on all applied filters (search, chips, advanced filters)
     * @return List of filtered opportunities
     */
    private List<ArbitrageOpportunity> filterOpportunities() {
        if (allOpportunities == null || allOpportunities.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Create a filtered list based on all filters
        List<ArbitrageOpportunity> filteredList = new ArrayList<>();
        
        for (ArbitrageOpportunity opportunity : allOpportunities) {
            try {
                // Skip null opportunities
                if (opportunity == null) {
                    continue;
                }
                
                // Skip opportunities with negative profit
                if (opportunity.getProfitPercent() <= 0) {
                    continue;
                }
                
                // Apply search filter if any
                if (!currentSearchQuery.isEmpty()) {
                    String symbol = opportunity.getNormalizedSymbol() != null ? 
                        opportunity.getNormalizedSymbol().toLowerCase(Locale.getDefault()) : "";
                    String buyExchange = opportunity.getExchangeBuy() != null ? 
                        opportunity.getExchangeBuy().toLowerCase(Locale.getDefault()) : "";
                    String sellExchange = opportunity.getExchangeSell() != null ? 
                        opportunity.getExchangeSell().toLowerCase(Locale.getDefault()) : "";
                    
                    if (!symbol.contains(currentSearchQuery) && 
                        !buyExchange.contains(currentSearchQuery) && 
                        !sellExchange.contains(currentSearchQuery)) {
                        continue;
                    }
                }
                
                // Apply chip filter if any
                if (!currentChipFilter.isEmpty()) {
                    String symbol = opportunity.getNormalizedSymbol() != null ? 
                        opportunity.getNormalizedSymbol().toUpperCase(Locale.getDefault()) : "";
                    if (!symbol.contains(currentChipFilter)) {
                        continue;
                    }
                }
                
                // Apply advanced filters
                if (!opportunityMatchesAdvancedFilters(opportunity)) {
                    continue;
                }
                
                // If it passed all filters, add to the filtered list
                filteredList.add(opportunity);
            } catch (Exception e) {
                Log.e("OpportunitiesFragment", "Error filtering opportunity: " + e.getMessage(), e);
                // Continue with next opportunity on error
            }
        }
        
        return filteredList;
    }
} 