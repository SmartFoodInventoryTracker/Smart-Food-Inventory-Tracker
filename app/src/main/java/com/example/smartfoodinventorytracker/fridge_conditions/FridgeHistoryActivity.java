package com.example.smartfoodinventorytracker.fridge_conditions;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;

import android.app.DatePickerDialog;
import android.widget.Button;
import android.widget.DatePicker;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


import java.util.ArrayList;
import java.util.List;

public class FridgeHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FridgeHistoryAdapter adapter;
    private final List<FridgeHistoryItem> mockHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fridge_history);

        recyclerView = findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        generateMockData();
        adapter = new FridgeHistoryAdapter(mockHistory);
        recyclerView.setAdapter(adapter);
    }

    private void generateMockData() {
        mockHistory.add(new FridgeHistoryItem("2025-04-02 14:30", 5.6, 60, 15, 22, 18));
        mockHistory.add(new FridgeHistoryItem("2025-04-01 13:10", 6.2, 58, 10, 18, 12));
        mockHistory.add(new FridgeHistoryItem("2025-03-31 16:45", 4.9, 62, 13, 20, 14));
    }
}
