package com.example.smartfoodinventorytracker.fridge_conditions;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.smartfoodinventorytracker.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FridgeGraph2Activity extends AppCompatActivity {
    private LineChart lineChart;
    private Spinner metricSpinner;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fridge_graph2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        metricSpinner = findViewById(R.id.metricSpinner2);
        lineChart = findViewById(R.id.lineChart_2);

        metricSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedMetric = parent.getItemAtPosition(pos).toString();
                loadMockGraph(selectedMetric);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        setUpToolbar();
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
    private void loadMockGraph(String metric) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("inventory");
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



        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            entries.add(new Entry(i, (float) (Math.random() * 100)));
        }

        LineDataSet dataSet = new LineDataSet(entries, metric);
        dataSet.setColor(getResources().getColor(R.color.green));
        dataSet.setValueTextSize(12f);
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate(); // refresh

        // Customize X-Axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
    }
}