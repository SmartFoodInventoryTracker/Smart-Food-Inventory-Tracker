package com.example.smartfoodinventorytracker.fridge_conditions;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.notifications.NotificationHelper;
import com.github.anastr.speedviewlib.SpeedView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FridgeConditionsActivity extends AppCompatActivity {

    private TextView coText, lpgText, nh4Text;
    private TextView tempText, humidityText;
    private SpeedView speedTemp, speedHum;
    private LinearLayout colorBar, arrowRow;

    // Cards (inflate from item_gas_card)
    private View cardCO, cardLPG, cardNH4;

    // Gas values
    private TextView coValue, lpgValue, nh4Value;

    // SpeedView gauges
    private SpeedView speedCO, speedLPG, speedNH4;

    private List<TextView> arrowViews = new ArrayList<>();

    private final int[] gradientColors = {
            0xFF00AA00, 0xFF00D400, 0xFF00FF00, 0xFF50FF00, 0xFF7CFF00,
            0xFFA8FF00, 0xFFD4FF00, 0xFFEFFF00, 0xFFFFFF00, 0xFFFFE000,
            0xFFFFC000, 0xFFFFA000, 0xFFFF8000, 0xFFFF4000, 0xFFFF0000
    };

    private final int totalBlocks = 15;

    private static final Map<String, Number> lastStoredValues = new HashMap<>();
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fridge_conditions);  // your updated XML

        setUpToolbar();
        initViews();
        setUpOverallBar();
        fetchDataFromFirebase();
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initViews() {
        // 🌡️ Temp & Humidity
        tempText = findViewById(R.id.tempText);
        humidityText = findViewById(R.id.humidityText);
        speedTemp = findViewById(R.id.speedViewTemp);
        speedHum = findViewById(R.id.speedViewHum);

        // 📊 Overall bar
        colorBar = findViewById(R.id.colorBar);
        arrowRow = findViewById(R.id.arrowRow);

        // 🌫️ CO Card
        cardCO = findViewById(R.id.card_co);
        coValue = cardCO.findViewById(R.id.gasValue);
        speedCO = cardCO.findViewById(R.id.gasGauge);
        ImageView coIcon = cardCO.findViewById(R.id.gasIcon);
        TextView coLabel = cardCO.findViewById(R.id.gasLabel);
        coIcon.setImageResource(R.drawable.ic_co);
        coLabel.setText("CO :");

        // 🔥 LPG Card
        cardLPG = findViewById(R.id.card_lpg);
        lpgValue = cardLPG.findViewById(R.id.gasValue);
        speedLPG = cardLPG.findViewById(R.id.gasGauge);
        ImageView lpgIcon = cardLPG.findViewById(R.id.gasIcon);
        TextView lpgLabel = cardLPG.findViewById(R.id.gasLabel);
        lpgIcon.setImageResource(R.drawable.ic_lpg);
        lpgLabel.setText("LPG :");

        // 🧪 NH₄ Card
        cardNH4 = findViewById(R.id.card_nh4);
        nh4Value = cardNH4.findViewById(R.id.gasValue);
        speedNH4 = cardNH4.findViewById(R.id.gasGauge);
        ImageView nh4Icon = cardNH4.findViewById(R.id.gasIcon);
        TextView nh4Label = cardNH4.findViewById(R.id.gasLabel);
        nh4Icon.setImageResource(R.drawable.ic_smoke);
        nh4Label.setText("NH₄ :");
    }

    private void setUpOverallBar() {
        for (int i = 0; i < totalBlocks; i++) {
            View block = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(60, 40);
            params.setMargins(4, 0, 4, 0);
            block.setLayoutParams(params);
            block.setBackgroundColor(gradientColors[i]);
            colorBar.addView(block);

            TextView arrow = new TextView(this);
            arrow.setLayoutParams(params);
            arrow.setGravity(Gravity.CENTER);
            arrow.setTextColor(Color.BLACK);
            arrow.setTextSize(18);
            arrowViews.add(arrow);
            arrowRow.addView(arrow);
        }
    }

    private void setArrowPosition(int index) {
        for (int i = 0; i < arrowViews.size(); i++) {
            arrowViews.get(i).setText(i == index ? "▲" : "");
        }
    }

    private void fetchDataFromFirebase() {
        databaseRef = FirebaseDatabase.getInstance().getReference().child("inventory");

        databaseRef.orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(FridgeConditionsActivity.this, "No data found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot snap : snapshot.getChildren()) {
                    Double temp = snap.child("temperature").getValue(Double.class);
                    Double hum = snap.child("humidity").getValue(Double.class);
                    Integer co = snap.child("co").getValue(Integer.class);
                    Integer lpg = snap.child("lpg").getValue(Integer.class);
                    Integer smoke = snap.child("smoke").getValue(Integer.class);

                    Integer tempCond = snap.child("temperature condition").getValue(Integer.class);
                    Integer humCond = snap.child("humidity condition").getValue(Integer.class);
                    Integer coCond = snap.child("co condition").getValue(Integer.class);
                    Integer lpgCond = snap.child("lpg condition").getValue(Integer.class);
                    Integer smokeCond = snap.child("smoke condition").getValue(Integer.class);
                    Integer overallCond = snap.child("overall condition").getValue(Integer.class);

                    tempText.setText(temp != null ? temp + "°C" : "-- °C");
                    humidityText.setText(hum != null ? hum + "%" : "-- %");
                    coValue.setText(co != null ? co + " ppm" : "-- ppm");
                    lpgValue.setText(lpg != null ? lpg + " ppm" : "-- ppm");
                    nh4Value.setText(smoke != null ? smoke + " ppm" : "-- ppm");

                    setGauge(tempCond, "t");
                    setGauge(humCond, "h");
                    setGauge(coCond, "c");
                    setGauge(lpgCond, "l");
                    setGauge(smokeCond, "s");
                    setGauge(overallCond, "ov");

                    triggerNotification("Temperature", temp, tempCond);
                    triggerNotification("Humidity", hum, humCond);
                    triggerNotification("CO Level", co, coCond);
                    triggerNotification("LPG Level", lpg, lpgCond);
                    triggerNotification("Smoke Level", smoke, smokeCond);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(FridgeConditionsActivity.this, "Failed to load data!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setGauge(Integer val, String hint) {
        if (val == null) val = 0;
        float level = val == 0 ? 0f : val < 3 ? 30f : val < 9 ? 70f : 100f;

        switch (hint) {
            case "t": speedTemp.speedTo(level); break;
            case "h": speedHum.speedTo(level); break;
            case "c": speedCO.speedTo(level); break;
            case "l": speedLPG.speedTo(level); break;
            case "s": speedNH4.speedTo(level); break;
            case "ov":
                int index = Math.min(14, Math.max(0, val * 14 / 10));
                setArrowPosition(index);
                break;
        }
    }

    private void triggerNotification(String type, Number newValue, Integer condition) {
        if (newValue == null || condition == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        NotificationHelper helper = new NotificationHelper(getApplicationContext(), false, uid);

        Number prev = lastStoredValues.get(type);
        if (prev == null || !prev.equals(newValue)) {
            helper.sendConditionNotification(uid, type, newValue.longValue(), condition);
            lastStoredValues.put(type, newValue);
        }
    }
}