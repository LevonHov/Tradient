package com.example.tradient.data.fee;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to calculate different types of fees with special handling 
 * for exchange-specific fee structures and discount mechanisms.
 */
public class FeeCalculator {
    
    // Available discount types
    public enum DiscountType {
        NONE,
        BNB_PAYMENT,  // Binance BNB payment discount (25%)
        VIP_TIER,     // Exchange VIP status discount
        CUSTOM        // Custom discount
    }
    
    private static final Map<String, Map<DiscountType, Double>> EXCHANGE_DISCOUNT_RATES = initDiscountRates();
    
    /**
     * Initialize the exchange discount rates.
     *
     * @return Map of exchange names to discount types and rates
     */
    private static Map<String, Map<DiscountType, Double>> initDiscountRates() {
        Map<String, Map<DiscountType, Double>> discounts = new HashMap<>();
        
        // Binance discounts
        Map<DiscountType, Double> binanceDiscounts = new HashMap<>();
        binanceDiscounts.put(DiscountType.BNB_PAYMENT, 0.25); // 25% discount with BNB
        discounts.put("Binance", binanceDiscounts);
        
        // Coinbase discounts - could be added later
        Map<DiscountType, Double> coinbaseDiscounts = new HashMap<>();
        discounts.put("Coinbase", coinbaseDiscounts);
        
        // Kraken discounts - could be added later
        Map<DiscountType, Double> krakenDiscounts = new HashMap<>();
        discounts.put("Kraken", krakenDiscounts);
        
        // Bybit discounts - could be added later
        Map<DiscountType, Double> bybitDiscounts = new HashMap<>();
        discounts.put("Bybit", bybitDiscounts);
        
        return discounts;
    }
    
    /**
     * Calculate the fee with potential discounts.
     *
     * @param exchangeName The name of the exchange
     * @param fee The fee structure
     * @param amount The transaction amount
     * @param discountType The type of discount to apply
     * @return The calculated fee amount after discounts
     */
    public static double calculateFeeWithDiscount(String exchangeName, Fee fee, 
                                                 double amount, DiscountType discountType) {
        double baseFeeCost = fee.calculateFee(amount);
        
        if (discountType == DiscountType.NONE) {
            return baseFeeCost;
        }
        
        Map<DiscountType, Double> discounts = EXCHANGE_DISCOUNT_RATES.get(exchangeName);
        if (discounts == null || !discounts.containsKey(discountType)) {
            return baseFeeCost; // No discount available
        }
        
        double discountRate = discounts.get(discountType);
        return baseFeeCost * (1 - discountRate);
    }
    
    /**
     * Calculate net amount after fees for buying.
     *
     * @param price The price of the asset
     * @param quantity The quantity being purchased
     * @param fee The fee structure
     * @return The total cost including the fee
     */
    public static double calculateTotalBuyCost(double price, double quantity, Fee fee) {
        double tradeAmount = price * quantity;
        double feeCost = fee.calculateFee(tradeAmount);
        return tradeAmount + feeCost;
    }
    
    /**
     * Calculate net amount after fees for selling.
     *
     * @param price The price of the asset
     * @param quantity The quantity being sold
     * @param fee The fee structure
     * @return The net amount received after fees
     */
    public static double calculateNetSellProceeds(double price, double quantity, Fee fee) {
        double tradeAmount = price * quantity;
        double feeCost = fee.calculateFee(tradeAmount);
        return tradeAmount - feeCost;
    }
    
    /**
     * Calculate the net profit from a buy/sell arbitrage opportunity.
     *
     * @param buyPrice The price to buy at
     * @param sellPrice The price to sell at
     * @param quantity The quantity being traded
     * @param buyFee The fee structure for buying
     * @param sellFee The fee structure for selling
     * @return The absolute profit after fees
     */
    public static double calculateArbitrageProfit(double buyPrice, double sellPrice,
                                               double quantity, Fee buyFee, Fee sellFee) {
        // Calculate total buy cost with fees
        double buyAmount = buyPrice * quantity;
        double buyFeeCost = buyFee.calculateFee(buyAmount);
        double totalBuyCost = buyAmount + buyFeeCost;
        
        // Calculate net sell proceeds after fees
        double sellAmount = sellPrice * quantity;
        double sellFeeCost = sellFee.calculateFee(sellAmount);
        double netSellProceeds = sellAmount - sellFeeCost;
        
        // Calculate net profit
        return netSellProceeds - totalBuyCost;
    }
    
    /**
     * Calculate the percentage profit from a buy/sell arbitrage opportunity.
     *
     * @param buyPrice The price to buy at
     * @param sellPrice The price to sell at
     * @param quantity The quantity being traded
     * @param buyFee The fee structure for buying
     * @param sellFee The fee structure for selling
     * @return The percentage profit relative to investment
     */
    public static double calculateArbitrageProfitPercentage(double buyPrice, double sellPrice,
                                                         double quantity, Fee buyFee, Fee sellFee) {
        // Calculate absolute profit
        double absoluteProfit = calculateArbitrageProfit(buyPrice, sellPrice, quantity, buyFee, sellFee);
        
        // Calculate total investment (buy cost including fees)
        double buyAmount = buyPrice * quantity;
        double buyFeeCost = buyFee.calculateFee(buyAmount);
        double totalInvestment = buyAmount + buyFeeCost;
        
        // Calculate percentage profit
        return (absoluteProfit / totalInvestment) * 100;
    }
    
    /**
     * Register a new exchange discount or update an existing one.
     *
     * @param exchangeName The name of the exchange
     * @param discountType The type of discount
     * @param discountRate The discount rate as a decimal (e.g., 0.25 for 25%)
     */
    public static void registerExchangeDiscount(String exchangeName, 
                                               DiscountType discountType,
                                               double discountRate) {
        EXCHANGE_DISCOUNT_RATES.computeIfAbsent(exchangeName, k -> new HashMap<>())
                             .put(discountType, discountRate);
    }
    
    /**
     * Utility method to calculate profit percentage directly from prices and fees.
     * This replaces the incorrect formula: ((sellPrice - buyPrice) / buyPrice * 100) - buyFee - sellFee
     * with the correct formula that properly accounts for fee effects.
     *
     * @param buyPrice The buy price
     * @param sellPrice The sell price
     * @param buyFeePercent The buy fee as a decimal (e.g., 0.001 for 0.1%)
     * @param sellFeePercent The sell fee as a decimal (e.g., 0.001 for 0.1%)
     * @return The profit percentage
     */
    public static double calculateCorrectProfitPercentage(double buyPrice, double sellPrice, 
                                                double buyFeePercent, double sellFeePercent) {
        // Normalize fee tiers to ensure they're in decimal format
        if (buyFeePercent > 0.1) {
            buyFeePercent /= 100.0;
        }
        
        if (sellFeePercent > 0.1) {
            sellFeePercent /= 100.0;
        }
        
        // Calculate effective buy cost with fee
        double effectiveBuyCost = buyPrice * (1 + buyFeePercent);
        
        // Calculate effective sell revenue with fee
        double effectiveSellRevenue = sellPrice * (1 - sellFeePercent);
        
        // Calculate net profit per unit
        double netProfitPerUnit = effectiveSellRevenue - effectiveBuyCost;
        
        // Calculate percentage profit relative to investment
        return (netProfitPerUnit / effectiveBuyCost) * 100;
    }
} 