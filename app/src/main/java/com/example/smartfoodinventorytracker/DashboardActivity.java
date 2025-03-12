package com.example.smartfoodinventorytracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navView;
    private TextView greetingText;
    private LinearLayout inventoryButton, shoppingListButton, fridgeConditionButton;
    private SharedPreferences shared;
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
        setUpUi();

        // ✅ Setup Toolbar
        setUpToolBar();

        // ✅ Setup Navigation Drawer with Full Menu
        setUpNavBar();

        // ✅ Load User Name for Dynamic Greeting
        loadUserName();
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

