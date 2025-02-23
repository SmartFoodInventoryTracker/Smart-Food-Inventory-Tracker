package com.example.smartfoodinventorytracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText forgotPasswordEmailEditText, newPasswordEditText, confirmNewPasswordEditText;
    private Button resetPasswordButton;
    private TextView backToLoginTextView;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        // ✅ Handle Edge-to-Edge Layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // ✅ Link UI Elements
        forgotPasswordEmailEditText = findViewById(R.id.forgotPasswordEmailEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmNewPasswordEditText = findViewById(R.id.confirmNewPasswordEditText);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        backToLoginTextView = findViewById(R.id.backToLoginTextView);

        // ✅ Reset Password Logic
        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = forgotPasswordEmailEditText.getText().toString().trim();
                String newPassword = newPasswordEditText.getText().toString().trim();
                String confirmNewPassword = confirmNewPasswordEditText.getText().toString().trim();

                // ✅ Field Validations
                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmNewPassword)) {
                    Toast.makeText(ForgotPasswordActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                } else if (!newPassword.equals(confirmNewPassword)) {
                    Toast.makeText(ForgotPasswordActivity.this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                } else if (newPassword.length() < 6) {
                    Toast.makeText(ForgotPasswordActivity.this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
                } else {
                    // ✅ Check if user is already authenticated
                    if (currentUser != null && currentUser.getEmail().equals(email)) {
                        // ✅ Prevent reusing the same password
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(ForgotPasswordActivity.this, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "User not authenticated. Please log in first.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // ✅ Back to Login Navigation
        backToLoginTextView.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }
}
