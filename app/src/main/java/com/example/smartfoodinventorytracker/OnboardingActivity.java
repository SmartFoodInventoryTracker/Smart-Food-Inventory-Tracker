package com.example.smartfoodinventorytracker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OnboardingActivity extends AppCompatActivity {

    private TextView rotatingTitle, rotatingDescription;
    private String[] titles = {
            "Welcome to Smart Food Inventory Tracker!",
            "Track & Monitor Food Expiry",
            "Automated Grocery Lists",
            "Stay Notified & Reduce Waste"
    };

    private String[] descriptions = {
            "Manage your food inventory efficiently.",
            "Know exactly when your food expires.",
            "Auto-generate grocery lists based on your needs.",
            "Receive real-time alerts and reduce food waste."
    };

    private int currentIndex = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable textRotator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // ✅ Link UI elements
        rotatingTitle = findViewById(R.id.rotatingTitle);
        rotatingDescription = findViewById(R.id.rotatingDescription);
        Button loginButton = findViewById(R.id.loginButton);
        Button signUpButton = findViewById(R.id.signUpButton);

        // ✅ Rotate Texts Every 3 Seconds
        textRotator = new Runnable() {
            @Override
            public void run() {
                currentIndex = (currentIndex + 1) % titles.length;
                rotatingTitle.setText(titles[currentIndex]);
                rotatingDescription.setText(descriptions[currentIndex]);
                handler.postDelayed(this, 4000); // Rotate every 4 seconds
            }
        };
        handler.post(textRotator);

        // ✅ Login Button Click
        loginButton.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));

        // ✅ Sign Up Button Click
        signUpButton.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(textRotator); // ✅ Stop rotating when activity is destroyed
    }
}
