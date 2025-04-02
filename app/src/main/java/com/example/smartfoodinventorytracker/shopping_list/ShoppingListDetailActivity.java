package com.example.smartfoodinventorytracker.shopping_list;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.smartfoodinventorytracker.shopping_list.AddShoppingProductDialogFragment.AddShoppingProductListener;
import com.example.smartfoodinventorytracker.shopping_list.AddShoppingManualProductDialogFragment.ManualShoppingProductListener;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListDetailActivity extends AppCompatActivity implements
        AddShoppingProductListener, ManualShoppingProductListener {

    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private List<Product> productList;
    private ShoppingListItemAdapter adapter;
    private boolean isShoppingMode = false;
    private String listKey; // The shopping list's unique key

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list_detail);
        EdgeToEdge.enable(this);

        // Apply system insets for proper padding.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup toolbar with back button.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Get list name and set title.
        String listName = getIntent().getStringExtra("listName");
        TextView listDisplayName = findViewById(R.id.shoppingListTitle);
        listDisplayName.setText(listName != null ? listName : "Shopping List");

        // Get mode extra (edit vs. shopping).
        String mode = getIntent().getStringExtra("mode");
        isShoppingMode = "shopping".equals(mode);

        // Get the list key from Intent extras BEFORE creating the adapter.
        listKey = getIntent().getStringExtra("listKey");
        if (listKey == null) {
            Toast.makeText(this, "No list key provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Retrieve userId.
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Setup FloatingActionButton for adding a product in edit mode.
        FloatingActionButton addButton = findViewById(R.id.fab_add_item);
        if (isShoppingMode) {
            addButton.setVisibility(View.GONE);
        } else {
            addButton.setVisibility(View.VISIBLE);
            addButton.setOnClickListener(v -> {
                // Launch the shopping-specific dialog for adding a product.
                AddShoppingProductDialogFragment dialog = new AddShoppingProductDialogFragment();
                dialog.setListener(this);
                dialog.show(getSupportFragmentManager(), "AddShoppingProductDialog");
            });
        }

        // Setup RecyclerView.
        recyclerView = findViewById(R.id.shoppingRecyclerView);
        emptyMessage = findViewById(R.id.emptyShoppingMessage);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        adapter = new ShoppingListItemAdapter(this, productList, userId, listKey);
        recyclerView.setAdapter(adapter);

        // Load the shopping list items from Firebase.
        loadShoppingListItems(listKey);
    }

    private void loadShoppingListItems(String listKey) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Firebase path: users/{uid}/shopping-list/{listKey}/items
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("shopping-list")
                .child(listKey)
                .child("items");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    Product product = itemSnapshot.getValue(Product.class);
                    if (product != null) {
                        productList.add(product);
                    }
                }
                adapter.notifyDataSetChanged();
                updateEmptyMessage();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ShoppingListDetailActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyMessage() {
        if (productList.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // ----------------- AddShoppingProductListener Methods -----------------

    @Override
    public void onAddManually() {
        // Launch the shopping-specific manual dialog.
        AddShoppingManualProductDialogFragment manualDialog = new AddShoppingManualProductDialogFragment();
        manualDialog.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        manualDialog.setManualProductListener(this);
        manualDialog.show(getSupportFragmentManager(), "AddShoppingManualProductDialog");
    }

    @Override
    public void onScanBarcode() {
        Toast.makeText(this, "Barcode scanning not implemented for shopping list.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProductAdded() {
        // Optional callback; leave empty if not used.
    }

    // ----------------- ManualShoppingProductListener Method -----------------

    @Override
    public void onProductAdded(Product product) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("shopping-list")
                .child(listKey)
                .child("items")
                .child(product.getBarcode())
                .setValue(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ShoppingListDetailActivity.this, "Product added to shopping list!", Toast.LENGTH_SHORT).show();
                    productList.add(product);
                    adapter.notifyItemInserted(productList.size() - 1);
                    updateEmptyMessage();
                })
                .addOnFailureListener(e -> Toast.makeText(ShoppingListDetailActivity.this, "Failed to add product", Toast.LENGTH_SHORT).show());
    }
}
