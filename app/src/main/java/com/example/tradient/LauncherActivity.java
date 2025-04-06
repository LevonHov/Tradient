package com.example.tradient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradient.demo.ArbitrageActivity;

/**
 * Launcher Activity to choose between different modes of the application
 */
public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        
        // Button to launch the standard app
        Button standardAppButton = findViewById(R.id.standardAppButton);
        standardAppButton.setOnClickListener(v -> {
            Intent intent = new Intent(LauncherActivity.this, MainActivity.class);
            startActivity(intent);
        });
        
        // Button to launch the arbitrage demo
        Button arbitrageButton = findViewById(R.id.arbitrageButton);
        arbitrageButton.setOnClickListener(v -> {
            Intent intent = new Intent(LauncherActivity.this, ArbitrageActivity.class);
            startActivity(intent);
        });
    }
} 