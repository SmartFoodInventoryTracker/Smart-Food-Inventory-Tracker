package com.example.smartfoodinventorytracker.fridge_conditions;

import android.os.Bundle;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartfoodinventorytracker.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import java.util.*;

public class FridgeGraphActivity extends AppCompatActivity {

    private LineChart lineChart;
    private Spinner metricSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fridge_graph);

        metricSpinner = findViewById(R.id.metricSpinner);
        lineChart = findViewById(R.id.lineChart);

        metricSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedMetric = parent.getItemAtPosition(pos).toString();
                loadMockGraph(selectedMetric);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadMockGraph(String metric) {
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
