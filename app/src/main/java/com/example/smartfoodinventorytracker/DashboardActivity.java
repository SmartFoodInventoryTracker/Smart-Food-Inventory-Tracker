package com.example.smartfoodinventorytracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextView greetingText;
    private LinearLayout inventoryButton, shoppingListButton, fridgeConditionButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // ✅ Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ✅ Initialize Views
        drawerLayout = findViewById(R.id.drawerLayout);
        greetingText = findViewById(R.id.greetingText);
        inventoryButton = findViewById(R.id.inventoryButton);
        shoppingListButton = findViewById(R.id.shoppingListButton);
        fridgeConditionButton = findViewById(R.id.fridgeConditionButton);

        // ✅ Setup Toolbar
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // ✅ Setup Navigation Drawer with If-Else Logic
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_inventory) {
                Toast.makeText(this, "Inventory", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_shopping_lists) {
                Toast.makeText(this, "Shopping Lists", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_fridge_condition) {
                Toast.makeText(this, "Fridge Condition", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_notifications) {
                Toast.makeText(this, "Notifications Center", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Settings Clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_logout) {
                logout();
            } else {
                Toast.makeText(this, "Unknown Option", Toast.LENGTH_SHORT).show();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // ✅ Load User Name for Dynamic Greeting
        loadUserName();

        // ✅ Dashboard Button Click Listeners
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
    }

    private void loadUserName() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        if (name == null || name.isEmpty()) {
                            greetingText.setText("Hi, User");
                        } else {
                            greetingText.setText("Hi, " + name);
                        }
                    } else {
                        greetingText.setText("Hi, User");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show());
    }


    // ✅ Handle Logout Logic
    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserName(); // ✅ Reload user data when returning
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
