package com.example.smartfoodinventorytracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
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
        databaseReference = FirebaseDatabase.getInstance().getReference("inventory");

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
            Log.d("BarcodeScanner", "Barcode button clicked"); // Debugging log
            Intent intent = new Intent(InventoryActivity.this, BarcodeScannerActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String barcode = data.getStringExtra("scannedBarcode");
            fetchProductData(barcode);
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
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Product added to inventory!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
    }

    private void fetchInventoryData() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    Product product = productSnapshot.getValue(Product.class);
                    productList.add(product);
                }
                inventoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching data", error.toException());
            }
        });
    }

}
