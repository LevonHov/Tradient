package com.example.tradient.data.http;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * Provider for OkHttpClient instances.
 * Centralizes HTTP client creation and configuration.
 */
public class HttpClientProvider {
    
    // Public constants for timeouts so they can be reused by other components
    public static final int DEFAULT_CONNECT_TIMEOUT = 10;
    public static final int DEFAULT_READ_TIMEOUT = 30;
    public static final int DEFAULT_WRITE_TIMEOUT = 30;
    
    private static OkHttpClient sharedClient;
    
    /**
     * Get a shared OkHttpClient instance with default settings.
     * The shared instance is created once and reused.
     *
     * @return Shared OkHttpClient instance
     */
    public static synchronized OkHttpClient getSharedClient() {
        if (sharedClient == null) {
            sharedClient = createDefaultClient();
        }
        return sharedClient;
    }
    
    /**
     * Create a new OkHttpClient with default timeouts.
     *
     * @return New OkHttpClient instance
     */
    public static OkHttpClient createDefaultClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Create a new OkHttpClient with custom timeouts.
     *
     * @param connectTimeout Connect timeout in seconds
     * @param readTimeout Read timeout in seconds
     * @param writeTimeout Write timeout in seconds
     * @return New OkHttpClient instance with custom timeouts
     */
    public static OkHttpClient createClient(int connectTimeout, int readTimeout, int writeTimeout) {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .build();
    }
} 