package com.example.smartfoodinventorytracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InventoryActivity extends AppCompatActivity {
    private RecyclerView inventoryRecyclerView;
    private DatabaseReference databaseReference;
    private InventoryAdapter inventoryAdapter;
    private List<Product> productList = new ArrayList<>();
    private RequestQueue requestQueue; // For API calls
    private ImageButton addButton;
    private ImageButton sortButton;
    private NavigationView infonav;
    private NavigationView sortnav;

    private Button donebutton;
    private Button cancelbutton;

    private EditText name;
    private EditText brand;
    private EditText year;
    private EditText month;
    private EditText day;

    private Button expirydate;
    private Button dateadded;
    private CheckBox ascended;
    private  CheckBox descended;
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

        addButton = findViewById(R.id.addbutton);
        infonav = findViewById(R.id.info_nav);
        donebutton = findViewById(R.id.donebutton);
        cancelbutton = findViewById(R.id.cancelbutton);
        name = findViewById(R.id.productname);
        brand = findViewById(R.id.productbrand);
        year = findViewById(R.id.editTextNumber4);
        month = findViewById(R.id.editTextNumber3);
        day = findViewById(R.id.editTextNumber2);
        sortButton = findViewById(R.id.sortbutton);
        sortnav = findViewById(R.id.sort_nav);
        ascended = findViewById(R.id.ascended);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if(infonav.getVisibility()==View.GONE)
              {
                  infonav.setVisibility(View.VISIBLE);
                  sortnav.setVisibility(View.GONE);
              }

            }
        });

        donebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name_s = name.getText().toString();
                String brand_s = brand.getText().toString();
                String year_s = year.getText().toString();
                String month_s = month.getText().toString();
                String day_s = day.getText().toString();
                List<String> date_ = List.of(year_s,month_s,day_s);
                ClearTextValue();
                AddProduct(name_s, brand_s, date_);
                infonav.setVisibility(View.GONE);
            }
        });

        cancelbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remove_add();


            }
        });

        sortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sortclick();

            }
        });



        // Fetch inventory data from Firebase
        fetchInventoryData();

        // ✅ Request Camera Permission
        checkCameraPermission();
    }

    private void AddProduct(String name, String brand, List<String> expirationdate)
    {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(brand) || expirationdate.isEmpty()) {
            Toast.makeText(this, "Product details cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        //in theory look for barcode here
        String expiration_date = expirationdate.get(2)+"/"+expirationdate.get(1)+"/"+expirationdate.get(0);
        String uni_name_brand = name +brand+expiration_date; //ensuring that each product have different id
        String barcode = UUID.nameUUIDFromBytes(uni_name_brand.getBytes()).toString();
        Product product = new Product(barcode,name, brand);
        product.setExpiryDate(expiration_date);
        databaseReference.child(barcode).setValue(product)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product added to inventory!", Toast.LENGTH_SHORT).show();
                    Log.d("Firebase", "Product saved: " + barcode + " - " + name);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
    }

    private void ClearTextValue()
    {
        String empty= "";
        this.name.setText(empty);
        this.brand.setText(empty);
        this.year.setText(empty);
        this.month.setText(empty);
        this.day.setText(empty);
    }
    private void remove_add()
    {
        ClearTextValue();
        infonav.setVisibility(View.GONE);
    }

    private void Sortclick()
    {
        if(infonav.getVisibility()!=View.GONE)
        {
            remove_add();
        }
        if(sortnav.getVisibility()==View.GONE)
        {
            sortnav.setVisibility(View.VISIBLE);
        }
    }

    private void remove_sort()
    {
        sortnav.setVisibility(View.GONE);
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
            remove_add();
            remove_sort();
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
                    Log.d("Firebase", "Product saved: " + barcode + " - " + name);
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
