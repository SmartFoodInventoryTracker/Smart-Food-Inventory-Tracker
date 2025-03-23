package com.example.smartfoodinventorytracker;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.util.Log;


public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchFridge, switchExpiry;
    private EditText inputExpiredHours, inputWeek1Days, inputWeek2Days;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "user_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        switchFridge = findViewById(R.id.switch_fridge);
        switchExpiry = findViewById(R.id.switch_expiry);
        inputExpiredHours = findViewById(R.id.input_expired_hours);
        inputWeek1Days = findViewById(R.id.input_week1_days);
        inputWeek2Days = findViewById(R.id.input_week2_days);

        // Load saved values
        switchFridge.setChecked(prefs.getBoolean("fridge_alerts", true));
        switchExpiry.setChecked(prefs.getBoolean("expiry_alerts", true));
        inputExpiredHours.setText(String.valueOf(prefs.getInt("expired_every_hours", 4)));
        inputWeek1Days.setText(String.valueOf(prefs.getInt("week1_every_days", 2)));
        inputWeek2Days.setText(String.valueOf(prefs.getInt("week2_every_days", 3)));

        // Save on toggle
        switchFridge.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("fridge_alerts", isChecked).apply());

        switchExpiry.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("expiry_alerts", isChecked).apply());
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("expired_every_hours", parseNumber(inputExpiredHours.getText().toString(), 4));
        editor.putInt("week1_every_days", parseNumber(inputWeek1Days.getText().toString(), 2));
        editor.putInt("week2_every_days", parseNumber(inputWeek2Days.getText().toString(), 3));

        editor.apply();
    }

    private int parseNumber(String text, int defaultVal) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

}