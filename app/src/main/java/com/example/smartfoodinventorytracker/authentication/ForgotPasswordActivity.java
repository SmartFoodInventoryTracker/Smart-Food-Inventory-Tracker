package com.example.smartfoodinventorytracker.authentication;

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

import com.example.smartfoodinventorytracker.R;
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

        // Makes sure the content doesn't overlap with the system UI (like status bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        forgotPasswordEmailEditText = findViewById(R.id.forgotPasswordEmailEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmNewPasswordEditText = findViewById(R.id.confirmNewPasswordEditText);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        backToLoginTextView = findViewById(R.id.backToLoginTextView);

        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = forgotPasswordEmailEditText.getText().toString().trim();
                String newPassword = newPasswordEditText.getText().toString().trim();
                String confirmNewPassword = confirmNewPasswordEditText.getText().toString().trim();

                // Make sure no field is left empty and the email/passwords are valid
                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmNewPassword)) {
                    Toast.makeText(ForgotPasswordActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                } else if (!newPassword.equals(confirmNewPassword)) {
                    Toast.makeText(ForgotPasswordActivity.this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                } else if (newPassword.length() < 6) {
                    Toast.makeText(ForgotPasswordActivity.this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
                } else {
                    // Only allow password reset if the logged-in user matches the provided email
                    if (currentUser != null && currentUser.getEmail().equals(email)) {
                        // Firebase only lets authenticated users update their password directly
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(ForgotPasswordActivity.this, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                                        finish();
                                    } else {
                                        // This can fail if the session is too old or network is down
                                        Toast.makeText(ForgotPasswordActivity.this, "Error: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // User isn't logged in or email doesn't match
                        Toast.makeText(ForgotPasswordActivity.this, "User not authenticated. Please log in first.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        backToLoginTextView.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
    }
}
