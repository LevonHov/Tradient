package com.example.tradient.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.tradient.R;
import com.example.tradient.ui.utils.WidgetAnimator;
import com.google.android.material.card.MaterialCardView;

/**
 * Manager class for key metric widgets in the Tradient app.
 */
public class KeyMetricWidgetManager {

    private final MaterialCardView cardView;
    private final ConstraintLayout container;
    private final TextView metricValue;
    private final TextView changeIndicator;
    
    // Hardcoded colors until resource compilation succeeds
    private static final int POSITIVE_CHANGE = Color.parseColor("#00C087");
    private static final int POSITIVE_CHANGE_BG = Color.parseColor("#1A00C087");
    private static final int NEGATIVE_CHANGE = Color.parseColor("#FF5C5C");
    private static final int NEGATIVE_CHANGE_BG = Color.parseColor("#1AFF5C5C");
    
    /**
     * Constructor that initializes the widget and sets up animations.
     *
     * @param rootView The widget's root view
     */
    public KeyMetricWidgetManager(@NonNull View rootView) {
        // Find views - use hardcoded IDs or direct child access
        cardView = (MaterialCardView) rootView;
        container = (ConstraintLayout) cardView.getChildAt(0);
        
        // Find children by traversing view hierarchy, not by ID
        ViewGroup layout = container;
        
        // Get child at index 1 (metric value) and 2 (change indicator)
        // The typical order in ConstraintLayout would be:
        // 0: Title, 1: Value, 2: Change indicator, 3: More icon
        metricValue = (TextView) layout.getChildAt(1);
        changeIndicator = (TextView) layout.getChildAt(2);
        
        // Set up animations
        setupAnimations();
    }
    
    /**
     * Sets up all animations for the widget.
     */
    private void setupAnimations() {
        // Apply touch feedback to card
        WidgetAnimator.applyTouchFeedback(cardView);
        
        // Set click listener for card
        cardView.setOnClickListener(v -> {
            // Temporarily disable while performing update
            WidgetAnimator.temporarilyDisable(cardView, 500);
            
            // Apply smooth transition before updating data
            WidgetAnimator.applyPropertyTransition(container, 500);
            
            // Update metrics (simulated)
            updateMetrics();
        });
    }
    
    /**
     * Updates the metric values with animations.
     */
    public void updateMetrics() {
        // This would typically fetch data from a ViewModel
        // For demonstration, we'll just toggle between two values
        
        String currentValue = metricValue.getText().toString();
        if (currentValue.equals("$1,234.56") || currentValue.isEmpty()) {
            metricValue.setText("$2,345.67");
            changeIndicator.setText("+23.4%");
            changeIndicator.setTextColor(POSITIVE_CHANGE);
            changeIndicator.getBackground().setTint(POSITIVE_CHANGE_BG);
        } else {
            metricValue.setText("$1,234.56");
            changeIndicator.setText("+12.3%");
            changeIndicator.setTextColor(POSITIVE_CHANGE);
            changeIndicator.getBackground().setTint(POSITIVE_CHANGE_BG);
        }
    }
    
    /**
     * Show negative change with appropriate styling.
     */
    public void showNegativeChange() {
        // Apply smooth transition
        WidgetAnimator.applyPropertyTransition(container, 500);
        
        // Update UI
        metricValue.setText("$987.65");
        changeIndicator.setText("-5.2%");
        changeIndicator.setTextColor(NEGATIVE_CHANGE);
        changeIndicator.getBackground().setTint(NEGATIVE_CHANGE_BG);
    }
} 