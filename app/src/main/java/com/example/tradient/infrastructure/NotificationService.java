package com.example.tradient.infrastructure;

import android.util.Log;

import com.example.tradient.data.interfaces.INotificationService;
import com.example.tradient.data.interfaces.ArbitrageResult;

/**
 * Implementation of notification service for logging and alerts
 */
public class NotificationService implements INotificationService {
    
    private static final String TAG = "TradientNotification";
    
    @Override
    public void logInfo(String message) {
        Log.i(TAG, message);
    }
    
    @Override
    public void logError(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
    }
    
    @Override
    public void logWarning(String message) {
        Log.w(TAG, message);
    }
    
    @Override
    public void logDebug(String message) {
        Log.d(TAG, message);
    }

    @Override
    public void notify(String title, String message, String type) {
        
    }

    @Override
    public void notifyArbitrageError(Throwable throwable) {
        Log.e(TAG, "Arbitrage error occurred", throwable);
    }
    
    @Override
    public void notifyArbitrageOpportunity(ArbitrageResult result) {
        Log.i(TAG, "Arbitrage opportunity detected: " + result.toString());
    }
} 