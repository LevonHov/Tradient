package com.example.tradient.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


import java.util.Arrays;
import java.util.Date;

public class ArbitrageOpportunity implements Parcelable {
    private TradingPair pair;
    private String exchangeBuy;
    private String exchangeSell;
    private double potentialProfit;

    // Additional fields to store more detailed information
    private String normalizedSymbol;
    private String symbolBuy;
    private String symbolSell;
    private double buyPrice;
    private double sellPrice;
    private double profitPercent;
    private double successfulArbitragePercent; // New property for successful arbitrage percentage

    // Fee-related fields
    private double buyFeePercentage;
    private double sellFeePercentage;
    private boolean isBuyMaker;  // Whether buy order is expected to be a maker order
    private boolean isSellMaker; // Whether sell order is expected to be a maker order

    // Slippage-related fields
    private double buySlippage;  // Expected slippage for the buy side
    private double sellSlippage; // Expected slippage for the sell side
    private Ticker buyTicker;    // Ticker data for the buy side
    private Ticker sellTicker;   // Ticker data for the sell side

    // Advanced metrics for better decision making
    private double priceDifferencePercentage;
    private double netProfitPercentage;
    private double riskScore;
    private double liquidity;
    private double volatility;
    private boolean isViable;

    private Date timestamp;
    private boolean executed;
    private double fees;
    private double slippage;

    // Add new fields to store time metrics and liquidity information
    private double estimatedTimeMinutes;
    private double roiEfficiency;
    private double liquidityFactor;
    private double buyExchangeLiquidity;
    private double sellExchangeLiquidity;
    private boolean isTimeSensitive;

    // Add missing fields
    private double volume;
    private double orderBookDepth;
    private double priceVolatility;
    private double totalSlippagePercentage;

    // Add a direct reference to RiskAssessment
    private RiskAssessment riskAssessment;

    public ArbitrageOpportunity() {
        this.timestamp = new Date();
        this.executed = false;
    }

    public ArbitrageOpportunity(double potentialProfit, String exchangeSell, String exchangeBuy, TradingPair pair) {
        this.potentialProfit = potentialProfit;
        this.exchangeSell = exchangeSell;
        this.exchangeBuy = exchangeBuy;
        this.pair = pair;
        this.timestamp = new Date();
        this.executed = false;
    }

    /**
     * New constructor for direct exchange-to-exchange comparison with more detailed data
     *
     * @param normalizedSymbol The normalized symbol used for comparison
     * @param symbolBuy        The symbol on the buy exchange
     * @param symbolSell       The symbol on the sell exchange
     * @param exchangeBuy      The name of the exchange to buy on
     * @param exchangeSell     The name of the exchange to sell on
     * @param buyPrice         The price to buy at
     * @param sellPrice        The price to sell at
     * @param profitPercent    The percentage profit of this opportunity
     */
    public ArbitrageOpportunity(
            String normalizedSymbol,
            String symbolBuy,
            String symbolSell,
            String exchangeBuy,
            String exchangeSell,
            double buyPrice,
            double sellPrice,
            double profitPercent) {
        this.normalizedSymbol = normalizedSymbol;
        this.symbolBuy = symbolBuy;
        this.symbolSell = symbolSell;
        this.exchangeBuy = exchangeBuy;
        this.exchangeSell = exchangeSell;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.profitPercent = profitPercent;
        this.potentialProfit = profitPercent;
        this.pair = new TradingPair(normalizedSymbol);
        this.successfulArbitragePercent = 0.0; // Default value; risk assessment can override this later
        this.timestamp = new Date();
        this.executed = false;
    }

    /**
     * Comprehensive constructor for arbitrage opportunities with fee and order type details.
     *
     * @param exchangeBuy       The exchange to buy from
     * @param exchangeSell      The exchange to sell on
     * @param tradingPair       The trading pair symbol
     * @param amount            The amount to trade
     * @param buyPrice          The price to buy at
     * @param sellPrice         The price to sell at
     * @param profit            The raw profit amount
     * @param profitPercent     The profit as a percentage
     * @param successRate       The calculated success rate (0-100)
     * @param buyFeePercentage  The buy fee as a percentage
     * @param sellFeePercentage The sell fee as a percentage
     * @param isBuyMaker        Whether the buy order is a maker order
     * @param isSellMaker       Whether the sell order is a maker order
     * @param priceDiffPercent  The price difference percentage
     * @param netProfitPercent  The net profit percentage after fees
     * @param riskScore         The risk score
     * @param liquidity         The liquidity assessment
     * @param volatility        The volatility assessment
     * @param isViable          Whether the opportunity is considered viable
     */
    public ArbitrageOpportunity(
            String exchangeBuy,
            String exchangeSell,
            String tradingPair,
            double amount,
            double buyPrice,
            double sellPrice,
            double profit,
            double profitPercent,
            int successRate,
            double buyFeePercentage,
            double sellFeePercentage,
            boolean isBuyMaker,
            boolean isSellMaker,
            double priceDiffPercent,
            double netProfitPercent,
            double riskScore,
            double liquidity,
            double volatility,
            boolean isViable) {

        this.normalizedSymbol = tradingPair;
        this.symbolBuy = tradingPair;
        this.symbolSell = tradingPair;
        this.exchangeBuy = exchangeBuy;
        this.exchangeSell = exchangeSell;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.profitPercent = profitPercent;
        this.potentialProfit = profit;
        this.pair = new TradingPair(tradingPair);
        this.successfulArbitragePercent = successRate;

        // Set fee-related properties
        this.buyFeePercentage = buyFeePercentage;
        this.sellFeePercentage = sellFeePercentage;
        this.isBuyMaker = isBuyMaker;
        this.isSellMaker = isSellMaker;

        // Set advanced metrics
        this.priceDifferencePercentage = priceDiffPercent;
        this.netProfitPercentage = netProfitPercent;
        this.riskScore = riskScore;
        this.liquidity = liquidity;
        this.volatility = volatility;
        this.isViable = isViable;
        this.timestamp = new Date();
        this.executed = false;
    }

    public TradingPair getPair() {
        return pair;
    }

    public String getExchangeBuy() {
        return exchangeBuy;
    }

    public String getExchangeSell() {
        return exchangeSell;
    }

    public double getPotentialProfit() {
        return potentialProfit;
    }


    public void setPair(TradingPair pair) {
        this.pair = pair;
    }

    public void setExchangeBuy(String exchangeBuy) {
        this.exchangeBuy = exchangeBuy;
    }

    public void setExchangeSell(String exchangeSell) {
        this.exchangeSell = exchangeSell;
    }

    public void setPotentialProfit(double potentialProfit) {
        this.potentialProfit = potentialProfit;
    }

    /**
     * Updated setter to compute the successfulArbitragePercent property based on the
     * median value of all risk properties in the RiskAssessment.
     */


    public String getNormalizedSymbol() {
        return normalizedSymbol;
    }

    public String getSymbolBuy() {
        return symbolBuy;
    }

    public String getSymbolSell() {
        return symbolSell;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    /**
     * Sets the profit percentage and validates it against buy and sell prices.
     * The profit percentage is stored as a percentage value (e.g., 42.15 for 42.15%).
     * 
     * @param profitPercent The profit percentage to set
     */
    public void setProfitPercent(double profitPercent) {
        // Store the original value for logging
        double originalValue = profitPercent;
        
        // Calculate expected profit based on prices
        if (buyPrice > 0 && sellPrice > 0) {
            double expectedProfit = ((sellPrice - buyPrice) / buyPrice) * 100;
            
            // Check if there's a significant discrepancy (more than 1 percentage point)
            if (Math.abs(profitPercent - expectedProfit) > 1.0) {
                Log.w("ArbitrageOpportunity", String.format(
                    "Profit percentage discrepancy detected for %s: provided=%.2f%%, calculated=%.2f%%, using calculated value",
                    normalizedSymbol, profitPercent, expectedProfit));
                
                // Use the calculated value instead
                this.profitPercent = expectedProfit;
            } else {
                // Use the provided value
                this.profitPercent = profitPercent;
            }
        } else {
            // If prices aren't available, just store the value
            this.profitPercent = profitPercent;
        }
        
        // Log if we made a correction
        if (this.profitPercent != originalValue) {
            Log.d("ArbitrageOpportunity", String.format(
                "Corrected profit for %s: from %.2f%% to %.2f%%",
                normalizedSymbol, originalValue, this.profitPercent));
        }
    }

    /**
     * Gets the profit percentage.
     * The value is stored as a percentage (e.g., 42.15 for 42.15%).
     * 
     * @return The profit percentage
     */
    public double getProfitPercent() {
        // Validate the stored profit percentage against prices
        if (buyPrice > 0 && sellPrice > 0) {
            double expectedProfit = ((sellPrice - buyPrice) / buyPrice) * 100;
            
            // If significant discrepancy, recalculate and log
            if (Math.abs(profitPercent - expectedProfit) > 1.0) {
                Log.w("ArbitrageOpportunity", String.format(
                    "Invalid profit detected in getProfitPercent for %s: stored=%.2f%%, calculated=%.2f%%, using calculated",
                    normalizedSymbol, profitPercent, expectedProfit));
                
                // Update the stored value
                profitPercent = expectedProfit;
            }
        }
        
        return profitPercent;
    }
    
    /**
     * Gets the profit percentage formatted as a string with 2 decimal places.
     * 
     * @return The formatted profit percentage string (e.g., "42.15%")
     */
    public String getFormattedProfitPercent() {
        return String.format("%.2f%%", getProfitPercent());
    }

    /**
     * Gets the successful arbitrage percentage calculated as the median
     * of all risk assessment properties.
     *
     * @return The percentage likelihood of successfully executing the arbitrage.
     */
    public double getSuccessfulArbitragePercent() {
        return successfulArbitragePercent;
    }

    /**
     * Sets the successful arbitrage percentage.
     *
     * @param successfulArbitragePercent The percentage likelihood.
     */
    public void setSuccessfulArbitragePercent(double successfulArbitragePercent) {
        this.successfulArbitragePercent = successfulArbitragePercent;
    }

    /**
     * Get the buy fee percentage.
     *
     * @return The buy fee percentage
     */
    public double getBuyFeePercentage() {
        return buyFeePercentage;
    }

    /**
     * Get the sell fee percentage.
     *
     * @return The sell fee percentage
     */
    public double getSellFeePercentage() {
        return sellFeePercentage;
    }

    /**
     * Check if the buy order is expected to be a maker order.
     *
     * @return True if the buy order is a maker order, false for taker
     */
    public boolean isBuyMaker() {
        return isBuyMaker;
    }

    /**
     * Check if the sell order is expected to be a maker order.
     *
     * @return True if the sell order is a maker order, false for taker
     */
    public boolean isSellMaker() {
        return isSellMaker;
    }

    /**
     * Get the price difference percentage between buy and sell prices.
     *
     * @return The price difference as a percentage
     */
    public double getPriceDifferencePercentage() {
        return priceDifferencePercentage;
    }

    /**
     * Sets the net profit percentage after fees.
     * The net profit is stored as a percentage value (e.g., 41.5 for 41.5%).
     * 
     * @param netProfitPercentage The net profit percentage to set
     */
    public void setNetProfitPercentage(double netProfitPercentage) {
        this.netProfitPercentage = netProfitPercentage;
    }

    /**
     * Gets the net profit percentage after fees.
     * The value is stored as a percentage (e.g., 41.5 for 41.5%).
     *
     * @return The net profit percentage
     */
    public double getNetProfitPercentage() {
        return netProfitPercentage;
    }
    
    /**
     * Gets the net profit percentage as a formatted string with 2 decimal places.
     * 
     * @return The formatted net profit percentage string (e.g., "42.15%")
     */
    public String getFormattedNetProfitPercentage() {
        return String.format("%.2f%%", getNetProfitPercentage());
    }

    /**
     * Get the calculated risk score.
     *
     * @return The risk score
     */
    public double getRiskScore() {
        return riskScore;
    }

    /**
     * Sets the risk score for this arbitrage opportunity.
     * 
     * @param riskScore Risk score (0-1, higher is better/less risky)
     */
    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    /**
     * Get the liquidity assessment.
     *
     * @return The liquidity score
     */
    public double getLiquidity() {
        return liquidity;
    }

    /**
     * Sets the liquidity score for this arbitrage opportunity.
     * 
     * @param liquidity Liquidity score (0-1, higher is better)
     */
    public void setLiquidity(double liquidity) {
        this.liquidity = liquidity;
    }

    /**
     * Get the volatility assessment.
     *
     * @return The volatility score
     */
    public double getVolatility() {
        return volatility;
    }

    /**
     * Sets the volatility score for this arbitrage opportunity.
     * 
     * @param volatility Volatility score (0-1, higher is better/less volatile)
     */
    public void setVolatility(double volatility) {
        this.volatility = volatility;
    }

    /**
     * Check if the opportunity is considered viable.
     *
     * @return True if viable, false otherwise
     */
    public boolean isViable() {
        return isViable;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(normalizedSymbol).append(": Buy on ").append(exchangeBuy)
                .append(" (").append(isBuyMaker ? "maker" : "taker").append(" fee: ")
                .append(String.format("%.4f%%", buyFeePercentage * 100)).append(") at ")
                .append(buyPrice).append(", Sell on ").append(exchangeSell)
                .append(" (").append(isSellMaker ? "maker" : "taker").append(" fee: ")
                .append(String.format("%.4f%%", sellFeePercentage * 100)).append(") at ")
                .append(sellPrice).append(System.lineSeparator())
                .append("Profit: ").append(String.format("%.2f%%", profitPercent))
                .append(", Net: ").append(String.format("%.2f%%", netProfitPercentage))
                .append(", Success Rate: ").append(String.format("%.0f%%", successfulArbitragePercent))
                .append(", Viable: ").append(isViable);

        return sb.toString();
    }


    public String getBuySymbol() {
        return symbolBuy;
    }

    /**
     * Gets the sell symbol for this arbitrage opportunity
     *
     * @return The sell symbol
     */
    public String getSellSymbol() {
        return symbolSell;
    }

    /**
     * Gets the buy slippage for this arbitrage opportunity
     *
     * @return The buy slippage as a decimal (e.g., 0.001 for 0.1%)
     */
    public double getBuySlippage() {
        return buySlippage;
    }

    /**
     * Sets the buy slippage for this arbitrage opportunity
     *
     * @param buySlippage The buy slippage as a decimal (e.g., 0.001 for 0.1%)
     */
    public void setBuySlippage(double buySlippage) {
        this.buySlippage = buySlippage;
    }

    /**
     * Gets the sell slippage for this arbitrage opportunity
     *
     * @return The sell slippage as a decimal (e.g., 0.001 for 0.1%)
     */
    public double getSellSlippage() {
        return sellSlippage;
    }

    /**
     * Sets the sell slippage for this arbitrage opportunity
     *
     * @param sellSlippage The sell slippage as a decimal (e.g., 0.001 for 0.1%)
     */
    public void setSellSlippage(double sellSlippage) {
        this.sellSlippage = sellSlippage;
    }

    /**
     * Gets the ticker data for the buy side of this arbitrage opportunity
     *
     * @return The buy ticker
     */
    public Ticker getBuyTicker() {
        return buyTicker;
    }

    /**
     * Sets the ticker data for the buy side of this arbitrage opportunity
     *
     * @param buyTicker The buy ticker
     */
    public void setBuyTicker(Ticker buyTicker) {
        this.buyTicker = buyTicker;
    }

    /**
     * Gets the ticker data for the sell side of this arbitrage opportunity
     *
     * @return The sell ticker
     */
    public Ticker getSellTicker() {
        return sellTicker;
    }

    /**
     * Sets the ticker data for the sell side of this arbitrage opportunity
     *
     * @param sellTicker The sell ticker
     */
    public void setSellTicker(Ticker sellTicker) {
        this.sellTicker = sellTicker;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public double getFees() {
        return fees;
    }

    public void setFees(double fees) {
        this.fees = fees;
    }

    public double getSlippage() {
        return slippage;
    }

    /**
     * Sets the slippage value for this arbitrage opportunity.
     * 
     * @param slippage Slippage value (usually a small decimal, e.g., 0.01 for 1%)
     */
    public void setSlippage(double slippage) {
        this.slippage = slippage;
    }

    /**
     * Generate a unique key for this opportunity based on the symbol and exchanges
     *
     * @return A string key that uniquely identifies this opportunity
     */
    public String getOpportunityKey() {
        return normalizedSymbol + "_" + exchangeBuy + "_" + exchangeSell;
    }

    // Calculate potential profit in base currency (REMOVED - TO BE REIMPLEMENTED)
    public double calculatePotentialProfit(double tradeAmount) {
        // *** PROFIT CALCULATION REMOVED - TO BE REIMPLEMENTED ***
        return 0.0;
    }

    /**
     * Alias for getNormalizedSymbol() to maintain compatibility with refactored code
     *
     * @return The normalized symbol
     */
    public String getSymbol() {
        return getNormalizedSymbol();
    }

    /**
     * Alias for getExchangeBuy() to maintain compatibility with refactored code
     *
     * @return The buy exchange name
     */
    public String getBuyExchangeName() {
        return getExchangeBuy();
    }

    /**
     * Alias for getExchangeSell() to maintain compatibility with refactored code
     *
     * @return The sell exchange name
     */
    public String getSellExchangeName() {
        return getExchangeSell();
    }

    public void setBuyFeePercentage(double buyFeePercentage) {
        this.buyFeePercentage = buyFeePercentage;
    }

    public void setSellFeePercentage(double sellFeePercentage) {
        this.sellFeePercentage = sellFeePercentage;
    }

    public void setViable(boolean viable) {
        this.isViable = viable;
    }

    /**
     * Get the estimated time for this arbitrage operation in minutes
     *
     * @return Estimated time in minutes
     */
    public double getEstimatedTimeMinutes() {
        return estimatedTimeMinutes;
    }

    /**
     * Set the estimated time for this arbitrage operation
     *
     * @param estimatedTimeMinutes Time in minutes
     */
    public void setEstimatedTimeMinutes(double estimatedTimeMinutes) {
        this.estimatedTimeMinutes = estimatedTimeMinutes;
    }

    /**
     * Get the ROI efficiency (profit per hour)
     *
     * @return ROI efficiency as percentage per hour
     */
    public double getRoiEfficiency() {
        return roiEfficiency;
    }

    /**
     * Set the ROI efficiency
     *
     * @param roiEfficiency ROI efficiency value
     */
    public void setRoiEfficiency(double roiEfficiency) {
        this.roiEfficiency = roiEfficiency;
    }

    /**
     * Get liquidity factor for this opportunity (0-1)
     *
     * @return Liquidity factor
     */
    public double getLiquidityFactor() {
        return liquidityFactor;
    }

    /**
     * Set liquidity factor
     *
     * @param liquidityFactor Liquidity factor value
     */
    public void setLiquidityFactor(double liquidityFactor) {
        this.liquidityFactor = liquidityFactor;
    }

    /**
     * Get buy exchange liquidity depth
     *
     * @return Buy exchange liquidity
     */
    public double getBuyExchangeLiquidity() {
        return buyExchangeLiquidity;
    }

    /**
     * Set buy exchange liquidity depth
     *
     * @param buyExchangeLiquidity Liquidity value
     */
    public void setBuyExchangeLiquidity(double buyExchangeLiquidity) {
        this.buyExchangeLiquidity = buyExchangeLiquidity;
    }

    /**
     * Get sell exchange liquidity depth
     *
     * @return Sell exchange liquidity
     */
    public double getSellExchangeLiquidity() {
        return sellExchangeLiquidity;
    }

    /**
     * Set sell exchange liquidity depth
     *
     * @param sellExchangeLiquidity Liquidity value
     */
    public void setSellExchangeLiquidity(double sellExchangeLiquidity) {
        this.sellExchangeLiquidity = sellExchangeLiquidity;
    }

    /**
     * Check if this opportunity is time-sensitive
     *
     * @return True if opportunity is time-sensitive
     */
    public boolean isTimeSensitive() {
        return isTimeSensitive;
    }

    /**
     * Set whether this opportunity is time-sensitive
     *
     * @param timeSensitive Time sensitivity flag
     */
    public void setTimeSensitive(boolean timeSensitive) {
        isTimeSensitive = timeSensitive;
    }

    /**
     * Get formatted time string for display
     *
     * @return Formatted time string
     */
    public String getFormattedTime() {
        // Format time as minutes or hours
        if (estimatedTimeMinutes < 60) {
            return String.format("%.0fm", estimatedTimeMinutes);
        } else {
            return String.format("%.1fh", estimatedTimeMinutes / 60.0);
        }
    }

    /**
     * Get formatted ROI efficiency string for display
     *
     * @return Formatted ROI efficiency string
     */
    public String getFormattedROIEfficiency() {
        return String.format("%.2f%%/h", roiEfficiency);
    }

    // Constructor used for Parcel
    protected ArbitrageOpportunity(Parcel in) {
        try {
            // Read basic fields with null safety
            pair = in.readParcelable(TradingPair.class.getClassLoader());
            exchangeBuy = in.readString();
            exchangeSell = in.readString();
            potentialProfit = in.readDouble();
            normalizedSymbol = in.readString();
            symbolBuy = in.readString();
            symbolSell = in.readString();
            
            // Read numeric fields with validation
            buyPrice = in.readDouble();
            if (Double.isNaN(buyPrice) || buyPrice <= 0) buyPrice = 0.0001;
            
            sellPrice = in.readDouble();
            if (Double.isNaN(sellPrice) || sellPrice <= 0) sellPrice = 0.0001;
            
            profitPercent = in.readDouble();
            if (Double.isNaN(profitPercent)) profitPercent = 0.0;
            
            successfulArbitragePercent = in.readDouble();
            if (Double.isNaN(successfulArbitragePercent)) successfulArbitragePercent = 0.0;
            
            buyFeePercentage = in.readDouble();
            if (Double.isNaN(buyFeePercentage) || buyFeePercentage < 0) buyFeePercentage = 0.1;
            
            sellFeePercentage = in.readDouble();
            if (Double.isNaN(sellFeePercentage) || sellFeePercentage < 0) sellFeePercentage = 0.1;
            
            isBuyMaker = in.readByte() != 0;
            isSellMaker = in.readByte() != 0;
            
            buySlippage = in.readDouble();
            if (Double.isNaN(buySlippage) || buySlippage < 0) buySlippage = 0.001;
            
            sellSlippage = in.readDouble();
            if (Double.isNaN(sellSlippage) || sellSlippage < 0) sellSlippage = 0.001;
            
            // Read complex objects with null safety
            buyTicker = in.readParcelable(Ticker.class.getClassLoader());
            sellTicker = in.readParcelable(Ticker.class.getClassLoader());
            
            // Read more numeric fields with validation
            priceDifferencePercentage = in.readDouble();
            if (Double.isNaN(priceDifferencePercentage)) priceDifferencePercentage = 0.0;
            
            netProfitPercentage = in.readDouble();
            if (Double.isNaN(netProfitPercentage)) netProfitPercentage = 0.0;
            
            riskScore = in.readDouble();
            if (Double.isNaN(riskScore) || riskScore < 0 || riskScore > 1) riskScore = 0.5;
            
            liquidity = in.readDouble();
            if (Double.isNaN(liquidity) || liquidity < 0 || liquidity > 1) liquidity = 0.5;
            
            volatility = in.readDouble();
            if (Double.isNaN(volatility) || volatility < 0 || volatility > 1) volatility = 0.5;
            
            isViable = in.readByte() != 0;
            
            long tmpTimestamp = in.readLong();
            timestamp = tmpTimestamp == -1 ? new Date() : new Date(tmpTimestamp);
            
            executed = in.readByte() != 0;
            
            fees = in.readDouble();
            if (Double.isNaN(fees) || fees < 0) fees = 0.0;
            
            slippage = in.readDouble();
            if (Double.isNaN(slippage) || slippage < 0) slippage = 0.001;
            
            estimatedTimeMinutes = in.readDouble();
            if (Double.isNaN(estimatedTimeMinutes) || estimatedTimeMinutes <= 0) estimatedTimeMinutes = 3.0;
            
            roiEfficiency = in.readDouble();
            if (Double.isNaN(roiEfficiency)) roiEfficiency = 0.0;
            
            liquidityFactor = in.readDouble();
            if (Double.isNaN(liquidityFactor)) liquidityFactor = 0.5;
            
            buyExchangeLiquidity = in.readDouble();
            if (Double.isNaN(buyExchangeLiquidity)) buyExchangeLiquidity = 0.5;
            
            sellExchangeLiquidity = in.readDouble();
            if (Double.isNaN(sellExchangeLiquidity)) sellExchangeLiquidity = 0.5;
            
            isTimeSensitive = in.readByte() != 0;
            
            volume = in.readDouble();
            if (Double.isNaN(volume) || volume < 0) volume = 0.0;
            
            orderBookDepth = in.readDouble();
            if (Double.isNaN(orderBookDepth) || orderBookDepth < 0) orderBookDepth = 0.0;
            
            priceVolatility = in.readDouble();
            if (Double.isNaN(priceVolatility) || priceVolatility < 0) priceVolatility = 0.0;
            
            totalSlippagePercentage = in.readDouble();
            if (Double.isNaN(totalSlippagePercentage) || totalSlippagePercentage < 0) totalSlippagePercentage = 0.001;
            
            // Ensure we have a valid risk assessment
            riskAssessment = in.readParcelable(RiskAssessment.class.getClassLoader());
            if (riskAssessment == null) {
                riskAssessment = new RiskAssessment();
                riskAssessment.setOverallRiskScore(riskScore);
                riskAssessment.setLiquidityScore(liquidity);
                riskAssessment.setVolatilityScore(volatility);
                riskAssessment.setSlippageEstimate(totalSlippagePercentage);
                riskAssessment.setBuyFeePercentage(buyFeePercentage);
                riskAssessment.setSellFeePercentage(sellFeePercentage);
                riskAssessment.setExecutionTimeEstimate(estimatedTimeMinutes);
            }
        } catch (Exception e) {
            // Provide safe defaults if anything goes wrong with parcel reading
            Log.e("ArbitrageOpportunity", "Error reading from parcel", e);
            
            // Set default values for critical fields
            if (timestamp == null) timestamp = new Date();
            if (normalizedSymbol == null) normalizedSymbol = "UNKNOWN/UNKNOWN";
            if (symbolBuy == null) symbolBuy = normalizedSymbol;
            if (symbolSell == null) symbolSell = normalizedSymbol;
            if (exchangeBuy == null) exchangeBuy = "Unknown";
            if (exchangeSell == null) exchangeSell = "Unknown";
            if (buyPrice <= 0) buyPrice = 0.0001;
            if (sellPrice <= 0) sellPrice = 0.0001;
            if (Double.isNaN(riskScore) || riskScore <= 0) riskScore = 0.5;
            if (Double.isNaN(liquidity) || liquidity <= 0) liquidity = 0.5;
            if (Double.isNaN(volatility) || volatility <= 0) volatility = 0.5;
            if (Double.isNaN(estimatedTimeMinutes) || estimatedTimeMinutes <= 0) estimatedTimeMinutes = 3.0;
            
            // Create risk assessment if it doesn't exist
            if (riskAssessment == null) {
                riskAssessment = new RiskAssessment();
                riskAssessment.setOverallRiskScore(0.5);
                riskAssessment.setLiquidityScore(0.5);
                riskAssessment.setVolatilityScore(0.5);
                riskAssessment.setExecutionTimeEstimate(3.0);
            }
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(pair, flags);
        dest.writeString(exchangeBuy);
        dest.writeString(exchangeSell);
        dest.writeDouble(potentialProfit);
        dest.writeString(normalizedSymbol);
        dest.writeString(symbolBuy);
        dest.writeString(symbolSell);
        dest.writeDouble(buyPrice);
        dest.writeDouble(sellPrice);
        dest.writeDouble(profitPercent);
        dest.writeDouble(successfulArbitragePercent);
        dest.writeDouble(buyFeePercentage);
        dest.writeDouble(sellFeePercentage);
        dest.writeByte((byte) (isBuyMaker ? 1 : 0));
        dest.writeByte((byte) (isSellMaker ? 1 : 0));
        dest.writeDouble(buySlippage);
        dest.writeDouble(sellSlippage);
        dest.writeParcelable(buyTicker, flags);
        dest.writeParcelable(sellTicker, flags);
        dest.writeDouble(priceDifferencePercentage);
        dest.writeDouble(netProfitPercentage);
        dest.writeDouble(riskScore);
        dest.writeDouble(liquidity);
        dest.writeDouble(volatility);
        dest.writeByte((byte) (isViable ? 1 : 0));
        dest.writeLong(timestamp != null ? timestamp.getTime() : -1);
        dest.writeByte((byte) (executed ? 1 : 0));
        dest.writeDouble(fees);
        dest.writeDouble(slippage);
        dest.writeDouble(estimatedTimeMinutes);
        dest.writeDouble(roiEfficiency);
        dest.writeDouble(liquidityFactor);
        dest.writeDouble(buyExchangeLiquidity);
        dest.writeDouble(sellExchangeLiquidity);
        dest.writeByte((byte) (isTimeSensitive ? 1 : 0));
        dest.writeDouble(volume);
        dest.writeDouble(orderBookDepth);
        dest.writeDouble(priceVolatility);
        dest.writeDouble(totalSlippagePercentage);
        dest.writeParcelable(riskAssessment, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ArbitrageOpportunity> CREATOR = new Creator<ArbitrageOpportunity>() {
        @Override
        public ArbitrageOpportunity createFromParcel(Parcel in) {
            return new ArbitrageOpportunity(in);
        }

        @Override
        public ArbitrageOpportunity[] newArray(int size) {
            return new ArbitrageOpportunity[size];
        }
    };

    /**
     * Get the percentage profit for this opportunity
     */
    public double getPercentageProfit() {
        return netProfitPercentage;
    }

    /**
     * Set the net profit percentage
     */
    public void setPercentageProfit(double percentageProfit) {
        this.netProfitPercentage = percentageProfit;
    }

    /**
     * Get the buy fee as a decimal percentage (e.g., 0.001 for 0.1%)
     */
    public double getBuyFee() {
        return buyFeePercentage;
    }

    /**
     * Get the sell fee as a decimal percentage (e.g., 0.001 for 0.1%)
     */
    public double getSellFee() {
        return sellFeePercentage;
    }

    // Add getters for the missing fields
    public double getVolume() {
        if (buyTicker != null && sellTicker != null) {
            return Math.min(buyTicker.getVolume(), sellTicker.getVolume());
        }
        return volume;
    }

    public double getOrderBookDepth() {
        if (buyTicker != null && sellTicker != null) {
            return Math.min(
                    buyTicker.getBidAmount() + buyTicker.getAskAmount(),
                    sellTicker.getBidAmount() + sellTicker.getAskAmount()
            );
        }
        return orderBookDepth;
    }

    public double getPriceVolatility() {
        if (buyTicker != null && sellTicker != null) {
            double buyVolatility = (buyTicker.getHighPrice() - buyTicker.getLowPrice()) / buyTicker.getLastPrice();
            double sellVolatility = (sellTicker.getHighPrice() - sellTicker.getLowPrice()) / sellTicker.getLastPrice();
            return Math.max(buyVolatility, sellVolatility);
        }
        return priceVolatility;
    }

    public double getTotalSlippagePercentage() {
        return buySlippage + sellSlippage;
    }

    // Add setters for the new fields
    public void setVolume(double volume) {
        this.volume = volume;
    }

    public void setOrderBookDepth(double orderBookDepth) {
        this.orderBookDepth = orderBookDepth;
    }

    public void setPriceVolatility(double priceVolatility) {
        this.priceVolatility = priceVolatility;
    }

    public void setTotalSlippagePercentage(double totalSlippagePercentage) {
        this.totalSlippagePercentage = totalSlippagePercentage;
    }

    /**
     * Get the risk assessment for this opportunity.
     * If no risk assessment has been set, create a default one.
     *
     * @return The risk assessment for this opportunity
     */
    public RiskAssessment getRiskAssessment() {
        if (riskAssessment == null) {
            riskAssessment = createDefaultRiskAssessment();
        }
        return riskAssessment;
    }
    
    /**
     * Set the risk assessment for this opportunity.
     * This also updates related fields in the opportunity.
     *
     * @param riskAssessment The risk assessment to set
     */
    public void setRiskAssessment(RiskAssessment riskAssessment) {
        this.riskAssessment = riskAssessment;
        
        // Update related fields from the risk assessment
        if (riskAssessment != null) {
            // Make sure all risk-related fields are consistently applied from assessment
            // This prevents risk values from appearing different across views
            
            // Set core risk metrics
            this.riskScore = riskAssessment.getOverallRiskScore();
            this.liquidity = riskAssessment.getLiquidityScore();
            this.volatility = riskAssessment.getVolatilityScore();
            this.slippage = riskAssessment.getSlippageRisk();
            
            // Set derived metrics
            this.totalSlippagePercentage = riskAssessment.getSlippageEstimate();
            
            // Update fee information if available in assessment
            if (riskAssessment.getBuyFeePercentage() > 0) {
                this.buyFeePercentage = riskAssessment.getBuyFeePercentage();
            }
            
            if (riskAssessment.getSellFeePercentage() > 0) {
                this.sellFeePercentage = riskAssessment.getSellFeePercentage();
            }
            
            // Calculate success rate based on risk score
            this.successfulArbitragePercent = Math.max(0, Math.min(100, riskAssessment.getOverallRiskScore() * 100));
            
            // Set execution time with validation
            double execTime = riskAssessment.getExecutionTimeEstimate();
            if (execTime > 0) {
                this.estimatedTimeMinutes = execTime;
            } else if (this.estimatedTimeMinutes <= 0) {
                // Set a reasonable default if no valid value is available
                this.estimatedTimeMinutes = 3.0;
            }
            
            // Set ROI efficiency
            if (riskAssessment.getRoiEfficiency() > 0) {
                this.roiEfficiency = riskAssessment.getRoiEfficiency();
            } else if (this.estimatedTimeMinutes > 0) {
                // Calculate ROI efficiency based on profit and execution time
                this.roiEfficiency = (this.profitPercent / 100) * (60.0 / this.estimatedTimeMinutes);
            }
            
            // Debug log of applied risk values
            Log.d("ArbitrageOpportunity", String.format(
                "Applied risk values to %s: Risk=%.2f, Liquidity=%.2f, Volatility=%.2f, Time=%.1f min",
                this.normalizedSymbol,
                this.riskScore,
                this.liquidity,
                this.volatility,
                this.estimatedTimeMinutes));
        }
    }

    /**
     * Create a default risk assessment with reasonable values.
     *
     * @return A default risk assessment
     */
    private RiskAssessment createDefaultRiskAssessment() {
        RiskAssessment assessment = new RiskAssessment();
        
        // Set default values
        assessment.setOverallRiskScore(0.5); // Moderate risk
        assessment.setLiquidityScore(0.5);   // Average liquidity
        assessment.setVolatilityScore(0.5);  // Average volatility
        assessment.setSlippageRisk(0.005);   // 0.5% slippage
        assessment.setFeeImpact((buyFeePercentage + sellFeePercentage) / 200.0); // Convert to 0-1 scale
        assessment.setExecutionTimeEstimate(5.0); // 5 minutes execution time
        assessment.setExecutionSpeedRisk(0.5);
        assessment.setMarketDepthScore(0.5);
        
        // Set exchange information and fees
        assessment.setExchangeBuy(this.exchangeBuy);
        assessment.setExchangeSell(this.exchangeSell);
        assessment.setBuyFeePercentage(this.buyFeePercentage);
        assessment.setSellFeePercentage(this.sellFeePercentage);
        
        return assessment;
    }

    /**
     * Calculate the total fee percentage (buy + sell)
     * 
     * @return Total fee percentage
     */
    public double getTotalFeePercentage() {
        return buyFeePercentage + sellFeePercentage;
    }

    /**
     * Recalculate the profit percentage using the comprehensive fee model.
     * This ensures all types of fees are properly accounted for: trading fees,
     * withdrawal fees, network fees, and any deposit fees.
     * 
     * @param initialAmount The initial amount in base currency to use for calculation
     * @return The updated profit percentage after recalculation
     */
    public double recalculateComprehensiveProfit(double initialAmount) {
        if (buyPrice <= 0 || sellPrice <= 0) {
            Log.w("ArbitrageOpportunity", "Cannot recalculate profit with invalid prices");
            return profitPercent;
        }
        
        // Extract base asset from normalized symbol
        String baseAsset = "";
        if (normalizedSymbol != null && normalizedSymbol.contains("/")) {
            baseAsset = normalizedSymbol.split("/")[0];
        } else if (pair != null) {
            baseAsset = pair.getBaseAsset();
        }
        
        if (baseAsset.isEmpty()) {
            Log.w("ArbitrageOpportunity", "Cannot determine base asset for comprehensive profit calculation");
            return profitPercent;
        }
        
        // Use the ArbitrageProcessing utility for consistent calculation
        try {
            double recalculatedProfit = com.example.tradient.util.ArbitrageProcessing.calculateComprehensiveProfitPercentage(
                initialAmount,
                buyPrice,
                sellPrice,
                exchangeBuy,
                exchangeSell,
                baseAsset,
                buyFeePercentage,
                sellFeePercentage
            );
            
            // Update the profit percentages
            this.profitPercent = recalculatedProfit;
            this.netProfitPercentage = recalculatedProfit; // Net profit is already included in comprehensive calculation
            
            Log.d("ArbitrageOpportunity", String.format(
                "Recalculated comprehensive profit for %s: %.4f%% (includes all fees)",
                normalizedSymbol, recalculatedProfit));
                
            return recalculatedProfit;
        } catch (Exception e) {
            Log.e("ArbitrageOpportunity", "Error recalculating comprehensive profit: " + e.getMessage(), e);
            return profitPercent;
        }
    }
    
    /**
     * Validates the stored profit percentage against a comprehensive profit calculation
     * that includes all types of fees.
     * 
     * @param initialAmount The initial amount to use for calculation
     * @return True if the stored profit is accurate, false if it was corrected
     */
    public boolean validateComprehensiveProfit(double initialAmount) {
        double originalProfit = this.profitPercent;
        double recalculatedProfit = recalculateComprehensiveProfit(initialAmount);
        
        // Check if there's a significant discrepancy (more than 0.5 percentage point)
        boolean isAccurate = Math.abs(originalProfit - recalculatedProfit) < 0.5;
        
        if (!isAccurate) {
            Log.w("ArbitrageOpportunity", String.format(
                "Profit percentage discrepancy detected for %s: original=%.2f%%, comprehensive=%.2f%%",
                normalizedSymbol, originalProfit, recalculatedProfit));
        }
        
        return isAccurate;
    }
}