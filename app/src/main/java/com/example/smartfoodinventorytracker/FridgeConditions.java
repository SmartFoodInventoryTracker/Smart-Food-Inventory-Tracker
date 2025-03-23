package com.example.smartfoodinventorytracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.anastr.speedviewlib.AwesomeSpeedometer;
import com.github.anastr.speedviewlib.SpeedView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FridgeConditions extends AppCompatActivity {

    private TextView tempText, humidityText, coText, lpgText, smokeText;
    private SpeedView speedTemp, speedHum, speedLPG, speedCO,speedSmoke;
    private DatabaseReference databaseRef;

    private final int pointerIndex = 3; // 0-based index, so 3 is the 4th block
    private final int totalBlocks = 15;

    private final int[] gradientColors = {
            0xFF00AA00,  // deep green
            0xFF00D400,  // green-darker
            0xFF00FF00,  // green
            0xFF50FF00,  // bright lime
            0xFF7CFF00,  // lime green
            0xFFA8FF00,  // soft lime
            0xFFD4FF00,  // lime tint
            0xFFEFFF00,  // yellow-lime
            0xFFFFFF00,  // yellow
            0xFFFFE000,  // yellow tint
            0xFFFFC000,  // golden yellow
            0xFFFFA000,  // orange-yellow
            0xFFFF8000,  // orange
            0xFFFF4000,  // red-orange
            0xFFFF0000   // red
    };

    private List<TextView> arrowViews = new ArrayList<>();


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

        speedSmoke=findViewById(R.id.speedViewSmoke);
        speedCO=findViewById(R.id.speedViewCO);
        speedTemp=findViewById(R.id.speedViewTemp);
        speedHum=findViewById(R.id.speedViewHum);
        speedLPG=findViewById(R.id.speedViewLPG);
        setUpOverallBar();

    }

    private void setUpOverallBar()
    {
        LinearLayout colorBar = findViewById(R.id.colorBar);
        LinearLayout arrowRow = findViewById(R.id.arrowRow);

        for (int i = 0; i < totalBlocks; i++) {
            // Color block
            View block = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(60, 60);
            params.setMargins(4, 0, 4, 0);
            block.setLayoutParams(params);
            block.setBackgroundColor(gradientColors[i]);
            colorBar.addView(block);

            // Arrow placeholder
            TextView arrow = new TextView(this);
            arrow.setLayoutParams(params);
            arrow.setText(i == pointerIndex ? "▲" : "");
            arrow.setGravity(Gravity.CENTER);
            arrow.setTextColor(Color.BLACK);
            arrow.setTextSize(20);
            arrowViews.add(arrow);
            arrowRow.addView(arrow);
        }

        setArrowPosition(14);
    }
    private void setArrowPosition(int index) {
        for (int i = 0; i < arrowViews.size(); i++) {
            arrowViews.get(i).setText(i == index ? "▲" : "");
        }
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
                        Integer smoke_condition = snapshot.child("smoke condition").getValue(Integer.class);
                        Integer co_condition = snapshot.child("co condition").getValue(Integer.class);
                        Integer lpg_condition = snapshot.child("lpg condition").getValue(Integer.class);
                        Integer temp_condition = snapshot.child("temperature condition").getValue(Integer.class);
                        Integer humidity_condition = snapshot.child("humidity condition").getValue(Integer.class);
                        Integer overall_condition = snapshot.child("overall condition").getValue(Integer.class);


                        tempText.setText(temperature != null ? "Temperature: " + temperature + "°C" : "-- °C");
                        humidityText.setText(humidity != null ? "Humidity: " + humidity + "%" : "-- %");
                        coText.setText(co != null ? "CO: " + co + " ppm" : "-- ppm");
                        lpgText.setText(lpg != null ? "LPG: " + lpg + " ppm" : "-- ppm");
                        smokeText.setText(smoke != null ? "NH4: " + smoke + " ppm" : "-- ppm");


                        //Temp Gauge
                        setGauge(temp_condition,"t");
                        //Humidity Gauge
                        setGauge(humidity_condition,"h");
                        //CO gauge
                        setGauge(co_condition,"c");
                        //LPG gauge
                        setGauge(lpg_condition,"l");
                        //Smoke gauge
                        setGauge(smoke_condition,"s");
                        // Overall Gauge
                        setGauge(overall_condition, "ov");

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


    void setGauge(Integer val, String hint)
    {
        switch (hint)
        {
            case "t":
                if(val>=0 && val<3)
                {
                    speedTemp.speedTo(30);
                }
                else if(val>=3 && val<9)
                {
                    speedTemp.speedTo(70);
                }
                else if(val>=9)
                {
                    speedTemp.speedTo(90);
                }
                break;


            case "h":
                if(val>=0 && val<3)
                {
                    speedHum.speedTo(30);
                }
                else if(val>=3 && val<9)
                {
                    speedHum.speedTo(70);
                }
                else if(val>=9)
                {
                    speedHum.speedTo(90);
                }
                break;

            case "c":
                if(val>=0 && val<3)
                {
                    speedCO.speedTo(30);
                }
                else if(val>=3 && val<9)
                {
                    speedCO.speedTo(70);
                }
                else if(val>=9)
                {
                    speedCO.speedTo(90);
                }
                break;

            case "l":
                if(val>=0 && val<3)
                {
                    speedLPG.speedTo(30);
                }
                else if(val>=3 && val<9)
                {
                    speedLPG.speedTo(70);
                }
                else if(val>=9)
                {
                    speedLPG.speedTo(90);
                }
                break;

            case "s":
                if(val>=0 && val<3)
                {
                    speedSmoke.speedTo(30);
                }
                else if(val>=3 && val<9)
                {
                    speedSmoke.speedTo(70);
                }
                else if(val>=9)
                {
                    speedSmoke.speedTo(90);
                }
                break;


            case "ov":
                int grade = val*14/10;
                setArrowPosition(grade);
                break;

        }

    }

}