package com.example.smartfoodinventorytracker.shopping_list;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import androidx.core.app.NavUtils;

public class ShoppingListDetailActivity extends AppCompatActivity implements
        AddShoppingProductListener, ManualShoppingProductListener {

    // Define string constants for mode titles.
    private static final String EDIT_MODE_TITLE = "✍️ Edit Mode";
    private static final String SHOPPING_MODE_TITLE = "🛍️ Shopping Mode";

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
        toolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(this));

        // Get list name and set title.
        String listName = getIntent().getStringExtra("listName");
        TextView listDisplayName = findViewById(R.id.shoppingListTitle);
        listDisplayName.setText(listName != null ? listName : "Shopping List");

        // Get mode extra.
        String mode = getIntent().getStringExtra("mode");
        isShoppingMode = "shopping".equals(mode);

        TextView headerTitle = findViewById(R.id.headerTitle);
        headerTitle.setText(isShoppingMode ? SHOPPING_MODE_TITLE : EDIT_MODE_TITLE);

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

        toolbar.setNavigationOnClickListener(v -> {
            if (isShoppingMode) {
                new AlertDialog.Builder(this)
                        .setTitle("Leave Shopping Mode?")
                        .setMessage("Do you want to leave shopping mode and return to edit mode?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            isShoppingMode = false;
                            adapter.setShoppingMode(false);
                            adapter.notifyDataSetChanged();
                            updateBottomButtons();
                            ((TextView)findViewById(R.id.headerTitle)).setText(EDIT_MODE_TITLE);
                        })
                        .setNegativeButton("No", null)
                        .show();
            } else {
                finish();
            }
        });

        // Secondary button functionality depends on mode.
        secondaryBtn.setOnClickListener(v -> {
            if (!isShoppingMode) {
                // "Go Shopping" logic.
                new AlertDialog.Builder(this)
                        .setTitle("Enter Shopping Mode")
                        .setMessage("Are you sure you want to switch to shopping mode?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            isShoppingMode = true;
                            updateBottomButtons();
                            adapter.setShoppingMode(isShoppingMode);
                            adapter.notifyDataSetChanged();
                            headerTitle.setText(SHOPPING_MODE_TITLE);
                            Toast.makeText(this, "Shopping mode activated", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            } else {
                // "Confirm Purchase" logic.
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Purchase")
                        .setMessage("Are you sure you want to confirm purchase?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            DatabaseReference inventoryRef = FirebaseDatabase.getInstance()
                                    .getReference("users")
                                    .child(userId)
                                    .child("inventory_product");

                            // Step 1: Read existing inventory.
                            inventoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot inventorySnapshot) {
                                    Map<String, Product> existingInventory = new HashMap<>();
                                    for (DataSnapshot itemSnap : inventorySnapshot.getChildren()) {
                                        Product p = itemSnap.getValue(Product.class);
                                        if (p != null) {
                                            existingInventory.put(p.getBarcode(), p);
                                        }
                                    }

                                    // Step 2: Merge shopping list items into inventory.
                                    for (Product shoppingProduct : productList) {
                                        boolean merged = false;
                                        for (Product inventoryProduct : existingInventory.values()) {
                                            if (productsMatch(inventoryProduct, shoppingProduct)) {
                                                // Merge quantity and update date added.
                                                int combinedQty = inventoryProduct.getQuantity() + shoppingProduct.getQuantity();
                                                if (combinedQty > 99) {
                                                    combinedQty = 99;
                                                    Toast.makeText(ShoppingListDetailActivity.this,
                                                            "Quantity capped at 99 for " + shoppingProduct.getName(), Toast.LENGTH_SHORT).show();
                                                }

                                                inventoryProduct.setQuantity(combinedQty);
                                                inventoryProduct.setDateAdded(getCurrentDate());

                                                inventoryRef.child(inventoryProduct.getBarcode()).setValue(inventoryProduct);

                                                merged = true;
                                                break;
                                            }
                                        }
                                        if (!merged) {
                                            // Different product: assign new ID and insert as a new item.
                                            String newId = inventoryRef.push().getKey();
                                            shoppingProduct.setDateAdded(getCurrentDate());
                                            if (newId != null) {
                                                shoppingProduct.barcode = newId;
                                                inventoryRef.child(newId).setValue(shoppingProduct);
                                            }
                                        }
                                    }

                                    Toast.makeText(ShoppingListDetailActivity.this, "Purchase confirmed", Toast.LENGTH_SHORT).show();
                                    // Update lastUsed timestamp.
                                    DatabaseReference listMetaRef = FirebaseDatabase.getInstance()
                                            .getReference("users")
                                            .child(userId)
                                            .child("shopping-list")
                                            .child(listKey)
                                            .child("lastUsed");
                                    listMetaRef.setValue(System.currentTimeMillis() / 1000);

                                    // Clear expiry info from shopping products.
                                    DatabaseReference shoppingListRef = FirebaseDatabase.getInstance()
                                            .getReference("users")
                                            .child(userId)
                                            .child("shopping-list")
                                            .child(listKey)
                                            .child("items");

                                    shoppingListRef.removeValue().addOnSuccessListener(aVoid -> {
                                        for (Product p : productList) {
                                            p.setExpiryDate("Not set");
                                        }
                                        Map<String, Product> mergedMap = new LinkedHashMap<>();
                                        for (Product p : productList) {
                                            String key = normalize(p.name) + "_" + normalize(p.brand);
                                            if (mergedMap.containsKey(key)) {
                                                Product existing = mergedMap.get(key);
                                                int combinedQty = existing.getQuantity() + p.getQuantity();
                                                if (combinedQty > 50) {
                                                    combinedQty = 50;
                                                    Toast.makeText(ShoppingListDetailActivity.this,
                                                            "Quantity capped at 50 for " + p.getName(), Toast.LENGTH_SHORT).show();
                                                }
                                                existing.setQuantity(combinedQty);
                                                existing.name = capitalizeSentence(p.name);
                                            } else {
                                                mergedMap.put(key, p);
                                            }
                                        }

                                        shoppingListRef.removeValue().addOnSuccessListener(unused -> {
                                            for (Product p : mergedMap.values()) {
                                                shoppingListRef.child(p.getBarcode()).setValue(p);
                                            }
                                            productList.clear();
                                            productList.addAll(mergedMap.values());
                                            adapter.notifyDataSetChanged();
                                            updateEmptyMessage();
                                        });
                                        adapter.notifyDataSetChanged();
                                        updateEmptyMessage();
                                    });
                                    adapter.notifyDataSetChanged();
                                    // Switch back to Edit Mode.
                                    new AlertDialog.Builder(ShoppingListDetailActivity.this)
                                            .setTitle("Purchase Complete")
                                            .setMessage("Your purchase was saved. What would you like to do next?")
                                            .setPositiveButton("Return to Edit Mode", (dialogInterface, i) -> {
                                                isShoppingMode = false;
                                                headerTitle.setText(EDIT_MODE_TITLE);
                                                updateBottomButtons();
                                                adapter.setShoppingMode(false);
                                                adapter.notifyDataSetChanged();
                                            })
                                            .setNegativeButton("Go to Inventory", (dialogInterface, i) -> {
                                                finish();
                                                startActivity(new Intent(ShoppingListDetailActivity.this, com.example.smartfoodinventorytracker.inventory.InventoryActivity.class));
                                            })
                                            .setCancelable(false)
                                            .show();

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(ShoppingListDetailActivity.this, "Failed to access inventory", Toast.LENGTH_SHORT).show();
                                }
                            });
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
        loadShoppingListItems(listKey);
    }

    private boolean productsMatch(Product a, Product b) {
        return normalize(a.name).equals(normalize(b.name)) &&
                normalize(a.brand).equals(normalize(b.brand)) &&
                normalize(a.expiryDate).equals(normalize(b.expiryDate));
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private String capitalizeSentence(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shopping_list_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all) {
            confirmDeleteAllItems();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteAllItems() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Items")
                .setMessage("Are you sure you want to delete all items from this list?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference shoppingListRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(userId)
                            .child("shopping-list")
                            .child(listKey)
                            .child("items");
                    shoppingListRef.removeValue().addOnSuccessListener(aVoid -> {
                        productList.clear();
                        adapter.notifyDataSetChanged();
                        updateEmptyMessage();
                        Toast.makeText(this, "All items deleted", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete items", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    private void updateBottomButtons() {
        int black = getResources().getColor(android.R.color.black, null);
        int blue = getResources().getColor(R.color.gray, null); // Using gray as blue in edit mode

        if (isShoppingMode) {
            addItemBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(black));
            secondaryBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(black));
            secondaryBtn.setText("Confirm Purchase");
        } else {
            addItemBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue));
            secondaryBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue));
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

    @Override
    public void onAddManually() {
        AddShoppingManualProductDialogFragment manualDialog = new AddShoppingManualProductDialogFragment();
        manualDialog.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        manualDialog.setManualProductListener(this);
        manualDialog.setExistingProducts(productList);
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

    @Override
    public void onProductAdded(Product product) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        for (Product existing : productList) {
            if (productsMatch(existing, product)) {
                Toast.makeText(this, "Product already exists", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("shopping-list")
                .child(listKey)
                .child("items")
                .child(product.getBarcode())
                .setValue(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product added to shopping list!", Toast.LENGTH_SHORT).show();
                    productList.add(product);
                    adapter.notifyItemInserted(productList.size() - 1);
                    updateEmptyMessage();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
    }
}
