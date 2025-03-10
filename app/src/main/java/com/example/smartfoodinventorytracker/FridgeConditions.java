package com.example.smartfoodinventorytracker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FridgeConditions extends AppCompatActivity {

    private TextView tempText, humidityText, coText, lpgText, smokeText;
    private DatabaseReference databaseRef;
    private Button updateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fridge_conditions);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setUpUi();
        fetchDataFromFirebase();
    }

    private void setUpUi(){
        setUpToolbar();
        tempText = findViewById(R.id.tempText);
        humidityText = findViewById(R.id.humidityText);
        coText = findViewById(R.id.coText);
        lpgText = findViewById(R.id.lpgText);
        smokeText = findViewById(R.id.smokeText);
        updateButton = findViewById(R.id.updateButton);

        // ✅ Refresh data when button is clicked
        updateButton.setOnClickListener(v -> fetchDataFromFirebase());
    }

    private void setUpToolbar(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void fetchDataFromFirebase() {
        databaseRef = FirebaseDatabase.getInstance().getReference().child("inventory");

        databaseRef.orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Double temperature = snapshot.child("temperature").getValue(Double.class);
                        Double humidity = snapshot.child("humidity").getValue(Double.class);
                        Integer co = snapshot.child("co").getValue(Integer.class);
                        Integer lpg = snapshot.child("lpg").getValue(Integer.class);
                        Integer smoke = snapshot.child("smoke").getValue(Integer.class);

                        tempText.setText(temperature != null ? "Temperature: " + temperature + "°C" : "-- °C");
                        humidityText.setText(humidity != null ? "Humidity: " + humidity + "%" : "-- %");
                        coText.setText(co != null ? "CO: " + co + " ppm" : "-- ppm");
                        lpgText.setText(lpg != null ? "LPG: " + lpg + " ppm" : "-- ppm");
                        smokeText.setText(smoke != null ? "NH4: " + smoke + " ppm" : "-- ppm");
                    }
                } else {
                    Toast.makeText(FridgeConditions.this, "No data found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(FridgeConditions.this, "Failed to load data!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
