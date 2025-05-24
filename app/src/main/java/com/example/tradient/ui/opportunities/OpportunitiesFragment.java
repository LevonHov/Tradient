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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
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

import com.example.tradient.ui.filter.ArbitrageFilterBottomSheet;
import com.example.tradient.ui.filter.FilterCriteria;
import com.example.tradient.ui.filter.FilterBottomSheet;
import com.example.tradient.ui.filter.FilterDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Remove reference to Filter class
// import com.example.tradient.model.Filter;

public class OpportunitiesFragment extends Fragment implements FilterBottomSheet.FilterAppliedListener {

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
    private String currentChipFilter = ""; // For the topmost quick symbol filter chips

    // --- BEGIN State for Inline Advanced Filter Panel ---
    // These are for the UI elements directly in fragment_opportunities.xml's advanced panel.
    // If this panel is secondary or a way to pre-fill the BottomSheet, their primary role isn't direct filtering anymore.
    private float inlineMinProfitPercent = 0f;
    private float inlineMaxProfitPercent = 50f;
    private Set<String> inlineSelectedExchanges = new HashSet<>();
    private float inlineMaxRiskLevelSliderValue = 5f; // Value from the inline risk slider (1-10)
    private int inlineMaxExecutionTimeMinutes = -1;
    private float inlineMinVolume = 1f;
    private boolean isInlineFilterPanelVisible = false;
    // --- END State for Inline Advanced Filter Panel ---

    private FloatingActionButton filterFab; // FAB to open the BottomSheet
    // This currentAppliedFilterCriteria is the single source of truth for applied advanced filters from the BottomSheet.
    private ArbitrageFilterBottomSheet.FilterCriteria currentAppliedFilterCriteria;

    // Define Risk Level constants to match BottomSheet for clarity in mapping
    private static final String RISK_LOW = "Low";
    private static final String RISK_MEDIUM = "Medium";
    private static final String RISK_HIGH = "High";
    private static final String RISK_VERY_HIGH = "Very High";
    // Default risk level string from FilterCriteria if no explicit selection or for "any"
    private static final String DEFAULT_RISK_LEVEL_FROM_CRITERIA = "Medium"; // Matches FilterCriteria's default

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_opportunities, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeBaseViews(view);
        initializeInlineAdvancedFilterViews(view); // Initializes UI elements of the inline panel

        // Initialize with a default (empty/non-active) filter criteria
        currentAppliedFilterCriteria = new ArbitrageFilterBottomSheet.FilterCriteria();

        // Register for filter dialog results
        getParentFragmentManager().setFragmentResultListener("filterRequestKey", 
            getViewLifecycleOwner(), 
            (requestKey, result) -> {
                Log.d("OpportunitiesFragment", "Received filter dialog result");
                
                FilterCriteria newCriteria = result.getParcelable("filterCriteria");
                if (newCriteria != null) {
                    Log.d("OpportunitiesFragment", "Converting FilterCriteria to ArbitrageFilterBottomSheet.FilterCriteria");
                    
                    // Create new ArbitrageFilterBottomSheet.FilterCriteria
                    ArbitrageFilterBottomSheet.FilterCriteria convertedCriteria = new ArbitrageFilterBottomSheet.FilterCriteria();
                    
                    // Set profit percentages
                    convertedCriteria.setMinProfitPercentage((double)newCriteria.getMinProfitPercent());
                    convertedCriteria.setMaxProfitPercentage((double)newCriteria.getMaxProfitPercent());
                    
                    Log.d("OpportunitiesFragment", String.format("Setting profit range: %.2f - %.2f",
                        newCriteria.getMinProfitPercent(), newCriteria.getMaxProfitPercent()));
                    
                    // Convert risk levels
                    Set<String> riskLevels = newCriteria.getRiskLevels();
                    if (riskLevels != null && !riskLevels.isEmpty()) {
                        String riskLevel = riskLevels.iterator().next();
                        Log.d("OpportunitiesFragment", "Setting risk level: " + riskLevel);
                        convertedCriteria.setRiskLevel(riskLevel);
                    } else {
                        Log.d("OpportunitiesFragment", "No risk level selected, using Any");
                        convertedCriteria.setRiskLevel(ArbitrageFilterBottomSheet.FilterCriteria.RISK_LEVEL_ANY);
                    }
                    
                    // Convert exchanges
                    Set<String> exchanges = newCriteria.getExchanges();
                    if (exchanges != null && !exchanges.isEmpty()) {
                        List<String> exchangeList = new ArrayList<>(exchanges);
                        Log.d("OpportunitiesFragment", "Setting exchanges: " + exchangeList);
                        convertedCriteria.setSourceExchanges(exchangeList);
                        convertedCriteria.setDestinationExchanges(exchangeList);
                    } else {
                        Log.d("OpportunitiesFragment", "No exchanges selected");
                        convertedCriteria.setSourceExchanges(new ArrayList<>());
                        convertedCriteria.setDestinationExchanges(new ArrayList<>());
                    }
                    
                    // Update current criteria and apply filters
                    currentAppliedFilterCriteria = convertedCriteria;
                    Log.d("OpportunitiesFragment", "Updated filter criteria: " + currentAppliedFilterCriteria);
                    
                    applyFiltersAndSort();
                    updateFilterFabAppearance();
                    updateFilterInfo();
                } else {
                    Log.w("OpportunitiesFragment", "Received null filter criteria from dialog");
                }
            });

        opportunitiesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        opportunityAdapter = new OpportunityAdapter(requireContext());
        opportunitiesRecyclerView.setAdapter(opportunityAdapter);

        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText("Loading opportunities..."); // Using string resource 

        setupSearchFunctionality();
        setupTopFilterChips();
        setupInlineAdvancedFilterPanelListeners(); // Sets up listeners for the inline panel UI

        ExchangeRepository repository = new ExchangeRepository(requireContext());
        ViewModelFactory factory = new ViewModelFactory(repository);
        viewModel = new ViewModelProvider(requireActivity(), factory).get(ArbitrageViewModel.class);
        observeViewModel();
        viewModel.initialize();

        filterFab = view.findViewById(R.id.filter_fab);
        if (filterFab != null) {
            filterFab.setOnClickListener(v -> showFilterBottomSheet());
        }
        updateFilterFabAppearance(); // Initial appearance based on default criteria
        updateFilterInfo(); // Initial update for the filter info chips
    }

    private void initializeBaseViews(View view) {
        opportunitiesRecyclerView = view.findViewById(R.id.opportunities_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        searchEditText = view.findViewById(R.id.searchEditText);
        clearButton = view.findViewById(R.id.clearButton);
        filterButton = view.findViewById(R.id.filterButton); // This might be the FAB or another button
        filterChipGroup = view.findViewById(R.id.filterChipGroup); // Top quick filter chips
        filterChipsScrollView = view.findViewById(R.id.filterChipsScrollView);
        matchingOpportunitiesText = view.findViewById(R.id.matchingOpportunitiesText);
    }

    private void initializeInlineAdvancedFilterViews(View view) {
        advancedFilterFab = view.findViewById(R.id.advancedFilterFab); // FAB for inline panel toggle
        filterPanel = view.findViewById(R.id.filterPanel); // The inline panel itself
        profitRangeSlider = view.findViewById(R.id.profitRangeSlider);
        profitMinValue = view.findViewById(R.id.profitMinValue);
        profitMaxValue = view.findViewById(R.id.profitMaxValue);
        exchangeChipGroup = view.findViewById(R.id.exchangeChipGroup); // Inline panel exchanges
        riskLevelSlider = view.findViewById(R.id.riskLevelSlider);
        executionTimeSpinner = view.findViewById(R.id.executionTimeSpinner);
        volumeSlider = view.findViewById(R.id.volumeSlider);
        resetFiltersButton = view.findViewById(R.id.resetFiltersButton); // For inline panel
        applyFiltersButton = view.findViewById(R.id.applyFiltersButton); // For inline panel
    }

    private void setupTopFilterChips() {
        if (filterChipGroup == null) return;
        for (int i = 0; i < filterChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) filterChipGroup.getChildAt(i);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    String chipText = buttonView.getText().toString();
                    currentChipFilter = chipText.equals("All") ? "" : chipText;
                    searchEditText.setText(currentChipFilter);
                    applyFiltersAndSort(); // Central method to apply all filters
                    } else {
                     // If a chip is unchecked, and it was the active currentChipFilter, clear it
                    if (buttonView.getText().toString().equals(currentChipFilter)) {
                        currentChipFilter = "";
                        // Optionally clear searchEditText if it was auto-filled by this chip
                        // searchEditText.setText(""); 
                        applyFiltersAndSort();
                    }
                }
            });
        }
    }
    
    private void setupInlineAdvancedFilterPanelListeners() {
        if (filterPanel == null && advancedFilterFab == null) return; 

        if (advancedFilterFab != null) {
            advancedFilterFab.setOnClickListener(v -> toggleInlineFilterPanel());
        }
        if (filterPanel == null) {
            Log.w("OpportunitiesFragment", "filterPanel is null, cannot set up inline listeners.");
            return;
        }

        profitRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            if (values.size() >= 2) {
                inlineMinProfitPercent = values.get(0);
                inlineMaxProfitPercent = values.get(1);
                if (profitMinValue != null) profitMinValue.setText(String.format(Locale.US, "%.1f%%", inlineMinProfitPercent));
                if (profitMaxValue != null) profitMaxValue.setText(String.format(Locale.US, "%.1f%%+", inlineMaxProfitPercent));
                // Live updates from inline panel can be contentious. 
                // For now, let's NOT have sliders immediately apply filters to avoid too many refreshes.
                // User must click the inline panel's "Apply Filters" button.
            }
        });

        initializeInlineExchangeChips();

        riskLevelSlider.addOnChangeListener((slider, value, fromUser) -> {
            inlineMaxRiskLevelSliderValue = value;
            View rootView = getView();
            if (rootView != null) {
                TextView riskLevelValueText = rootView.findViewById(R.id.riskLevelValue);
                if (riskLevelValueText != null) riskLevelValueText.setText(getRiskLevelName(convertRiskSliderToScore(value)));
            }
             // No immediate filter application here either.
        });

        executionTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // TODO: Define R.array.execution_time_values_minutes in arrays.xml (e.g., <integer-array name="execution_time_values_minutes"><item>-1</item><item>1</item>...</integer-array>)
                int[] minutesMap = {-1, 1, 5, 15, 30}; //getResources().getIntArray(R.array.execution_time_values_minutes); // Example resource
                if (position >= 0 && position < minutesMap.length) {
                    inlineMaxExecutionTimeMinutes = minutesMap[position];
                }
                 // No immediate filter application.
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { 
                inlineMaxExecutionTimeMinutes = -1; 
                // No immediate filter application.
            }
        });

        volumeSlider.addOnChangeListener((slider, value, fromUser) -> {
            inlineMinVolume = value;
            // No immediate filter application.
        });

        resetFiltersButton.setOnClickListener(v -> {
            resetAllFilters();
        });

        applyFiltersButton.setOnClickListener(v -> {
            // Update currentAppliedFilterCriteria with values from the inline panel
            if (currentAppliedFilterCriteria == null) {
                currentAppliedFilterCriteria = new ArbitrageFilterBottomSheet.FilterCriteria();
            }

            currentAppliedFilterCriteria.setMinProfitPercentage(inlineMinProfitPercent);
            currentAppliedFilterCriteria.setMaxProfitPercentage(inlineMaxProfitPercent);
            
            // Convert inline risk slider value (1-10) to a risk level string for FilterCriteria
            currentAppliedFilterCriteria.setRiskLevel(mapSliderValueToFilterCriteriaRiskLevel(inlineMaxRiskLevelSliderValue));
            
            // Max Slippage: Inline panel does not have a slippage slider.
            // So, currentAppliedFilterCriteria.maxSlippagePercentage remains as it was (from BS or default).
            // If we wanted inline panel to reset it, we'd do:
            // currentAppliedFilterCriteria.setMaxSlippagePercentage(FilterCriteria.DEFAULT_MAX_SLIPPAGE);

            // Execution Time: Convert inlineMaxExecutionTimeMinutes to seconds for FilterCriteria
            if (inlineMaxExecutionTimeMinutes == -1) { // "Any time"
                // TODO: Ensure FilterCriteria.DEFAULT_MIN_EXECUTION_TIME is defined
                currentAppliedFilterCriteria.setMinExecutionTime(0); 
                // TODO: Ensure FilterCriteria.DEFAULT_MAX_EXECUTION_TIME is defined
                currentAppliedFilterCriteria.setMaxExecutionTime(300);
            } else {
                currentAppliedFilterCriteria.setMinExecutionTime(0); // Assuming inline only sets max
                currentAppliedFilterCriteria.setMaxExecutionTime(inlineMaxExecutionTimeMinutes * 60.0); // Convert minutes to seconds
            }

            // Exchanges: Use inlineSelectedExchanges for both source and destination for simplicity from inline panel.
            // FilterCriteria expects List<String>. Ensure inlineSelectedExchanges is compatible.
            currentAppliedFilterCriteria.setSourceExchanges(new ArrayList<>(inlineSelectedExchanges));
            currentAppliedFilterCriteria.setDestinationExchanges(new ArrayList<>(inlineSelectedExchanges));
            
            // Volume: ArbitrageFilterBottomSheet.FilterCriteria does not have a dedicated volume field.
            // The filtering for inlineMinVolume happens in the fallback part of opportunityMatchesAdvancedFilters.
            // If volume were part of FilterCriteria, we'd set it here:
            // currentAppliedFilterCriteria.setMinVolume(inlineMinVolume);

            // Cryptocurrencies: Inline panel doesn't have crypto selection.
            // currentAppliedFilterCriteria.cryptocurrencies remains as is.

            applyFiltersAndSort(); 
            updateFilterFabAppearance();
            updateFilterInfo();
            // Optional: Close the inline panel after applying
            // if (isInlineFilterPanelVisible) {
            // toggleInlineFilterPanel(); 
            // }
            Log.d("OpportunitiesFragment", "Applied inline panel filters to currentAppliedFilterCriteria: " + currentAppliedFilterCriteria.toString());
        });
    }
    
    private void initializeInlineExchangeChips() {
        if (exchangeChipGroup == null) return;
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
                        inlineSelectedExchanges.clear();
                    }
                } else {
                    // Uncheck "All Exchanges" chip
                    Chip allChip = exchangeChipGroup.findViewById(R.id.chip_exchange_all);
                    if (allChip.isChecked()) {
                        allChip.setChecked(false);
                    }
                    
                    if (isChecked) {
                        inlineSelectedExchanges.add(exchange);
                    } else {
                        inlineSelectedExchanges.remove(exchange);
                    }
                    
                    // If no exchanges selected, check "All Exchanges"
                    if (inlineSelectedExchanges.isEmpty()) {
                        allChip.setChecked(true);
                    }
                }
                if (isInlineFilterPanelVisible) applyFiltersAndSort();
            });
        }
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
            showFilterDialog();
        });
    }
    
    private void showFilterDialog() {
        try {
            Log.d("OpportunitiesFragment", "showFilterDialog: Creating new FilterDialogFragment");
            FilterDialogFragment filterDialog = new FilterDialogFragment();
            
            // Convert ArbitrageFilterBottomSheet.FilterCriteria to FilterCriteria
            if (currentAppliedFilterCriteria != null) {
                Log.d("OpportunitiesFragment", "showFilterDialog: Converting current filter criteria");
                
                // Create new FilterCriteria instance
                FilterCriteria dialogCriteria = new FilterCriteria();
                
                // Set profit percentages
                dialogCriteria.setMinProfitPercent((float)currentAppliedFilterCriteria.getMinProfitPercentage());
                dialogCriteria.setMaxProfitPercent((float)currentAppliedFilterCriteria.getMaxProfitPercentage());
                
                Log.d("OpportunitiesFragment", String.format("showFilterDialog: Setting profit range: %.2f - %.2f",
                    currentAppliedFilterCriteria.getMinProfitPercentage(),
                    currentAppliedFilterCriteria.getMaxProfitPercentage()));
                
                // Convert risk level to risk levels set
                Set<String> riskLevels = new HashSet<>();
                String currentRisk = currentAppliedFilterCriteria.getRiskLevel();
                if (currentRisk != null && !ArbitrageFilterBottomSheet.FilterCriteria.RISK_LEVEL_ANY.equals(currentRisk)) {
                    riskLevels.add(currentRisk);
                    Log.d("OpportunitiesFragment", "showFilterDialog: Adding risk level: " + currentRisk);
                } else {
                    Log.d("OpportunitiesFragment", "showFilterDialog: No specific risk level selected");
                }
                dialogCriteria.setRiskLevels(riskLevels);
                
                // Convert exchanges - combine source and destination exchanges
                Set<String> exchanges = new HashSet<>();
                
                // Add source exchanges if any
                List<String> sourceExchanges = currentAppliedFilterCriteria.getSourceExchanges();
                if (sourceExchanges != null && !sourceExchanges.isEmpty()) {
                    exchanges.addAll(sourceExchanges);
                    Log.d("OpportunitiesFragment", "showFilterDialog: Adding source exchanges: " + sourceExchanges);
                }
                
                // Add destination exchanges if any
                List<String> destExchanges = currentAppliedFilterCriteria.getDestinationExchanges();
                if (destExchanges != null && !destExchanges.isEmpty()) {
                    exchanges.addAll(destExchanges);
                    Log.d("OpportunitiesFragment", "showFilterDialog: Adding destination exchanges: " + destExchanges);
                }
                
                dialogCriteria.setExchanges(exchanges);
                
                // Create bundle and set arguments
                Bundle args = new Bundle();
                args.putParcelable("current_filter", dialogCriteria);
                filterDialog.setArguments(args);
                
                Log.d("OpportunitiesFragment", "showFilterDialog: Created dialog criteria: " + dialogCriteria);
            } else {
                Log.d("OpportunitiesFragment", "showFilterDialog: No current filter criteria, using defaults");
            }
            
            filterDialog.show(getParentFragmentManager(), "FilterDialog");
            Log.d("OpportunitiesFragment", "showFilterDialog: Dialog shown");
            
        } catch (Exception e) {
            Log.e("OpportunitiesFragment", "Error showing filter dialog: " + e.getMessage(), e);
            Log.e("OpportunitiesFragment", "Stack trace:", e);
            Toast.makeText(requireContext(), "Could not open filter dialog", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void toggleInlineFilterPanel() {
        isInlineFilterPanelVisible = !isInlineFilterPanelVisible;
        filterPanel.setVisibility(isInlineFilterPanelVisible ? View.VISIBLE : View.GONE);
        
        // Rotate the FAB icon to indicate open/closed state
        advancedFilterFab.setRotation(isInlineFilterPanelVisible ? 45 : 0);
    }
    
    private void resetAllFilters() {
        // Reset BottomSheet Filter Criteria to default
        currentAppliedFilterCriteria = new ArbitrageFilterBottomSheet.FilterCriteria();

        // Reset Inline Panel UI and corresponding state variables
        if (profitRangeSlider != null) { 
            profitRangeSlider.setValues(0f, 50f);
            this.inlineMinProfitPercent = 0f;
            this.inlineMaxProfitPercent = 50f;
            // TODO: Define R.string.profit_min_default_text in strings.xml
            if(profitMinValue != null) profitMinValue.setText("0%"); 
            // TODO: Define R.string.profit_max_default_text in strings.xml
            if(profitMaxValue != null) profitMaxValue.setText("50%+");
        }
        if (exchangeChipGroup != null) {
            this.inlineSelectedExchanges.clear();
            for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) exchangeChipGroup.getChildAt(i);
                // TODO: Ensure R.id.chip_exchange_all_inline exists in your inline panel layout
                if (chip != null) chip.setChecked(chip.getId() == R.id.chip_exchange_all); 
            }
        }
        if (riskLevelSlider != null) {
            riskLevelSlider.setValue(5f); // Default slider position
            this.inlineMaxRiskLevelSliderValue = 5f;
            View rootView = getView();
            if (rootView != null) {
                TextView riskLevelValueText = rootView.findViewById(R.id.riskLevelValue);
                if (riskLevelValueText != null) riskLevelValueText.setText(getRiskLevelName(convertRiskSliderToScore(this.inlineMaxRiskLevelSliderValue)));
            }
        }
        if (executionTimeSpinner != null) {
            executionTimeSpinner.setSelection(0); // "Any time"
            this.inlineMaxExecutionTimeMinutes = -1;
        }
        if (volumeSlider != null) {
            volumeSlider.setValue(1f); // Default volume slider position
            this.inlineMinVolume = 1f;
        }
        
        // Reset simple search and top chip filter
        if (searchEditText != null) searchEditText.setText("");
        this.currentSearchQuery = "";
        // For the top filterChipGroup (quick symbol filters), we should clear its check, not remove all views if they are static.
        if (filterChipGroup != null && filterChipGroup.isSingleSelection()) { 
            filterChipGroup.clearCheck();
        }
        this.currentChipFilter = "";

        applyFiltersAndSort(); // Re-apply all (now reset) filters
        updateFilterFabAppearance(); 
        updateFilterInfo(); // Update the top summary chips

        if (matchingOpportunitiesText != null) {
            // TODO: Define R.string.filters_reset_text in strings.xml
             matchingOpportunitiesText.setText("Filters Reset"); 
        }
    }
    
    private void applyFiltersAndSort() {
        filteredOpportunities = filterOpportunities(); // Correctly call the reinstated method
        sortOpportunitiesByProfit(filteredOpportunities, false);
        if (opportunityAdapter != null) { // Add null check for adapter
            opportunityAdapter.updateOpportunities(filteredOpportunities);
            opportunityAdapter.notifyDataSetChanged(); // Consider more specific notify calls if performance is an issue
        }
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
                filteredOpportunities = filterOpportunities(); // Correctly call the reinstated method
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
        if (opportunity == null) {
            Log.w("OpportunitiesFragment", "Null opportunity in advanced filters");
            return false;
        }

        // Apply simple text search (applies to all cases, before advanced filters)
        if (!currentSearchQuery.isEmpty()) {
            String symbol = opportunity.getNormalizedSymbol() != null ? opportunity.getNormalizedSymbol().toLowerCase(Locale.getDefault()) : "";
            String buyEx = opportunity.getExchangeBuy() != null ? opportunity.getExchangeBuy().toLowerCase(Locale.getDefault()) : "";
            String sellEx = opportunity.getExchangeSell() != null ? opportunity.getExchangeSell().toLowerCase(Locale.getDefault()) : "";
            if (!symbol.contains(currentSearchQuery) && !buyEx.contains(currentSearchQuery) && !sellEx.contains(currentSearchQuery)) {
                return false;
            }
        }

        // Apply top-level quick chip filter (applies to all cases, before advanced filters)
        if (!currentChipFilter.isEmpty()) {
            String symbol = opportunity.getNormalizedSymbol() != null ? opportunity.getNormalizedSymbol().toUpperCase(Locale.getDefault()) : "";
            // Assuming currentChipFilter is also uppercase or comparison is case-insensitive if needed.
            if (!symbol.contains(currentChipFilter)) {
                return false;
            }
        }

        // Skip non-positive profit opportunities early (applies to all advanced filtering strategies)
        if (opportunity.getProfitPercent() <= 0) {
            return false;
        }

        // Check if advanced filters from ArbitrageFilterBottomSheet are active and should be used
        if (currentAppliedFilterCriteria != null && currentAppliedFilterCriteria.hasActiveFilters()) {
            // Profit Percentage Filter
            if (opportunity.getProfitPercent() < currentAppliedFilterCriteria.getMinProfitPercentage() ||
                (currentAppliedFilterCriteria.getMaxProfitPercentage() < 50.0f && // 50.0f is "Any" or max
                 opportunity.getProfitPercent() > currentAppliedFilterCriteria.getMaxProfitPercentage())) {
                return false;
            }

            // Risk Level Filter
            String criteriaRiskLevel = currentAppliedFilterCriteria.getRiskLevel();
            // TODO: Ensure FilterCriteria.RISK_LEVEL_ANY is defined
            if (criteriaRiskLevel != null && !"Any".equals(criteriaRiskLevel)) { 
                double oppRiskScore = getRiskScoreForOpportunity(opportunity); // Assumes 0.0 (high risk) to 1.0 (low risk)
                boolean match = false;
                // TODO: Ensure FilterCriteria.RISK_LEVEL_LOW is defined
                if ("Low".equals(criteriaRiskLevel) && oppRiskScore >= 0.75) match = true;
                // TODO: Ensure FilterCriteria.RISK_LEVEL_MEDIUM is defined
                else if ("Medium".equals(criteriaRiskLevel) && oppRiskScore >= 0.4 && oppRiskScore < 0.75) match = true;
                // TODO: Ensure FilterCriteria.RISK_LEVEL_HIGH is defined
                else if ("High".equals(criteriaRiskLevel) && oppRiskScore >= 0.1 && oppRiskScore < 0.4) match = true;
                // TODO: Ensure FilterCriteria.RISK_LEVEL_VERY_HIGH is defined
                else if ("Very High".equals(criteriaRiskLevel) && oppRiskScore < 0.1) match = true;
                if (!match) return false;
            }

            // Max Slippage Filter (Criteria stores as percentage, e.g., 1.0 for 1%)
            // Opportunity stores as decimal, e.g., 0.01 for 1%
            if (currentAppliedFilterCriteria.getMaxSlippagePercentage() < 2.0f) { // 2.0f is "Any" or max
                 if (opportunity.getTotalSlippagePercentage() > (currentAppliedFilterCriteria.getMaxSlippagePercentage() / 100.0)) {
                    return false;
                }
            }


            // Execution Time Filter (Criteria stores min/max in seconds)
            // Opportunity stores estimatedTimeMinutes
            double oppExecTimeSeconds = opportunity.getEstimatedTimeMinutes() * 60.0;
            if (currentAppliedFilterCriteria.getMaxExecutionTime() < 300) { // 300s (5 min) is "Any" from criteria
                if (oppExecTimeSeconds < currentAppliedFilterCriteria.getMinExecutionTime() ||
                    oppExecTimeSeconds > currentAppliedFilterCriteria.getMaxExecutionTime()) {
                    return false;
                }
            }


            // Source Exchanges Filter
            List<String> sourceExchanges = currentAppliedFilterCriteria.getSourceExchanges();
            if (sourceExchanges != null && !sourceExchanges.isEmpty()) { // Empty list means "Any"
                if (!sourceExchanges.contains(opportunity.getExchangeBuy())) {
                    return false;
                }
            }

            // Destination Exchanges Filter
            List<String> destExchanges = currentAppliedFilterCriteria.getDestinationExchanges();
            if (destExchanges != null && !destExchanges.isEmpty()) { // Empty list means "Any"
                if (!destExchanges.contains(opportunity.getExchangeSell())) {
                    return false;
                }
            }
            
            // Volume is not explicitly in currentAppliedFilterCriteria from ArbitrageFilterBottomSheet.
            // If it were, it would be handled here.

            return true; // Passed all active filters from currentAppliedFilterCriteria
        }
        // Fallback to Inline Panel Filters ONLY IF currentAppliedFilterCriteria is null or not active.
        // This logic assumes that if the BottomSheet was used and resulted in "no active filters",
        // then we respect that and don't fall back to the inline panel's last state unless explicitly desired.
        // To make the inline panel truly independent when the bottom sheet has no active filters,
        // one might add a specific check here like: "if (isInlineFilterPanelVisible && !currentAppliedFilterCriteria.hasActiveFilters())"
        // For now, this structure prioritizes BottomSheet criteria if present and active.
        else {
            // Profit filter (inline)
            if (opportunity.getProfitPercent() < inlineMinProfitPercent || (inlineMaxProfitPercent < 50.0f && opportunity.getProfitPercent() > inlineMaxProfitPercent)) {
                return false;
            }

            // Exchanges filter (inline)
            if (!inlineSelectedExchanges.isEmpty()) {
                if (opportunity.getExchangeBuy() == null || opportunity.getExchangeSell() == null ||
                    (!inlineSelectedExchanges.contains(opportunity.getExchangeBuy()) && !inlineSelectedExchanges.contains(opportunity.getExchangeSell()))) {
                    return false;
                }
            }

            // Risk filter (inline)
            if (inlineMaxRiskLevelSliderValue < 10.0f) { // 10.0f on slider means "any risk"
                try {
                    double minAcceptableRiskScore = convertRiskSliderToScore(inlineMaxRiskLevelSliderValue);
                    if (getRiskScoreForOpportunity(opportunity) < minAcceptableRiskScore) {
                        return false;
                    }
                } catch (Exception e) {
                    Log.e("OpportunitiesFragment", "Error in inline risk filtering: " + e.getMessage());
                }
            }

            // Execution Time filter (inline)
            if (inlineMaxExecutionTimeMinutes != -1) { // -1 indicates "any time"
                if (opportunity.getEstimatedTimeMinutes() > inlineMaxExecutionTimeMinutes) {
                    return false;
                }
            }

            // Volume filter (inline)
            if (opportunity.getVolume() < inlineMinVolume) { // Assumes inlineMinVolume from slider
                return false;
            }
            
            return true; // Passed all active inline filters (or no inline filters were restrictive)
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
     * Shows the filter bottom sheet dialog
     */
    private void showFilterBottomSheet() {
        if (getActivity() != null && isAdded()) {
            // Ensure currentAppliedFilterCriteria is not null before passing to BottomSheet
            if (currentAppliedFilterCriteria == null) {
                currentAppliedFilterCriteria = new ArbitrageFilterBottomSheet.FilterCriteria();
            }
            
            // Decision: If the inline panel is visible AND has been interacted with (i.e., its state differs from the currentAppliedFilterCriteria),
            // should we pre-fill the BottomSheet with the inline panel's values?
            // For now, we pass currentAppliedFilterCriteria as is. The user can explicitly use the inline panel's "Apply" button
            // to update currentAppliedFilterCriteria if they want those values reflected in the BottomSheet.
            
            ArbitrageFilterBottomSheet bottomSheet = ArbitrageFilterBottomSheet.newInstance(currentAppliedFilterCriteria);
            bottomSheet.setFilterListener(new ArbitrageFilterBottomSheet.FilterListener() {
                @Override
                public void onFiltersApplied(ArbitrageFilterBottomSheet.FilterCriteria filters) {
                    if (filters != null) {
                        currentAppliedFilterCriteria = filters; // This is the new source of truth
                        applyFiltersAndSort(); 
                        updateFilterFabAppearance();
                        updateFilterInfo(); // Update summary based on new criteria
                        Log.d("OpportunitiesFragment", "BottomSheet Filters applied: " + filters.toString());
                    } else {
                        Log.d("OpportunitiesFragment", "Received null filters from BottomSheet");
                    }
                }
            });
            bottomSheet.show(getParentFragmentManager(), bottomSheet.getTag());
        }
    }
    
    /**
     * Callback when filters are applied
     */
    @Override
    public void onFiltersApplied(FilterCriteria filter) {
        // This is the callback from the old FilterBottomSheet.FilterAppliedListener
        // It should not be confused with the ArbitrageFilterBottomSheet.FilterListener
        Log.d("OpportunitiesFragment", "onFiltersApplied (Deprecated Listener - FilterCriteria): " + filter.toString());
        // If this is still somehow active and intended, you need to decide how it interacts
        // with currentAppliedFilterCriteria from ArbitrageFilterBottomSheet.
    }
    
    /**
     * Updates the filter FAB to show if filters are active
     */
    private void updateFilterFabAppearance() {
        if (filterFab == null) return;
        if (currentAppliedFilterCriteria != null && currentAppliedFilterCriteria.hasActiveFilters()) {
            filterFab.setImageResource(R.drawable.ic_filter_active);
            // Consider adding a badge or visual cue if specific types of filters are active
        } else {
            filterFab.setImageResource(R.drawable.ic_filter); // Default/inactive icon
        }
    }
    
    /**
     * Applies the current filters to the data
     */
    private void applyFilters() { // This method is called by the old FilterBottomSheet.FilterAppliedListener
                                 // We should consolidate to applyFiltersAndSort()
        Log.w("OpportunitiesFragment", "applyFilters() called - check if this is from legacy listener");
        applyFiltersAndSort();
    }

    private void updateFilterInfo() {
        // This method updates the top quick filter display area (filterChipGroup, not the inline panel)
        // to show a summary of active ArbitrageFilterBottomSheet criteria.
        if (getContext() == null || getView() == null) return; // Need context for Chips and view for findViewById
        
        // Get the filter summary scroll view
        HorizontalScrollView filterSummaryScrollView = getView().findViewById(R.id.filter_summary_scroll_view);
        if (filterSummaryScrollView == null) { 
            Log.w("OpportunitiesFragment", "filterSummaryScrollView is null in updateFilterInfo.");
        }

        ChipGroup activeFiltersSummaryChipGroup = getView().findViewById(R.id.filter_chip_group_summary_placeholder);

        if (activeFiltersSummaryChipGroup == null) {
            Log.w("OpportunitiesFragment", "ChipGroup R.id.filter_chip_group_summary_placeholder not found. Filter summary will not be displayed.");
            if (filterSummaryScrollView != null) filterSummaryScrollView.setVisibility(View.GONE); 
            return; 
        }
        
        activeFiltersSummaryChipGroup.removeAllViews();

        if (currentAppliedFilterCriteria != null && currentAppliedFilterCriteria.hasActiveFilters()) {
            String summary = currentAppliedFilterCriteria.getFilterSummary(); // getFilterSummary() no longer takes context

            if (summary != null && !summary.isEmpty()) {
                Chip filterSummaryChip = new Chip(getContext());
                filterSummaryChip.setText(summary);
                filterSummaryChip.setCloseIconVisible(true);
                filterSummaryChip.setOnCloseIconClickListener(v -> {
                    resetAllFilters(); // Resetting all filters when the summary chip is closed
                });
                activeFiltersSummaryChipGroup.addView(filterSummaryChip);
                // Make the scroll view for this summary group visible only if the group itself is visible and has content
                if (filterSummaryScrollView != null) { 
                    filterSummaryScrollView.setVisibility(View.VISIBLE);
                }
            } else {
                 // No summary string, but filters are active - show generic message or hide
                 // For now, if summary is empty, we hide the scroll view.
                 if (filterSummaryScrollView != null) filterSummaryScrollView.setVisibility(View.GONE);
            }
        } else {
            // No active filters from currentAppliedFilterCriteria
            if (filterSummaryScrollView != null) filterSummaryScrollView.setVisibility(View.GONE);
        }
    }

    /**
     * Maps the raw slider value (e.g., 1-10) from the inline risk slider 
     * to the appropriate risk level string constant defined in FilterCriteria.
     */
    private String mapSliderValueToFilterCriteriaRiskLevel(float sliderValue) {
        double riskScore = convertRiskSliderToScore(sliderValue); // Converts 1-10 slider to 0.0-1.0 score
        
        // These thresholds should align with how risk scores are interpreted elsewhere,
        // and with the string constants in FilterCriteria.
        if (riskScore >= 0.75) return "Low"; //FilterCriteria.RISK_LEVEL_LOW;
        if (riskScore >= 0.4) return "Medium"; //FilterCriteria.RISK_LEVEL_MEDIUM;
        if (riskScore >= 0.1) return "High"; //FilterCriteria.RISK_LEVEL_HIGH;
        return "Very High"; //FilterCriteria.RISK_LEVEL_VERY_HIGH; // Lowest scores map to Very High risk
    }

    private List<ArbitrageOpportunity> filterOpportunities() {
        if (allOpportunities == null) {
            return new ArrayList<>();
        }
        // Apply all filters: simple search, top chip, and then advanced/inline logic
        return allOpportunities.stream()
                .filter(this::opportunityMatchesAdvancedFilters) // This now handles all filter logic
                .collect(Collectors.toList());
    }
} 