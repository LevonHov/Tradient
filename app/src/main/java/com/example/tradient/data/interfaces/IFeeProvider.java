package com.example.tradient.data.interfaces;

import com.example.tradient.data.fee.Fee;
import com.example.tradient.data.fee.FeeTracker;

/**
 * Interface for fee-related exchange functionality.
 * Follows the Interface Segregation Principle by separating fee concerns.
 */
public interface IFeeProvider {
    /**
     * Get the maker fee for this exchange.
     *
     * @return The maker fee
     */
    Fee getMakerFee();
    
    /**
     * Get the taker fee for this exchange.
     *
     * @return The taker fee
     */
    Fee getTakerFee();
    
    /**
     * Get the maker fee for a specific trading pair.
     * This allows for specialized fee structures for certain pairs.
     *
     * @param tradingPair The trading pair
     * @return The maker fee for this pair
     */
    Fee getMakerFee(String tradingPair);
    
    /**
     * Get the taker fee for a specific trading pair.
     * This allows for specialized fee structures for certain pairs.
     *
     * @param tradingPair The trading pair
     * @return The taker fee for this pair
     */
    Fee getTakerFee(String tradingPair);
    
    /**
     * Update fee tiers based on trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume
     */
    void updateFeesTiers(double thirtyDayVolume);
    
    /**
     * Calculate and track a fee for a transaction.
     *
     * @param tradingPair The trading pair
     * @param amount The transaction amount
     * @param isMaker Whether this is a maker order
     * @return The calculated fee amount
     */
    double calculateAndTrackFee(String tradingPair, double amount, boolean isMaker);
    
    /**
     * Get the fee tracker for this exchange.
     *
     * @return The fee tracker
     */
    FeeTracker getFeeTracker();
    
    /**
     * Set whether a specific fee discount is enabled.
     * For Binance, this would be the BNB discount.
     *
     * @param discountType The type of discount
     * @param enabled Whether the discount is enabled
     */
    void setFeeDiscount(String discountType, boolean enabled);
    
    /**
     * Check if a specific fee discount is enabled.
     *
     * @param discountType The type of discount
     * @return true if the discount is enabled, false otherwise
     */
    boolean hasFeeDiscount(String discountType);
} 