package com.example.tradient.data.factory;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.tradient.repository.ExchangeRepository;
import com.example.tradient.viewmodel.ArbitrageViewModel;

/**
 * Factory class for creating ViewModels with dependencies.
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final ExchangeRepository exchangeRepository;

    public ViewModelFactory(ExchangeRepository exchangeRepository) {
        this.exchangeRepository = exchangeRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ArbitrageViewModel.class)) {
            return (T) new ArbitrageViewModel(exchangeRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
} 