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
        
        try {
            // Extract existing filter criteria if provided
            Bundle args = getArguments();
            Log.d(TAG, "onCreate: Arguments bundle is " + (args != null ? "present" : "null"));
            
            if (args != null && args.containsKey("current_filter")) {
                Log.d(TAG, "onCreate: Found current_filter in arguments");
                try {
                    Object parcelable = args.getParcelable("current_filter");
                    Log.d(TAG, "onCreate: Parcelable type is: " + (parcelable != null ? parcelable.getClass().getName() : "null"));
                    
                    currentCriteria = args.getParcelable("current_filter");
                    if (currentCriteria == null) {
                        Log.w(TAG, "onCreate: Parcelable was null, creating new FilterCriteria");
                        currentCriteria = new FilterCriteria();
                    } else {
                        Log.d(TAG, "onCreate: Successfully retrieved FilterCriteria: " + currentCriteria);
                    }
                } catch (ClassCastException e) {
                    Log.e(TAG, "onCreate: ClassCastException while getting Parcelable. Expected FilterCriteria but got different type", e);
                    Log.e(TAG, "onCreate: Stack trace for debugging:", e);
                    currentCriteria = new FilterCriteria();
                }
            } else {
                Log.d(TAG, "onCreate: No current_filter in arguments, creating new FilterCriteria");
                // Initialize with default criteria
                currentCriteria = new FilterCriteria();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Log.e(TAG, "onCreate: Full stack trace for debugging:", e);
            currentCriteria = new FilterCriteria(); // Fallback to default
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
            Log.d(TAG, "onViewCreated: Starting view initialization");
            
            // Initialize UI components
            profitRangeSlider = view.findViewById(R.id.profitRangeSlider);
            riskLevelChipGroup = view.findViewById(R.id.riskLevelChipGroup);
            exchangeChipGroup = view.findViewById(R.id.exchangeChipGroup);
            resetButton = view.findViewById(R.id.resetButton);
            applyButton = view.findViewById(R.id.applyButton);
            
            // Log the status of each view
            Log.d(TAG, "onViewCreated: View initialization status:");
            Log.d(TAG, "- profitRangeSlider: " + (profitRangeSlider != null ? "found" : "missing"));
            Log.d(TAG, "- riskLevelChipGroup: " + (riskLevelChipGroup != null ? "found" : "missing"));
            Log.d(TAG, "- exchangeChipGroup: " + (exchangeChipGroup != null ? "found" : "missing"));
            Log.d(TAG, "- resetButton: " + (resetButton != null ? "found" : "missing"));
            Log.d(TAG, "- applyButton: " + (applyButton != null ? "found" : "missing"));
            
            // Verify UI components are found
            if (profitRangeSlider == null || riskLevelChipGroup == null || 
                exchangeChipGroup == null || resetButton == null || applyButton == null) {
                String missingViews = "";
                if (profitRangeSlider == null) missingViews += "profitRangeSlider, ";
                if (riskLevelChipGroup == null) missingViews += "riskLevelChipGroup, ";
                if (exchangeChipGroup == null) missingViews += "exchangeChipGroup, ";
                if (resetButton == null) missingViews += "resetButton, ";
                if (applyButton == null) missingViews += "applyButton, ";
                missingViews = missingViews.replaceAll(", $", "");
                
                Log.e(TAG, "onViewCreated: Missing required views: " + missingViews);
                Toast.makeText(requireContext(), "Error initializing filter dialog", Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }
            
            Log.d(TAG, "onViewCreated: All views found, setting up initial values");
            
            // Set up initial values
            setupInitialValues();
            
            Log.d(TAG, "onViewCreated: Setting up click listeners");
            
            // Set up click listeners
            setupClickListeners();
            
            Log.d(TAG, "onViewCreated: View initialization complete");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onViewCreated: " + e.getMessage(), e);
            Log.e(TAG, "onViewCreated: Full stack trace:", e);
            Toast.makeText(requireContext(), "An error occurred setting up filters", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }
    
    private void setupInitialValues() {
        Log.d(TAG, "setupInitialValues: Starting setup");
        
        if (currentCriteria == null) {
            Log.w(TAG, "setupInitialValues: currentCriteria is null, creating new instance");
            currentCriteria = new FilterCriteria();
        }

        try {
            // Set profit range
            Log.d(TAG, "setupInitialValues: Setting profit range - min: " + currentCriteria.getMinProfitPercent() + 
                      ", max: " + currentCriteria.getMaxProfitPercent());
            
            try {
                profitRangeSlider.setValues(
                    (float) currentCriteria.getMinProfitPercent(), 
                    (float) currentCriteria.getMaxProfitPercent()
                );
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "setupInitialValues: Error setting profit range values", e);
                // Fallback to default values from arrays.xml
                profitRangeSlider.setValues(0f, 50f);
            }
            
            // Set risk level chips
            Log.d(TAG, "setupInitialValues: Setting risk levels");
            Set<String> riskLevels = currentCriteria.getRiskLevels();
            if (riskLevels != null && !riskLevels.isEmpty()) {
                Log.d(TAG, "setupInitialValues: Found risk levels: " + riskLevels);
                for (int i = 0; i < riskLevelChipGroup.getChildCount(); i++) {
                    View child = riskLevelChipGroup.getChildAt(i);
                    if (child instanceof Chip) {
                        Chip chip = (Chip) child;
                        CharSequence text = chip.getText();
                        Log.d("CRASH_PROBE", "Chip text check - chip ID: " + chip.getId() + 
                            ", text null? " + (text == null) + 
                            ", text empty? " + (text != null && text.length() == 0));
                        String chipText = text != null ? text.toString() : "";
                        boolean shouldBeChecked = riskLevels.contains(chipText);
                        Log.d(TAG, "setupInitialValues: Risk chip '" + chipText + "' should be " + 
                                  (shouldBeChecked ? "checked" : "unchecked"));
                        chip.setChecked(shouldBeChecked);
                    }
                }
            } else {
                Log.d(TAG, "setupInitialValues: No risk levels to set");
            }
            
            // Set exchange chips
            Log.d(TAG, "setupInitialValues: Setting exchanges");
            Set<String> exchanges = currentCriteria.getExchanges();
            if (exchanges != null && !exchanges.isEmpty()) {
                Log.d(TAG, "setupInitialValues: Found exchanges: " + exchanges);
                for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
                    View child = exchangeChipGroup.getChildAt(i);
                    if (child instanceof Chip) {
                        Chip chip = (Chip) child;
                        CharSequence text = chip.getText();
                        Log.d("CRASH_PROBE", "Chip text check - chip ID: " + chip.getId() + 
                            ", text null? " + (text == null) + 
                            ", text empty? " + (text != null && text.length() == 0));
                        String chipText = text != null ? text.toString() : "";
                        boolean shouldBeChecked = exchanges.contains(chipText);
                        Log.d(TAG, "setupInitialValues: Exchange chip '" + chipText + "' should be " + 
                                  (shouldBeChecked ? "checked" : "unchecked"));
                        chip.setChecked(shouldBeChecked);
                    }
                }
            } else {
                Log.d(TAG, "setupInitialValues: No exchanges to set");
            }
            
            Log.d(TAG, "setupInitialValues: Completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting initial values: " + e.getMessage(), e);
            Log.e(TAG, "setupInitialValues: Full stack trace:", e);
            // Don't rethrow - we want to continue even if some values couldn't be set
        }
    }
    
    private void setupClickListeners() {
        // Reset button
        resetButton.setOnClickListener(v -> {
            try {
                profitRangeSlider.setValues(0f, 50f);
                
                // Clear all chip selections
                for (int i = 0; i < riskLevelChipGroup.getChildCount(); i++) {
                    Chip chip = (Chip) riskLevelChipGroup.getChildAt(i);
                    if (chip != null) {
                        chip.setChecked(false);
                    }
                }
                
                for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
                    Chip chip = (Chip) exchangeChipGroup.getChildAt(i);
                    if (chip != null) {
                        chip.setChecked(false);
                    }
                }
                
                // Reset current criteria
                currentCriteria = new FilterCriteria();
            } catch (Exception e) {
                Log.e(TAG, "Error in reset: " + e.getMessage(), e);
            }
        });
        
        // Apply button
        applyButton.setOnClickListener(v -> {
            try {
                // Collect filter criteria
                collectFilterCriteria();
                
                // Send result back to parent fragment
                Bundle result = new Bundle();
                result.putParcelable("filterCriteria", currentCriteria);
                getParentFragmentManager().setFragmentResult("filterRequestKey", result);
                
                // Dismiss the dialog
                dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error applying filters: " + e.getMessage(), e);
                Toast.makeText(requireContext(), "Error applying filters", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void collectFilterCriteria() {
        try {
            if (currentCriteria == null) {
                currentCriteria = new FilterCriteria();
            }

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
                if (chip != null && chip.isChecked()) {
                    selectedRiskLevels.add(chip.getText().toString());
                }
            }
            currentCriteria.setRiskLevels(selectedRiskLevels);
            
            // Get selected exchanges
            Set<String> selectedExchanges = new HashSet<>();
            for (int i = 0; i < exchangeChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) exchangeChipGroup.getChildAt(i);
                if (chip != null && chip.isChecked()) {
                    selectedExchanges.add(chip.getText().toString());
                }
            }
            currentCriteria.setExchanges(selectedExchanges);
            
        } catch (Exception e) {
            Log.e(TAG, "Error collecting filter criteria: " + e.getMessage(), e);
            throw e; // Re-throw to be caught by caller
        }
    }
} 