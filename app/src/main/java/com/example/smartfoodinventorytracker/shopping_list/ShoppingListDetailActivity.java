package com.example.smartfoodinventorytracker.shopping_list;

import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.InventoryAdapter;
import com.example.smartfoodinventorytracker.inventory.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private SearchView searchView;
    private FloatingActionButton fab;
    private Button confirmListButton;

    // List of shopping list items (Product objects)
    private List<Product> shoppingList = new ArrayList<>();
    // Using the InventoryAdapter for now to display items
    private InventoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list_detail);

        // Retrieve the push key and display name from the Intent extras
        String listKey = getIntent().getStringExtra("listKey");
        String listName = getIntent().getStringExtra("listName");
        if (listName == null) {
            listName = "Shopping List";
        }
        if (listKey == null) {
            Toast.makeText(this, "Error: List key is missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set up the Toolbar using the display name
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView listDisplayName = findViewById(R.id.listDisplayName);
        listDisplayName.setText(listName); // ✅ Display list name here instead


        // Get references for UI elements
        recyclerView = findViewById(R.id.shoppingRecyclerView);
        emptyMessage = findViewById(R.id.emptyShoppingMessage);
        searchView = findViewById(R.id.searchView);
        fab = findViewById(R.id.fab_add_item);
        confirmListButton = findViewById(R.id.btnConfirmList);

        // Set up RecyclerView with a LinearLayoutManager and our adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter(this, shoppingList, FirebaseAuth.getInstance().getCurrentUser().getUid());
        recyclerView.setAdapter(adapter);

        // Update the empty state immediately
        updateEmptyState();

        // Set up SearchView to filter the list as user types
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        // Set the Confirm List button logic
        confirmListButton.setOnClickListener(v -> confirmList());

        // Load shopping list items using the push key. IMPORTANT: We reference the "items" child.
        loadShoppingListItems(listKey);

        // Set up the FAB click listener for adding new items (placeholder for now)
        fab.setOnClickListener(v -> {
            // TODO: Open AddManualProductDialogFragment or BarcodeScannerActivity to add new items
            Toast.makeText(ShoppingListDetailActivity.this, "Add item functionality not implemented yet", Toast.LENGTH_SHORT).show();
        });
    }

    // Update UI if the list is empty or not
    private void updateEmptyState() {
        if (shoppingList.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Load items from the "items" child of the shopping list node in Firebase
    private void loadShoppingListItems(String listKey) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference listRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("shopping-list")
                .child(listKey)
                .child("items");

        listRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Product> productList = new ArrayList<>();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    Product product = itemSnapshot.getValue(Product.class);
                    if (product != null) {
                        productList.add(product);
                    }
                }
                shoppingList.clear();
                shoppingList.addAll(productList);
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ShoppingListDetailActivity.this, "Failed to load list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Confirm the shopping list by pushing items to inventory and marking the list as "last_used"
    private void confirmList() {
        if (shoppingList.isEmpty()) {
            Toast.makeText(this, "Cannot confirm an empty list.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String listName = getIntent().getStringExtra("listName");
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // 1. Push each item from the shopping list into the inventory
        DatabaseReference inventoryRef = userRef.child("inventory_product");
        for (Product product : shoppingList) {
            String productId = inventoryRef.push().getKey();
            if (productId != null) {
                inventoryRef.child(productId).setValue(product);
            }
        }

        // 2. Set this list as "last_used" (clearing previous data first)
        DatabaseReference lastUsedRef = userRef.child("shopping-list").child("last_used");
        lastUsedRef.setValue(null); // Clear previous last_used
        for (Product product : shoppingList) {
            String productId = lastUsedRef.push().getKey();
            if (productId != null) {
                lastUsedRef.child(productId).setValue(product);
            }
        }

        Toast.makeText(this, "List confirmed and items added to inventory", Toast.LENGTH_LONG).show();
        finish();
    }
}
