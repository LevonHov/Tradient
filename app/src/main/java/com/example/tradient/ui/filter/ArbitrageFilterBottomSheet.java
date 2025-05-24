package com.example.tradient.ui.filter;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ArbitrageFilterBottomSheet extends BottomSheetDialogFragment {
    
    // Add FilterCriteria as inner class
    /**
     * Inner class that encapsulates all filter criteria for arbitrage opportunities
     */
    public static class FilterCriteria implements Parcelable {
        // Default values
        private double minProfitPercentage = 0.0;
        private double maxProfitPercentage = 50.0;
        private double maxSlippagePercentage = 2.0; // Default 2.0% (effectively "Any" or a high threshold)
        private double minExecutionTime = 0.0;    // Default 0 seconds
        private double maxExecutionTime = 300.0;  // Default 300 seconds (5 minutes, effectively "Any")
        private String riskLevel = RISK_LEVEL_ANY; // Default to "Any" risk
        private List<String> sourceExchanges = new ArrayList<>();      // Empty list means "Any"
        private List<String> destinationExchanges = new ArrayList<>(); // Empty list means "Any"

        // Constants for Risk Levels (as expected by OpportunitiesFragment)
        public static final String RISK_LEVEL_ANY = "Any";
        public static final String RISK_LEVEL_LOW = "Low";
        public static final String RISK_LEVEL_MEDIUM = "Medium";
        public static final String RISK_LEVEL_HIGH = "High";
        public static final String RISK_LEVEL_VERY_HIGH = "Very High";

        // Constants for default execution times (as expected by OpportunitiesFragment)
        public static final double DEFAULT_MIN_EXECUTION_TIME = 0.0;
        public static final double DEFAULT_MAX_EXECUTION_TIME = 300.0; // 5 minutes

        // Constants for default slippage (if needed, e.g. for hasActiveFilters)
        public static final double DEFAULT_MAX_SLIPPAGE = 2.0; // 2.0%

        // Constants for default profit percentages
        public static final double DEFAULT_MIN_PROFIT_PERCENTAGE = 0.0;
        public static final double DEFAULT_MAX_PROFIT_PERCENTAGE = 50.0;

        /**
         * Default constructor with default values
         */
        public FilterCriteria() {
            // Default values initialized in field declarations
            // Ensure lists are initialized if not done in declaration
            if (sourceExchanges == null) sourceExchanges = new ArrayList<>();
            if (destinationExchanges == null) destinationExchanges = new ArrayList<>();
        }

        /**
         * Constructor to recreate object from a parcel
         */
        protected FilterCriteria(Parcel in) {
            minProfitPercentage = in.readDouble();
            maxProfitPercentage = in.readDouble();
            maxSlippagePercentage = in.readDouble();
            minExecutionTime = in.readDouble();
            maxExecutionTime = in.readDouble();
            riskLevel = in.readString();
            
            // Read source exchanges (handle null case)
            if (in.readInt() == 1) {
                sourceExchanges = new ArrayList<>();
                in.readStringList(sourceExchanges);
            }
            
            // Read destination exchanges (handle null case)
            if (in.readInt() == 1) {
                destinationExchanges = new ArrayList<>();
                in.readStringList(destinationExchanges);
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeDouble(minProfitPercentage);
            dest.writeDouble(maxProfitPercentage);
            dest.writeDouble(maxSlippagePercentage);
            dest.writeDouble(minExecutionTime);
            dest.writeDouble(maxExecutionTime);
            dest.writeString(riskLevel != null ? riskLevel : RISK_LEVEL_ANY); // Write default if null
            
            // Write source exchanges (handle null case)
            dest.writeInt(sourceExchanges != null ? 1 : 0);
            if (sourceExchanges != null) {
                dest.writeStringList(sourceExchanges);
            }
            
            // Write destination exchanges (handle null case)
            dest.writeInt(destinationExchanges != null ? 1 : 0);
            if (destinationExchanges != null) {
                dest.writeStringList(destinationExchanges);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<FilterCriteria> CREATOR = new Creator<FilterCriteria>() {
            @Override
            public FilterCriteria createFromParcel(Parcel in) {
                return new FilterCriteria(in);
            }

            @Override
            public FilterCriteria[] newArray(int size) {
                return new FilterCriteria[size];
            }
        };

        /**
         * Checks if any filters are active (non-default values)
         */
        public boolean hasActiveFilters() {
            // Check against more robust default constants
            return minProfitPercentage > 0.0
                    || maxProfitPercentage < 50.0 // Assuming 50.0 is a non-filtering max for profit range
                    || maxSlippagePercentage < DEFAULT_MAX_SLIPPAGE 
                    || minExecutionTime > DEFAULT_MIN_EXECUTION_TIME
                    || maxExecutionTime < DEFAULT_MAX_EXECUTION_TIME
                    || (sourceExchanges != null && !sourceExchanges.isEmpty())
                    || (destinationExchanges != null && !destinationExchanges.isEmpty())
                    || (riskLevel != null && !RISK_LEVEL_ANY.equals(riskLevel));
        }

        /**
         * Returns a short summary of active filters for display
         * @param context Optional: for accessing string resources for i18n. Not used in this version.
         */
        public String getFilterSummary(android.content.Context context) { // Added context for future i18n
            StringBuilder summary = new StringBuilder();
            NumberFormat percentInstance = NumberFormat.getPercentInstance(Locale.US);
            percentInstance.setMaximumFractionDigits(1);

            if (minProfitPercentage > 0.0 || maxProfitPercentage < 50.0) {
                String maxProfitString = (maxProfitPercentage >= 50.0) ? "∞" : percentInstance.format(maxProfitPercentage / 100.0);
                summary.append(String.format(Locale.US, "Profit: %s-%s", 
                               percentInstance.format(minProfitPercentage / 100.0), 
                               maxProfitString));
            }
            
            if (riskLevel != null && !RISK_LEVEL_ANY.equals(riskLevel)) {
                if (summary.length() > 0) summary.append(", ");
                summary.append("Risk: ").append(riskLevel);
            }

            if (maxSlippagePercentage < DEFAULT_MAX_SLIPPAGE) {
                if (summary.length() > 0) summary.append(", ");
                summary.append(String.format(Locale.US, "Slippage ≤ %s", percentInstance.format(maxSlippagePercentage / 100.0)));
            }
            
            if (minExecutionTime > DEFAULT_MIN_EXECUTION_TIME || maxExecutionTime < DEFAULT_MAX_EXECUTION_TIME) {
                if (summary.length() > 0) summary.append(", ");
                summary.append(String.format(Locale.US, "Time: %.0fs-%.0fs", minExecutionTime, maxExecutionTime));
            }

            if (sourceExchanges != null && !sourceExchanges.isEmpty()) {
                if (summary.length() > 0) summary.append(", ");
                summary.append("From: ").append(String.join(",", sourceExchanges));
            }
            
            if (destinationExchanges != null && !destinationExchanges.isEmpty()) {
                if (summary.length() > 0) summary.append(", ");
                summary.append("To: ").append(String.join(",", destinationExchanges));
            }
            
            if (summary.length() == 0 && hasActiveFilters()) { // Should not happen if hasActiveFilters is true
                return "Filters active"; // Generic message if specific filters don't form a summary string
            } else if (summary.length() == 0 && !hasActiveFilters()) {
                return ""; // No active filters, no summary
            }
            
            return summary.toString();
        }

        // GetFilterSummary without context for compatibility with OpportunitiesFragment call if needed temporarily
        public String getFilterSummary() {
            return getFilterSummary(null); // Call the main one with null context
        }

        @Override
        public String toString() {
            return "FilterCriteria{" +
                    "minProfit=" + minProfitPercentage +
                    "%, maxProfit=" + maxProfitPercentage +
                    "%, maxSlippage=" + maxSlippagePercentage +
                    "%, execTime=" + minExecutionTime + "-" + maxExecutionTime +
                    "s, risk=" + riskLevel +
                    "}";
        }

        // Getters and setters
        public double getMinProfitPercentage() {
            return minProfitPercentage;
        }

        public void setMinProfitPercentage(double minProfitPercentage) {
            this.minProfitPercentage = minProfitPercentage;
        }

        public double getMaxProfitPercentage() {
            return maxProfitPercentage;
        }

        public void setMaxProfitPercentage(double maxProfitPercentage) {
            this.maxProfitPercentage = maxProfitPercentage;
        }

        public double getMaxSlippagePercentage() {
            return maxSlippagePercentage;
        }

        public void setMaxSlippagePercentage(double maxSlippagePercentage) {
            this.maxSlippagePercentage = maxSlippagePercentage;
        }

        public double getMinExecutionTime() {
            return minExecutionTime;
        }

        public void setMinExecutionTime(double minExecutionTime) {
            this.minExecutionTime = minExecutionTime;
        }

        public double getMaxExecutionTime() {
            return maxExecutionTime;
        }

        public void setMaxExecutionTime(double maxExecutionTime) {
            this.maxExecutionTime = maxExecutionTime;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public List<String> getSourceExchanges() {
            return sourceExchanges;
        }

        public void setSourceExchanges(List<String> sourceExchanges) {
            this.sourceExchanges = sourceExchanges;
        }

        public List<String> getDestinationExchanges() {
            return destinationExchanges;
        }

        public void setDestinationExchanges(List<String> destinationExchanges) {
            this.destinationExchanges = destinationExchanges;
        }
    }
    
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
    private FilterListener filterListener;
    
    // Current filter state
    private FilterCriteria currentFilters;
    
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
    
    // Add a class member variable to store the view
    private View rootView;
    
    // Example: Assuming your "Any" chips have specific IDs
    private Chip chipSourceExchangeAny; // TODO: Ensure this ID exists
    private Chip chipDestExchangeAny;   // TODO: Ensure this ID exists
    
    public ArbitrageFilterBottomSheet() {
        // Required empty constructor
        currentFilters = new FilterCriteria();
    }
    
    /**
     * Creates a new instance of the filter bottom sheet
     * @param currentFilters The current filter criteria to initialize the UI with
     * @return A new instance of the filter bottom sheet
     */
    public static ArbitrageFilterBottomSheet newInstance(FilterCriteria currentFilters) {
        ArbitrageFilterBottomSheet fragment = new ArbitrageFilterBottomSheet();
        
        // Store the current filters in a Bundle to survive configuration changes
        Bundle args = new Bundle();
        args.putParcelable("current_filters", currentFilters);
        fragment.setArguments(args);
        
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set the style here to ensure it's applied
        setStyle(STYLE_NORMAL, R.style.ThemeOverlay_Tradient_BottomSheetDialog);
        
        // Retrieve filters from arguments if they exist
        if (getArguments() != null && getArguments().containsKey("current_filters")) {
            currentFilters = getArguments().getParcelable("current_filters");
        } else {
            currentFilters = new FilterCriteria();
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
        
        // Debug toast
        Toast.makeText(getContext(), "Filter sheet created", Toast.LENGTH_SHORT).show();
        
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
        
        // Initialize source and destination exchange chip groups
        chipGroupSourceExchanges = view.findViewById(R.id.chipGroupSourceExchanges);
        chipGroupDestExchanges = view.findViewById(R.id.chipGroupDestExchanges);
        
        // Initialize buttons
        btnApplyFilters = view.findViewById(R.id.btnApplyFilters);
        btnResetFilters = view.findViewById(R.id.btnResetFilters);
        
        // Initialize "Any" exchange chips
        chipSourceExchangeAny = view.findViewById(R.id.chipSourceAny);
        chipDestExchangeAny = view.findViewById(R.id.chipDestAny);
    }
    
    private void setupListeners() {
        // Profit Range Slider
        sliderProfitRange.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            tvMinProfit.setText(formatPercent(values.get(0)));
            tvMaxProfit.setText(values.get(1) >= 50.0f ? "50.0%+" : formatPercent(values.get(1)));
            // Uncheck profit range chips if slider is manually changed
            uncheckAllChips(chipGroupProfitRanges);
        });
        
        // Profit Range Chip Group
        chipGroupProfitRanges.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                setProfitRangeFromChip(checkedId);
            }
        });

        // Slippage Slider
        sliderSlippage.addOnChangeListener((slider, value, fromUser) -> {
            tvMaxSlippage.setText(formatPercent(value));
        });

        // Risk Level Chip Group
        chipGroupRiskLevel.setOnCheckedChangeListener((group, checkedId) -> {
            // Logic to ensure only one chip is selected is usually handled by app:singleSelection="true"
            // If direct update to currentFilters is needed here, it can be done.
            // For now, we'll read it on "Apply".
        });

        // Execution Time Slider
        sliderExecutionTime.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            tvMinExecutionTime.setText(formatTime(values.get(0)));
            tvMaxExecutionTime.setText(formatTime(values.get(1)));
            // Uncheck execution time chips if slider is manually changed
            uncheckAllChips(chipGroupExecutionTimes);
        });
        
        // Execution Time Chip Group
        chipGroupExecutionTimes.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                setExecutionTimeFromChip(checkedId);
            }
        });

        // Source Exchanges Chip Group
        if (chipGroupSourceExchanges != null) {
            chipGroupSourceExchanges.setOnCheckedChangeListener((group, checkedId) -> {
                handleExchangeChipChange(group, checkedId, R.id.chipSourceAny, true);
            });
        }

        // Destination Exchanges Chip Group
        if (chipGroupDestExchanges != null) {
            chipGroupDestExchanges.setOnCheckedChangeListener((group, checkedId) -> {
                handleExchangeChipChange(group, checkedId, R.id.chipDestAny, false);
            });
        }

        // Apply Filters Button
        if (btnApplyFilters != null) {
            btnApplyFilters.setOnClickListener(v -> {
                // Collect all filter values from UI elements into currentFilters
                currentFilters.setMinProfitPercentage(sliderProfitRange.getValues().get(0));
                currentFilters.setMaxProfitPercentage(sliderProfitRange.getValues().get(1));
                currentFilters.setMaxSlippagePercentage(sliderSlippage.getValue());
                currentFilters.setMinExecutionTime(sliderExecutionTime.getValues().get(0));
                currentFilters.setMaxExecutionTime(sliderExecutionTime.getValues().get(1));
                currentFilters.setRiskLevel(getSelectedRiskLevel(chipGroupRiskLevel));
                
                // Get selected exchanges, handling the "Any" case internally in getSelectedExchangesList
                currentFilters.setSourceExchanges(getSelectedExchangesList(chipGroupSourceExchanges, R.id.chipSourceAny));
                currentFilters.setDestinationExchanges(getSelectedExchangesList(chipGroupDestExchanges, R.id.chipDestAny));

                if (filterListener != null) {
                    filterListener.onFiltersApplied(currentFilters);
                }
                dismiss();
            });
        }
        
        // Reset Filters Button
        if (btnResetFilters != null) {
            btnResetFilters.setOnClickListener(v -> resetFilters());
        }
    }
    
    // Consolidated handler for exchange chip group changes
    private void handleExchangeChipChange(ChipGroup group, int checkedId, int anyChipId, boolean isSourceGroup) {
        if (checkedId == View.NO_ID) { // All chips in the group are unchecked
             // If nothing is selected, default to "Any"
            Chip anyChip = group.findViewById(anyChipId);
            if (anyChip != null && !anyChip.isChecked()) {
                anyChip.setChecked(true); // This will re-trigger the listener, be careful of loops
            }
            setOtherExchangeChipsEnabled(group, anyChipId, false); // Disable specific exchanges if "Any" is forced selected
            return;
        }

        if (checkedId == anyChipId) { // "Any Exchange" chip is selected
            // Uncheck all other chips in this group
            for (int i = 0; i < group.getChildCount(); i++) {
                Chip chip = (Chip) group.getChildAt(i);
                if (chip.getId() != anyChipId && chip.isChecked()) {
                    chip.setChecked(false); // This might re-trigger listener, use with caution or add flags
                }
            }
            setOtherExchangeChipsEnabled(group, anyChipId, false);
            // Update currentFilters for the respective group
            if (isSourceGroup) {
                currentFilters.setSourceExchanges(new ArrayList<>()); // Empty list signifies "Any"
            } else {
                currentFilters.setDestinationExchanges(new ArrayList<>()); // Empty list signifies "Any"
            }
        } else { // A specific exchange chip is selected
            // Uncheck the "Any Exchange" chip in this group
            Chip anyChip = group.findViewById(anyChipId);
            if (anyChip != null && anyChip.isChecked()) {
                anyChip.setChecked(false);
            }
            setOtherExchangeChipsEnabled(group, anyChipId, true);
            // Collect selected specific exchanges and update currentFilters
            // This part is handled by getSelectedExchangesList when Apply is clicked
        }
    }

    /**
     * Enables or disables all chips in a ChipGroup except for the "Any Exchange" chip.
     * @param group The ChipGroup containing the exchange chips.
     * @param anyChipId The ID of the "Any Exchange" chip within the group.
     * @param enabled True to enable specific exchange chips, false to disable them.
     */
    private void setOtherExchangeChipsEnabled(ChipGroup group, int anyChipId, boolean enabled) {
        if (group == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View view = group.getChildAt(i);
            if (view instanceof Chip && view.getId() != anyChipId) {
                view.setEnabled(enabled);
                // Optionally change alpha to indicate disabled state visually
                // view.setAlpha(enabled ? 1.0f : 0.5f);
            }
        }
    }

    /**
     * Selects the "Any Exchange" chip and deselects/disables others.
     * @param group The ChipGroup (source or destination).
     * @param anyChipId The resource ID of the "Any Exchange" chip in this group.
     * @param isSelectingAny True if "Any" is being selected, false if a specific exchange is being selected (to enable others).
     */
    private void selectAnyExchangeIfNeeded(ChipGroup group, int anyChipId, boolean isSelectingAny) {
        if (group == null) return;
        Chip anyChip = group.findViewById(anyChipId);
        if (anyChip == null) return;

        if (isSelectingAny) {
            if (!anyChip.isChecked()) {
                anyChip.setChecked(true); // This will trigger onCheckedChangeListener
            } else {
                // If "Any" is already checked, ensure others are disabled
                setOtherExchangeChipsEnabled(group, anyChipId, false);
            }
        } else {
            // If a specific exchange is selected, "Any" should be unchecked and others enabled
            if (anyChip.isChecked()) {
                anyChip.setChecked(false); // This will trigger onCheckedChangeListener
            }
            setOtherExchangeChipsEnabled(group, anyChipId, true);
        }
    }

    private String getSelectedRiskLevel(ChipGroup chipGroup) {
        int checkedChipId = chipGroup.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip selectedChip = chipGroup.findViewById(checkedChipId);
            if (selectedChip != null) {
                return selectedChip.getText().toString();
            }
        }
        return RISK_MEDIUM; // Default if somehow no chip is selected (though selectionRequired is true)
    }

    private List<String> getSelectedExchangesList(ChipGroup chipGroup, int anyChipId) {
        List<String> selectedExchanges = new ArrayList<>();
        if (chipGroup == null) return selectedExchanges; // Return empty if group is null

        Chip selectedChip = chipGroup.findViewById(chipGroup.getCheckedChipId());
        if (selectedChip != null && selectedChip.getId() == anyChipId) {
            return selectedExchanges; // "Any" is selected, so return empty list for FilterCriteria convention
        }

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.isChecked() && chip.getId() != anyChipId) {
                selectedExchanges.add(chip.getText().toString());
            }
        }
        return selectedExchanges;
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
        if (currentFilters == null) {
            currentFilters = new FilterCriteria(); // Should not happen if newInstance is used
        }

        // Profit
        sliderProfitRange.setValues((float)currentFilters.getMinProfitPercentage(), (float)currentFilters.getMaxProfitPercentage());
        tvMinProfit.setText(formatPercent((float)currentFilters.getMinProfitPercentage()));
        tvMaxProfit.setText(currentFilters.getMaxProfitPercentage() >= 50.0f ? "50.0%+" : formatPercent((float)currentFilters.getMaxProfitPercentage()));
        // Deselect profit chips initially, slider takes precedence. Or, try to match a chip.
        uncheckAllChips(chipGroupProfitRanges);


        // Risk Level
        String risk = currentFilters.getRiskLevel();
        Chip riskChipToSelect = null;
        if (RISK_LOW.equalsIgnoreCase(risk)) riskChipToSelect = rootView.findViewById(R.id.chipRiskLow);
        else if (RISK_HIGH.equalsIgnoreCase(risk)) riskChipToSelect = rootView.findViewById(R.id.chipRiskHigh);
        else if (RISK_VERY_HIGH.equalsIgnoreCase(risk)) riskChipToSelect = rootView.findViewById(R.id.chipRiskVeryHigh);
        else riskChipToSelect = rootView.findViewById(R.id.chipRiskMedium); // Default to medium
        
        if (riskChipToSelect != null) {
            riskChipToSelect.setChecked(true);
        } else {
             Chip mediumChip = rootView.findViewById(R.id.chipRiskMedium); // Fallback
             if(mediumChip != null) mediumChip.setChecked(true);
        }


        // Slippage
        sliderSlippage.setValue((float)currentFilters.getMaxSlippagePercentage());
        tvMaxSlippage.setText(formatPercent((float)currentFilters.getMaxSlippagePercentage()));

        // Execution Time
        sliderExecutionTime.setValues((float)currentFilters.getMinExecutionTime(), (float)currentFilters.getMaxExecutionTime());
        tvMinExecutionTime.setText(formatTime((float)currentFilters.getMinExecutionTime()));
        tvMaxExecutionTime.setText(formatTime((float)currentFilters.getMaxExecutionTime()));
        // Deselect time chips initially. Or, try to match a chip.
        uncheckAllChips(chipGroupExecutionTimes);


        // Source Exchanges
        updateExchangeChipGroupSelection(chipGroupSourceExchanges, currentFilters.getSourceExchanges(), R.id.chipSourceAny);

        // Destination Exchanges
        updateExchangeChipGroupSelection(chipGroupDestExchanges, currentFilters.getDestinationExchanges(), R.id.chipDestAny);
    }
    
    private void updateExchangeChipGroupSelection(ChipGroup group, List<String> selectedNames, int anyChipId) {
        uncheckAllChips(group); // Start fresh
        Chip anyChip = group.findViewById(anyChipId);

        if (selectedNames == null || selectedNames.isEmpty()) {
            if (anyChip != null) anyChip.setChecked(true);
            return;
        }
        
        boolean specificSelected = false;
        for (String name : selectedNames) {
            for (int i = 0; i < group.getChildCount(); i++) {
                Chip chip = (Chip) group.getChildAt(i);
                if (name.equalsIgnoreCase(chip.getText().toString())) {
                    chip.setChecked(true);
                    specificSelected = true;
                        break;
                }
            }
        }
        // If no specific chips were matched and "Any" chip exists, check it.
        // Or, if anyChipId should be unchecked if specifics are selected.
        if (anyChip != null) {
            anyChip.setChecked(!specificSelected);
        }
    }
    
    private void resetFilters() {
        // Reset sliders to default values
        sliderProfitRange.setValues((float) FilterCriteria.DEFAULT_MIN_PROFIT_PERCENTAGE, (float) FilterCriteria.DEFAULT_MAX_PROFIT_PERCENTAGE);
        sliderSlippage.setValue((float) FilterCriteria.DEFAULT_MAX_SLIPPAGE);
        sliderExecutionTime.setValues((float) FilterCriteria.DEFAULT_MIN_EXECUTION_TIME, (float) FilterCriteria.DEFAULT_MAX_EXECUTION_TIME);

        // Reset Risk Level to "Any" (or your default)
        // TODO: Ensure R.id.chip_risk_any exists and is the ID of your "Any" risk chip

        // Reset Exchange ChipGroups to "Any Exchange"
        // TODO: Ensure chipSourceExchangeAny and chipDestExchangeAny are valid references to your "Any" chips in the layout
        selectAnyExchangeIfNeeded(chipGroupSourceExchanges, chipSourceExchangeAny != null ? chipSourceExchangeAny.getId() : View.NO_ID, true);
        selectAnyExchangeIfNeeded(chipGroupDestExchanges, chipDestExchangeAny != null ? chipDestExchangeAny.getId() : View.NO_ID, true);

        // Reset quick select chip groups (profit ranges, execution times)
        uncheckAllChips(chipGroupProfitRanges);
        uncheckAllChips(chipGroupExecutionTimes);
        
        // Update UI text views
        updateSliderTextViews();

        // Create a new default FilterCriteria object
        currentFilters = new FilterCriteria();
        // Optionally, notify listener if immediate reset application is desired without clicking "Apply"
        // if (filterListener != null) {
        // filterListener.onFiltersApplied(currentFilters);
        // }
    }
    
    private void updateSliderTextViews() {
        if (tvMinProfit != null && tvMaxProfit != null && sliderProfitRange != null) {
            List<Float> profitValues = sliderProfitRange.getValues();
            tvMinProfit.setText(formatPercent(profitValues.get(0)));
            tvMaxProfit.setText(profitValues.get(1) >= 50.0f ? "50.0%+" : formatPercent(profitValues.get(1)));
        }
        if (tvMaxSlippage != null && sliderSlippage != null) { // Assuming only max slippage shown, or adjust if min also present
            tvMaxSlippage.setText(formatPercent(sliderSlippage.getValue()));
        }
        if (tvMinExecutionTime != null && tvMaxExecutionTime != null && sliderExecutionTime != null) {
            List<Float> timeValues = sliderExecutionTime.getValues();
            tvMinExecutionTime.setText(formatTime(timeValues.get(0)));
            tvMaxExecutionTime.setText(formatTime(timeValues.get(1)));
        }
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
    public void setFilterListener(FilterListener listener) {
        this.filterListener = listener;
    }
    
    /**
     * Interface for filter callbacks
     */
    public interface FilterListener {
        void onFiltersApplied(FilterCriteria filters);
    }
} 