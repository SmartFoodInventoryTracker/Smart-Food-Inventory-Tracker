package com.example.smartfoodinventorytracker.inventory;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.widget.SearchView;

import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.utils.BarcodeScannerActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import androidx.core.app.NavUtils;

public class InventoryActivity extends AppCompatActivity
        implements AddProductDialogFragment.AddProductDialogListener,
        AddManualProductDialogFragment.ManualProductListener {
    private RecyclerView inventoryRecyclerView;
    private boolean isManualAddition = false;
    private DatabaseReference databaseReference;
    private InventoryAdapter inventoryAdapter;
    private List<Product> productList = new ArrayList<>();

    private RequestQueue requestQueue; // For API calls


    private int selectedSortOption = -1; // Default: No sort selected

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inventory);

        // Initialize Firebase Database Reference
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("inventory_product");

        // Initialize Volley for API calls
        requestQueue = Volley.newRequestQueue(this);

        // Set up UI
        setUpToolbar();

        // Initialize RecyclerView
        inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView);
        inventoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        inventoryAdapter = new InventoryAdapter(this, productList, userId);
        inventoryRecyclerView.setAdapter(inventoryAdapter);



        FloatingActionButton fabAddProduct = findViewById(R.id.fab_add_product);
        fabAddProduct.setOnClickListener(v -> {
            AddProductDialogFragment dialog = new AddProductDialogFragment();
            dialog.show(getSupportFragmentManager(), "AddProductDialog");
        });

        SearchView searchView = findViewById(R.id.searchView);
        ImageButton filterButton = findViewById(R.id.btn_filter);

        searchView.setQuery("", false); // Clear any previous input
        searchView.clearFocus(); // Remove focus to hide blinking cursor
        searchView.setQueryHint("Search for a product"); //  Always show the hint


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                inventoryAdapter.filter(query);  // Apply filter
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    searchView.setQueryHint("Search for a product");  // ✅ Restore hint when empty
                }
                inventoryAdapter.filter(newText);  // Apply filter on text change
                return true;
            }
        });

        // Handle Filter Button Click (just for now, no sorting logic yet)
        filterButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(InventoryActivity.this, v);
            popup.getMenuInflater().inflate(R.menu.sort_menu, popup.getMenu());

            // ✅ Make all items checkable
            for (int i = 0; i < popup.getMenu().size(); i++) {
                popup.getMenu().getItem(i).setCheckable(true);
            }

            // ✅ Restore the last selected sort
            if (selectedSortOption != -1) {
                popup.getMenu().findItem(selectedSortOption).setChecked(true);
            }

            try {
                Field popupField = popup.getClass().getDeclaredField("mPopup");
                popupField.setAccessible(true);
                Object menuPopupHelper = popupField.get(popup);
                Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                setForceIcons.invoke(menuPopupHelper, true);

                // ✅ Apply Custom Background
                View popupView = ((View) menuPopupHelper.getClass().getMethod("getPopup").invoke(menuPopupHelper));
                popupView.setBackgroundResource(R.drawable.popup_background);

            } catch (Exception e) {
                e.printStackTrace();
            }

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();

                // ✅ Uncheck previous selection
                if (selectedSortOption != -1) {
                    popup.getMenu().findItem(selectedSortOption).setChecked(false);
                }

                // ✅ Update the selected sort option
                selectedSortOption = itemId;
                item.setChecked(true);

                // ✅ Apply Sorting Logic
                if (itemId == R.id.sort_expiry_asc) {

                    ExpirydateSort(true);
                    this.inventoryAdapter.sorting = InventoryAdapter.Sorting.EXP_DATE_ASC;

                } else if (itemId == R.id.sort_expiry_desc) {

                    ExpirydateSort(false);
                    this.inventoryAdapter.sorting = InventoryAdapter.Sorting.EXP_DATE_DES;
                } else if (itemId == R.id.sort_date_added_asc) {

                    DateAddedSort(true);
                    this.inventoryAdapter.sorting = InventoryAdapter.Sorting.DATE_ADD_ASC;
                } else if (itemId == R.id.sort_date_added_desc) {

                    DateAddedSort(false);
                    this.inventoryAdapter.sorting = InventoryAdapter.Sorting.DATE_ADD_DES;
                }

                return true;
            });

            popup.show();
        });

        // Fetch inventory data from Firebase
        fetchInventoryData();

        // Request Camera Permission
        checkCameraPermission();

    }

    public void update_productwhendeleted()
    {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();







         inventoryRecyclerView.setAdapter(inventoryAdapter);

        //fetchInventoryData();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.inventory_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all_inventory) {
            confirmDeleteAllInventoryItems();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteAllInventoryItems() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Inventory Items")
                .setMessage("Are you sure you want to delete all items from your inventory?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(userId)
                            .child("inventory_product");

                    ref.removeValue().addOnSuccessListener(aVoid -> {
                        productList.clear();
                        inventoryAdapter.updateList(productList);
                        Toast.makeText(this, "All inventory items deleted", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete inventory items", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }


    @Override
    protected void onStart() {
        super.onStart();
        fetchInventoryData();
    }

    public void UpdatingSorting()
    {
        InventoryAdapter.Sorting sorting = inventoryAdapter.sorting;
        System.out.println();
        Log.d("G", "Inventory sorting " + inventoryAdapter.sorting + " items"); // ✅ Debugging log


        if (inventoryAdapter.sorting== InventoryAdapter.Sorting.EXP_DATE_ASC) {
            Toast.makeText(getApplicationContext(), "Sorting Exp", Toast.LENGTH_SHORT).show();

            ExpirydateSort(true);


        } else if (inventoryAdapter.sorting== InventoryAdapter.Sorting.EXP_DATE_DES) {
            Toast.makeText(getApplicationContext(), "Sorting Exp2", Toast.LENGTH_SHORT).show();
            ExpirydateSort(false);

        } else if (inventoryAdapter.sorting== InventoryAdapter.Sorting.DATE_ADD_ASC) {

            DateAddedSort(true);

        } else if (inventoryAdapter.sorting== InventoryAdapter.Sorting.DATE_ADD_DES) {

            DateAddedSort(false);

        }


    }


    private void AddProduct(String name, String brand, List<String> expirationdate)
    {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(brand) || expirationdate.isEmpty()) {
            Toast.makeText(this, "Product details cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        //in theory look for barcode here
        LocalDate currentDate = LocalDate.now();
        String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("d/M/yyyy"));
        System.out.println(formattedDate);
        String expiration_date = expirationdate.get(2)+"/"+expirationdate.get(1)+"/"+expirationdate.get(0);
        String uni_name_brand = name +brand+expiration_date; //ensuring that each product have different id
        String barcode = UUID.nameUUIDFromBytes(uni_name_brand.getBytes()).toString();
        Product product = new Product(barcode,name, brand);
        product.setExpiryDate(expiration_date);
        product.setDateAdded(formattedDate);


        databaseReference.child(barcode).setValue(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product added to inventory!", Toast.LENGTH_SHORT).show();
                    Log.d("Firebase", "Product saved: " + barcode + " - " + name);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
    }


    private void ExpirydateSort(boolean ascended) {
        if (productList.isEmpty()) return;

        Product[] productArray = productList.toArray(new Product[0]);

        if (ascended)
            MergeSort.sortexp(productArray, 0, productArray.length - 1, MergeSort.OrderType.ASCENDING);
        else
            MergeSort.sortexp(productArray, 0, productArray.length - 1, MergeSort.OrderType.DESCENDING);

        productList.clear();
        productList.addAll(Arrays.asList(productArray));

        runOnUiThread(() -> {
            inventoryAdapter.updateList(productList);
            fetchInventoryData(); // Ensure accurate data
        });
    }


    private void DateAddedSort(boolean ascended) {
        Product[] productArray = productList.toArray(new Product[0]);

        if (ascended)
            MergeSort.sortadded(productArray, 0, productArray.length - 1, MergeSort.OrderType.ASCENDING);
        else
            MergeSort.sortadded(productArray, 0, productArray.length - 1, MergeSort.OrderType.DESCENDING);

        // ✅ Update the list in Adapter correctly
        productList.clear();
        productList.addAll(Arrays.asList(productArray));
        runOnUiThread(() -> {
            inventoryAdapter.updateList(productList);
            fetchInventoryData(); // Ensure accurate data
        });
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(this));

    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // ✅ Show an explanation before requesting permission
                new AlertDialog.Builder(this)
                        .setTitle("Camera Permission Needed")
                        .setMessage("This app requires camera access to scan barcodes. Please allow camera access.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            ActivityCompat.requestPermissions(InventoryActivity.this, new String[]{Manifest.permission.CAMERA}, 101);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                // ✅ Directly request permission (for first-time users)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("CameraPermission", "Camera permission granted.");
                Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                // ✅ If user selected "Don't ask again", show a dialog to open Settings
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    showSettingsDialog();
                } else {
                    Toast.makeText(this, "Camera permission is required for barcode scanning.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // ✅ Show a Dialog to Redirect User to App Settings
    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Camera Permission Denied")
                .setMessage("This app needs camera access to scan barcodes. Please enable it in Settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("scannedBarcode")) {
                String barcode = data.getStringExtra("scannedBarcode");
                Log.d("InventoryActivity", "Received Barcode: " + barcode);  // ✅ Debugging Log
                fetchProductData(barcode);
            } else {
                Log.e("InventoryActivity", "No barcode received");
                Toast.makeText(this, "No barcode scanned", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onAddManually() {
        // Set the flag so we know this is a manual addition.
        isManualAddition = true;
        AddManualProductDialogFragment manualDialog = new AddManualProductDialogFragment();
        manualDialog.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        manualDialog.setManualProductListener(this);
        manualDialog.show(getSupportFragmentManager(), "ManualProductDialog");
    }


    @Override
    public void onScanBarcode() {
        Intent intent = new Intent(this, BarcodeScannerActivity.class);
        startActivityForResult(intent, 1);

    }

    @Deprecated
    @Override
    public void onProductAdded() {

    }


    private void fetchProductData(String barcode) {
        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("product")) {
                            JSONObject product = response.getJSONObject("product");
                            String productName = product.optString("product_name", "Unknown Product");
                            String brand = product.optString("brands", "Unknown Brand");

                            // ✅ Correctly fetch the image URL
                            String imageUrl = product.optString("image_url", null);
                            if (imageUrl == null || imageUrl.isEmpty()) {
                                Log.w("ProductImage", "No image URL found for barcode: " + barcode);
                            } else {
                                Log.d("ProductImage", "Fetched image URL: " + imageUrl);
                            }

                            // ✅ Pass image URL to Firebase
                            saveProductToFirebase(barcode, productName, brand);
                        } else {
                            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    private void saveProductToFirebase(String barcode, String name, String brand) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference inventoryRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("inventory_product");

        String expiry = "Not set"; // Or fetch from user input if available

        Product newProduct = new Product(barcode, name, brand);
        newProduct.setDateAdded(getCurrentDate());
        newProduct.setExpiryDate(expiry);
        newProduct.setQuantity(1); // Default quantity

        inventoryRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Failed to access inventory", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean merged = false;
            for (DataSnapshot itemSnap : task.getResult().getChildren()) {
                Product existing = itemSnap.getValue(Product.class);
                if (existing == null) continue;

                boolean sameName = normalize(existing.getName()).equals(normalize(name));
                boolean sameBrand = normalize(existing.getBrand()).equals(normalize(brand));
                boolean sameExpiry = normalize(existing.getExpiryDate()).equals(normalize(expiry));

                if (sameName && sameBrand && sameExpiry) {
                    int mergedQty = existing.getQuantity() + 1;
                    if (mergedQty > 99) {
                        mergedQty = 99;
                        Toast.makeText(this, "Merged quantity capped at 99 for " + name, Toast.LENGTH_SHORT).show();
                    }
                    existing.setQuantity(mergedQty);

                    existing.setDateAdded(getCurrentDate());
                    inventoryRef.child(existing.getBarcode()).setValue(existing);
                    Toast.makeText(this, "Merged with existing item", Toast.LENGTH_SHORT).show();
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                // Generate a new ID if expiry is different
                String newId = inventoryRef.push().getKey();
                if (newId != null) {
                    newProduct.barcode = newId;
                    inventoryRef.child(newId).setValue(newProduct)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Product added", Toast.LENGTH_SHORT).show();

                                // ✅ Open edit dialog
                                ProductDetailsDialogFragment dialog = ProductDetailsDialogFragment.newInstance(newProduct);
                                dialog.setUserId(userId);
                                dialog.setProductDialogListener(new ProductDetailsDialogFragment.ProductDialogListener() {
                                    @Override
                                    public void onProductUpdated(Product updatedProduct) {
                                        // Optional: update UI
                                    }

                                    @Override
                                    public void onProductDeleted(String barcode) {
                                        // Optional: handle delete
                                    }
                                });
                                dialog.show(InventoryActivity.this.getSupportFragmentManager(), "ProductDetailsDialog");

                                // ✅ Store product name temporarily in intent
                                Intent intent = getIntent();
                                intent.putExtra("data", name); // For search bar
                                intent.putExtra("sortNewest", true); // Signal to sort by newest
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
                }
            }

        });
    }


    private void fetchInventoryData() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                update_productwhendeleted();

                productList.clear();

                List<Product> tempProductList = new ArrayList<>(); // Temporary list for adapter

                Log.d("Firebase", "Snapshot Children Count: " + snapshot.getChildrenCount()); // ✅ Log Firebase data count

                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    Product product = productSnapshot.getValue(Product.class);
                    if (product != null) {
                        tempProductList.add(product);
                    }
                }

                if (tempProductList.isEmpty()) {
                    Log.e("Firebase", "No products were retrieved!"); // 🚨 Debugging message
                }

                productList.addAll(tempProductList);
                inventoryAdapter.updateList(tempProductList);

                // ✅ Show "Inventory empty" if list is empty
                TextView emptyMessage = findViewById(R.id.emptyInventoryMessage);
                if (productList.isEmpty()) {
                    emptyMessage.setVisibility(View.VISIBLE);
                    inventoryRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyMessage.setVisibility(View.GONE);
                    inventoryRecyclerView.setVisibility(View.VISIBLE);
                }

                UpdatingSorting();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching data", error.toException());
            }
        });
    }

    @Override
    public void onProductAdded(Product newProduct) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference inventoryRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("inventory_product");

        inventoryRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Failed to access inventory", Toast.LENGTH_SHORT).show();
                return;
            }

            DataSnapshot snapshot = task.getResult();
            boolean merged = false;

            for (DataSnapshot itemSnap : snapshot.getChildren()) {
                Product existing = itemSnap.getValue(Product.class);
                if (existing == null) continue;

                boolean sameName = normalize(existing.getName()).equals(normalize(newProduct.getName()));
                boolean sameBrand = normalize(existing.getBrand()).equals(normalize(newProduct.getBrand()));
                boolean sameExpiry = normalize(existing.getExpiryDate()).equals(normalize(newProduct.getExpiryDate()));

                if (sameName && sameBrand && sameExpiry) {
                    int mergedQty = existing.getQuantity() + newProduct.getQuantity();
                    if (mergedQty > 99) {
                        mergedQty = 99;
                        Toast.makeText(this, "Merged quantity capped at 99 for " + newProduct.getName(), Toast.LENGTH_SHORT).show();
                    }
                    existing.setQuantity(mergedQty);
                    existing.setDateAdded(getCurrentDate());
                    inventoryRef.child(existing.getBarcode()).setValue(existing);
                    Toast.makeText(this, "Merged with existing product", Toast.LENGTH_SHORT).show();
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                String newId = inventoryRef.push().getKey();
                if (newId != null) {
                    newProduct.barcode = newId;
                    newProduct.setDateAdded(getCurrentDate());
                    inventoryRef.child(newId).setValue(newProduct)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Product added", Toast.LENGTH_SHORT).show();
                                // Only open the edit dialog if this wasn't a manual addition.
                                if (!isManualAddition) {
                                    ProductDetailsDialogFragment dialog = ProductDetailsDialogFragment.newInstance(newProduct);
                                    dialog.setUserId(userId);
                                    dialog.setProductDialogListener(new ProductDetailsDialogFragment.ProductDialogListener() {
                                        @Override
                                        public void onProductUpdated(Product updatedProduct) {
                                            // Optional: update UI
                                        }
                                        @Override
                                        public void onProductDeleted(String barcode) {
                                            // Optional: handle delete
                                        }
                                    });
                                    dialog.show(getSupportFragmentManager(), "ProductDetailsDialog");
                                }
                                // Reset the flag after processing.
                                isManualAddition = false;
                                Intent intent = getIntent();
                                intent.putExtra("data", newProduct.getName());
                                intent.putExtra("sortNewest", true);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
                }
            }

        });
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }


}
