package com.example.tradient.domain.risk;

import android.util.Log;

/**
 * Validates risk scores and risk-related calculations.
 */
public final class RiskScoreValidator {
    private static final String TAG = "RiskScoreValidator";

    private RiskScoreValidator() {} // Prevent instantiation

    /**
     * Validates if a risk score is within acceptable bounds.
     * @param score The risk score to validate
     * @return true if the score is valid, false otherwise
     */
    public static boolean isValidRiskScore(double score) {
        return score >= RiskScoreConstants.MIN_RISK_SCORE && 
               score <= RiskScoreConstants.MAX_RISK_SCORE;
    }

    /**
     * Normalizes a risk score to the valid range [0,1].
     * @param score The risk score to normalize
     * @return Normalized risk score
     */
    public static double normalizeRiskScore(double score) {
        return Math.max(RiskScoreConstants.MIN_RISK_SCORE, 
                       Math.min(RiskScoreConstants.MAX_RISK_SCORE, score));
    }

    /**
     * Validates volume data for risk calculation.
     * @param volume The volume to validate
     * @return true if volume is valid, false otherwise
     */
    public static boolean isValidVolume(double volume) {
        return volume >= RiskScoreConstants.MIN_VOLUME_THRESHOLD;
    }

    /**
     * Validates volatility data for risk calculation.
     * @param volatility The volatility to validate
     * @return true if volatility is valid, false otherwise
     */
    public static boolean isValidVolatility(double volatility) {
        return volatility >= 0 && volatility <= RiskScoreConstants.MAX_VOLATILITY_THRESHOLD;
    }

    /**
     * Validates slippage data for risk calculation.
     * @param slippage The slippage to validate
     * @return true if slippage is valid, false otherwise
     */
    public static boolean isValidSlippage(double slippage) {
        return slippage >= 0 && slippage <= RiskScoreConstants.MAX_SLIPPAGE_THRESHOLD;
    }

    /**
     * Validates market depth data for risk calculation.
     * @param depth The market depth to validate
     * @param averageVolume The average volume for normalization
     * @return true if market depth is valid, false otherwise
     */
    public static boolean isValidMarketDepth(double depth, double averageVolume) {
        return depth >= RiskScoreConstants.MIN_MARKET_DEPTH_THRESHOLD * averageVolume;
    }

    /**
     * Logs a warning if a risk score is invalid.
     * @param score The risk score to check
     * @param source The source of the risk score
     */
    public static void logInvalidRiskScore(double score, String source) {
        if (!isValidRiskScore(score)) {
            Log.w(TAG, "Invalid risk score from " + source + ": " + score);
        }
    }

    /**
     * Gets the risk level description for a given risk score.
     * @param score The risk score
     * @return Risk level description
     */
    public static String getRiskLevelDescription(double score) {
        if (score < RiskScoreConstants.EXTREME_RISK_THRESHOLD) {
            return "Extreme Risk";
        } else if (score < RiskScoreConstants.VERY_HIGH_RISK_THRESHOLD) {
            return "Very High Risk";
        } else if (score < RiskScoreConstants.HIGH_RISK_THRESHOLD) {
            return "High Risk";
        } else if (score < RiskScoreConstants.MODERATE_HIGH_RISK_THRESHOLD) {
            return "Moderate High Risk";
        } else if (score < RiskScoreConstants.BALANCED_RISK_THRESHOLD) {
            return "Balanced Risk";
        } else if (score < RiskScoreConstants.MODERATE_RISK_THRESHOLD) {
            return "Moderate Risk";
        } else if (score < RiskScoreConstants.LOW_RISK_THRESHOLD) {
            return "Low Risk";
        } else if (score < RiskScoreConstants.VERY_LOW_RISK_THRESHOLD) {
            return "Very Low Risk";
        } else {
            return "Minimal Risk";
        }
    }
} 