package com.example.tradient.domain.slippage;

import java.util.List;

/**
 * Basic slippage calculator that uses a linear approach based on order book volume.
 * Walks through the order book to estimate price impact.
 */
public class LinearVolumeSlippageCalculator implements SlippageCalculator {
    
    @Override
    public SlippageResult calculateBuySlippage(OrderBookSnapshot orderBookSnapshot, double orderSize) {
        List<OrderBookSnapshot.PriceLevel> asks = orderBookSnapshot.getAsks();
        
        if (asks.isEmpty()) {
            return createDefaultResult(0.0, orderSize, SlippageConfidence.VERY_LOW);
        }
        
        // Get the best ask price as base price
        double basePrice = asks.get(0).getPrice();
        
        // Walk the order book
        double remainingSize = orderSize;
        double weightedAvgPrice = 0;
        double filledSize = 0;
        
        for (OrderBookSnapshot.PriceLevel level : asks) {
            double levelPrice = level.getPrice();
            double levelVolume = level.getVolume();
            
            double sizeToTake = Math.min(remainingSize, levelVolume);
            weightedAvgPrice += levelPrice * sizeToTake;
            filledSize += sizeToTake;
            remainingSize -= sizeToTake;
            
            if (remainingSize <= 0) {
                break;
            }
        }
        
        // If we couldn't fill the entire order
        if (filledSize < orderSize) {
            // Use the last ask price plus a penalty
            double lastPrice = asks.get(asks.size() - 1).getPrice();
            double unfulfillablePenalty = lastPrice * 0.05; // 5% penalty for unfulfillable portion
            weightedAvgPrice += (orderSize - filledSize) * (lastPrice + unfulfillablePenalty);
            filledSize = orderSize;
        }
        
        double effectivePrice = weightedAvgPrice / filledSize;
        double slippagePercentage = ((effectivePrice - basePrice) / basePrice) * 100;
        
        // Determine confidence based on order book coverage
        int depth = asks.size();
        double volumeCoverage = Math.min(1.0, filledSize / orderSize);
        SlippageConfidence confidence = SlippageConfidence.determineConfidence(depth, volumeCoverage, true);
        
        return new SlippageResult(
                slippagePercentage,
                effectivePrice,
                basePrice,
                orderSize,
                confidence,
                getName()
        );
    }
    
    @Override
    public SlippageResult calculateSellSlippage(OrderBookSnapshot orderBookSnapshot, double orderSize) {
        List<OrderBookSnapshot.PriceLevel> bids = orderBookSnapshot.getBids();
        
        if (bids.isEmpty()) {
            return createDefaultResult(0.0, orderSize, SlippageConfidence.VERY_LOW);
        }
        
        // Get the best bid price as base price
        double basePrice = bids.get(0).getPrice();
        
        // Walk the order book
        double remainingSize = orderSize;
        double weightedAvgPrice = 0;
        double filledSize = 0;
        
        for (OrderBookSnapshot.PriceLevel level : bids) {
            double levelPrice = level.getPrice();
            double levelVolume = level.getVolume();
            
            double sizeToTake = Math.min(remainingSize, levelVolume);
            weightedAvgPrice += levelPrice * sizeToTake;
            filledSize += sizeToTake;
            remainingSize -= sizeToTake;
            
            if (remainingSize <= 0) {
                break;
            }
        }
        
        // If we couldn't fill the entire order
        if (filledSize < orderSize) {
            // Use the last bid price minus a penalty
            double lastPrice = bids.get(bids.size() - 1).getPrice();
            double unfulfillablePenalty = lastPrice * 0.05; // 5% penalty for unfulfillable portion
            weightedAvgPrice += (orderSize - filledSize) * (lastPrice - unfulfillablePenalty);
            filledSize = orderSize;
        }
        
        double effectivePrice = weightedAvgPrice / filledSize;
        double slippagePercentage = ((basePrice - effectivePrice) / basePrice) * 100;
        
        // Determine confidence based on order book coverage
        int depth = bids.size();
        double volumeCoverage = Math.min(1.0, filledSize / orderSize);
        SlippageConfidence confidence = SlippageConfidence.determineConfidence(depth, volumeCoverage, true);
        
        return new SlippageResult(
                slippagePercentage,
                effectivePrice,
                basePrice,
                orderSize,
                confidence,
                getName()
        );
    }
    
    @Override
    public String getName() {
        return "LinearVolumeSlippage";
    }
    
    private SlippageResult createDefaultResult(double basePrice, double orderSize, SlippageConfidence confidence) {
        // Default to 1% slippage when unable to calculate
        return new SlippageResult(
                1.0,
                basePrice * 1.01, // 1% higher for buy
                basePrice,
                orderSize,
                confidence,
                getName() + "-Default"
        );
    }
} 