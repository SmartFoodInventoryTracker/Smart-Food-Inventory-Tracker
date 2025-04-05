package com.example.smartfoodinventorytracker.fridge_conditions;

import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.core.app.NavUtils;

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
    private DatabaseReference databaseReference;
    private List<TextView> arrowViews = new ArrayList<>();

    private final int[] gradientColors = {
            0xFF00AA00, 0xFF00D400, 0xFF00FF00, 0xFF50FF00, 0xFF7CFF00,
            0xFFA8FF00, 0xFFD4FF00, 0xFFEFFF00, 0xFFFFFF00, 0xFFFFE000,
            0xFFFFC000, 0xFFFFA000, 0xFFFF8000, 0xFFFF4000, 0xFFFF0000
    };

    private final int totalBlocks = 15;

    private static final Map<String, Number> lastStoredValues = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fridge_conditions);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("inventory");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setUpToolbar();
        initViews();
        setUpOverallBar();
        fetchDataFromFirebase();
    }

    private void setUpToolbar(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> NavUtils.navigateUpFromSameTask(this));
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

        ImageView coInfoIcon = cardCO.findViewById(R.id.infoIcon);
        LinearLayout coOverlay = cardCO.findViewById(R.id.infoOverlay);
        ImageView coClose = cardCO.findViewById(R.id.closeInfo);
        TextView coInfoText = cardCO.findViewById(R.id.infoText);
        TextView coInfoTitle = cardCO.findViewById(R.id.infoTitle);
        View coMainLayout = cardCO.findViewById(R.id.gasContentLayout);

        coInfoTitle.setText("CO (Carbon Monoxide)");
        coInfoText.setText("A colorless, odorless gas. High levels may indicate spoilage or poor airflow inside the fridge.");

        coInfoIcon.setOnClickListener(v -> {
            coOverlay.setVisibility(View.VISIBLE);
            coOverlay.setAlpha(0f);
            coOverlay.setTranslationY(20f);
            coOverlay.animate().alpha(1f).translationY(0f).setDuration(200).start();
            coMainLayout.setAlpha(0.3f);
            speedCO.setAlpha(0.2f);
        });

        coClose.setOnClickListener(v -> {
            coOverlay.animate().alpha(0f).translationY(20f).setDuration(150).withEndAction(() -> {
                coOverlay.setVisibility(View.GONE);
                coOverlay.setAlpha(1f);
            }).start();
            coMainLayout.setAlpha(1f);
            speedCO.setAlpha(1f);
        });


        // 🔥 LPG Card
        cardLPG = findViewById(R.id.card_lpg);
        lpgValue = cardLPG.findViewById(R.id.gasValue);
        speedLPG = cardLPG.findViewById(R.id.gasGauge);
        ImageView lpgIcon = cardLPG.findViewById(R.id.gasIcon);
        TextView lpgLabel = cardLPG.findViewById(R.id.gasLabel);
        lpgIcon.setImageResource(R.drawable.ic_lpg);
        lpgLabel.setText("LPG :");

        ImageView lpgInfoIcon = cardLPG.findViewById(R.id.infoIcon);
        LinearLayout lpgOverlay = cardLPG.findViewById(R.id.infoOverlay);
        ImageView lpgClose = cardLPG.findViewById(R.id.closeInfo);
        TextView lpgInfoText = cardLPG.findViewById(R.id.infoText);
        TextView lpgInfoTitle = cardLPG.findViewById(R.id.infoTitle);
        View lpgMainLayout = cardLPG.findViewById(R.id.gasContentLayout);

        lpgInfoTitle.setText("LPG (Liquefied Petroleum Gas)");
        lpgInfoText.setText("Not normally found in fridges. Its presence could suggest a gas leak or chemical contamination.");

        lpgInfoIcon.setOnClickListener(v -> {
            lpgOverlay.setVisibility(View.VISIBLE);
            lpgOverlay.setAlpha(0f);
            lpgOverlay.setTranslationY(20f);
            lpgOverlay.animate().alpha(1f).translationY(0f).setDuration(200).start();
            lpgMainLayout.setAlpha(0.3f);
            speedLPG.setAlpha(0.2f);
        });

        lpgClose.setOnClickListener(v -> {
            lpgOverlay.animate().alpha(0f).translationY(20f).setDuration(150).withEndAction(() -> {
                lpgOverlay.setVisibility(View.GONE);
                lpgOverlay.setAlpha(1f);
            }).start();
            lpgMainLayout.setAlpha(1f);
            speedLPG.setAlpha(1f);
        });


        // 🧪 NH₄ Card
        cardNH4 = findViewById(R.id.card_nh4);
        nh4Value = cardNH4.findViewById(R.id.gasValue);
        speedNH4 = cardNH4.findViewById(R.id.gasGauge);
        ImageView nh4Icon = cardNH4.findViewById(R.id.gasIcon);
        TextView nh4Label = cardNH4.findViewById(R.id.gasLabel);
        nh4Icon.setImageResource(R.drawable.ic_smoke);
        nh4Label.setText("NH₄ :");

        ImageView nh4InfoIcon = cardNH4.findViewById(R.id.infoIcon);
        LinearLayout nh4Overlay = cardNH4.findViewById(R.id.infoOverlay);
        ImageView nh4Close = cardNH4.findViewById(R.id.closeInfo);
        TextView nh4InfoText = cardNH4.findViewById(R.id.infoText);
        TextView nh4InfoTitle = cardNH4.findViewById(R.id.infoTitle);
        View nh4MainLayout = cardNH4.findViewById(R.id.gasContentLayout);

        nh4InfoTitle.setText("NH₄ (Ammonia)");
        nh4InfoText.setText("Commonly produced by spoiling food. High levels can suggest meat, dairy, or produce spoilage.");

        nh4InfoIcon.setOnClickListener(v -> {
            nh4Overlay.setVisibility(View.VISIBLE);
            nh4Overlay.setAlpha(0f);
            nh4Overlay.setTranslationY(20f);
            nh4Overlay.animate().alpha(1f).translationY(0f).setDuration(200).start();
            nh4MainLayout.setAlpha(0.3f);
            speedNH4.setAlpha(0.2f);
        });

        nh4Close.setOnClickListener(v -> {
            nh4Overlay.animate().alpha(0f).translationY(20f).setDuration(150).withEndAction(() -> {
                nh4Overlay.setVisibility(View.GONE);
                nh4Overlay.setAlpha(1f);
            }).start();
            nh4MainLayout.setAlpha(1f);
            speedNH4.setAlpha(1f);
        });

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean tooltipShown = false;

        LinearLayout infoHelperTooltip = cardCO.findViewById(R.id.infoHelperTooltip);

        if (!tooltipShown) {
            infoHelperTooltip.setVisibility(View.VISIBLE);
            infoHelperTooltip.setAlpha(0f);
            infoHelperTooltip.setTranslationX(100f); // Start farther right for more visible slide

            infoHelperTooltip.animate()
                    .translationX(0f)               // Slide into position
                    .alpha(1f)                      // Fade in
                    .setDuration(600)               // Slower slide in
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()) // Smooth approach
                    .withEndAction(() -> infoHelperTooltip.postDelayed(() -> {
                        infoHelperTooltip.animate()
                                .translationX(100f) // Slide back out to the right
                                .alpha(0f)
                                .setDuration(600)   // Slower fade & slide out
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .withEndAction(() -> infoHelperTooltip.setVisibility(View.GONE))
                                .start();
                    }, 3500))
                    .start();

            prefs.edit().putBoolean("tooltip_shown", true).apply();
        }


        // 📜 History Button
        ImageView historyButton = findViewById(R.id.historyLogo);
        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(FridgeConditionsActivity.this, FridgeHistoryActivity.class);
            startActivity(intent);
        });

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

    private void saveDataCondition()
    {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("inventory");

    }
    private void fetchDataFromFirebase() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("inventory");

        databaseReference.orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
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

//