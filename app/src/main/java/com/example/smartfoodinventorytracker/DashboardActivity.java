package com.example.smartfoodinventorytracker;

import static com.example.smartfoodinventorytracker.R.id.fridgeConditionButton;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

public class DashboardActivity extends AppCompatActivity {


    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navView;
    private TextView greetingText;
    private LinearLayout inventoryButton, shoppingListButton, fridgeConditionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // ✅ Initialize Views
        setUpUi();
        // ✅ Setup Toolbar
       setUpToolBar();

        // ✅ Setup Navigation Drawer with Full Menu
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_inventory) {
                Toast.makeText(this, "Inventory", Toast.LENGTH_SHORT).show();
                goToInventory();
            } else if (id == R.id.nav_shopping_lists) {
                Toast.makeText(this, "Shopping Lists", Toast.LENGTH_SHORT).show();
                goToShoppingList();
            } else if (id == R.id.nav_fridge_condition) {
                Toast.makeText(this, "Fridge Condition", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_notifications) {
                Toast.makeText(this, "Notifications Center", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_settings) {
                Toast.makeText(this, "Settings Clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_logout) {
                Toast.makeText(this, "Logout Clicked", Toast.LENGTH_SHORT).show();
                logout();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // ✅ Dashboard Button Click Listeners


        // ✅ Set Greeting (Optional: Pass username dynamically)
        greetingText.setText("Hi, User");
    }

    private void setUpUi()
    {
        drawerLayout = findViewById(R.id.drawerLayout);
        navView= findViewById(R.id.navigationView);
        greetingText = findViewById(R.id.greetingText);
        inventoryButton = findViewById(R.id.inventoryButton);
        shoppingListButton = findViewById(R.id.shoppingListButton);
        fridgeConditionButton = findViewById(R.id.fridgeConditionButton);

        //Set onClickListeners
        setUpOnClickListeners();
    }
    private void setUpToolBar(){
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        toolbar.setNavigationOnClickListener(v ->{
         if(drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
         else
            drawerLayout.openDrawer(GravityCompat.START);
        });
    }

    private void setUpOnClickListeners(){
        //Inventory button onClickListener
        inventoryButton.setOnClickListener(v -> {
            goToInventory();
        });
        //Shopping button onClickListener
        shoppingListButton.setOnClickListener(v -> {
            goToShoppingList();
        });
        //Fridge button onClickListener
        fridgeConditionButton.setOnClickListener(v -> {
            Toast.makeText(this, "Fridge Condition Clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, FridgeConditionActivity.class);
            // startActivity(intent);
        });
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

    private void goToInventory(){
        Toast.makeText(this, "Inventory Clicked", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Inventory.class);
        startActivity(intent);
    }
    private void goToShoppingList()
    {
        Toast.makeText(this, "Shopping", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, ShoppingList.class);
        startActivity(intent);
    }
}
