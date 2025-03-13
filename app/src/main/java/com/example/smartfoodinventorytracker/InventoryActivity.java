package com.example.smartfoodinventorytracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {
    private RecyclerView inventoryRecyclerView;
    private DatabaseReference databaseReference;
    private InventoryAdapter inventoryAdapter;
    private List<Product> productList = new ArrayList<>();
    private RequestQueue requestQueue; // For API calls

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inventory);

        // Initialize Firebase Database Reference
        databaseReference = FirebaseDatabase.getInstance().getReference("inventory_product");

        // Initialize Volley for API calls
        requestQueue = Volley.newRequestQueue(this);

        // Set up UI
        setUpToolbar();

        // Initialize RecyclerView
        inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView);
        inventoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        inventoryAdapter = new InventoryAdapter(productList);
        inventoryRecyclerView.setAdapter(inventoryAdapter);

        // Fetch inventory data from Firebase
        fetchInventoryData();

        // ✅ Request Camera Permission
        checkCameraPermission();
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        // Handle barcode scanner button click
        ImageView barcodeScannerButton = findViewById(R.id.barcodeLogo);
        barcodeScannerButton.setOnClickListener(v -> {
            Log.d("BarcodeScanner", "Barcode button clicked");  // ✅ Debugging Log
            Intent intent = new Intent(InventoryActivity.this, BarcodeScannerActivity.class);
            startActivityForResult(intent, 1);  // ✅ Use startActivityForResult() instead of startActivity()
        });
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


    private void fetchProductData(String barcode) {
        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.has("product")) {
                            JSONObject product = response.getJSONObject("product");
                            String productName = product.optString("product_name", "Unknown Product");
                            String brand = product.optString("brands", "Unknown Brand");

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
        Product product = new Product(barcode, name, brand);
        databaseReference.child(barcode).setValue(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product added to inventory!", Toast.LENGTH_SHORT).show();
                    Log.d("Firebase", "Product saved in inventory_product: " + barcode + " - " + name);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
    }

    private void fetchInventoryData() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    Product product = productSnapshot.getValue(Product.class);
                    if (product != null) {
                        Log.d("Firebase", "Loaded product from inventory_product: " + product.getName());
                        productList.add(product);
                    }
                }
                inventoryAdapter.notifyDataSetChanged();

                // ✅ Show "Inventory empty" if list is empty
                TextView emptyMessage = findViewById(R.id.emptyInventoryMessage);
                if (productList.isEmpty()) {
                    emptyMessage.setVisibility(View.VISIBLE);
                    inventoryRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyMessage.setVisibility(View.GONE);
                    inventoryRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching data", error.toException());
            }
        });
    }

}
