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

    private List<Product> shoppingList = new ArrayList<>();
    private InventoryAdapter adapter;  // You can subclass this later for shopping list
    private Button confirmListButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list_detail);

        // Get list name from intent and set title
        String listName = getIntent().getStringExtra("listName");
        if (listName == null) listName = "Shopping List";

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(listName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // UI references
        recyclerView = findViewById(R.id.shoppingRecyclerView);
        emptyMessage = findViewById(R.id.emptyShoppingMessage);
        searchView = findViewById(R.id.searchView);
        fab = findViewById(R.id.fab_add_item);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter(this, shoppingList, "TEMP_USER_ID"); // Replace later
        recyclerView.setAdapter(adapter);

        // Handle empty state
        updateEmptyState();

        // Handle search
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

        confirmListButton = findViewById(R.id.btnConfirmList);

        confirmListButton.setOnClickListener(v -> confirmList());

        loadShoppingListItems(listName);


        // Handle FAB click
        fab.setOnClickListener(v -> {
            // TODO: Open AddManualProductDialogFragment or BarcodeScannerActivity
        });
    }

    private void updateEmptyState() {
        if (shoppingList.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void loadShoppingListItems(String listName) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference listRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("shopping-list")
                .child(listName);

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

    private void confirmList() {
        if (shoppingList.isEmpty()) {
            Toast.makeText(this, "Cannot confirm an empty list.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String listName = getIntent().getStringExtra("listName");
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // 1. Push items to inventory
        DatabaseReference inventoryRef = userRef.child("inventory_product");
        for (Product product : shoppingList) {
            String productId = inventoryRef.push().getKey();
            if (productId != null) {
                inventoryRef.child(productId).setValue(product);
            }
        }

        // 2. Save this list as "last_used"
        DatabaseReference lastUsedRef = userRef.child("shopping-list").child("last_used");
        lastUsedRef.setValue(null); // clear previous
        for (Product product : shoppingList) {
            String productId = lastUsedRef.push().getKey();
            if (productId != null) {
                lastUsedRef.child(productId).setValue(product);
            }
        }

        Toast.makeText(this, "List confirmed and items added to inventory", Toast.LENGTH_LONG).show();
        finish(); // optionally return to ShoppingListActivity
    }


}
