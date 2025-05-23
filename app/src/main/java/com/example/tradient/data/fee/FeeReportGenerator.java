package com.example.tradient.data.fee;

import com.example.tradient.data.service.ExchangeService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for generating fee reports and demonstrations of the fee system.
 */
public class FeeReportGenerator {
    
    /**
     * Generate a consolidated fee report for multiple exchanges.
     *
     * @param exchanges List of exchange services
     * @return A string containing the consolidated fee report
     */
    public static String generateConsolidatedReport(List<ExchangeService> exchanges) {
        StringBuilder report = new StringBuilder();
        report.append("===== CONSOLIDATED FEE REPORT =====\n\n");
        
        // Get overall statistics
        double totalFeesPaid = 0.0;
        int totalTransactions = 0;
        double totalSavings = 0.0;
        
        // Fees by exchange
        report.append("FEES BY EXCHANGE:\n");
        for (ExchangeService exchange : exchanges) {
            FeeTracker tracker = exchange.getFeeTracker();
            double exchangeFees = tracker.getTotalFeesPaid();
            int exchangeTransactions = tracker.getAllFees().size();
            double exchangeSavings = tracker.getTotalDiscountSavings();
            
            report.append(String.format("%s: %.8f (%d transactions, %.8f savings)\n",
                    exchange.getExchangeName(), exchangeFees, exchangeTransactions, exchangeSavings));
            
            totalFeesPaid += exchangeFees;
            totalTransactions += exchangeTransactions;
            totalSavings += exchangeSavings;
        }
        report.append("\n");
        
        // Overall summary
        report.append("OVERALL SUMMARY:\n");
        report.append(String.format("Total Fees Paid: %.8f\n", totalFeesPaid));
        report.append(String.format("Total Transactions: %d\n", totalTransactions));
        report.append(String.format("Total Discount Savings: %.8f\n", totalSavings));
        report.append("\n");
        
        // Trading pair summary
        report.append("FEES BY TRADING PAIR:\n");
        Map<String, Double> tradingPairFees = exchanges.stream()
                .flatMap(e -> e.getFeeTracker().getFeesByTradingPairReport().entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, 
                        Collectors.summingDouble(Map.Entry::getValue)));
        
        for (Map.Entry<String, Double> entry : tradingPairFees.entrySet()) {
            report.append(String.format("%s: %.8f\n", entry.getKey(), entry.getValue()));
        }
        
        return report.toString();
    }
    
    /**
     * Demonstrate different fee structures and calculations.
     *
     * @return A string containing the demonstration
     */
    public static String generateFeeDemo() {
        StringBuilder demo = new StringBuilder();
        demo.append("===== FEE SYSTEM DEMONSTRATION =====\n\n");
        
        ExchangeFeeFactory feeFactory = ExchangeFeeFactory.getInstance();
        
        // 1. DEMO DIFFERENT FEE TYPES
        demo.append("FEE TYPE COMPARISON:\n");
        
        // Fixed fee
        FixedFee fixedFee = new FixedFee(5.0, "Fixed withdrawal fee");
        demo.append(String.format("Fixed Fee: %s\n", fixedFee.getDescription()));
        demo.append(String.format("  $100 transaction: $%.2f fee\n", fixedFee.calculateFee(100)));
        demo.append(String.format("  $1000 transaction: $%.2f fee\n", fixedFee.calculateFee(1000)));
        demo.append(String.format("  $10000 transaction: $%.2f fee\n\n", fixedFee.calculateFee(10000)));
        
        // Percentage fee
        PercentageFee percentageFee = new PercentageFee(0.001, true, "Standard 0.1% maker fee");
        demo.append(String.format("Percentage Fee: %s\n", percentageFee.getDescription()));
        demo.append(String.format("  $100 transaction: $%.2f fee (%.2f%%)\n", 
                percentageFee.calculateFee(100), percentageFee.calculateFee(100) / 100 * 100));
        demo.append(String.format("  $1000 transaction: $%.2f fee (%.2f%%)\n", 
                percentageFee.calculateFee(1000), percentageFee.calculateFee(1000) / 1000 * 100));
        demo.append(String.format("  $10000 transaction: $%.2f fee (%.2f%%)\n\n", 
                percentageFee.calculateFee(10000), percentageFee.calculateFee(10000) / 10000 * 100));
        
        // 2. DEMO EXCHANGE-SPECIFIC FEES
        demo.append("EXCHANGE FEE COMPARISON:\n");
        
        // Low volume trader
        demo.append("Low Volume Trader (30-day volume: $5,000):\n");
        Fee binanceLowVolMaker = feeFactory.createFee("Binance", 5000, true);
        Fee coinbaseLowVolMaker = feeFactory.createFee("Coinbase", 5000, true);
        Fee krakenLowVolMaker = feeFactory.createFee("Kraken", 5000, true);
        
        // Using $1,000 trade amount for comparison (more realistic)
        double tradeAmount = 1000.0;
        
        demo.append(String.format("  Binance: %s - $%.2f trade = $%.2f fee (%.2f%%)\n", 
                binanceLowVolMaker.getDescription(), tradeAmount, 
                binanceLowVolMaker.calculateFee(tradeAmount),
                binanceLowVolMaker.calculateFee(tradeAmount) / tradeAmount * 100));
        demo.append(String.format("  Coinbase: %s - $%.2f trade = $%.2f fee (%.2f%%)\n", 
                coinbaseLowVolMaker.getDescription(), tradeAmount, 
                coinbaseLowVolMaker.calculateFee(tradeAmount),
                coinbaseLowVolMaker.calculateFee(tradeAmount) / tradeAmount * 100));
        demo.append(String.format("  Kraken: %s - $%.2f trade = $%.2f fee (%.2f%%)\n\n", 
                krakenLowVolMaker.getDescription(), tradeAmount, 
                krakenLowVolMaker.calculateFee(tradeAmount),
                krakenLowVolMaker.calculateFee(tradeAmount) / tradeAmount * 100));
        
        // High volume trader
        demo.append("High Volume Trader (30-day volume: $1,000,000):\n");
        Fee binanceHighVolMaker = feeFactory.createFee("Binance", 1000000, true);
        Fee coinbaseHighVolMaker = feeFactory.createFee("Coinbase", 1000000, true);
        Fee krakenHighVolMaker = feeFactory.createFee("Kraken", 1000000, true);
        
        demo.append(String.format("  Binance: %s - $%.2f trade = $%.2f fee (%.2f%%)\n", 
                binanceHighVolMaker.getDescription(), tradeAmount, 
                binanceHighVolMaker.calculateFee(tradeAmount),
                binanceHighVolMaker.calculateFee(tradeAmount) / tradeAmount * 100));
        demo.append(String.format("  Coinbase: %s - $%.2f trade = $%.2f fee (%.2f%%)\n", 
                coinbaseHighVolMaker.getDescription(), tradeAmount, 
                coinbaseHighVolMaker.calculateFee(tradeAmount),
                coinbaseHighVolMaker.calculateFee(tradeAmount) / tradeAmount * 100));
        demo.append(String.format("  Kraken: %s - $%.2f trade = $%.2f fee (%.2f%%)\n\n", 
                krakenHighVolMaker.getDescription(), tradeAmount, 
                krakenHighVolMaker.calculateFee(tradeAmount),
                krakenHighVolMaker.calculateFee(tradeAmount) / tradeAmount * 100));
        
        // 3. DEMO DISCOUNTS
        demo.append("DISCOUNT COMPARISON:\n");
        
        // Binance with BNB discount
        Fee binanceNormalFee = feeFactory.createFee("Binance", 0, false);
        Fee binanceBnbFee = feeFactory.createFee("Binance", 0, false, true);
        
        demo.append(String.format("Binance Standard: %s - $%.2f trade = $%.2f fee (%.2f%%)\n", 
                binanceNormalFee.getDescription(), tradeAmount, 
                binanceNormalFee.calculateFee(tradeAmount),
                binanceNormalFee.calculateFee(tradeAmount) / tradeAmount * 100));
        demo.append(String.format("Binance with BNB: %s - $%.2f trade = $%.2f fee (%.2f%%)\n", 
                binanceBnbFee.getDescription(), tradeAmount, 
                binanceBnbFee.calculateFee(tradeAmount),
                binanceBnbFee.calculateFee(tradeAmount) / tradeAmount * 100));
        demo.append(String.format("Savings: $%.2f (%.2f%%)\n\n", 
                binanceNormalFee.calculateFee(tradeAmount) - binanceBnbFee.calculateFee(tradeAmount),
                (binanceNormalFee.calculateFee(tradeAmount) - binanceBnbFee.calculateFee(tradeAmount)) / 
                        binanceNormalFee.calculateFee(tradeAmount) * 100));
        
        // 4. ARBITRAGE PROFIT EXAMPLE
        demo.append("ARBITRAGE PROFIT EXAMPLE:\n");
        
        double btcPrice = 50000.0;        // BTC base price
        double priceDiffPercent = 0.2;    // 0.2% price difference
        double buyPrice = btcPrice;       // BTC price on exchange A
        double sellPrice = btcPrice * (1 + priceDiffPercent / 100); // BTC price on exchange B with 0.2% higher price
        double quantity = 0.01;           // 0.01 BTC (a more realistic amount for a retail trader)
        
        Fee buyFee = feeFactory.createFee("Binance", 1000000, false);
        Fee sellFee = feeFactory.createFee("Coinbase", 1000000, false);
        
        // *** PROFIT CALCULATION REMOVED - TO BE REIMPLEMENTED ***
        // Use placeholder values
        double profit = 0.0;
        double profitPercent = 0.0;
        
        demo.append(String.format("Buy %.4f BTC on Binance at $%.2f with %s (fee: $%.2f)\n", 
                quantity, buyPrice, buyFee.getDescription(), buyFee.calculateFee(buyPrice * quantity)));
        demo.append(String.format("Sell %.4f BTC on Coinbase at $%.2f with %s (fee: $%.2f)\n", 
                quantity, sellPrice, sellFee.getDescription(), sellFee.calculateFee(sellPrice * quantity)));
        demo.append(String.format("Price difference: %.2f%%\n", priceDiffPercent));
        demo.append(String.format("Profit after fees: PLACEHOLDER - TO BE REIMPLEMENTED\n\n"));
        
        // 5. TRANSACTION TRACKING EXAMPLE
        demo.append("TRANSACTION TRACKING EXAMPLE:\n");
        FeeTracker tracker = new FeeTracker();
        
        // Add some sample transactions with more realistic trade sizes
        TransactionFee tx1 = new TransactionFee(
                "tx1", "Binance", "BTCUSDT", 1.0, FeeType.PERCENTAGE,
                null, "Binance taker fee", 0.001, 0.0, false);
        TransactionFee tx2 = new TransactionFee(
                "tx2", "Coinbase", "BTC-USD", 5.0, FeeType.PERCENTAGE,
                null, "Coinbase taker fee", 0.0025, 0.0, false);
        TransactionFee tx3 = new TransactionFee(
                "tx3", "Binance", "ETHUSDT", 0.5, FeeType.PERCENTAGE,
                null, "Binance taker fee with BNB discount", 0.001, 0.25, false);
        
        tracker.trackFee(tx1);
        tracker.trackFee(tx2);
        tracker.trackFee(tx3);
        
        demo.append(tracker.generateFeeSummaryReport());
        
        return demo.toString();
    }

    public String generate(
            double buyPrice, double sellPrice, double quantity,
            Fee buyFee, Fee sellFee,
            String buyExchange, String sellExchange,
            boolean includeProfitDetails) {
        StringBuilder report = new StringBuilder();
        
        report.append("=== Fee Report ===\n");
        report.append("Buy Exchange: ").append(buyExchange).append("\n");
        report.append("Sell Exchange: ").append(sellExchange).append("\n\n");
        
        // Basic trade info
        report.append("Buy Price: ").append(formatPrice(buyPrice)).append("\n");
        report.append("Sell Price: ").append(formatPrice(sellPrice)).append("\n");
        report.append("Quantity: ").append(formatQuantity(quantity)).append("\n\n");
        
        // Fee Structures
        report.append("Buy Fee Structure: ").append(describeFee(buyFee)).append("\n");
        report.append("Sell Fee Structure: ").append(describeFee(sellFee)).append("\n\n");
        
        // Calculate basic trade values
        double buyValue = buyPrice * quantity;
        double sellValue = sellPrice * quantity;
        
        report.append("Buy Value (before fees): ").append(formatValue(buyValue)).append("\n");
        report.append("Sell Value (before fees): ").append(formatValue(sellValue)).append("\n\n");
        
        // Calculate fees
        double buyFeeCost = buyFee.calculateFee(buyValue);
        double sellFeeCost = sellFee.calculateFee(sellValue);
        
        report.append("Buy Fee Amount: ").append(formatValue(buyFeeCost));
        report.append(" (").append(formatPercentage(buyFeeCost / buyValue * 100)).append(")\n");
        
        report.append("Sell Fee Amount: ").append(formatValue(sellFeeCost));
        report.append(" (").append(formatPercentage(sellFeeCost / sellValue * 100)).append(")\n\n");
        
        // Net values
        double buyValueWithFee = buyValue + buyFeeCost;
        double sellValueAfterFee = sellValue - sellFeeCost;
        
        report.append("Total Buy Cost (with fees): ").append(formatValue(buyValueWithFee)).append("\n");
        report.append("Net Sell Proceeds (after fees): ").append(formatValue(sellValueAfterFee)).append("\n\n");
        
        // *** PROFIT CALCULATION REMOVED - TO BE REIMPLEMENTED ***
        // Use placeholder values
        double profit = 0.0;
        double profitPercent = 0.0;
        
        if (includeProfitDetails) {
            report.append("Profit Calculation: PLACEHOLDER - TO BE REIMPLEMENTED\n");
            report.append("Net Profit: ").append(formatValue(profit)).append("\n");
            report.append("Profit Percentage: ").append(formatPercentage(profitPercent)).append("\n\n");
        }
        
        return report.toString();
    }

    /**
     * Format price values with appropriate precision
     */
    private String formatPrice(double price) {
        if (price < 0.001) {
            return String.format("%.8f", price);
        } else if (price < 1.0) {
            return String.format("%.6f", price);
        } else if (price < 1000.0) {
            return String.format("%.4f", price);
        } else {
            return String.format("%.2f", price);
        }
    }
    
    /**
     * Format quantity values with appropriate precision
     */
    private String formatQuantity(double quantity) {
        if (quantity < 0.001) {
            return String.format("%.8f", quantity);
        } else if (quantity < 1.0) {
            return String.format("%.6f", quantity);
        } else {
            return String.format("%.4f", quantity);
        }
    }
    
    /**
     * Format currency values
     */
    private String formatValue(double value) {
        return String.format("$%.2f", value);
    }
    
    /**
     * Format percentage values
     */
    private String formatPercentage(double percentage) {
        return String.format("%.2f%%", percentage);
    }
    
    /**
     * Describe a fee structure in human-readable format
     */
    private String describeFee(Fee fee) {
        if (fee == null) {
            return "No fee information";
        }
        return fee.getDescription();
    }
} 