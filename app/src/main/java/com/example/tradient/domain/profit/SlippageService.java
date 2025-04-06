package com.example.tradient.domain.profit;

import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.OrderBookEntry;

import java.util.List;

/**
 * Service that calculates market slippage for buy and sell orders based on order book data
 */
public class SlippageService {
    
    /**
     * Calculate the slippage for a market buy order
     * @param orderBook The order book containing ask data
     * @param tradeSize The size of the trade in base currency units
     * @return The slippage percentage as a decimal (e.g., 0.01 = 1%)
     */
    public double calculateBuySlippage(OrderBook orderBook, double tradeSize) {
        List<OrderBookEntry> asks = orderBook.getAsks();
        if (asks == null || asks.isEmpty()) {
            return 0.005; // Default 0.5% slippage if no data
        }
        
        double totalCost = 0;
        double totalQuantity = 0;
        double initialPrice = asks.get(0).getPrice();
        
        for (OrderBookEntry entry : asks) {
            double available = entry.getQuantity();
            double needed = tradeSize - totalQuantity;
            
            if (needed <= 0) {
                break;
            }
            
            double toTake = Math.min(available, needed);
            totalCost += toTake * entry.getPrice();
            totalQuantity += toTake;
            
            if (totalQuantity >= tradeSize) {
                break;
            }
        }
        
        // If we couldn't fill the order completely
        if (totalQuantity < tradeSize) {
            // Add a penalty for insufficient liquidity
            return 0.02; // 2% slippage
        }
        
        double averagePrice = totalCost / totalQuantity;
        double slippage = (averagePrice - initialPrice) / initialPrice;
        
        return Math.max(0, slippage); // Can't have negative slippage
    }
    
    /**
     * Calculate the slippage for a market sell order
     * @param orderBook The order book containing bid data
     * @param tradeSize The size of the trade in base currency units
     * @return The slippage percentage as a decimal (e.g., 0.01 = 1%)
     */
    public double calculateSellSlippage(OrderBook orderBook, double tradeSize) {
        List<OrderBookEntry> bids = orderBook.getBids();
        if (bids == null || bids.isEmpty()) {
            return 0.005; // Default 0.5% slippage if no data
        }
        
        double totalRevenue = 0;
        double totalQuantity = 0;
        double initialPrice = bids.get(0).getPrice();
        
        for (OrderBookEntry entry : bids) {
            double available = entry.getQuantity();
            double needed = tradeSize - totalQuantity;
            
            if (needed <= 0) {
                break;
            }
            
            double toTake = Math.min(available, needed);
            totalRevenue += toTake * entry.getPrice();
            totalQuantity += toTake;
            
            if (totalQuantity >= tradeSize) {
                break;
            }
        }
        
        // If we couldn't fill the order completely
        if (totalQuantity < tradeSize) {
            // Add a penalty for insufficient liquidity
            return 0.02; // 2% slippage
        }
        
        double averagePrice = totalRevenue / totalQuantity;
        double slippage = (initialPrice - averagePrice) / initialPrice;
        
        return Math.max(0, slippage); // Can't have negative slippage
    }
} 