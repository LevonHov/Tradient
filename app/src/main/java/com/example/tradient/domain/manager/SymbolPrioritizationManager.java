package com.example.tradient.domain.manager;

import android.util.Log;

import com.example.tradient.data.model.ArbitrageOpportunity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager responsible for prioritizing trading symbols
 */
public class SymbolPrioritizationManager {
    private static final String TAG = "SymbolPrioritizationMgr";
    
    // Maps symbols to their priority data
    private final ConcurrentHashMap<String, SymbolPriorityData> symbolPriorityMap = new ConcurrentHashMap<>();
    
    // Cached list of symbols sorted by priority
    private List<String> prioritizedSymbols = new ArrayList<>();
    
    /**
     * Initialize the manager
     */
    public SymbolPrioritizationManager() {
        // Initial setup if needed
    }
    
    /**
     * Initialize with priority symbols
     */
    public void initWithPrioritySymbols(List<String> prioritySymbols) {
        for (String symbol : prioritySymbols) {
            SymbolPriorityData data = new SymbolPriorityData();
            data.symbol = symbol;
            data.priorityScore = 100; // Very high priority
            symbolPriorityMap.put(symbol, data);
        }
        refreshPrioritizedList();
    }
    
    /**
     * Updates symbol priority data
     */
    public void updateSymbolData(String symbol, double volume, double volatility) {
        SymbolPriorityData data = symbolPriorityMap.computeIfAbsent(
            symbol, k -> new SymbolPriorityData());
        
        data.symbol = symbol;
        data.dailyVolume = volume;
        data.volatilityScore = volatility;
        
        // Simple priority score calculation
        data.priorityScore = (volume * 0.7) + (volatility * 0.3);
        
        refreshPrioritizedList();
    }
    
    /**
     * Recalculates the prioritized list of symbols
     */
    private void refreshPrioritizedList() {
        // Create a new list with all symbols
        List<String> newPrioritizedList = new ArrayList<>(symbolPriorityMap.size());
        for (SymbolPriorityData data : symbolPriorityMap.values()) {
            newPrioritizedList.add(data.symbol);
        }
        
        // Sort by priority score (descending)
        Collections.sort(newPrioritizedList, (s1, s2) -> {
            SymbolPriorityData d1 = symbolPriorityMap.get(s1);
            SymbolPriorityData d2 = symbolPriorityMap.get(s2);
            if (d1 == null || d2 == null) return 0;
            return Double.compare(d2.priorityScore, d1.priorityScore);
        });
        
        // Update the reference
        prioritizedSymbols = newPrioritizedList;
    }
    
    /**
     * Gets a batch of symbols to process
     */
    public List<String> getNextSymbolBatch(int batchSize, int tier) {
        if (prioritizedSymbols.isEmpty()) {
            return Collections.emptyList();
        }
        
        int startIndex = tier * batchSize;
        int endIndex = Math.min(startIndex + batchSize, prioritizedSymbols.size());
        
        // Return empty list if we're beyond available symbols
        if (startIndex >= prioritizedSymbols.size()) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(prioritizedSymbols.subList(startIndex, endIndex));
    }
    
    /**
     * Get high priority symbols
     */
    public List<String> getHighPrioritySymbols() {
        return getNextSymbolBatch(20, 0);
    }
    
    /**
     * Internal data structure for symbol prioritization
     */
    static class SymbolPriorityData {
        String symbol;              // Normalized trading pair symbol
        double dailyVolume;         // 24h trading volume in USD
        double volatilityScore;     // Volatility metric (0-100)
        double priorityScore;       // Computed priority score
    }
} 