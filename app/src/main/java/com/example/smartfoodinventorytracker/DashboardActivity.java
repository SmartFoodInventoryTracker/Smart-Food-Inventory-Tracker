package com.example.smartfoodinventorytracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextView greetingText;
    private LinearLayout inventoryButton, shoppingListButton, fridgeConditionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // ✅ Initialize Views
        drawerLayout = findViewById(R.id.drawerLayout);
        greetingText = findViewById(R.id.greetingText);
        inventoryButton = findViewById(R.id.inventoryButton);
        shoppingListButton = findViewById(R.id.shoppingListButton);
        fridgeConditionButton = findViewById(R.id.fridgeConditionButton);

        // ✅ Setup Toolbar
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // ✅ Setup Navigation Drawer
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_settings) {
                Toast.makeText(this, "Settings Clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_logout) {
                Toast.makeText(this, "Logout Clicked", Toast.LENGTH_SHORT).show();
                logout();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // ✅ Button Click Listeners
        inventoryButton.setOnClickListener(v -> {
            Toast.makeText(this, "Inventory Clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, InventoryActivity.class);
            // startActivity(intent);
        });

        shoppingListButton.setOnClickListener(v -> {
            Toast.makeText(this, "Shopping Lists Clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, ShoppingListActivity.class);
            // startActivity(intent);
        });

        fridgeConditionButton.setOnClickListener(v -> {
            Toast.makeText(this, "Fridge Condition Clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, FridgeConditionActivity.class);
            // startActivity(intent);
        });

        // ✅ Set Greeting (Optional: Pass username dynamically)
        greetingText.setText("Hi, User");
    }

    // ✅ Handle Logout Logic
    private void logout() {
        // Example logout logic — update based on your auth
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
}
