package com.example.smartfoodinventorytracker.fridge_conditions;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.smartfoodinventorytracker.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


public class FridgeGraphActivity extends AppCompatActivity {
    private LineChart lineChart;
    private Spinner metricSpinner;

    private Spinner timespinner;

    private  TIME time_filter = TIME.ALL_TIME;
    private String metric_graph_data="Temperature";
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
        timespinner = findViewById(R.id.time_spinner);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("fridge_condition");
        List<FridgeHistoryItem> historyList = new ArrayList<>();
        metricSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                metric_graph_data = parent.getItemAtPosition(pos).toString();
                loadMockGraph(metric_graph_data);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}

        });

        timespinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedMetric = parent.getItemAtPosition(pos).toString();
                settime_scaled(selectedMetric);
                loadMockGraph(metric_graph_data);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        setUpToolbar();

        FloatingActionButton exportButton = findViewById(R.id.btnExport);
        exportButton.setOnClickListener(v -> {
            try {
                String fileName = "fridge_graph_" + System.currentTimeMillis() + ".png";
                File imageFile = new File(getExternalFilesDir(null), fileName);

                // Save chart to file
                lineChart.saveToPath(fileName, imageFile.getParent());

                Uri uri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        imageFile
                );

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Share Fridge Graph"));

            } catch (Exception e) {
                Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void setUpToolbar(){
        Toolbar toolbar = findViewById(R.id.graphToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(this));
    }

    private void settime_scaled(String metric)
    {
        if(Objects.equals(metric, "Per Hour"))
        {
            this.time_filter = TIME.PER_HOUR;
        }else if(Objects.equals(metric, "Per Day"))
        {
            this.time_filter = TIME.PER_DAY;
        }
        else if(Objects.equals(metric, "Per Minute"))
        {
            this.time_filter = TIME.PER_MIN;
        }
        else {
            this.time_filter = TIME.ALL_TIME;
        }
        Toast.makeText(FridgeGraphActivity.this,
                "Time selected: "+time_filter+"| metric",
                Toast.LENGTH_SHORT).show();
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


                List<FridgeHistoryItem> filteredValue = filterByTime(historyList,time_filter);
                // Now that historyList is populated, extract the metric and plot the graph
                List<Number> metricValues = FridgeHistoryItem.extractMetricList(filteredValue, metric);
                List<String> stringdate = FridgeHistoryItem.extractMetricList(filteredValue);
                // Call the plotting method based on the metric
                if (metric.equals("Temperature")||metric.equals("Humidity")) {
                    plotGraph((List<Double>) (List<?>) metricValues,filteredValue,  metric); // Cast to List<Double> for temperature
                } else {
                    plotGraphInt((List<Integer>) (List<?>) metricValues,filteredValue, metric); // Cast to List<Integer> for other metrics
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
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

    private void styleChartAppearance(LineDataSet dataSet, String label) {
        int color;
        switch (label) {
            case "Temperature":
                color = getResources().getColor(R.color.teal_700); break;
            case "Humidity":
                color = getResources().getColor(R.color.purple_500); break;
            case "CO₂":
                color = getResources().getColor(R.color.co_color); break;
            case "LPG":
                color = getResources().getColor(R.color.lpg_color); break;
            case "NH₃":
                color = getResources().getColor(R.color.nh4_color); break;
            default:
                color = getResources().getColor(R.color.graph_line); break;
        }


        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setValueTextColor(getResources().getColor(R.color.text_secondary));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(5f);
        dataSet.setCircleHoleRadius(2.5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setDrawValues(false);
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setHighLightColor(color);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
    }


    private void configureChartBasics(LineChart chart, String label) {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.animateX(700);
        chart.getLegend().setEnabled(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.text_secondary));
        xAxis.setDrawGridLines(false);

        chart.getAxisLeft().setTextColor(getResources().getColor(R.color.text_secondary));
        chart.getAxisRight().setEnabled(false);
    }



    private void plotGraph(List<Double> values, List<FridgeHistoryItem> items, String label) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            entries.add(new Entry(i, values.get(i).floatValue()));
        }

        lineChart.clear();
        LineDataSet dataSet = new LineDataSet(entries, label);
        styleChartAppearance(dataSet, label);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        configureChartBasics(lineChart, label);

        // ✅ Attach custom marker tooltip (only once, and with chartView set!)
        CustomMarkerView markerView = new CustomMarkerView(this, R.layout.layout_custom_marker, items);
        markerView.setChartView(lineChart);
        lineChart.setMarker(markerView);

        lineChart.invalidate();
    }


    private void plotGraphInt(List<Integer> values, List<FridgeHistoryItem> items, String label) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            entries.add(new Entry(i, values.get(i)));
        }

        lineChart.clear();
        LineDataSet dataSet = new LineDataSet(entries, label);
        styleChartAppearance(dataSet, label);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        configureChartBasics(lineChart, label);

        // ✅ Attach custom marker tooltip (no duplicate!)
        CustomMarkerView markerView = new CustomMarkerView(this, R.layout.layout_custom_marker, items);
        markerView.setChartView(lineChart);
        lineChart.setMarker(markerView);

        lineChart.invalidate();
    }



    // Format the date based on the time filter
    private String formatDate(LocalDateTime dateTime) {
        switch (time_filter) {
            case PER_DAY:
                return dateTime.toLocalDate().toString(); // Format as "yyyy-MM-dd"
            case PER_HOUR:
                return dateTime.toLocalDate().toString() + " " + dateTime.getHour() + ":00"; // Format as "yyyy-MM-dd HH"
            case PER_MIN:
                return dateTime.toLocalDate().toString() + " " + dateTime.getHour() + ":" + String.format("%02d", dateTime.getMinute()); // Format as "yyyy-MM-dd HH:mm"
            default:
                return dateTime.toString(); // Default format: "yyyy-MM-dd HH:mm:ss"
        }
    }
    public class LineChartXAxisValueFormatter extends IndexAxisValueFormatter {

        @Override
        public String getFormattedValue(float value) {
            // Convert the float value (time in seconds) to milliseconds
            int val = (int) value;
            String dateStr = Integer.toString(val);
            // Ensure the string has the correct length
            if (dateStr.length() != 14) {
                throw new IllegalArgumentException("Input must be a 14-digit integer in yyyymmddhhmmss format.");
            }

            // Split the string into date and time parts
            String year = dateStr.substring(0, 4);
            String month = dateStr.substring(4, 6);
            String day = dateStr.substring(6, 8);
            String hour = dateStr.substring(8, 10);
            String minute = dateStr.substring(10, 12);
            String second = dateStr.substring(12, 14);

            // Return the formatted string in "yyyy:mm:dd HH:mm:ss"
            return String.format("%s:%s:%s %s:%s:%s", year, month, day, hour, minute, second);


        }
    }
}