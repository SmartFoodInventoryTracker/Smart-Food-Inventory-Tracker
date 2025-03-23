package com.example.smartfoodinventorytracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.cardview.widget.CardView;


public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navView;
    private TextView greetingText;
    private CardView inventoryButton, shoppingListButton, fridgeConditionButton;
    private SharedPreferences shared;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this); // ✅ Modern edge-to-edge mode

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        requestNotificationPermissionIfNeeded(); // ✅ Ask permission early

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        NotificationHelper notificationHelper = new NotificationHelper(this, true, userId);
        notificationHelper.startFridgeMonitoringService();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setUpUi();
        setUpToolBar();
        setUpNavBar();
        loadUserName();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (view, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setTranslationY(statusBarHeight);
            return insets;
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadUserName(); // ✅ Reload user data when returning
    }

    private void setUpUi() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navigationView);
        greetingText = findViewById(R.id.greetingText);
        inventoryButton = findViewById(R.id.inventoryButton);
        shoppingListButton = findViewById(R.id.shoppingListButton);
        fridgeConditionButton = findViewById(R.id.fridgeConditionButton);

        // Set onClickListeners
        setUpOnClickListeners();
    }

    private void setUpToolBar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START))
                drawerLayout.closeDrawer(GravityCompat.START);
            else
                drawerLayout.openDrawer(GravityCompat.START);
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 200);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notifications denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setUpNavBar() {
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_inventory) {
                Toast.makeText(this, "Inventory", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, InventoryActivity.class));
            } else if (id == R.id.nav_shopping_lists) {
                Toast.makeText(this, "Shopping Lists", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ShoppingList.class));
            } else if (id == R.id.nav_fridge_condition) {
                Toast.makeText(this, "Fridge Condition", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, FridgeConditions.class));
            } else if (id == R.id.nav_notifications) {
                Toast.makeText(this, "Notifications Center", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, NotificationCenter.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Settings Clicked", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_logout) {
                logout();
            } else {
                Toast.makeText(this, "Unknown Option", Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setUpOnClickListeners() {
        // Inventory button onClickListener
        inventoryButton.setOnClickListener(v -> goToInventory());

        // Shopping button onClickListener
        shoppingListButton.setOnClickListener(v -> goToShoppingList());

        // Fridge button onClickListener
        fridgeConditionButton.setOnClickListener(v -> goToFridgeConditions());
    }

    private void setUpGreetings() {
        shared = getApplicationContext().getSharedPreferences("event_preferences", Context.MODE_PRIVATE);
    }

    // ✅ Load User Name from Firestore
    private void loadUserName() {
        // Determine greeting based on time of day
        String greeting;
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            greeting = "Good morning";
        } else if (hour >= 12 && hour < 18) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }

        // Set rounded background programmatically
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ContextCompat.getColor(this, android.R.color.black));  // black background
        bg.setCornerRadius(100); // large radius for round edges
        greetingText.setBackground(bg);

        // ✅ Try loading from SharedPreferences first
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String cachedName = prefs.getString("cached_name", null);

        if (cachedName != null && !cachedName.isEmpty()) {
            greetingText.setText("👋 " + greeting + ", " + cachedName);
        } else {
            greetingText.setText("👋 " + greeting + ", loading...");
        }

        // ✅ Then load fresh data from Firestore
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    String name = null;

                    if (document.exists()) {
                        name = document.getString("name");
                    }

                    if (name != null && !name.trim().isEmpty()) {
                        greetingText.setText("👋 " + greeting + ", " + name);

                        // ✅ Save to SharedPreferences for next time
                        prefs.edit().putString("cached_name", name).apply();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }


    // ✅ Handle Logout Logic
    private void logout() {
        FirebaseAuth.getInstance().signOut();
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply();

        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ✅ Handle Back Press for Drawer
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void goToInventory() {
        Toast.makeText(this, "Inventory Clicked", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, InventoryActivity.class);
        startActivity(intent);
    }

    private void goToShoppingList() {
        Toast.makeText(this, "Shopping", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, ShoppingList.class);
        startActivity(intent);
    }

    private void goToFridgeConditions() {
        Toast.makeText(this, "Fridge Condition Clicked", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, FridgeConditions.class);
        startActivity(intent);
    }
}

