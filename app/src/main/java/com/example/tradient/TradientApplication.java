package com.example.tradient;

import android.app.Application;
import android.util.Log;
import java.util.Map;

public class TradientApplication extends Application {
    private static final String TAG = "TradientApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Store the original handler to chain it later
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                // Log the full exception details
                Log.e(TAG, "FATAL UNCAUGHT EXCEPTION in thread " + thread.getName() + ": ", throwable);
                
                // Special handling for Gemini-related errors
                if (throwable != null && throwable.getMessage() != null && 
                    throwable.getMessage().contains("Gemini")) {
                    Log.e(TAG, "Gemini-related error detected. Additional context:", throwable);
                    Log.e(TAG, "Thread state: " + thread.getState());
                    Log.e(TAG, "Thread group: " + (thread.getThreadGroup() != null ? thread.getThreadGroup().getName() : "null"));
                    
                    // Log the full stack trace of all threads for context
                    Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
                    for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                        Thread t = entry.getKey();
                        if (t != thread) { // Skip the crashed thread as we already logged it
                            Log.d(TAG, "Stack trace for thread " + t.getName() + ":");
                            for (StackTraceElement element : entry.getValue()) {
                                Log.d(TAG, "    " + element.toString());
                            }
                        }
                    }
                }

                // Chain to the original handler if it exists
                if (originalHandler != null) {
                    originalHandler.uncaughtException(thread, throwable);
                }
            } catch (Throwable t) {
                // If anything goes wrong in our error handling, make sure to log it
                Log.e(TAG, "Error in uncaught exception handler", t);
                if (originalHandler != null) {
                    originalHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }
} 