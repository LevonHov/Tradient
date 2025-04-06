package com.example.tradient.ui.opportunities;

import android.util.Log;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.Exchange;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.data.service.ExchangeServiceFactory;
import com.example.tradient.domain.market.ExchangeLiquidityService;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Example class showing how to use the ExchangeLiquidityService with OpportunityDetailViewModel.
 * This demonstrates how to integrate real liquidity data from exchanges.
 */
public class LiquidityAnalysisExample {
    private static final String TAG = "LiquidityAnalysis";
    
    private final ExchangeLiquidityService liquidityService;
    private final OpportunityDetailViewModel viewModel;
    
    /**
     * Create a new liquidity analysis example for a given opportunity.
     * 
     * @param viewModel The view model containing opportunity data
     */
    public LiquidityAnalysisExample(OpportunityDetailViewModel viewModel) {
        this.viewModel = viewModel;
        this.liquidityService = new ExchangeLiquidityService();
    }
    
    /**
     * Fetch and analyze real liquidity data for the current opportunity.
     * This method demonstrates a complete liquidity analysis with all metrics.
     */
    public void analyzeOpportunityLiquidity() {
        ArbitrageOpportunity opportunity = viewModel.getOpportunity();
        if (opportunity == null) {
            Log.e(TAG, "No opportunity available for analysis");
            return;
        }
        
        String symbol = opportunity.getSymbol();
        String buyExchangeName = opportunity.getBuyExchangeName();
        String sellExchangeName = opportunity.getSellExchangeName();
        
        // Get exchange services
        ExchangeService buyExchangeService = getExchangeService(buyExchangeName);
        ExchangeService sellExchangeService = getExchangeService(sellExchangeName);
        
        if (buyExchangeService == null || sellExchangeService == null) {
            Log.e(TAG, "Failed to get exchange services");
            return;
        }
        
        // Calculate comprehensive arbitrage liquidity metrics
        ExchangeLiquidityService.ArbitrageLiquidityMetrics metrics = 
                liquidityService.calculateArbitrageLiquidity(
                        buyExchangeService, 
                        sellExchangeService, 
                        symbol);
        
        // Log the results for demonstration
        logLiquidityMetrics(metrics, opportunity);
        
        // Update ViewModel with liquidity data (would be implemented in the actual ViewModel)
        updateViewModel(metrics);
    }
    
    /**
     * Get exchange service for a given exchange name.
     */
    private ExchangeService getExchangeService(String exchangeName) {
        try {
            Exchange exchange = Exchange.valueOf(exchangeName.toUpperCase());
            return ExchangeServiceFactory.getExchangeService(exchange);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid exchange name: " + exchangeName);
            return null;
        }
    }
    
    /**
     * Log detailed liquidity metrics (for demonstration).
     */
    private void logLiquidityMetrics(
            ExchangeLiquidityService.ArbitrageLiquidityMetrics metrics, 
            ArbitrageOpportunity opportunity) {
        
        Log.d(TAG, "===== LIQUIDITY ANALYSIS: " + opportunity.getSymbol() + " =====");
        
        // Exchange details
        Log.d(TAG, "Buy Exchange: " + opportunity.getBuyExchangeName());
        Log.d(TAG, "Sell Exchange: " + opportunity.getSellExchangeName());
        
        // Price information
        Log.d(TAG, "Best bid (sell exchange): " + metrics.getSellExchangeMetrics().getBestBid());
        Log.d(TAG, "Best ask (buy exchange): " + metrics.getBuyExchangeMetrics().getBestAsk());
        Log.d(TAG, "Cross-exchange spread: " + metrics.getCrossExchangeSpread());
        Log.d(TAG, "Spread percentage: " + (metrics.getSpreadPercentage() * 100) + "%");
        
        // Liquidity volumes
        Log.d(TAG, "Buy exchange liquidity: $" + metrics.getBuyExchangeMetrics().getAvailableLiquidity());
        Log.d(TAG, "Sell exchange liquidity: $" + metrics.getSellExchangeMetrics().getAvailableLiquidity());
        Log.d(TAG, "Combined liquidity: $" + metrics.getCombinedLiquidity());
        
        // Slippage for different order sizes
        Log.d(TAG, "----- SLIPPAGE BY ORDER SIZE -----");
        for (Map.Entry<Double, Double> entry : metrics.getSlippageMap().entrySet()) {
            Log.d(TAG, "$" + entry.getKey() + ": " + (entry.getValue() * 100) + "%");
        }
        
        // Optimal trade size
        Log.d(TAG, "Optimal trade size: $" + metrics.getOptimalTradeSize());
        
        // Calculate net profit after slippage for different order sizes
        Log.d(TAG, "----- NET PROFIT BY ORDER SIZE (after slippage) -----");
        double fees = opportunity.getBuyFee() + opportunity.getSellFee();
        double[] sizes = {1000, 5000, 10000, 25000, 50000};
        
        for (double size : sizes) {
            double netProfit = metrics.calculateNetProfit(size, fees);
            Log.d(TAG, "$" + size + ": " + (netProfit * 100) + "%");
        }
    }
    
    /**
     * Update ViewModel with liquidity data.
     * This is just a placeholder - in a real implementation, 
     * the ViewModel would expose these values via LiveData.
     */
    private void updateViewModel(ExchangeLiquidityService.ArbitrageLiquidityMetrics metrics) {
        // In a real implementation, these would be LiveData objects in the ViewModel
        // that the UI would observe
        
        // Example implementation:
        // viewModel.setLiquidityValue(metrics.getCombinedLiquidity());
        // viewModel.setOptimalTradeSize(metrics.getOptimalTradeSize());
        // viewModel.setSlippageMap(metrics.getSlippageMap());
    }
    
    /**
     * Example of calculating slippage for a specific trade size.
     * 
     * @param tradeSize Size of the trade in USD
     * @return Expected slippage percentage
     */
    public double calculateSlippageForTradeSize(double tradeSize) {
        ArbitrageOpportunity opportunity = viewModel.getOpportunity();
        if (opportunity == null) {
            return 0.0;
        }
        
        // Get exchange services
        ExchangeService buyExchangeService = getExchangeService(opportunity.getBuyExchangeName());
        ExchangeService sellExchangeService = getExchangeService(opportunity.getSellExchangeName());
        
        if (buyExchangeService == null || sellExchangeService == null) {
            return 0.0;
        }
        
        // Calculate metrics
        ExchangeLiquidityService.ArbitrageLiquidityMetrics metrics = 
                liquidityService.calculateArbitrageLiquidity(
                        buyExchangeService, 
                        sellExchangeService, 
                        opportunity.getSymbol());
        
        // Get slippage for specified size
        return metrics.getSlippageForSize(tradeSize);
    }
} 