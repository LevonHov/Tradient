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

    public double getProfitPercent() {
        return profitPercent;
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
     * Get the net profit percentage after accounting for all fees.
     *
     * @return The net profit percentage
     */
    public double getNetProfitPercentage() {
        return netProfitPercentage;
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
     * Get the liquidity assessment.
     *
     * @return The liquidity score
     */
    public double getLiquidity() {
        return liquidity;
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

    public void setNetProfitPercentage(double netProfitPercentage) {
        this.netProfitPercentage = netProfitPercentage;
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
        pair = in.readParcelable(TradingPair.class.getClassLoader());
        exchangeBuy = in.readString();
        exchangeSell = in.readString();
        potentialProfit = in.readDouble();
        normalizedSymbol = in.readString();
        symbolBuy = in.readString();
        symbolSell = in.readString();
        buyPrice = in.readDouble();
        sellPrice = in.readDouble();
        profitPercent = in.readDouble();
        successfulArbitragePercent = in.readDouble();
        buyFeePercentage = in.readDouble();
        sellFeePercentage = in.readDouble();
        isBuyMaker = in.readByte() != 0;
        isSellMaker = in.readByte() != 0;
        buySlippage = in.readDouble();
        sellSlippage = in.readDouble();
        buyTicker = in.readParcelable(Ticker.class.getClassLoader());
        sellTicker = in.readParcelable(Ticker.class.getClassLoader());
        priceDifferencePercentage = in.readDouble();
        netProfitPercentage = in.readDouble();
        riskScore = in.readDouble();
        liquidity = in.readDouble();
        volatility = in.readDouble();
        isViable = in.readByte() != 0;
        long tmpTimestamp = in.readLong();
        timestamp = tmpTimestamp == -1 ? null : new Date(tmpTimestamp);
        executed = in.readByte() != 0;
        fees = in.readDouble();
        slippage = in.readDouble();
        estimatedTimeMinutes = in.readDouble();
        roiEfficiency = in.readDouble();
        liquidityFactor = in.readDouble();
        buyExchangeLiquidity = in.readDouble();
        sellExchangeLiquidity = in.readDouble();
        isTimeSensitive = in.readByte() != 0;
        volume = in.readDouble();
        orderBookDepth = in.readDouble();
        priceVolatility = in.readDouble();
        totalSlippagePercentage = in.readDouble();
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
     * Calculates and updates the risk assessment for this opportunity.
     * This method should be called whenever the opportunity data changes.
     */


}