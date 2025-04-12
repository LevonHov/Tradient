package com.example.tradient.domain.risk;

/**
 * Constants for risk score calculations and thresholds.
 * All risk scores are on a 0-1 scale where:
 * - 0.0 represents highest risk
 * - 1.0 represents lowest risk
 */
public final class RiskScoreConstants {
    private RiskScoreConstants() {} // Prevent instantiation

    // Risk score bounds
    public static final double MIN_RISK_SCORE = 0.0;
    public static final double MAX_RISK_SCORE = 1.0;
    
    // Risk level thresholds
    public static final double EXTREME_RISK_THRESHOLD = 0.1;    // 0.0 - 0.1
    public static final double VERY_HIGH_RISK_THRESHOLD = 0.2;  // 0.1 - 0.2
    public static final double HIGH_RISK_THRESHOLD = 0.3;       // 0.2 - 0.3
    public static final double MODERATE_HIGH_RISK_THRESHOLD = 0.4; // 0.3 - 0.4
    public static final double BALANCED_RISK_THRESHOLD = 0.5;   // 0.4 - 0.5
    public static final double MODERATE_RISK_THRESHOLD = 0.6;   // 0.5 - 0.6
    public static final double LOW_RISK_THRESHOLD = 0.7;        // 0.6 - 0.7
    public static final double VERY_LOW_RISK_THRESHOLD = 0.8;   // 0.7 - 0.8
    public static final double MINIMAL_RISK_THRESHOLD = 1.0;    // 0.8 - 1.0

    // Default weights for risk factors
    public static final double DEFAULT_LIQUIDITY_WEIGHT = 0.25;
    public static final double DEFAULT_VOLATILITY_WEIGHT = 0.20;
    public static final double DEFAULT_SLIPPAGE_WEIGHT = 0.15;
    public static final double DEFAULT_MARKET_DEPTH_WEIGHT = 0.10;
    public static final double DEFAULT_EXECUTION_SPEED_WEIGHT = 0.10;
    public static final double DEFAULT_FEE_WEIGHT = 0.10;
    public static final double DEFAULT_MARKET_REGIME_WEIGHT = 0.05;
    public static final double DEFAULT_SENTIMENT_WEIGHT = 0.05;

    // Normalization constants
    public static final double VOLUME_NORMALIZATION_FACTOR = 1000000.0; // 1M volume
    public static final double SPREAD_NORMALIZATION_FACTOR = 0.01;      // 1% spread
    public static final double FEE_NORMALIZATION_FACTOR = 0.001;        // 0.1% fee
    public static final double SLIPPAGE_NORMALIZATION_FACTOR = 0.005;   // 0.5% slippage

    // Validation thresholds
    public static final double MIN_VOLUME_THRESHOLD = 10000.0;
    public static final double MAX_VOLATILITY_THRESHOLD = 0.03; // 3%
    public static final double MAX_SLIPPAGE_THRESHOLD = 0.01;  // 1%
    public static final double MIN_MARKET_DEPTH_THRESHOLD = 0.1; // 10% of average volume
} 