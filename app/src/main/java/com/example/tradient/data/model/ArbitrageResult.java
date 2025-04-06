package com.example.tradient.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of the ArbitrageResult interface.
 * This class represents the result of an arbitrage opportunity scan,
 * containing a list of opportunities found along with metadata.
 */
public class ArbitrageResult implements com.example.tradient.data.interfaces.ArbitrageResult {
    private final List<ArbitrageOpportunity> opportunities;
    private final long timestamp;
    
    /**
     * Default constructor that creates an empty result.
     */
    public ArbitrageResult() {
        this.opportunities = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Constructor with a list of opportunities.
     *
     * @param opportunities The list of arbitrage opportunities
     */
    public ArbitrageResult(List<ArbitrageOpportunity> opportunities) {
        this.opportunities = opportunities != null ? 
            new ArrayList<>(opportunities) : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public List<ArbitrageOpportunity> getOpportunities() {
        return Collections.unmodifiableList(opportunities);
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public int getOpportunityCount() {
        return opportunities.size();
    }
    
    @Override
    public ArbitrageOpportunity getBestOpportunity() {
        if (opportunities.isEmpty()) {
            return null;
        }
        
        return opportunities.stream()
            .max((a, b) -> Double.compare(a.getPotentialProfit(), b.getPotentialProfit()))
            .orElse(null);
    }
    
    /**
     * Add an opportunity to the result.
     *
     * @param opportunity The opportunity to add
     */
    public void addOpportunity(ArbitrageOpportunity opportunity) {
        if (opportunity != null) {
            opportunities.add(opportunity);
        }
    }
    
    /**
     * Creates a string representation of this result including the number of opportunities.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ArbitrageResult with ").append(getOpportunityCount()).append(" opportunities\n");
        
        if (!opportunities.isEmpty()) {
            sb.append("Best opportunity: ")
              .append(getBestOpportunity().toString())
              .append("\n");
        }
        
        return sb.toString();
    }
} 