package com.example.smartfoodinventorytracker.shopping_list;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
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
    // false = Edit mode; true = Shopping mode.
    private boolean isShoppingMode = false;
    private String listKey; // The shopping list's unique key

    // Bottom container buttons.
    private Button addItemBtn, secondaryBtn;

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
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Get list name and set title.
        String listName = getIntent().getStringExtra("listName");
        TextView listDisplayName = findViewById(R.id.shoppingListTitle);
        listDisplayName.setText(listName != null ? listName : "Shopping List");

        // Get mode extra.
        String mode = getIntent().getStringExtra("mode");
        isShoppingMode = "shopping".equals(mode);

        // Get the list key from Intent extras.
        listKey = getIntent().getStringExtra("listKey");
        if (listKey == null) {
            Toast.makeText(this, "No list key provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize bottom buttons.
        addItemBtn = findViewById(R.id.addItemBtn);
        secondaryBtn = findViewById(R.id.secondaryBtn);
        updateBottomButtons();

        // "Add Item" button always launches the add product dialog.
        addItemBtn.setOnClickListener(v -> {
            AddShoppingProductDialogFragment dialog = new AddShoppingProductDialogFragment();
            dialog.setListener(this);
            dialog.show(getSupportFragmentManager(), "AddShoppingProductDialog");
        });

        // Secondary button functionality depends on mode.
        secondaryBtn.setOnClickListener(v -> {
            if (!isShoppingMode) {
                // "Go Shopping" functionality.
                new AlertDialog.Builder(this)
                        .setTitle("Enter Shopping Mode")
                        .setMessage("Are you sure you want to switch to shopping mode?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            isShoppingMode = true;
                            updateBottomButtons();
                            adapter.setShoppingMode(isShoppingMode);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(this, "Shopping mode activated", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            } else {
                // "Confirm Purchase" functionality.
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Purchase")
                        .setMessage("Are you sure you want to confirm purchase?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            DatabaseReference inventoryRef = FirebaseDatabase.getInstance()
                                    .getReference("users")
                                    .child(userId)
                                    .child("inventory_product");
                            for (Product product : productList) {
                                inventoryRef.child(product.getBarcode()).setValue(product);
                            }
                            Toast.makeText(this, "Products added to inventory!", Toast.LENGTH_SHORT).show();
                            // Optionally clear expiry info from the shopping products.
                            for (Product product : productList) {
                                product.setExpiryDate("Not set");
                            }
                            isShoppingMode = false;
                            updateBottomButtons();
                            adapter.setShoppingMode(isShoppingMode);
                            adapter.notifyDataSetChanged();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        // Setup RecyclerView.
        recyclerView = findViewById(R.id.shoppingRecyclerView);
        emptyMessage = findViewById(R.id.emptyShoppingMessage);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = new ArrayList<>();
        adapter = new ShoppingListItemAdapter(this, productList, isShoppingMode, userId, listKey);
        recyclerView.setAdapter(adapter);

        // Load the shopping list items from Firebase.
        loadShoppingListItems(listKey);
    }

    private void updateBottomButtons() {
        if (isShoppingMode) {
            // In shopping mode, update secondary button text.
            secondaryBtn.setText("Confirm Purchase");
        } else {
            // In edit mode, secondary button text is "Go Shopping".
            secondaryBtn.setText("Go Shopping");
        }
    }

    private void loadShoppingListItems(String listKey) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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
        AddShoppingManualProductDialogFragment manualDialog = new AddShoppingManualProductDialogFragment();
        manualDialog.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        manualDialog.setManualProductListener(this);
        // Pass the current mode so the manual dialog can show expiry if in shopping mode.
        manualDialog.setShoppingMode(isShoppingMode);
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
