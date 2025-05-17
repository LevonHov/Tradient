package com.example.tradient.ui.filter;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradient.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dialog fragment for filtering arbitrage opportunities
 */
public class FilterDialogFragment extends DialogFragment {
    
    private static final String TAG = "FilterDialogFragment";
    
    // UI Components
    private RangeSlider profitRangeSlider;
    private ChipGroup riskLevelChipGroup;
    private ChipGroup exchangeChipGroup;
    private Button resetButton;
    private Button applyButton;
    
    // Filter data
    private FilterCriteria currentCriteria;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Use dialog theme with rounded corners
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_MaterialComponents_Dialog_Alert);
        
        // Extract existing filter criteria if provided
        Bundle args = getArguments();
        if (args != null && args.containsKey("current_filter")) {
            currentCriteria = args.getParcelable("current_filter");
            Log.d(TAG, "Restored filter criteria: " + currentCriteria);
        } else {
            // Initialize with default criteria
            currentCriteria = new FilterCriteria();
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_filter, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
            // Initialize UI components
            profitRangeSlider = view.findViewById(R.id.profitRangeSlider);
            riskLevelChipGroup = view.findViewById(R.id.riskLevelChipGroup);
            exchangeChipGroup = view.findViewById(R.id.exchangeChipGroup);
            resetButton = view.findViewById(R.id.resetButton);
            applyButton = view.findViewById(R.id.applyButton);
            
            // Set up initial values
            setupInitialValues();
            
            // Set up click listeners
            setupClickListeners();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "An error occurred setting up filters", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupInitialValues() {
        // Set profit range
        if (currentCriteria != null) {
            profitRangeSlider.setValues(
                (float) currentCriteria.getMinProfitPercent(), 
                (float) currentCriteria.getMaxProfitPercent()
            );
            
            // Set risk level chips
            for (String riskLevel : currentCriteria.getRiskLevels()) {
                for (int i = 0; i < riskLevelChipGroup.getChildCount(); i++) {
                    Chip chip = (Chip) riskLevelChipGroup.getChildAt(i);
                    if (chip.getText().toString().equals(riskLevel)) {
                        chip.setChecked(true);
                        break;
                    }
                }
            }
            
            // Set exchange chips
            for (String exchange : currentCriteria.getExchanges()) {
                for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
                    Chip chip = (Chip) exchangeChipGroup.getChildAt(i);
                    if (chip.getText().toString().equals(exchange)) {
                        chip.setChecked(true);
                        break;
                    }
                }
            }
        }
    }
    
    private void setupClickListeners() {
        // Reset button
        resetButton.setOnClickListener(v -> {
            profitRangeSlider.setValues(0f, 100f);
            
            // Clear all chip selections
            for (int i = 0; i < riskLevelChipGroup.getChildCount(); i++) {
                ((Chip) riskLevelChipGroup.getChildAt(i)).setChecked(false);
            }
            
            for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
                ((Chip) exchangeChipGroup.getChildAt(i)).setChecked(false);
            }
            
            // Reset current criteria
            currentCriteria = new FilterCriteria();
        });
        
        // Apply button
        applyButton.setOnClickListener(v -> {
            // Collect filter criteria
            collectFilterCriteria();
            
            // Send result back to parent fragment
            Bundle result = new Bundle();
            result.putParcelable("filterCriteria", currentCriteria);
            getParentFragmentManager().setFragmentResult("filterRequestKey", result);
            
            // Dismiss the dialog
            dismiss();
        });
    }
    
    private void collectFilterCriteria() {
        try {
            // Get profit range values
            List<Float> profitValues = profitRangeSlider.getValues();
            if (profitValues != null && profitValues.size() >= 2) {
                currentCriteria.setMinProfitPercent(profitValues.get(0));
                currentCriteria.setMaxProfitPercent(profitValues.get(1));
            }
            
            // Get selected risk levels
            Set<String> selectedRiskLevels = new HashSet<>();
            for (int i = 0; i < riskLevelChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) riskLevelChipGroup.getChildAt(i);
                if (chip.isChecked()) {
                    selectedRiskLevels.add(chip.getText().toString());
                }
            }
            currentCriteria.setRiskLevels(selectedRiskLevels);
            
            // Get selected exchanges
            Set<String> selectedExchanges = new HashSet<>();
            for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) exchangeChipGroup.getChildAt(i);
                if (chip.isChecked()) {
                    selectedExchanges.add(chip.getText().toString());
                }
            }
            currentCriteria.setExchanges(selectedExchanges);
            
        } catch (Exception e) {
            Log.e(TAG, "Error collecting filter criteria: " + e.getMessage(), e);
        }
    }
} 