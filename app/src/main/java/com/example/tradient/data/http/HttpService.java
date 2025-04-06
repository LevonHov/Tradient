package com.example.tradient.data.http;

import com.example.tradient.data.interfaces.INotificationService;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for making HTTP requests using OkHttp.
 * Provides methods for common HTTP operations like GET and POST.
 */
public class HttpService {
    
    private static final int DEFAULT_TIMEOUT = 15; // seconds
    private final OkHttpClient httpClient;
    private INotificationService notificationService;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    /**
     * Constructor with notification service.
     *
     * @param notificationService The notification service for logging
     */
    public HttpService(INotificationService notificationService) {
        this.notificationService = notificationService;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * Default constructor without notification service.
     */
    public HttpService() {
        this(null);
    }
    
    /**
     * Make a synchronous GET request.
     *
     * @param url The URL to request
     * @return The response body as a string
     * @throws IOException If the request fails
     */
    public String get(String url) throws IOException {
        return get(url, new HashMap<>());
    }
    
    /**
     * Make a synchronous GET request with headers.
     *
     * @param url The URL to request
     * @param headers Map of headers to include
     * @return The response body as a string
     * @throws IOException If the request fails
     */
    public String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder requestBuilder = new Request.Builder().url(url);
        
        // Add headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }
        
        Request request = requestBuilder.build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }
    
    /**
     * Make a synchronous POST request with a JSON body.
     *
     * @param url The URL to request
     * @param jsonBody The JSON body to send
     * @return The response body as a string
     * @throws IOException If the request fails
     */
    public String postJson(String url, JSONObject jsonBody) throws IOException {
        return postJson(url, jsonBody, new HashMap<>());
    }
    
    /**
     * Make a synchronous POST request with a JSON body and headers.
     *
     * @param url The URL to request
     * @param jsonBody The JSON body to send
     * @param headers Map of headers to include
     * @return The response body as a string
     * @throws IOException If the request fails
     */
    public String postJson(String url, JSONObject jsonBody, Map<String, String> headers) throws IOException {
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body);
        
        // Add headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }
        
        Request request = requestBuilder.build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }
    
    /**
     * Parse a JSON string response into a JSONObject.
     *
     * @param response The JSON string response
     * @return Parsed JSONObject
     */
    public static JSONObject parseJsonObject(String response) {
        try {
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Parse a JSON string response into a JSONArray.
     *
     * @param response The JSON string response
     * @return Parsed JSONArray
     */
    public static JSONArray parseJsonArray(String response) {
        try {
            return new JSONArray(response);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Set the notification service.
     *
     * @param notificationService The notification service
     */
    public void setNotificationService(INotificationService notificationService) {
        this.notificationService = notificationService;
    }
} 