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
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class SignupActivity extends AppCompatActivity {

    private EditText signupEmailEditText, signupPasswordEditText, confirmPasswordEditText;
    private Button signupButton;
    private TextView loginRedirectTextView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // ✅ Handle Window Insets for Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // ✅ Link UI Elements
        signupEmailEditText = findViewById(R.id.signupEmailEditText);
        signupPasswordEditText = findViewById(R.id.signupPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        signupButton = findViewById(R.id.signupButton);
        loginRedirectTextView = findViewById(R.id.loginRedirectTextView);

        // ✅ Signup Button Logic
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = signupEmailEditText.getText().toString().trim();
                String password = signupPasswordEditText.getText().toString().trim();
                String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                    Toast.makeText(SignupActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(SignupActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                } else if (password.length() < 6) {
                    Toast.makeText(SignupActivity.this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
                } else if (!password.equals(confirmPassword)) {
                    Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                } else {
                    // ✅ Create User with Firebase
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(SignupActivity.this, "Signup Successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignupActivity.this, DashboardActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(SignupActivity.this, "Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        // ✅ Redirect to Login Page
        loginRedirectTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
}
