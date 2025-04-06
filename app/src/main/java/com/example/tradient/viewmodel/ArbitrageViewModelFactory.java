package com.example.tradient.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradient.repository.ExchangeRepository;

/**
 * Factory to create ArbitrageViewModel with dependencies
 */
public class ArbitrageViewModelFactory implements ViewModelProvider.Factory {
    
    private final ExchangeRepository exchangeRepository;
    
    public ArbitrageViewModelFactory(ExchangeRepository exchangeRepository) {
        this.exchangeRepository = exchangeRepository;
    }
    
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ArbitrageViewModel.class)) {
            return modelClass.cast(new ArbitrageViewModel(exchangeRepository));
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
} 