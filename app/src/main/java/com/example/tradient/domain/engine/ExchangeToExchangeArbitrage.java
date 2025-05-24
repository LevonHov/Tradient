package com.example.tradient.domain.engine;

import com.example.tradient.data.model.ArbitrageOpportunity;
import com.example.tradient.data.model.OrderBook;
import com.example.tradient.data.model.Ticker;
import com.example.tradient.data.model.TradingPair;
import com.example.tradient.data.fee.Fee;
import com.example.tradient.data.fee.FeeCalculator;
import com.example.tradient.data.fee.TransactionFee;
import com.example.tradient.data.service.ExchangeService;
import com.example.tradient.data.interfaces.*;
import com.example.tradient.domain.risk.RiskCalculator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class encapsulates the logic for detecting an arbitrage opportunity
 * between two exchanges for a given trading pair, taking fees into account.
 * All log statements have been removed and replaced with proper return values.
 */
public class ExchangeToExchangeArbitrage implements IArbitrageEngine {
    private final ExchangeService exchangeA;
    private final ExchangeService exchangeB;
    private final RiskCalculator riskCalculator;
    private double minProfitPercent;
    private final INotificationService notificationService;
    
    // Track configured exchanges
    private final List<ExchangeService> exchanges;
    
    // Minimum success rate to consider an arbitrage opportunity viable
    private static final int MINIMUM_SUCCESS_RATE = 70;

    /**
     * Constructor with notification service for detailed logging if needed
     * 
     * @param exchangeA First exchange service
     * @param exchangeB Second exchange service
     * @param riskCalculator Risk calculator for opportunity assessment
     * @param minProfitPercent Minimum profit percentage to consider
     * @param notificationService Notification service for logging
     */
    public ExchangeToExchangeArbitrage(ExchangeService exchangeA, ExchangeService exchangeB, 
                                     RiskCalculator riskCalculator, double minProfitPercent,
                                     INotificationService notificationService) {
        this.exchangeA = exchangeA;
        this.exchangeB = exchangeB;
        this.riskCalculator = riskCalculator;
        this.minProfitPercent = minProfitPercent;
        this.notificationService = notificationService;
        
        // Initialize exchanges list
        this.exchanges = new ArrayList<>();
        this.exchanges.add(exchangeA);
        this.exchanges.add(exchangeB);
    }
    
    /**
     * Constructor without notification service for simpler usage
     * 
     * @param exchangeA First exchange service
     * @param exchangeB Second exchange service
     * @param riskCalculator Risk calculator for opportunity assessment
     * @param minProfitPercent Minimum profit percentage to consider
     */
    public ExchangeToExchangeArbitrage(ExchangeService exchangeA, ExchangeService exchangeB,
                                     RiskCalculator riskCalculator, double minProfitPercent) {
        this(exchangeA, exchangeB, riskCalculator, minProfitPercent, null);
    }
    
    /**
     * Simple constructor for backward compatibility
     * 
     * @param exchangeA First exchange service
     * @param exchangeB Second exchange service
     */
    public ExchangeToExchangeArbitrage(ExchangeService exchangeA, ExchangeService exchangeB) {
        this(exchangeA, exchangeB, new RiskCalculator(0.1 / 100), 0.1, null);
    }

    /**
     * Calculates the potential arbitrage opportunity between two exchanges for a given trading pair.
     *
     * @param pair The trading pair to analyze.
     * @return An ArbitrageOpportunity object if an opportunity exists, null otherwise.
     */
    public ArbitrageOpportunity calculateArbitrage(TradingPair pair) {
        if (exchanges.size() < 2) {
            logDebug("Not enough exchanges for arbitrage calculation");
            return null;
        }

        try {
            // Choose the first two exchanges for simplicity
            ExchangeService exchangeA = exchanges.get(0);
            ExchangeService exchangeB = exchanges.get(1);

        // Get ticker data from both exchanges
            Ticker tickerA = exchangeA.getTicker(pair.getSymbol());
            Ticker tickerB = exchangeB.getTicker(pair.getSymbol());

        if (tickerA == null || tickerB == null) {
                logDebug("Missing ticker data for " + pair.getSymbol());
                return null;
            }
            
            // Get fee structures
            Fee buyFeeA = exchangeA.getMakerFee();
            Fee sellFeeA = exchangeA.getTakerFee();
            Fee buyFeeB = exchangeB.getMakerFee();
            Fee sellFeeB = exchangeB.getTakerFee();
            
            // Get prices
            double buyOnAPrice = tickerA.getAskPrice();
            double sellOnAPrice = tickerA.getBidPrice();
            double buyOnBPrice = tickerB.getAskPrice();
            double sellOnBPrice = tickerB.getBidPrice();
            
            // Determine a reasonable quantity for calculations
            // For high-value assets like BTC, use a smaller quantity
            double price = Math.max(buyOnAPrice, buyOnBPrice);
            double quantity = price > 1000 ? 0.1 : price > 100 ? 1 : price > 10 ? 10 : 100;
            
            // *** PROFIT CALCULATION REMOVED - TO BE REIMPLEMENTED ***
            // Set placeholder profit values
            double profitAB = 0.0;
            double profitBA = 0.0;
            double profitPercentAB = 0.0;
            double profitPercentBA = 0.0;
            
            // Create an arbitrage opportunity for one direction for testing
            // In future, we will calculate the most profitable direction
            ExchangeService buyExchange = exchangeA;
            ExchangeService sellExchange = exchangeB;
            double buyPrice = buyOnAPrice;
            double sellPrice = sellOnBPrice;
            double profitPercentage = 0.1; // Placeholder
            Fee buyFee = buyFeeA;
            Fee sellFee = sellFeeB;
            boolean isBuyMaker = true;
            boolean isSellMaker = true;
            
            return createArbitrageOpportunity(
                pair, buyExchange, sellExchange,
                buyPrice, sellPrice, profitPercentage,
                calculateAccurateExchangeFee(buyExchange.getExchangeName(), true), 
                calculateAccurateExchangeFee(sellExchange.getExchangeName(), false),
                isBuyMaker, isSellMaker
            );
            
        } catch (Exception e) {
            logError("Error calculating arbitrage for " + pair.getSymbol(), e);
            return null;
        }
    }

    /**
     * Calculates an accurate exchange fee based on the current standard rates
     * 
     * @param exchangeName The name of the exchange
     * @param isMaker Whether this is a maker (true) or taker (false) operation
     * @return The accurate fee percentage as a decimal (e.g., 0.0004 for 0.04%)
     */
    private double calculateAccurateExchangeFee(String exchangeName, boolean isMaker) {
        if (exchangeName == null) return 0.001; // Default 0.1%
        
        String exchange = exchangeName.toLowerCase();
        
        if (isMaker) {
            // Maker fees
            switch (exchange) {
                case "binance": return 0.0002; // 0.02% maker fee (spot)
                case "coinbase": return 0.0040; // 0.40% maker fee
                case "kraken": return 0.0016;  // 0.16% maker fee
                case "bybit": return 0.0001;   // 0.01% maker fee
                case "okx": return 0.0008;     // 0.08% maker fee
                case "kucoin": return 0.0008;  // 0.08% maker fee
                case "gemini": return 0.0025;  // 0.25% maker fee
                case "bitfinex": return 0.0010; // 0.10% maker fee
                default: return 0.0010;        // Default 0.10% maker fee
            }
        } else {
            // Taker fees
            switch (exchange) {
                case "binance": return 0.0004; // 0.04% taker fee (spot)
                case "coinbase": return 0.0060; // 0.60% taker fee
                case "kraken": return 0.0026;  // 0.26% taker fee
                case "bybit": return 0.0010;   // 0.10% taker fee
                case "okx": return 0.0010;     // 0.10% taker fee
                case "kucoin": return 0.0010;  // 0.10% taker fee
                case "gemini": return 0.0035;  // 0.35% taker fee
                case "bitfinex": return 0.0020; // 0.20% taker fee
                default: return 0.0010;        // Default 0.10% taker fee
            }
        }
    }

    /**
     * Determines the appropriate quantity to trade based on the token price
     * 
     * @param price The token price
     * @return Appropriate quantity for trading
     */
    private double determineAppropriateQuantity(double price) {
        if (price < 0.001) {
            logDebug("Low-value token detected. Using 1,000,000 units for calculations.");
            return 1000000; // 1 million units for micro-priced tokens like SHIB
        } else if (price < 1.0) {
            logDebug("Medium-value token detected. Using 1,000 units for calculations.");
            return 1000; // 1,000 units for tokens under $1
        } else if (price < 100.0) {
            logDebug("High-value token detected. Using 10 units for calculations.");
            return 10; // 10 units for tokens under $100
        } else {
            logDebug("Very high-value token detected. Using 0.01 units for calculations.");
            return 0.01; // 0.01 units for expensive tokens like BTC
        }
    }

    /**
     * Create an arbitrage opportunity object with risk assessment.
     * Older overloaded version for backward compatibility.
     *
     * @param pair The trading pair object
     * @param buyExchange The exchange to buy from
     * @param sellExchange The exchange to sell to
     * @param buyPrice The buy price
     * @param sellPrice The sell price
     * @param profitPercentage The profit percentage
     * @param buyFeePercentage The buy fee percentage
     * @param sellFeePercentage The sell fee percentage
     * @param isBuyMaker Whether the buy order is a maker order
     * @param isSellMaker Whether the sell order is a maker order
     * @return The arbitrage opportunity
     */
    private ArbitrageOpportunity createArbitrageOpportunity(
            TradingPair pair, 
            ExchangeService buyExchange,
            ExchangeService sellExchange, 
            double buyPrice, 
            double sellPrice,
            double profitPercentage, 
            double buyFeePercentage, 
            double sellFeePercentage,
            boolean isBuyMaker,
            boolean isSellMaker) {
        
        if (pair == null) {
            logError("Trading pair is null", null);
            return null;
        }
        
        String tradingPair = pair.getSymbol();
        double amount = determineAppropriateQuantity(buyPrice);
        double profit = (sellPrice - buyPrice) * amount;
        
        return createArbitrageOpportunity(
            buyExchange, sellExchange, tradingPair, amount, buyPrice, sellPrice,
            profit, profitPercentage, riskCalculator, buyFeePercentage, sellFeePercentage,
            isBuyMaker, isSellMaker);
    }

    /**
     * Create an arbitrage opportunity object with risk assessment.
     *
     * @param fromExchange     The exchange to buy from
     * @param toExchange       The exchange to sell to
     * @param tradingPair      The trading pair
     * @param amount           The amount to trade
     * @param buyPrice         The buy price
     * @param sellPrice        The sell price
     * @param profit           The calculated profit
     * @param profitPercentage The profit percentage
     * @param riskManager      The risk manager
     * @param buyFeePercentage The buy fee percentage
     * @param sellFeePercentage The sell fee percentage
     * @param isBuyMaker       Whether the buy order is a maker order
     * @param isSellMaker      Whether the sell order is a maker order
     * @return The arbitrage opportunity
     */
    private ArbitrageOpportunity createArbitrageOpportunity(
            IExchangeService fromExchange, IExchangeService toExchange,
            String tradingPair, double amount, double buyPrice, double sellPrice,
            double profit, double profitPercentage, IRiskManager riskManager,
            double buyFeePercentage, double sellFeePercentage, 
            boolean isBuyMaker, boolean isSellMaker) {
        
        try {
            // Get tickers for risk assessment
            Ticker buyTicker = fromExchange.getTicker(tradingPair);
            Ticker sellTicker = toExchange.getTicker(tradingPair);
            
            if (buyTicker == null || sellTicker == null) {
                logError("Missing ticker data for risk assessment", null);
                return null;
            }
            
            // Calculate price difference (this is not profit, just price difference)
            double priceDifference = sellPrice - buyPrice;
            double priceDifferencePercentage = (priceDifference / buyPrice) * 100;
            
            // *** PROFIT CALCULATION REMOVED - TO BE REIMPLEMENTED ***
            // Set placeholder values
            double netProfit = 0.0;
            double netProfitPercentage = 0.0;
            
            // Just use placeholder values for risk metrics
            double riskScore = 0.5; // Neutral risk score
            double liquidity = 0.5; // Medium liquidity
            double volatility = 0.5; // Medium volatility
            
            // Placeholder for success rate and viability
            int successRate = 50; // Neutral success rate
            boolean isViable = false; // Not viable until profit calculation is implemented
            
            // Create the arbitrage opportunity with placeholder values
            ArbitrageOpportunity opportunity = new ArbitrageOpportunity(
                    fromExchange.getExchangeName(),
                    toExchange.getExchangeName(),
                    tradingPair,
                    amount,
                    buyPrice,
                    sellPrice,
                    netProfit,
                    netProfitPercentage,
                    successRate,
                    buyFeePercentage,
                    sellFeePercentage,
                    isBuyMaker,
                    isSellMaker,
                    priceDifferencePercentage,
                    netProfitPercentage,
                    riskScore,
                    liquidity,
                    volatility,
                    isViable
            );
            
            return opportunity;
        } catch (Exception e) {
            logError("Error creating arbitrage opportunity: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Formats price values with appropriate scientific notation for small values
     * 
     * @param price The price to format
     * @return A formatted string representation of the price
     */
    private String formatPrice(double price) {
        if (price < 0.001) {
            return String.format("%.4E", price);
        } else {
            return String.format("%.8f", price);
        }
    }
    
    /**
     * Log a debug message if notification service is available
     * 
     * @param message The message to log
     */
    private void logDebug(String message) {
        if (notificationService != null) {
            notificationService.logDebug(message);
        }
    }

    /**
     * Calculate the arbitrage opportunity for a specific trading pair between two exchanges.
     *
     * @param fromExchange  The exchange to buy from
     * @param toExchange    The exchange to sell to
     * @param tradingPair   The trading pair (e.g., "BTCUSDT")
     * @param amount        The amount to trade
     * @param riskManager   The risk manager to evaluate risk
     * @param notifService  The notification service for logging
     * @return An ArbitrageOpportunity object, or null if no opportunity exists
     */
    @Override
    public ArbitrageOpportunity calculateArbitrage(
            IExchangeService fromExchange, IExchangeService toExchange,
            String tradingPair, double amount, IRiskManager riskManager,
            INotificationService notifService) {
        
        // Cast to ExchangeService if possible
        if (!(fromExchange instanceof ExchangeService) || !(toExchange instanceof ExchangeService)) {
            logError("Cannot calculate arbitrage - unsupported exchange type", 
                    new IllegalArgumentException("Exchanges must be instance of ExchangeService"));
            notifService.logError("Cannot calculate arbitrage - unsupported exchange type", 
                    new IllegalArgumentException("Exchanges must be instance of ExchangeService"));
            return null;
        }
        
        ExchangeService fromExchangeService = (ExchangeService) fromExchange;
        ExchangeService toExchangeService = (ExchangeService) toExchange;
        
        return calculateArbitrage(fromExchangeService, toExchangeService, tradingPair, 
                amount, riskManager, notifService);
    }
    
    /**
     * Determines if an order would be a maker or taker order based on the order book and price.
     * 
     * @param orderBook The order book for the trading pair
     * @param price The price at which the order would be placed
     * @param isBuy Whether this is a buy (true) or sell (false) order
     * @return true if this would be a maker order, false if it would be a taker order
     */
    private boolean isMakerOrder(OrderBook orderBook, double price, boolean isBuy) {
        if (orderBook == null) {
            // Conservative approach: if we don't have order book data, assume taker
            return false;
        }
        
        if (isBuy) {
            // For buy orders: if our price is below the lowest ask, it's a maker order
            double lowestAsk = orderBook.getAsks().isEmpty() ? Double.MAX_VALUE 
                    : orderBook.getAsks().get(0).getPrice();
            return price < lowestAsk;
        } else {
            // For sell orders: if our price is above the highest bid, it's a maker order
            double highestBid = orderBook.getBids().isEmpty() ? 0 
                    : orderBook.getBids().get(0).getPrice();
            return price > highestBid;
        }
    }
    
    /**
     * Internal implementation of arbitrage calculation for ExchangeService types.
     *
     * @param fromExchange  The exchange to buy from
     * @param toExchange    The exchange to sell to
     * @param tradingPair   The trading pair (e.g., "BTCUSDT")
     * @param amount        The amount to trade
     * @param riskManager   The risk manager to evaluate risk
     * @param notifService  The notification service for logging
     * @return An ArbitrageOpportunity object, or null if no opportunity exists
     */
    private ArbitrageOpportunity calculateArbitrage(
            ExchangeService fromExchange, ExchangeService toExchange,
            String tradingPair, double amount, IRiskManager riskManager,
            INotificationService notifService) {
        
        try {
            // Get fresh ticker data to ensure accurate pricing
            Ticker buyTicker = fromExchange.getTicker(tradingPair);
            Ticker sellTicker = toExchange.getTicker(tradingPair);
            
            // Get order books to determine if orders will be maker or taker
            OrderBook buyOrderBook = fromExchange.getOrderBook(tradingPair);
            OrderBook sellOrderBook = toExchange.getOrderBook(tradingPair);
            
            if (buyTicker == null || sellTicker == null) {
                notifService.logWarning("Cannot calculate arbitrage for " + tradingPair + 
                        " - Missing ticker data from " + 
                        (buyTicker == null ? fromExchange.getExchangeName() : toExchange.getExchangeName()));
                return null;
            }
            
            if (buyOrderBook == null || sellOrderBook == null) {
                notifService.logWarning("Cannot determine maker/taker status for " + tradingPair + 
                        " - Missing order book data");
                // We can still continue with a conservative assumption (taker fees)
            }
            
            double buyPrice = buyTicker.getAskPrice();
            double sellPrice = sellTicker.getBidPrice();
            
            // Determine if the orders will be maker or taker
            boolean isBuyMaker = isMakerOrder(buyOrderBook, buyPrice, true);
            boolean isSellMaker = isMakerOrder(sellOrderBook, sellPrice, false);
            
            notifService.logInfo("Order type for " + tradingPair + " on " + 
                    fromExchange.getExchangeName() + ": " + (isBuyMaker ? "Maker" : "Taker"));
            notifService.logInfo("Order type for " + tradingPair + " on " + 
                    toExchange.getExchangeName() + ": " + (isSellMaker ? "Maker" : "Taker"));
            
            // Get the effective fee percentages
            double buyFeePercentage = fromExchange.getFeePercentage(tradingPair, isBuyMaker);
            double sellFeePercentage = toExchange.getFeePercentage(tradingPair, isSellMaker);
            
            // Store prices from tickers
            // We're using the existing buyPrice and sellPrice variables that are already declared above
            buyPrice = buyTicker.getAskPrice();
            sellPrice = sellTicker.getBidPrice();
            
            // Set placeholder values
            double profit = 0.0;
            double profitPercentage = 0.0;
            
            // Log the fee details for transparency
            notifService.logInfo(tradingPair + " fee details - Buy: " + 
                    fromExchange.getExchangeName() + " " + 
                    (isBuyMaker ? "maker" : "taker") + " fee " + 
                    (buyFeePercentage * 100) + "%, Sell: " + 
                    toExchange.getExchangeName() + " " + 
                    (isSellMaker ? "maker" : "taker") + " fee " + 
                    (sellFeePercentage * 100) + "%");
            
            // For now, always create an arbitrage opportunity for testing
            // Later we will re-implement proper profitability checks
            notifService.logInfo("Creating placeholder arbitrage opportunity for " + tradingPair);
                
                // Get fresh ticker data for risk assessment
                buyTicker = fromExchange.getTicker(tradingPair);
                sellTicker = toExchange.getTicker(tradingPair);
                
                if (buyTicker == null || sellTicker == null) {
                    notifService.logWarning("Cannot assess risk for " + tradingPair + 
                            " - Missing ticker data for risk assessment");
                    return null;
                }
                
                return createArbitrageOpportunity(fromExchange, toExchange, tradingPair,
                        amount, buyPrice, sellPrice, profit, profitPercentage,
                        riskManager, buyFeePercentage, sellFeePercentage,
                        isBuyMaker, isSellMaker);
        } catch (Exception e) {
            notifService.logError("Error calculating arbitrage for " + tradingPair + 
                    " between " + fromExchange.getExchangeName() + 
                    " and " + toExchange.getExchangeName() + ": " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public ArbitrageResult scanForOpportunities() {
        if (exchanges.isEmpty()) {
            if (notificationService != null) {
                notificationService.logWarning("No exchanges configured for arbitrage scanning");
            }
            return new ArbitrageResultImpl(new ArrayList<>());
        }
        
        // Map to track which exchanges support each trading pair
        Map<TradingPair, Set<ExchangeService>> pairExchangeMap = new HashMap<>();
        
        // Collect all trading pairs from all exchanges and track supporting exchanges
        for (ExchangeService exchange : exchanges) {
            List<TradingPair> exchangePairs = exchange.getTradingPairs();
            if (exchangePairs == null || exchangePairs.isEmpty()) {
                continue;
            }
            
            for (TradingPair pair : exchangePairs) {
                pairExchangeMap.computeIfAbsent(pair, k -> new HashSet<>()).add(exchange);
            }
        }
        
        // Filter to pairs available on at least two exchanges (viable for arbitrage)
        List<TradingPair> viablePairs = pairExchangeMap.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (notificationService != null) {
            notificationService.logInfo("Found " + viablePairs.size() + 
                    " trading pairs available on at least two exchanges");
        }
        
        return scanForOpportunities(viablePairs);
    }
    
    @Override
    public ArbitrageResult scanForOpportunities(List<TradingPair> pairs) {
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();
        
        for (TradingPair pair : pairs) {
            ArbitrageOpportunity opportunity = calculateArbitrage(pair);
            if (opportunity != null) {
                opportunities.add(opportunity);
            }
        }
        
        // Return the arbitrage result with collected opportunities
        return new ArbitrageResultImpl(opportunities);
    }
    
    @Override
    public void addExchange(ExchangeService exchange) {
        if (exchange != null && !exchanges.contains(exchange)) {
            exchanges.add(exchange);
            if (notificationService != null) {
                notificationService.logInfo("Added exchange: " + exchange.getExchangeName());
            }
        }
    }
    
    @Override
    public void removeExchange(ExchangeService exchange) {
        if (exchange != null) {
            exchanges.remove(exchange);
            if (notificationService != null) {
                notificationService.logInfo("Removed exchange: " + exchange.getExchangeName());
            }
        }
    }
    
    @Override
    public List<ExchangeService> getExchanges() {
        return new ArrayList<>(exchanges);
    }
    
    @Override
    public void setMinProfitThreshold(double threshold) {
        this.minProfitPercent = threshold;
        if (notificationService != null) {
            notificationService.logInfo("Set minimum profit threshold to: " + threshold + "%");
        }
    }

    /**
     * Helper method to log an error with and without an exception.
     * This handles the signature difference in the INotificationService.
     *
     * @param message The error message
     * @param error The exception, or null if none
     */
    private void logError(String message, Throwable error) {
        if (notificationService != null) {
            notificationService.logError(message, error);
        }
    }
}