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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FridgeHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FridgeHistoryAdapter adapter;
    private final List<FridgeHistoryItem> mockHistory = new ArrayList<>();
    private DatabaseReference databaseReference;
    private String dateselected;
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
            Intent intent = new Intent(FridgeHistoryActivity.this, FridgeGraphActivity.class);
            startActivity(intent);
        });

        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show();
            generateMockData(); // or pull from Firebase later
            adapter.notifyDataSetChanged();
        });
        LocalDate currentDate = LocalDate.now();

// Format it to "yyyy-MM-dd" format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = currentDate.format(formatter);
        dateselected =formattedDate;
        // ✅ Load mock data
        generateMockData();
        adapter = new FridgeHistoryAdapter(mockHistory);
        recyclerView.setAdapter(adapter);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("fridge_condition");
    }
    private List<FridgeHistoryItem> filterByTime(List<FridgeHistoryItem> historyList, TIME timeFilter) {
        List<FridgeHistoryItem> filtered = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Add the first item by default
        FridgeHistoryItem lastAdded = historyList.get(0);
        filtered.add(lastAdded);
        LocalDateTime lastTime = LocalDateTime.parse(lastAdded.dateTime, formatter);

        // Variables to accumulate the values
        double accumulatedTemperature = lastAdded.temperature;
        double accumulatedHumidity = lastAdded.humidity;
        int accumulatedCo = lastAdded.co;
        int accumulatedLpg = lastAdded.lpg;
        int accumulatedSmoke = lastAdded.smoke;
        int sampleCount = 1;  // Count of samples for averaging

        for (int i = 1; i < historyList.size(); i++) {
            FridgeHistoryItem currentItem = historyList.get(i);
            LocalDateTime currentTime = LocalDateTime.parse(currentItem.dateTime, formatter);
            Duration duration = Duration.between(lastTime, currentTime);
            long seconds = duration.getSeconds();

            boolean shouldAdd = false;

            switch (timeFilter) {
                case PER_DAY:
                    shouldAdd = seconds >= 86400; // 24 hours
                    break;
                case PER_HOUR:
                    shouldAdd = seconds >= 3600;  // 1 hour
                    break;
                case PER_MIN:
                    shouldAdd = seconds >= 60;    // 1 minute
                    break;
                default:
                    shouldAdd = true;
                    break;
            }

            if (shouldAdd) {
                // Calculate the average of all accumulated values
                double avgTemperature = accumulatedTemperature / sampleCount;
                double avgHumidity = accumulatedHumidity / sampleCount;
                int avgCo = accumulatedCo / sampleCount;
                int avgLpg = accumulatedLpg / sampleCount;
                int avgSmoke = accumulatedSmoke / sampleCount;

                // Create a new FridgeHistoryItem with averaged values
                FridgeHistoryItem averagedItem = new FridgeHistoryItem(
                        currentItem.dateTime,
                        avgTemperature,
                        avgHumidity,
                        avgCo,
                        avgLpg,
                        avgSmoke
                );
                filtered.add(averagedItem);

                // Reset accumulation for the next time period
                accumulatedTemperature = currentItem.temperature;
                accumulatedHumidity = currentItem.humidity;
                accumulatedCo = currentItem.co;
                accumulatedLpg = currentItem.lpg;
                accumulatedSmoke = currentItem.smoke;
                sampleCount = 1;
                lastTime = currentTime;
            } else {
                // Accumulate values for the next sample
                accumulatedTemperature += currentItem.temperature;
                accumulatedHumidity += currentItem.humidity;
                accumulatedCo += currentItem.co;
                accumulatedLpg += currentItem.lpg;
                accumulatedSmoke += currentItem.smoke;
                sampleCount++;
            }
        }

        // Handle the last batch of samples
        if (sampleCount > 0) {
            double avgTemperature = accumulatedTemperature / sampleCount;
            double avgHumidity = accumulatedHumidity / sampleCount;
            int avgCo = accumulatedCo / sampleCount;
            int avgLpg = accumulatedLpg / sampleCount;
            int avgSmoke = accumulatedSmoke / sampleCount;

            FridgeHistoryItem lastAveragedItem = new FridgeHistoryItem(
                    lastAdded.dateTime,
                    avgTemperature,
                    avgHumidity,
                    avgCo,
                    avgLpg,
                    avgSmoke
            );
            filtered.add(lastAveragedItem);
        }

        if (timeFilter == TIME.ALL_TIME || historyList.isEmpty()) {
            return historyList;
        }
        return filtered;
    }

    private void generateMockData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("fridge_condition");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mockHistory.clear(); // Prevent duplication

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String dateTime = itemSnapshot.child("time").getValue(String.class);
                    Double temp = itemSnapshot.child("temperature").getValue(Double.class);
                    Double hum = itemSnapshot.child("humidity").getValue(Double.class);
                    Integer co = itemSnapshot.child("co").getValue(Integer.class);
                    Integer lpg = itemSnapshot.child("lpg").getValue(Integer.class);
                    Integer smoke = itemSnapshot.child("smoke").getValue(Integer.class);
                    Integer overallCond = itemSnapshot.child("overall condition").getValue(Integer.class);
// Extract the "yyyy:mm:dd" part of both dates
                    String firebaseDate = dateTime.substring(0, 10); // "yyyy:mm:dd"
                    String currentDate = dateselected;  // "yyyy:mm:dd"

// Compare the dates
                    if (firebaseDate.equals(currentDate)) {
                        FridgeHistoryItem item = new FridgeHistoryItem(dateTime, temp, hum, co, lpg, smoke);
                        mockHistory.add(item);

                        // 🖨️ Debug print each value
                        System.out.println("Item: " + item.dateTime + " | Temp: " + temp + " | Hum: " + hum +
                                " | CO: " + co + " | LPG: " + lpg + " | NH4: " + smoke);
                    }

                }

                // Now that mockHistory is ready, notify adapter
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FridgeHistoryActivity.this, "Failed to fetch data", Toast.LENGTH_SHORT).show();
            }
        });
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
                    generateMockData();
                    adapter.notifyDataSetChanged();
                    dateselected = selectedDate;
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
