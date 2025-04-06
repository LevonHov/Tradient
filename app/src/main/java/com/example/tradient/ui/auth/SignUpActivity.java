package com.example.tradient.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tradient.MainActivity;
import com.example.tradient.R;
import com.example.tradient.databinding.ActivitySignUpBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private ActivitySignUpBinding binding;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Set up listeners
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Sign up button
        binding.signUpButton.setOnClickListener(v -> {
            String username = binding.usernameEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();
            
            if (validateInputs(username, email, password, confirmPassword)) {
                registerUser(username, email, password);
            }
        });

        // Navigate to sign in
        binding.signInTextView.setOnClickListener(v -> {
            finish(); // Go back to sign in activity
        });

        // Terms and conditions
        binding.termsTextView.setOnClickListener(v -> {
            // TODO: Show terms and conditions dialog
            Toast.makeText(SignUpActivity.this, "Terms and Conditions", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean validateInputs(String username, String email, String password, String confirmPassword) {
        boolean isValid = true;

        // Reset errors
        binding.usernameEditText.setError(null);
        binding.emailEditText.setError(null);
        binding.passwordEditText.setError(null);
        binding.confirmPasswordEditText.setError(null);

        // Validate username
        if (TextUtils.isEmpty(username)) {
            binding.usernameEditText.setError("Username is required");
            isValid = false;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            binding.emailEditText.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.setError("Please enter a valid email address");
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            binding.passwordEditText.setError("Password is required");
            isValid = false;
        } else if (password.length() < 8) {
            binding.passwordEditText.setError("Password must be at least 8 characters long");
            isValid = false;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.confirmPasswordEditText.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            binding.confirmPasswordEditText.setError("Passwords do not match");
            isValid = false;
        }

        // Check terms and conditions
        if (!binding.termsCheckBox.isChecked()) {
            Toast.makeText(this, "Please accept the Terms and Conditions", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void registerUser(String username, String email, String password) {
        showLoading(true);
        
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign up success, update user profile
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            // Set display name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        showLoading(false);
                                        if (profileTask.isSuccessful()) {
                                            // Profile updated successfully
                                            Toast.makeText(SignUpActivity.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                                            navigateToMainActivity();
                                        } else {
                                            // If profile update fails
                                            String errorMessage = profileTask.getException() != null ?
                                                profileTask.getException().getMessage() : "Failed to update profile";
                                            Toast.makeText(SignUpActivity.this, errorMessage,
                                                Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        // If sign up fails, display a message to the user
                        showLoading(false);
                        String errorMessage = task.getException() != null ? 
                            task.getException().getMessage() : "Registration failed";
                        Toast.makeText(SignUpActivity.this, errorMessage, 
                            Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.loadingIndicator.setVisibility(View.VISIBLE);
            binding.signUpButton.setEnabled(false);
            binding.usernameEditText.setEnabled(false);
            binding.emailEditText.setEnabled(false);
            binding.passwordEditText.setEnabled(false);
            binding.confirmPasswordEditText.setEnabled(false);
            binding.termsCheckBox.setEnabled(false);
            binding.signInTextView.setEnabled(false);
        } else {
            binding.loadingIndicator.setVisibility(View.GONE);
            binding.signUpButton.setEnabled(true);
            binding.usernameEditText.setEnabled(true);
            binding.emailEditText.setEnabled(true);
            binding.passwordEditText.setEnabled(true);
            binding.confirmPasswordEditText.setEnabled(true);
            binding.termsCheckBox.setEnabled(true);
            binding.signInTextView.setEnabled(true);
        }
    }
} 