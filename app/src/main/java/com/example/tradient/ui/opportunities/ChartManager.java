package com.example.tradient.ui.opportunities;

import android.content.Context;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.example.tradient.R;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Manages chart creation and styling for the OpportunityDetailActivity.
 * Responsible for setting up market depth and price history charts.
 */
public class ChartManager {
    private final Context context;
    private final DecimalFormat priceFormat;
    
    /**
     * Initialize the chart manager
     *
     * @param context Application context for resource access
     */
    public ChartManager(Context context) {
        this.context = context;
        this.priceFormat = new DecimalFormat("#,##0.00###");
    }
    
    /**
     * Set up the market depth chart with order book data
     *
     * @param chart BarChart object to configure
     * @param buyOrderBook Order book from the buy exchange
     * @param sellOrderBook Order book from the sell exchange
     * @param buyExchangeName Name of the buy exchange
     * @param sellExchangeName Name of the sell exchange
     */
    public void setupMarketDepthChart(BarChart chart, OrderBook buyOrderBook, OrderBook sellOrderBook, 
                                     String buyExchangeName, String sellExchangeName) {
        if (chart == null || buyOrderBook == null || sellOrderBook == null) {
            return;
        }
        
        ArrayList<BarEntry> buyEntries = new ArrayList<>();
        ArrayList<BarEntry> sellEntries = new ArrayList<>();
        
        List<String> xAxisLabels = new ArrayList<>();
        
        // Calculate cumulative volume for better depth visualization
        double cumulativeBuyVolume = 0;
        double cumulativeSellVolume = 0;
        
        // Create bar entries for ask prices (buy exchange)
        Map<Double, Double> buyAsks = buyOrderBook.getAsksAsMap();
        // Sort keys for proper ordering
        List<Double> sortedAskPrices = new ArrayList<>(buyAsks.keySet());
        Collections.sort(sortedAskPrices); // Ascending order for asks
        
        int index = 0;
        for (Double price : sortedAskPrices) {
            double volume = buyAsks.get(price);
            cumulativeBuyVolume += volume;
            buyEntries.add(new BarEntry(index, (float) cumulativeBuyVolume));
            xAxisLabels.add(priceFormat.format(price));
            index++;
            // Limit to 10 entries for clarity
            if (index >= 10) break;
        }
        
        // Create bar entries for bid prices (sell exchange)
        Map<Double, Double> sellBids = sellOrderBook.getBidsAsMap();
        // Sort keys for proper ordering
        List<Double> sortedBidPrices = new ArrayList<>(sellBids.keySet());
        Collections.sort(sortedBidPrices, Collections.reverseOrder()); // Descending order for bids
        
        index = 10; // Offset for the right side of the chart
        for (Double price : sortedBidPrices) {
            double volume = sellBids.get(price);
            cumulativeSellVolume += volume;
            sellEntries.add(new BarEntry(index, (float) cumulativeSellVolume));
            xAxisLabels.add(priceFormat.format(price));
            index++;
            // Limit to 10 entries for clarity
            if (index >= 20) break;
        }
        
        // Calculate average price for mid-line
        double midPrice = 0;
        if (!sortedAskPrices.isEmpty() && !sortedBidPrices.isEmpty()) {
            midPrice = (sortedAskPrices.get(0) + sortedBidPrices.get(0)) / 2;
        }
        
        // Style data sets
        BarDataSet buyDataSet = new BarDataSet(buyEntries, buyExchangeName + " Asks");
        buyDataSet.setColor(ContextCompat.getColor(context, R.color.chart_sell_color));
        buyDataSet.setValueTextSize(10f);
        buyDataSet.setDrawValues(false);
        
        BarDataSet sellDataSet = new BarDataSet(sellEntries, sellExchangeName + " Bids");
        sellDataSet.setColor(ContextCompat.getColor(context, R.color.chart_buy_color));
        sellDataSet.setValueTextSize(10f);
        sellDataSet.setDrawValues(false);
        
        // Combine data sets
        BarData barData = new BarData(buyDataSet, sellDataSet);
        barData.setBarWidth(0.8f);
        
        // Style chart
        chart.setData(barData);
        chart.setFitBars(true);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setExtraBottomOffset(10f);
        chart.setExtraTopOffset(10f);
        chart.setPinchZoom(true);
        
        // Style X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(45f);
        xAxis.setLabelCount(10);
        xAxis.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
        xAxis.setDrawGridLines(false);
        
        // Style Y axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#222335"));
        leftAxis.setAxisLineColor(ContextCompat.getColor(context, R.color.secondary_text));
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000000) {
                    return String.format("%.1fM", value / 1000000);
                } else if (value >= 1000) {
                    return String.format("%.1fK", value / 1000);
                }
                return String.format("%.0f", value);
            }
        });
        
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Add center line separating buy and sell sides
        LimitLine centerLine = new LimitLine(9.5f, "");
        centerLine.setLineWidth(1f);
        centerLine.setLineColor(Color.GRAY);
        centerLine.enableDashedLine(10f, 10f, 0f);
        leftAxis.addLimitLine(centerLine);
        
        // Add mid price marker
        if (midPrice > 0) {
            String midPriceLabel = "Mid: " + priceFormat.format(midPrice);
            LimitLine midPriceLine = new LimitLine(0f, midPriceLabel);
            midPriceLine.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
            midPriceLine.setTextSize(11f);
            midPriceLine.setLineWidth(0f); // No line, just the label
            leftAxis.addLimitLine(midPriceLine);
        }
        
        // Style legend
        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
        legend.setTextSize(12f);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setFormSize(8f);
        legend.setXEntrySpace(10f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        
        // Add animation
        chart.animateY(800);
        chart.invalidate();
    }
    
    /**
     * Set up the price history chart with historical ticker data
     *
     * @param chart LineChart object to configure
     * @param buyTickers Historical tickers from the buy exchange
     * @param sellTickers Historical tickers from the sell exchange
     * @param buyExchangeName Name of the buy exchange
     * @param sellExchangeName Name of the sell exchange
     */
    public void setupPriceHistoryChart(LineChart chart, List<Ticker> buyTickers, List<Ticker> sellTickers,
                                      String buyExchangeName, String sellExchangeName) {
        if (chart == null || buyTickers == null || sellTickers == null || 
            buyTickers.isEmpty() || sellTickers.isEmpty()) {
            return;
        }
        
        ArrayList<Entry> buyEntries = new ArrayList<>();
        ArrayList<Entry> sellEntries = new ArrayList<>();
        ArrayList<Entry> spreadPercentEntries = new ArrayList<>();
        
        // Prepare timestamps for X-axis labeling
        List<Date> timestamps = new ArrayList<>();
        float minPrice = Float.MAX_VALUE;
        float maxPrice = 0;
        
        // Process buy exchange data
        for (int i = 0; i < buyTickers.size(); i++) {
            Ticker ticker = buyTickers.get(i);
            float price = (float) ticker.getLastPrice();
            buyEntries.add(new Entry(i, price));
            
            // Track min/max for scaling
            minPrice = Math.min(minPrice, price);
            maxPrice = Math.max(maxPrice, price);
            
            // Store timestamp for this data point
            timestamps.add(ticker.getTimestamp());
        }
        
        // Process sell exchange data
        for (int i = 0; i < sellTickers.size(); i++) {
            Ticker ticker = sellTickers.get(i);
            float price = (float) ticker.getLastPrice();
            sellEntries.add(new Entry(i, price));
            
            // Track min/max for scaling
            minPrice = Math.min(minPrice, price);
            maxPrice = Math.max(maxPrice, price);
        }
        
        // Create spread percentage entries for secondary axis
        int minSize = Math.min(buyTickers.size(), sellTickers.size());
        for (int i = 0; i < minSize; i++) {
            float buyPrice = (float) buyTickers.get(i).getLastPrice();
            float sellPrice = (float) sellTickers.get(i).getLastPrice();
            
            if (buyPrice > 0) {
                // Calculate spread as a percentage
                float spreadPct = Math.abs(sellPrice - buyPrice) / buyPrice * 100;
                spreadPercentEntries.add(new Entry(i, spreadPct));
            }
        }
        
        // Calculate price range and add padding
        float priceRange = maxPrice - minPrice;
        float paddedMin = Math.max(0, minPrice - (priceRange * 0.05f));
        float paddedMax = maxPrice + (priceRange * 0.05f);
        
        // Style line data sets
        LineDataSet buyDataSet = createLineDataSet(
                buyEntries, 
                buyExchangeName + " Price", 
                ContextCompat.getColor(context, R.color.chart_buy_color),
                ContextCompat.getColor(context, R.color.chart_buy_color_transparent));
        
        LineDataSet sellDataSet = createLineDataSet(
                sellEntries, 
                sellExchangeName + " Price", 
                ContextCompat.getColor(context, R.color.chart_sell_color),
                ContextCompat.getColor(context, R.color.chart_sell_color_transparent));
        
        // Add spread line with percentage values
        LineDataSet spreadDataSet = createSpreadLineDataSet(
                spreadPercentEntries, 
                "Price Spread %", 
                ContextCompat.getColor(context, R.color.chart_spread_color));
        
        // Set spread to use right axis
        spreadDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        
        // Combine data sets
        LineData lineData = new LineData(buyDataSet, sellDataSet, spreadDataSet);
        
        // Style chart
        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setExtraBottomOffset(10f);
        chart.setExtraTopOffset(10f);
        
        // Style X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(6, true);
        xAxis.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx < 0 || idx >= timestamps.size()) {
                    return "";
                }
                
                // Get relative time (e.g., 5h ago, 30m ago)
                Date date = timestamps.get(idx);
                long timeAgoMs = System.currentTimeMillis() - date.getTime();
                long minutesAgo = timeAgoMs / (60 * 1000);
                
                if (minutesAgo < 60) {
                    return minutesAgo + "m";
                } else {
                    long hoursAgo = minutesAgo / 60;
                    return hoursAgo + "h";
                }
            }
        });
        
        // Style left Y axis (price)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setLabelCount(8, false);
        leftAxis.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
        leftAxis.setGridColor(Color.parseColor("#222335"));
        leftAxis.setAxisLineColor(ContextCompat.getColor(context, R.color.secondary_text));
        leftAxis.setAxisMinimum(paddedMin);
        leftAxis.setAxisMaximum(paddedMax);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return priceFormat.format(value);
            }
        });
        
        // Style right Y axis (spread percentage)
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(true);
        rightAxis.setDrawGridLines(false);
        rightAxis.setTextColor(ContextCompat.getColor(context, R.color.chart_spread_color));
        rightAxis.setAxisLineColor(ContextCompat.getColor(context, R.color.chart_spread_color));
        rightAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.2f%%", value);
            }
        });
        
        // Style legend
        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
        legend.setTextSize(12f);
        legend.setForm(Legend.LegendForm.LINE);
        legend.setFormSize(8f);
        legend.setXEntrySpace(10f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        
        // Add animation
        chart.animateX(1000);
        chart.invalidate();
    }
    
    /**
     * Create a styled line data set for price history charts
     */
    private LineDataSet createLineDataSet(ArrayList<Entry> entries, String label, int color, int fillColor) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setDrawCircles(true);
        dataSet.setCircleRadius(3f);
        dataSet.setCircleHoleRadius(1.5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(fillColor);
        dataSet.setFillAlpha(50);
        
        return dataSet;
    }
    
    /**
     * Create a styled spread line data set
     */
    private LineDataSet createSpreadLineDataSet(ArrayList<Entry> entries, String label, int color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(1.5f);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.enableDashedLine(5f, 5f, 0f);
        
        return dataSet;
    }

    /**
     * Sets up a slippage impact chart showing how different trade sizes affect execution prices
     * 
     * @param chart The chart view to set up
     * @param buyOrderBook Buy exchange order book
     * @param sellOrderBook Sell exchange order book
     * @param buyExchangeName Name of buy exchange
     * @param sellExchangeName Name of sell exchange
     */
    public void setupSlippageImpactChart(LineChart chart, OrderBook buyOrderBook, OrderBook sellOrderBook,
                                  String buyExchangeName, String sellExchangeName) {
        if (chart == null || buyOrderBook == null || sellOrderBook == null) {
            return;
        }
        
        // Create data points for different trade sizes
        ArrayList<Entry> buyEntries = new ArrayList<>();
        ArrayList<Entry> sellEntries = new ArrayList<>();
        
        // Calculate effective prices at different trade sizes
        double[] tradeSizes = {1000, 5000, 10000, 20000, 50000, 100000};
        
        // Get price points from order books
        Map<Double, Double> buyAsks = buyOrderBook.getAsksAsMap();
        Map<Double, Double> sellBids = sellOrderBook.getBidsAsMap();
        
        // Sort price levels
        List<Double> buyAskPrices = new ArrayList<>(buyAsks.keySet());
        List<Double> sellBidPrices = new ArrayList<>(sellBids.keySet());
        Collections.sort(buyAskPrices);
        Collections.sort(sellBidPrices, Collections.reverseOrder());
        
        // Best prices (no slippage)
        double bestBuyPrice = buyAskPrices.isEmpty() ? 0 : buyAskPrices.get(0);
        double bestSellPrice = sellBidPrices.isEmpty() ? 0 : sellBidPrices.get(0);
        
        for (int i = 0; i < tradeSizes.length; i++) {
            double tradeSize = tradeSizes[i];
            
            // Calculate effective buy price with this trade size
            double effectiveBuyPrice = calculateEffectivePrice(buyAskPrices, buyAsks, tradeSize, true);
            double effectiveSellPrice = calculateEffectivePrice(sellBidPrices, sellBids, tradeSize, false);
            
            // Calculate slippage percentage
            double buySlippage = (effectiveBuyPrice - bestBuyPrice) / bestBuyPrice;
            double sellSlippage = (bestSellPrice - effectiveSellPrice) / bestSellPrice;
            
            // Add to chart entries (x = trade size, y = slippage percentage)
            buyEntries.add(new Entry((float)tradeSize, (float)(buySlippage * 100)));
            sellEntries.add(new Entry((float)tradeSize, (float)(sellSlippage * 100)));
        }
        
        // Create datasets
        LineDataSet buyDataSet = new LineDataSet(buyEntries, 
                buyExchangeName + " Buy Slippage");
        LineDataSet sellDataSet = new LineDataSet(sellEntries, 
                sellExchangeName + " Sell Slippage");
        
        // Style the datasets
        buyDataSet.setColor(ContextCompat.getColor(context, R.color.chart_buy_color));
        buyDataSet.setCircleColor(ContextCompat.getColor(context, R.color.chart_buy_color));
        buyDataSet.setLineWidth(2f);
        buyDataSet.setCircleRadius(3f);
        buyDataSet.setDrawValues(false);
        buyDataSet.setDrawCircleHole(false);
        
        sellDataSet.setColor(ContextCompat.getColor(context, R.color.chart_sell_color));
        sellDataSet.setCircleColor(ContextCompat.getColor(context, R.color.chart_sell_color));
        sellDataSet.setLineWidth(2f);
        sellDataSet.setCircleRadius(3f);
        sellDataSet.setDrawValues(false);
        sellDataSet.setDrawCircleHole(false);
        
        // Add data to chart
        LineData lineData = new LineData(buyDataSet, sellDataSet);
        chart.setData(lineData);
        
        // Style the chart
        chart.getDescription().setEnabled(false);
        chart.getLegend().setTextColor(ContextCompat.getColor(context, R.color.primary_text));
        
        // Configure axes
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "$" + formatNumber(value);
            }
        });
        
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value + "%";
            }
        });
        
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Refresh the chart
        chart.invalidate();
    }

    /**
     * Calculate effective price for a trade size based on order book
     * 
     * @param priceLevels Sorted list of price levels
     * @param volumeMap Map of price -> volume
     * @param tradeSize Size of trade in USD
     * @param isBuy Whether this is for buying (true) or selling (false)
     * @return Effective execution price
     */
    private double calculateEffectivePrice(List<Double> priceLevels, Map<Double, Double> volumeMap, 
                                         double tradeSize, boolean isBuy) {
        double remainingAmount = tradeSize;
        double totalCost = 0;
        double totalCoins = 0;
        
        for (Double price : priceLevels) {
            double volume = volumeMap.get(price);
            double orderValue = price * volume;
            
            if (orderValue >= remainingAmount) {
                // This level has enough liquidity to fill the remaining order
                double coinsToTake = remainingAmount / price;
                totalCoins += coinsToTake;
                totalCost += remainingAmount;
                break;
            } else {
                // Take the entire level and continue
                totalCoins += volume;
                totalCost += orderValue;
                remainingAmount -= orderValue;
            }
            
            // If we've processed all price levels and still have remaining amount
            if (remainingAmount > 0 && price.equals(priceLevels.get(priceLevels.size() - 1))) {
                // Add a penalty for exceeding available liquidity - assume 5% worse than last price
                double penaltyPrice = isBuy ? price * 1.05 : price * 0.95;
                double coinsToTake = remainingAmount / penaltyPrice;
                totalCoins += coinsToTake;
                totalCost += remainingAmount;
            }
        }
        
        // Calculate weighted average price
        return totalCoins > 0 ? totalCost / totalCoins : 0;
    }

    /**
     * Format a number for display
     */
    private String formatNumber(float number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000);
        } else {
            return String.format("%.0f", number);
        }
    }
} 