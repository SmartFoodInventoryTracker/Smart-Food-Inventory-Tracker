package com.example.smartfoodinventorytracker.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.smartfoodinventorytracker.dashboard.DashboardActivity;
import com.example.smartfoodinventorytracker.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;


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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // ✅ User is already signed in — skip onboarding
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_onboarding);
        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ Link UI elements
        rotatingTitle = findViewById(R.id.rotatingTitle);
        rotatingDescription = findViewById(R.id.rotatingDescription);
        Button loginButton = findViewById(R.id.loginButton);
        Button signUpButton = findViewById(R.id.signUpButton);

        ImageView logo = findViewById(R.id.appLogo); // or whatever the ID is for each page

        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.smart_food_inventory_logo);
        RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(getResources(), originalBitmap);

        // Adjust this to control roundness (the higher, the rounder)
        roundedDrawable.setCornerRadius(400f);
        roundedDrawable.setAntiAlias(true);

        logo.setImageDrawable(roundedDrawable);


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
