package com.example.tradient.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradient.R;
import com.example.tradient.data.factory.ViewModelFactory;
import com.example.tradient.repository.ExchangeRepository;
import com.example.tradient.viewmodel.ArbitrageViewModel;

import java.util.Map;

public class DashboardFragment extends Fragment {

    private ArbitrageViewModel viewModel;
    private TextView opportunitiesCountText;
    private TextView totalProfitText;
    private TextView activeExchangesText;
    private View chartPlaceholder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        opportunitiesCountText = view.findViewById(R.id.opportunities_count);
        totalProfitText = view.findViewById(R.id.total_profit);
        activeExchangesText = view.findViewById(R.id.active_exchanges);
        chartPlaceholder = view.findViewById(R.id.chart_placeholder);

        // Initialize ViewModel
        ExchangeRepository repository = new ExchangeRepository(requireContext());
        ViewModelFactory factory = new ViewModelFactory(repository);
        viewModel = new ViewModelProvider(requireActivity(), factory).get(ArbitrageViewModel.class);

        // Observe ViewModel data
        observeViewModel();

        // Initialize the data
        viewModel.initialize();
    }

    private void observeViewModel() {
        viewModel.getInitializationProgress().observe(getViewLifecycleOwner(), this::updateStats);
        viewModel.getArbitrageOpportunities().observe(getViewLifecycleOwner(), opportunities -> {
            if (opportunities != null) {
                opportunitiesCountText.setText(String.valueOf(opportunities.size()));
                // Display placeholder for total profit
                totalProfitText.setText("TBD");
            }
        });
    }

    private void updateStats(Map<String, Object> stats) {
        if (stats == null) return;
        
        // Update active exchanges count
        if (stats.containsKey("activeExchanges")) {
            activeExchangesText.setText(String.valueOf(stats.get("activeExchanges")));
        }
    }
} 