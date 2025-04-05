package com.example.smartfoodinventorytracker.fridge_conditions;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FridgeHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FridgeHistoryAdapter adapter;
    private final List<FridgeHistoryItem> mockHistory = new ArrayList<>();
    private DatabaseReference databaseReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fridge_history);

        // ✅ Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.historyToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Fridge History");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // ✅ RecyclerView setup
        recyclerView = findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // ✅ Buttons
        Button btnDateRange = findViewById(R.id.btnDateRange);
        Button btnGraph = findViewById(R.id.btnGraph);
        Button btnRefresh = findViewById(R.id.btnRefresh);

        btnDateRange.setOnClickListener(v -> openDatePicker());

        btnGraph.setOnClickListener(v -> {
            Intent intent = new Intent(FridgeHistoryActivity.this, FridgeGraph2Activity.class);
            startActivity(intent);
        });

        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show();
            generateMockData(); // or pull from Firebase later
            adapter.notifyDataSetChanged();
        });

        // ✅ Load mock data
        generateMockData();
        adapter = new FridgeHistoryAdapter(mockHistory);
        recyclerView.setAdapter(adapter);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("inventory");
    }

    private void generateMockData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("inventory");

        List<FridgeHistoryItem> historyList = new ArrayList<>();

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String dateTime = itemSnapshot.child("datetime").getValue(String.class);
                    Double temperature = itemSnapshot.child("temperature condition").getValue(Double.class);
                    Double humidity = itemSnapshot.child("humidity condition").getValue(Double.class);
                    Integer co = itemSnapshot.child("co condition").getValue(Integer.class);
                    Integer lpg = itemSnapshot.child("lpg condition").getValue(Integer.class);
                    Integer smoke = itemSnapshot.child("smoke condition").getValue(Integer.class);

                    if (dateTime != null && temperature != null && humidity != null && co != null && lpg != null && smoke != null) {
                        FridgeHistoryItem item = new FridgeHistoryItem(dateTime, temperature, humidity, co, lpg, smoke);
                        historyList.add(item);
                    }
                }

                // Use the historyList as needed, e.g., update UI or pass to an adapter
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors.
                //Log.e("FirebaseError", "Error fetching data", error.toException());
            }
        });
        mockHistory.clear(); // Prevent duplication
        for (FridgeHistoryItem item: historyList
             ) {
            mockHistory.add(item);

        }


    }

    private void openDatePicker() {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                FridgeHistoryActivity.this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String selectedDate = sdf.format(calendar.getTime());
                    Toast.makeText(this, "Selected: " + selectedDate, Toast.LENGTH_SHORT).show();

                    // 🔁 Filtering placeholder logic
                    // You could filter the list here using selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
}
