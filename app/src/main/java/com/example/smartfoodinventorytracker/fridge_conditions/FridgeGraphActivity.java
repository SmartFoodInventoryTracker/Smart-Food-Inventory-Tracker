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


public class FridgeGraphActivity extends AppCompatActivity {
    private LineChart lineChart;
    private Spinner metricSpinner;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fridge_graph);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        metricSpinner = findViewById(R.id.metricSpinner);
        lineChart = findViewById(R.id.lineChart);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("fridge_condition");
        List<FridgeHistoryItem> historyList = new ArrayList<>();
        metricSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedMetric = parent.getItemAtPosition(pos).toString();
                loadMockGraph(selectedMetric);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        //setUpToolbar();
    }




    private void loadMockGraph(String metric) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("fridge_condition");
        List<FridgeHistoryItem> historyList = new ArrayList<>();

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Populate historyList from the Firebase data
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String dateTime = itemSnapshot.child("time").getValue(String.class);
                    Double temp = itemSnapshot.child("temperature").getValue(Double.class);
                    Double hum = itemSnapshot.child("humidity").getValue(Double.class);
                    Integer co = itemSnapshot.child("co").getValue(Integer.class);
                    Integer lpg = itemSnapshot.child("lpg").getValue(Integer.class);
                    Integer smoke = itemSnapshot.child("smoke").getValue(Integer.class);
                    FridgeHistoryItem item = new FridgeHistoryItem(dateTime, temp, hum, co, lpg, smoke);
                    historyList.add(item);
                }

                // Now that historyList is populated, extract the metric and plot the graph
                List<Number> metricValues = FridgeHistoryItem.extractMetricList(historyList, metric);

                // Call the plotting method based on the metric
                if (metric.equals("Temperature")||metric.equals("Humidity")) {
                    plotGraph((List<Double>) (List<?>) metricValues, metric); // Cast to List<Double> for temperature
                } else {
                    plotGraphInt((List<Integer>) (List<?>) metricValues, metric); // Cast to List<Integer> for other metrics
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void plotGraph(List<Double> values, String label) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            entries.add(new Entry(i, values.get(i).floatValue())); // Convert Double to float for chart
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(getResources().getColor(R.color.green));
        dataSet.setValueTextSize(12f);
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
    }

    private void plotGraphInt(List<Integer> values, String label) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            entries.add(new Entry(i, values.get(i))); // Use Integer directly
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(getResources().getColor(R.color.green));
        dataSet.setValueTextSize(12f);
        dataSet.setLineWidth(2f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
    }
}