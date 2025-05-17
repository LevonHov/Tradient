package com.example.tradient.ui.opportunities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tradient.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bottom sheet dialog for filtering opportunities
 */
public class FilterBottomSheet extends BottomSheetDialogFragment {
    
    // Interface for communicating with the parent fragment
    public interface FilterAppliedListener {
        void onFiltersApplied(Filter filter);
    }
    
    private FilterAppliedListener listener;
    private Filter currentFilter = new Filter();
    
    // UI Components
    private RangeSlider profitRangeSlider;
    private TextView minProfitText;
    private TextView maxProfitText;
    private ChipGroup exchangeChipGroup;
    private SeekBar riskSeekBar;
    private TextView riskLevelText;
    private Button applyButton;
    private Button resetButton;
    
    /**
     * Sets the listener for filter apply events
     */
    public void setFilterAppliedListener(FilterAppliedListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use the proper BottomSheetDialog theme
        setStyle(STYLE_NORMAL, R.style.ThemeOverlay_Tradient_BottomSheetDialog);
        
        // Get any existing filter from arguments
        if (getArguments() != null && getArguments().containsKey("current_filter")) {
            try {
                Filter filter = getArguments().getParcelable("current_filter");
                if (filter != null) {
                    currentFilter = filter;
                }
            } catch (Exception e) {
                // Handle error gracefully
                currentFilter = new Filter();
            }
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
            // Set the background color of the bottom sheet to make everything more visible
            if (view.getParent() instanceof View) {
                View parent = (View) view.getParent();
                parent.setBackgroundColor(getResources().getColor(R.color.card_background, null));
            }
            
            // Initialize UI components
            initializeViews(view);
            
            // Set initial values based on current filter
            populateFilterValues();
            
            // Set up listeners
            setupListeners();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error initializing filter dialog", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }
    
    private void initializeViews(View view) {
        try {
            // Find views by ID with the corrected IDs from bottom_sheet_filter.xml
            profitRangeSlider = view.findViewById(R.id.profitRangeSlider);
            minProfitText = view.findViewById(R.id.minProfitText);
            maxProfitText = view.findViewById(R.id.maxProfitText);
            exchangeChipGroup = view.findViewById(R.id.exchangeChipGroup);
            riskSeekBar = view.findViewById(R.id.riskSeekBar);
            riskLevelText = view.findViewById(R.id.riskLevelText);
            applyButton = view.findViewById(R.id.applyButton);
            resetButton = view.findViewById(R.id.resetButton);
        } catch (Exception e) {
            throw new RuntimeException("Error finding views in filter dialog", e);
        }
    }
    
    private void populateFilterValues() {
        // Set profit range slider values
        if (profitRangeSlider != null) {
            List<Float> values = new ArrayList<>();
            values.add(currentFilter.getMinProfitPercent());
            values.add(currentFilter.getMaxProfitPercent());
            profitRangeSlider.setValues(values);
            
            // Update text displays
            updateProfitTexts(currentFilter.getMinProfitPercent(), currentFilter.getMaxProfitPercent());
        }
        
        // Set selected exchanges
        if (exchangeChipGroup != null) {
            Set<String> selectedExchanges = currentFilter.getSelectedExchanges();
            for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
                View view = exchangeChipGroup.getChildAt(i);
                if (view instanceof Chip) {
                    Chip chip = (Chip) view;
                    String exchange = chip.getText().toString();
                    chip.setChecked(selectedExchanges.contains(exchange) || 
                                   selectedExchanges.isEmpty() && "All Exchanges".equals(exchange));
                }
            }
        }
        
        // Set risk level
        if (riskSeekBar != null) {
            riskSeekBar.setProgress(Math.round(currentFilter.getMaxRiskLevel()));
            updateRiskLevelText(currentFilter.getMaxRiskLevel());
        }
    }
    
    private void setupListeners() {
        // Profit range slider change listener
        if (profitRangeSlider != null) {
            profitRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
                List<Float> values = slider.getValues();
                if (values.size() >= 2) {
                    float min = values.get(0);
                    float max = values.get(1);
                    updateProfitTexts(min, max);
                }
            });
        }
        
        // Risk seek bar listener
        if (riskSeekBar != null) {
            riskSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateRiskLevelText(progress);
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
        
        // Apply button listener
        if (applyButton != null) {
            applyButton.setOnClickListener(v -> {
                applyFilters();
                dismiss();
            });
        }
        
        // Reset button listener
        if (resetButton != null) {
            resetButton.setOnClickListener(v -> {
                resetFilters();
            });
        }
    }
    
    private void updateProfitTexts(float min, float max) {
        if (minProfitText != null) {
            minProfitText.setText(String.format("%.1f%%", min));
        }
        if (maxProfitText != null) {
            maxProfitText.setText(String.format("%.1f%%+", max));
        }
    }
    
    private void updateRiskLevelText(float riskLevel) {
        if (riskLevelText != null) {
            String riskText;
            if (riskLevel < 2) {
                riskText = "Very Low";
            } else if (riskLevel < 4) {
                riskText = "Low";
            } else if (riskLevel < 6) {
                riskText = "Medium";
            } else if (riskLevel < 8) {
                riskText = "High";
            } else {
                riskText = "Very High";
            }
            riskLevelText.setText(riskText);
        }
    }
    
    private void applyFilters() {
        try {
            // Create new filter with current values
            Filter filter = new Filter();
            
            // Set profit range
            if (profitRangeSlider != null) {
                List<Float> values = profitRangeSlider.getValues();
                if (values.size() >= 2) {
                    filter.setMinProfitPercent(values.get(0));
                    filter.setMaxProfitPercent(values.get(1));
                }
            }
            
            // Set selected exchanges
            if (exchangeChipGroup != null) {
                Set<String> selectedExchanges = new HashSet<>();
                boolean allSelected = false;
                
                for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
                    View view = exchangeChipGroup.getChildAt(i);
                    if (view instanceof Chip) {
                        Chip chip = (Chip) view;
                        if (chip.isChecked()) {
                            String exchange = chip.getText().toString();
                            if ("All Exchanges".equals(exchange)) {
                                allSelected = true;
                                break;
                            } else {
                                selectedExchanges.add(exchange);
                            }
                        }
                    }
                }
                
                if (!allSelected) {
                    filter.setSelectedExchanges(selectedExchanges);
                }
            }
            
            // Set risk level
            if (riskSeekBar != null) {
                filter.setMaxRiskLevel(riskSeekBar.getProgress());
            }
            
            // Notify listener
            if (listener != null) {
                listener.onFiltersApplied(filter);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error applying filters", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void resetFilters() {
        try {
            // Reset profit range slider
            if (profitRangeSlider != null) {
                List<Float> values = new ArrayList<>();
                values.add(0f);
                values.add(50f);
                profitRangeSlider.setValues(values);
                updateProfitTexts(0f, 50f);
            }
            
            // Reset exchanges (select only "All")
            if (exchangeChipGroup != null) {
                for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
                    View view = exchangeChipGroup.getChildAt(i);
                    if (view instanceof Chip) {
                        Chip chip = (Chip) view;
                        chip.setChecked("All Exchanges".equals(chip.getText().toString()));
                    }
                }
            }
            
            // Reset risk level
            if (riskSeekBar != null) {
                riskSeekBar.setProgress(5);
                updateRiskLevelText(5);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error resetting filters", Toast.LENGTH_SHORT).show();
        }
    }
} 