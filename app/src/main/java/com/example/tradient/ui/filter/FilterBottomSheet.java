package com.example.tradient.ui.filter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tradient.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A bottom sheet dialog for filtering arbitrage opportunities
 */
public class FilterBottomSheet extends BottomSheetDialogFragment {
    
    // UI Components
    private RangeSlider sliderProfitRange;
    private Slider sliderSlippage;
    private RangeSlider sliderExecutionTime;
    private ChipGroup chipGroupRiskLevel;
    private ChipGroup chipGroupSourceExchanges;
    private ChipGroup chipGroupDestExchanges;
    private ChipGroup chipGroupProfitRanges;
    private ChipGroup chipGroupExecutionTimes;
    private TextView tvMinProfit, tvMaxProfit;
    private TextView tvMinSlippage, tvMaxSlippage;
    private TextView tvMinExecutionTime, tvMaxExecutionTime;
    private Button btnApplyFilters, btnResetFilters;
    
    // Filter listener
    private FilterAppliedListener filterListener;
    
    // Current filter state
    private FilterCriteria currentFilter;
    
    // Risk levels
    private static final String RISK_LOW = "Low";
    private static final String RISK_MEDIUM = "Medium";
    private static final String RISK_HIGH = "High";
    private static final String RISK_VERY_HIGH = "Very High";
    
    // Exchanges
    private static final String EXCHANGE_ANY = "Any Exchange";
    private static final List<String> EXCHANGES = Arrays.asList(
            "Binance", "Coinbase", "Kraken", "OKX", "ByBit"
    );
    
    // Number formats
    private final NumberFormat percentFormat = NumberFormat.getPercentInstance(Locale.US);
    private final NumberFormat timeFormat = NumberFormat.getNumberInstance(Locale.US);
    
    // Reference to the view
    private View rootView;
    
    /**
     * Default constructor
     */
    public FilterBottomSheet() {
        // Required empty constructor
        currentFilter = new FilterCriteria();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set the style here to ensure it's applied
        setStyle(STYLE_NORMAL, R.style.ThemeOverlay_Tradient_BottomSheetDialog);
        
        // Get any passed filter from arguments
        Bundle args = getArguments();
        if (args != null && args.containsKey("current_filter")) {
            currentFilter = args.getParcelable("current_filter");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.bottom_sheet_arbitrage_filter, container, false);
        return rootView;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup number formats
        percentFormat.setMinimumFractionDigits(1);
        percentFormat.setMaximumFractionDigits(1);
        
        // Initialize UI components
        initializeViews(view);
        
        // Set up listeners
        setupListeners();
        
        // Initialize the UI with current filter values
        initializeWithCurrentFilters();
        
        // Make sure the bottom sheet expands when shown
        if (getDialog() != null) {
            getDialog().setOnShowListener(dialog -> {
                // Expand the bottom sheet when shown
                BottomSheetDialog bottomSheetDialog = 
                    (BottomSheetDialog) dialog;
                BottomSheetBehavior<FrameLayout> behavior = 
                    bottomSheetDialog.getBehavior();
                
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            });
        }
    }
    
    private void initializeViews(View view) {
        // Profit range
        sliderProfitRange = view.findViewById(R.id.sliderProfitRange);
        tvMinProfit = view.findViewById(R.id.tvMinProfit);
        tvMaxProfit = view.findViewById(R.id.tvMaxProfit);
        chipGroupProfitRanges = view.findViewById(R.id.chipGroupProfitRanges);
        
        // Risk level
        chipGroupRiskLevel = view.findViewById(R.id.chipGroupRiskLevel);
        
        // Slippage
        sliderSlippage = view.findViewById(R.id.sliderSlippage);
        tvMinSlippage = view.findViewById(R.id.tvMinSlippage);
        tvMaxSlippage = view.findViewById(R.id.tvMaxSlippage);
        
        // Execution time
        sliderExecutionTime = view.findViewById(R.id.sliderExecutionTime);
        tvMinExecutionTime = view.findViewById(R.id.tvMinExecutionTime);
        tvMaxExecutionTime = view.findViewById(R.id.tvMaxExecutionTime);
        chipGroupExecutionTimes = view.findViewById(R.id.chipGroupExecutionTimes);
        
        // Exchanges
        chipGroupSourceExchanges = view.findViewById(R.id.chipGroupSourceExchanges);
        chipGroupDestExchanges = view.findViewById(R.id.chipGroupDestExchanges);
        
        // Buttons
        btnApplyFilters = view.findViewById(R.id.btnApplyFilters);
        btnResetFilters = view.findViewById(R.id.btnResetFilters);
    }
    
    private void setupListeners() {
        // Profit Range Slider listener
        sliderProfitRange.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            float minProfit = values.get(0);
            float maxProfit = values.get(1);
            
            // Update text labels with current values
            tvMinProfit.setText(formatPercent(minProfit / 100));
            
            if (maxProfit >= slider.getValueTo()) {
                tvMaxProfit.setText(formatPercent(maxProfit / 100) + "+");
            } else {
                tvMaxProfit.setText(formatPercent(maxProfit / 100));
            }
            
            // Update current filters
            currentFilter.setMinProfitPercent(minProfit);
            currentFilter.setMaxProfitPercent(maxProfit);
            
            // Uncheck any preset chips
            uncheckAllChips(chipGroupProfitRanges);
        });
        
        // Profit preset chip group listener
        chipGroupProfitRanges.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = rootView.findViewById(checkedIds.get(0));
                if (selectedChip != null) {
                    setProfitRangeFromChip(selectedChip.getId());
                }
            }
        });
        
        // Slippage Slider listener
        sliderSlippage.addOnChangeListener((slider, value, fromUser) -> {
            float slippage = value;
            tvMaxSlippage.setText(formatPercent(slippage / 100));
            currentFilter.setMaxSlippagePercentage(slippage);
        });
        
        // Execution Time Range Slider listener
        sliderExecutionTime.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            float minTime = values.get(0);
            float maxTime = values.get(1);
            
            // Update text labels with current values
            tvMinExecutionTime.setText(formatTime(minTime));
            tvMaxExecutionTime.setText(formatTime(maxTime));
            
            // Update current filters
            currentFilter.setMinExecutionTime(minTime);
            currentFilter.setMaxExecutionTime(maxTime);
            
            // Uncheck any preset chips
            uncheckAllChips(chipGroupExecutionTimes);
        });
        
        // Execution time preset chip group listener
        chipGroupExecutionTimes.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = rootView.findViewById(checkedIds.get(0));
                if (selectedChip != null) {
                    setExecutionTimeFromChip(selectedChip.getId());
                }
            }
        });
        
        // Risk Level chip group listener
        chipGroupRiskLevel.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = rootView.findViewById(checkedIds.get(0));
                if (selectedChip != null) {
                    Set<String> riskLevels = new HashSet<>();
                    riskLevels.add(selectedChip.getText().toString());
                    currentFilter.setRiskLevels(riskLevels);
                }
            }
        });
        
        // Source Exchange chip group listener
        chipGroupSourceExchanges.setOnCheckedStateChangeListener((group, checkedIds) -> {
            handleExchangeSelection(checkedIds, true);
        });
        
        // Destination Exchange chip group listener
        chipGroupDestExchanges.setOnCheckedStateChangeListener((group, checkedIds) -> {
            handleExchangeSelection(checkedIds, false);
        });
        
        // Apply button listener
        btnApplyFilters.setOnClickListener(v -> {
            if (filterListener != null) {
                filterListener.onFiltersApplied(currentFilter);
            }
            dismiss();
        });
        
        // Reset button listener
        btnResetFilters.setOnClickListener(v -> {
            resetFilters();
        });
    }
    
    private void handleExchangeSelection(List<Integer> checkedIds, boolean isSource) {
        Set<String> selectedExchanges = new HashSet<>();
        boolean hasAny = false;
        
        for (int id : checkedIds) {
            Chip chip = rootView.findViewById(id);
            if (chip != null) {
                String exchange = chip.getText().toString();
                if (EXCHANGE_ANY.equals(exchange)) {
                    hasAny = true;
                    // When "Any Exchange" is selected, disable other chips
                    setOtherExchangeChipsEnabled(!hasAny, isSource);
                    break;
                } else {
                    selectedExchanges.add(exchange);
                }
            }
        }
        
        if (isSource) {
            // For source exchanges
            if (hasAny) {
                currentFilter.setSourceExchanges(null); // null means any
            } else {
                currentFilter.setSourceExchanges(new ArrayList<>(selectedExchanges));
            }
            
            // If no exchanges selected, reselect "Any"
            if (checkedIds.isEmpty()) {
                selectAnyExchange(true);
            }
        } else {
            // For destination exchanges
            if (hasAny) {
                currentFilter.setDestinationExchanges(null); // null means any
            } else {
                currentFilter.setDestinationExchanges(new ArrayList<>(selectedExchanges));
            }
            
            // If no exchanges selected, reselect "Any"
            if (checkedIds.isEmpty()) {
                selectAnyExchange(false);
            }
        }
        
        // Also update the exchanges set for compatibility with Filter interface
        Set<String> allExchanges = new HashSet<>();
        if (currentFilter.getSourceExchanges() != null) {
            allExchanges.addAll(currentFilter.getSourceExchanges());
        }
        if (currentFilter.getDestinationExchanges() != null) {
            allExchanges.addAll(currentFilter.getDestinationExchanges());
        }
        currentFilter.setExchanges(allExchanges);
    }
    
    private void setOtherExchangeChipsEnabled(boolean enabled, boolean isSource) {
        ChipGroup group = isSource ? chipGroupSourceExchanges : chipGroupDestExchanges;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (!EXCHANGE_ANY.equals(chip.getText().toString())) {
                    chip.setEnabled(enabled);
                    if (!enabled) {
                        chip.setChecked(false);
                    }
                }
            }
        }
    }
    
    private void selectAnyExchange(boolean isSource) {
        if (isSource) {
            for (int i = 0; i < chipGroupSourceExchanges.getChildCount(); i++) {
                View child = chipGroupSourceExchanges.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (EXCHANGE_ANY.equals(chip.getText().toString())) {
                        chip.setChecked(true);
                        break;
                    }
                }
            }
        } else {
            for (int i = 0; i < chipGroupDestExchanges.getChildCount(); i++) {
                View child = chipGroupDestExchanges.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (EXCHANGE_ANY.equals(chip.getText().toString())) {
                        chip.setChecked(true);
                        break;
                    }
                }
            }
        }
    }
    
    private void setProfitRangeFromChip(int chipId) {
        if (chipId == R.id.chipProfit0to1) {
            sliderProfitRange.setValues(0f, 1f);
        } else if (chipId == R.id.chipProfit1to3) {
            sliderProfitRange.setValues(1f, 3f);
        } else if (chipId == R.id.chipProfit3to5) {
            sliderProfitRange.setValues(3f, 5f);
        } else if (chipId == R.id.chipProfit5Plus) {
            sliderProfitRange.setValues(5f, 50f);
        }
    }
    
    private void setExecutionTimeFromChip(int chipId) {
        if (chipId == R.id.chipTime30s) {
            sliderExecutionTime.setValues(0f, 30f);
        } else if (chipId == R.id.chipTime1m) {
            sliderExecutionTime.setValues(0f, 60f);
        } else if (chipId == R.id.chipTime3m) {
            sliderExecutionTime.setValues(0f, 180f);
        } else if (chipId == R.id.chipTime5m) {
            sliderExecutionTime.setValues(0f, 300f);
        }
    }
    
    private void uncheckAllChips(ChipGroup chipGroup) {
        chipGroup.clearCheck();
    }
    
    private void initializeWithCurrentFilters() {
        // Set profit range
        sliderProfitRange.setValues(
                (float) currentFilter.getMinProfitPercent(),
                (float) currentFilter.getMaxProfitPercent()
        );
        
        // Set risk level
        Set<String> riskLevels = currentFilter.getRiskLevels();
        if (riskLevels != null && !riskLevels.isEmpty()) {
            String riskLevel = riskLevels.iterator().next();
            for (int i = 0; i < chipGroupRiskLevel.getChildCount(); i++) {
                View child = chipGroupRiskLevel.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (riskLevel.equals(chip.getText().toString())) {
                        chip.setChecked(true);
                        break;
                    }
                }
            }
        }
        
        // Set slippage
        sliderSlippage.setValue((float) currentFilter.getMaxSlippagePercentage());
        
        // Set execution time
        sliderExecutionTime.setValues(
                (float) currentFilter.getMinExecutionTime(),
                (float) currentFilter.getMaxExecutionTime()
        );
        
        // Set source exchanges
        List<String> sourceExchanges = currentFilter.getSourceExchanges();
        if (sourceExchanges == null || sourceExchanges.isEmpty()) {
            // If null or empty, select "Any Exchange"
            selectAnyExchange(true);
        } else {
            // Deselect "Any Exchange" first
            for (int i = 0; i < chipGroupSourceExchanges.getChildCount(); i++) {
                View child = chipGroupSourceExchanges.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (EXCHANGE_ANY.equals(chip.getText().toString())) {
                        chip.setChecked(false);
                        break;
                    }
                }
            }
            
            // Select specific exchanges
            for (int i = 0; i < chipGroupSourceExchanges.getChildCount(); i++) {
                View child = chipGroupSourceExchanges.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (sourceExchanges.contains(chip.getText().toString())) {
                        chip.setChecked(true);
                    }
                }
            }
        }
        
        // Set destination exchanges
        List<String> destExchanges = currentFilter.getDestinationExchanges();
        if (destExchanges == null || destExchanges.isEmpty()) {
            // If null or empty, select "Any Exchange"
            selectAnyExchange(false);
        } else {
            // Deselect "Any Exchange" first
            for (int i = 0; i < chipGroupDestExchanges.getChildCount(); i++) {
                View child = chipGroupDestExchanges.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (EXCHANGE_ANY.equals(chip.getText().toString())) {
                        chip.setChecked(false);
                        break;
                    }
                }
            }
            
            // Select specific exchanges
            for (int i = 0; i < chipGroupDestExchanges.getChildCount(); i++) {
                View child = chipGroupDestExchanges.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (destExchanges.contains(chip.getText().toString())) {
                        chip.setChecked(true);
                    }
                }
            }
        }
    }
    
    private void resetFilters() {
        // Reset all filters to defaults
        currentFilter = new FilterCriteria();
        
        // Reset UI components
        sliderProfitRange.setValues(0f, 50f);
        sliderSlippage.setValue(2.0f);
        sliderExecutionTime.setValues(0f, 300f);
        
        // Reset chip groups
        uncheckAllChips(chipGroupProfitRanges);
        uncheckAllChips(chipGroupExecutionTimes);
        
        // Reset risk level to Medium
        for (int i = 0; i < chipGroupRiskLevel.getChildCount(); i++) {
            View child = chipGroupRiskLevel.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(RISK_MEDIUM.equals(chip.getText().toString()));
            }
        }
        
        // Reset exchanges to "Any"
        selectAnyExchange(true);
        selectAnyExchange(false);
        setOtherExchangeChipsEnabled(false, true);
        setOtherExchangeChipsEnabled(false, false);
    }
    
    /**
     * Format a decimal as a percentage
     */
    private String formatPercent(float value) {
        return percentFormat.format(value);
    }
    
    /**
     * Format seconds as a human-readable time
     */
    private String formatTime(float seconds) {
        if (seconds < 60) {
            return Math.round(seconds) + "s";
        } else {
            return Math.round(seconds / 60) + "m";
        }
    }
    
    /**
     * Set the filter listener
     */
    public void setFilterAppliedListener(FilterAppliedListener listener) {
        this.filterListener = listener;
    }
    
    /**
     * Interface for filter callbacks
     */
    public interface FilterAppliedListener {
        void onFiltersApplied(FilterCriteria filter);
    }
    
    /**
     * Create a new instance with filter criteria
     */
    public static FilterBottomSheet newInstance(FilterCriteria filter) {
        FilterBottomSheet bottomSheet = new FilterBottomSheet();
        Bundle args = new Bundle();
        args.putParcelable("current_filter", filter);
        bottomSheet.setArguments(args);
        return bottomSheet;
    }
} 