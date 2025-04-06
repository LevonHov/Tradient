package com.example.tradient.util;

import com.example.tradient.data.fee.Fee;
import com.example.tradient.data.fee.FeeCalculator;
import com.example.tradient.data.fee.PercentageFee;
import com.example.tradient.domain.profit.ProfitCalculator;
import com.example.tradient.domain.profit.ProfitResult;

/**
 * Utility class to standardize arbitrage profit calculations across the app.
 * This ensures consistent fee handling and profit calculations.
 */
public class ArbitrageProcessing {

    /**
     * Calculate arbitrage profit correctly using fee objects
     * 
     * @param buyPrice The price to buy at
     * @param sellPrice The price to sell at
     * @param buyFeeObj The buy fee object
     * @param sellFeeObj The sell fee object
     * @param quantity The quantity to trade
     * @return A ProfitResult containing the profit calculation
     */
    public static ProfitResult calculateProfit(
            double buyPrice,
            double sellPrice,
            Fee buyFeeObj,
            Fee sellFeeObj,
            double quantity) {
        
        // Extract fee percentages (in decimal format)
        double buyFeePercent = extractFeePercentage(buyFeeObj);
        double sellFeePercent = extractFeePercentage(sellFeeObj);
        
        // Use the ProfitCalculator for correct calculation
        return ProfitCalculator.calculateArbitrageProfit(
                buyPrice, 
                sellPrice, 
                buyFeePercent, 
                sellFeePercent, 
                quantity);
    }
    
    /**
     * Calculate arbitrage profit directly with fee percentages
     * 
     * @param buyPrice The price to buy at
     * @param sellPrice The price to sell at
     * @param buyFeePercent The buy fee as decimal (e.g., 0.001 for 0.1%)
     * @param sellFeePercent The sell fee as decimal (e.g., 0.001 for 0.1%)
     * @param quantity The quantity to trade
     * @return A ProfitResult containing the profit calculation
     */
    public static ProfitResult calculateProfit(
            double buyPrice,
            double sellPrice,
            double buyFeePercent,
            double sellFeePercent,
            double quantity) {
        
        // Ensure fees are in decimal format
        double normalizedBuyFee = normalizeFeeTier(buyFeePercent);
        double normalizedSellFee = normalizeFeeTier(sellFeePercent);
        
        // Use the ProfitCalculator for correct calculation
        return ProfitCalculator.calculateArbitrageProfit(
                buyPrice, 
                sellPrice, 
                normalizedBuyFee, 
                normalizedSellFee, 
                quantity);
    }
    
    /**
     * Calculate profit percentage for arbitrage using the correct formula
     * 
     * @param buyPrice The price to buy at
     * @param sellPrice The price to sell at
     * @param buyFeePercent The buy fee as decimal (e.g., 0.001 for 0.1%)
     * @param sellFeePercent The sell fee as decimal (e.g., 0.001 for 0.1%)
     * @return The profit percentage
     */
    public static double calculateProfitPercentage(
            double buyPrice,
            double sellPrice,
            double buyFeePercent,
            double sellFeePercent) {
        
        // Ensure fees are in decimal format
        double normalizedBuyFee = normalizeFeeTier(buyFeePercent);
        double normalizedSellFee = normalizeFeeTier(sellFeePercent);
        
        // Calculate effective costs with fees
        double effectiveBuyCost = buyPrice * (1 + normalizedBuyFee);
        double effectiveSellRevenue = sellPrice * (1 - normalizedSellFee);
        
        // Calculate net profit per unit
        double netProfitPerUnit = effectiveSellRevenue - effectiveBuyCost;
        
        // Calculate percentage profit relative to investment
        return (netProfitPerUnit / effectiveBuyCost) * 100;
    }
    
    /**
     * Extract fee percentage from a Fee object
     * 
     * @param feeObj The fee object
     * @return The fee percentage as decimal (e.g., 0.001 for 0.1%)
     */
    private static double extractFeePercentage(Fee feeObj) {
        if (feeObj == null) {
            return 0.001; // Default 0.1% if no fee object
        }
        
        if (feeObj instanceof PercentageFee) {
            return ((PercentageFee) feeObj).getPercentage();
        }
        
        // For other fee types, calculate as percentage of standard amount
        return feeObj.calculateFee(10000) / 10000;
    }
    
    /**
     * Normalize fee tier to ensure it's in decimal format (0.001 for 0.1%)
     * 
     * @param fee The fee value to normalize
     * @return The normalized fee as decimal
     */
    private static double normalizeFeeTier(double fee) {
        // If fee is already small (< 0.1), it's likely already in decimal format
        if (fee < 0.1) {
            return fee;
        }
        
        // If fee is larger, it might be in percentage format, so convert to decimal
        return fee / 100.0;
    }
} 