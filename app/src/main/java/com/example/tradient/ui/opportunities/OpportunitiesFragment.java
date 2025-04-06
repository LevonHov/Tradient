package com.example.tradient.ui.opportunities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradient.R;
import com.example.tradient.data.factory.ViewModelFactory;
import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.repository.ExchangeRepository;
import com.example.tradient.viewmodel.ArbitrageViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    
    private List<ArbitrageOpportunity> allOpportunities = new ArrayList<>();
    private String currentSearchQuery = "";
    private String currentChipFilter = "";

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

        // Set up RecyclerView
        opportunitiesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        opportunityAdapter = new OpportunityAdapter(new ArrayList<>());
        opportunitiesRecyclerView.setAdapter(opportunityAdapter);

        // Set up search functionality
        setupSearchFunctionality();
        
        // Set up filter chips
        setupFilterChips();

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

    private void observeViewModel() {
        viewModel.getArbitrageOpportunities().observe(getViewLifecycleOwner(), this::updateOpportunities);
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), this::updateStatus);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), this::showError);
    }

    private void updateOpportunities(List<ArbitrageOpportunity> opportunities) {
        if (opportunities != null && !opportunities.isEmpty()) {
            allOpportunities = new ArrayList<>(opportunities);
            filterOpportunities();
        } else {
            allOpportunities.clear();
            showEmptyState();
        }
    }
    
    private void filterOpportunities() {
        List<ArbitrageOpportunity> filteredList = allOpportunities;
        
        // Apply text search filter
        if (!currentSearchQuery.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(op -> op.getSymbol().toLowerCase(Locale.getDefault()).contains(currentSearchQuery))
                    .collect(Collectors.toList());
        }
        
        // Apply chip filter
        if (!currentChipFilter.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(op -> op.getSymbol().contains(currentChipFilter))
                    .collect(Collectors.toList());
        }
        
        // Update UI based on filtered results
        if (filteredList.isEmpty() && !allOpportunities.isEmpty()) {
            emptyStateText.setText(R.string.no_search_results);
            emptyStateText.setVisibility(View.VISIBLE);
            opportunitiesRecyclerView.setVisibility(View.GONE);
        } else if (filteredList.isEmpty()) {
            showEmptyState();
        } else {
            emptyStateText.setVisibility(View.GONE);
            opportunitiesRecyclerView.setVisibility(View.VISIBLE);
            opportunityAdapter.updateOpportunities(filteredList);
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
} 