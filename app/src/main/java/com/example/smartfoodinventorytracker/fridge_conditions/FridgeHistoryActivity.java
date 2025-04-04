package com.example.smartfoodinventorytracker.fridge_conditions;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FridgeHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FridgeHistoryAdapter adapter;
    private final List<FridgeHistoryItem> mockHistory = new ArrayList<>();

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
    }

    private void generateMockData() {
        mockHistory.clear(); // Prevent duplication
        mockHistory.add(new FridgeHistoryItem("2025-04-02 14:30", 5.6, 60, 15, 22, 18));
        mockHistory.add(new FridgeHistoryItem("2025-04-01 13:10", 6.2, 58, 10, 18, 12));
        mockHistory.add(new FridgeHistoryItem("2025-03-31 16:45", 4.9, 62, 13, 20, 14));
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
