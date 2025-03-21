package com.example.smartfoodinventorytracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText nameEditText;
    private TextView emailTextView;
    private Button saveButton, backButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ✅ Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ✅ Link UI Elements
        nameEditText = findViewById(R.id.nameEditText);
        emailTextView = findViewById(R.id.emailTextView);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);

        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        // ✅ Load User Data
        userRef.get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        emailTextView.setText(document.getString("email"));
                        nameEditText.setText(document.getString("name"));
                    } else {
                        // If the user document doesn’t exist, create it
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", mAuth.getCurrentUser().getEmail());
                        userData.put("name", "");
                        userRef.set(userData);
                        emailTextView.setText(mAuth.getCurrentUser().getEmail());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show());

        // ✅ Save Name to Firestore
        saveButton.setOnClickListener(v -> {
            String newName = nameEditText.getText().toString().trim();
            if (TextUtils.isEmpty(newName)) {
                Toast.makeText(ProfileActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            userRef.update("name", newName)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                        // Redirect back to Dashboard after saving
                        Intent intent = new Intent(ProfileActivity.this, DashboardActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show());
        });

        // ✅ Back Button
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, DashboardActivity.class));
            finish();
        });
    }
}