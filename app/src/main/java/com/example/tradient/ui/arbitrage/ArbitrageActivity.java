package com.example.tradient.ui.arbitrage;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradient.R;
import com.example.tradient.ui.filter.ArbitrageFilterBottomSheet;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ArbitrageActivity extends AppCompatActivity implements ArbitrageFilterBottomSheet.FilterListener {

    private RecyclerView recyclerView;
    private FloatingActionButton fabFilter;
    private Chip chipActiveFilters;
    private TextView tvNoResults;
    
    // Keep track of current filters
    private ArbitrageFilterBottomSheet.FilterCriteria currentFilters = new ArbitrageFilterBottomSheet.FilterCriteria();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arbitrage);
        
        // Initialize UI components
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Arbitrage Opportunities");
        
        recyclerView = findViewById(R.id.recyclerView);
        fabFilter = findViewById(R.id.fabFilter);
        chipActiveFilters = findViewById(R.id.chipActiveFilters);
        tvNoResults = findViewById(R.id.tvNoResults);
        
        // Set up click listeners
        fabFilter.setOnClickListener(v -> showFilterBottomSheet());
        chipActiveFilters.setOnClickListener(v -> showFilterBottomSheet());
        
        // Initially hide the active filters chip
        chipActiveFilters.setVisibility(View.GONE);
        
        // Load data with default filters
        loadArbitrageOpportunities();
    }
    
    /**
     * Show the filter bottom sheet dialog
     */
    private void showFilterBottomSheet() {
        ArbitrageFilterBottomSheet bottomSheet = ArbitrageFilterBottomSheet.newInstance(currentFilters);
        bottomSheet.setFilterListener(this);
        
        // Set the style explicitly
        bottomSheet.setStyle(BottomSheetDialogFragment.STYLE_NORMAL, 
                              R.style.ThemeOverlay_Tradient_BottomSheetDialog);
        
        // Show the bottom sheet with a specific tag
        bottomSheet.show(getSupportFragmentManager(), "ArbitrageFilter");
        
        // Log for debugging
        Toast.makeText(this, "Opening filter sheet", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Load arbitrage opportunities based on current filters
     */
    private void loadArbitrageOpportunities() {
        // Here you would call your repository or API to get filtered data
        // For example:
        // arbitrageRepository.getFilteredOpportunities(currentFilters, this::displayOpportunities);
        
        // For demo purposes, just show a toast with the filter criteria
        Toast.makeText(this, "Loading with filters: " + currentFilters.toString(), Toast.LENGTH_SHORT).show();
        
        // Update UI to show active filters
        updateActiveFiltersChip();
    }
    
    /**
     * Update the active filters chip to show current filter state
     */
    private void updateActiveFiltersChip() {
        if (currentFilters.hasActiveFilters()) {
            chipActiveFilters.setText(currentFilters.getFilterSummary());
            chipActiveFilters.setVisibility(View.VISIBLE);
        } else {
            chipActiveFilters.setVisibility(View.GONE);
        }
    }
    
    /**
     * Callback when filters are applied from the bottom sheet
     */
    @Override
    public void onFiltersApplied(ArbitrageFilterBottomSheet.FilterCriteria filters) {
        // Update our current filters
        currentFilters = filters;
        
        // Reload data with new filters
        loadArbitrageOpportunities();
    }
} 