package com.example.smartfoodinventorytracker.settings;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import com.example.smartfoodinventorytracker.Bluetooth;
import com.example.smartfoodinventorytracker.R;

import java.io.IOException;


public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchFridge, switchExpiry;
    private Bluetooth btHelper;
    private Button save;
    private EditText ssidField, passwordField;
    private TextView inputExpiredHours, inputWeek1Days, inputWeek2Days;
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
        setUpBluetooth(this);
        setUpUi();


        /// Load saved values using a unified key "expired_every_minutes"
        switchFridge.setChecked(prefs.getBoolean("fridge_alerts", true));
        switchExpiry.setChecked(prefs.getBoolean("expiry_alerts", true));
        inputExpiredHours.setText(String.valueOf(prefs.getInt("expired_every_minutes", 4)));
        inputWeek1Days.setText(String.valueOf(prefs.getInt("week1_every_days", 2)));
        inputWeek2Days.setText(String.valueOf(prefs.getInt("week2_every_days", 3)));

        int expiredM = prefs.getInt("expired_every_minutes", 4);
        int week1D = prefs.getInt("week1_every_days", 2);
        int week2D = prefs.getInt("week2_every_days", 3);

// Change the number picker to allow 1 to 60 minutes for expiry alerts
        inputExpiredHours.setOnClickListener(v ->
                showNumberPicker("🔔 Frequency in minutes", 1, 60, expiredM, val -> {
                    inputExpiredHours.setText(String.valueOf(val));
                    prefs.edit().putInt("expired_every_minutes", val).apply();
                })
        );

        inputWeek1Days.setOnClickListener(v ->
                showNumberPicker("🔔 Frequency in Days", 1, 7, week1D, val -> {
                    inputWeek1Days.setText(String.valueOf(val));
                    prefs.edit().putInt("week1_every_days", val).apply();
                })
        );

        inputWeek2Days.setOnClickListener(v ->
                showNumberPicker("🔔 Frequency in Days", 1, 7, week2D, val -> {
                    inputWeek2Days.setText(String.valueOf(val));
                    prefs.edit().putInt("week2_every_days", val).apply();
                })
        );

        // Save on toggle
        switchFridge.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("fridge_alerts", isChecked).apply());

        switchExpiry.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("expiry_alerts", isChecked).apply());

        setUpToolbar();
    }

    private void setUpBluetooth(Context cont){
        btHelper = new Bluetooth(cont);
    }


    private void setUpUi()
    {
        switchFridge = findViewById(R.id.switch_fridge);
        switchExpiry = findViewById(R.id.switch_expiry);
        inputExpiredHours = findViewById(R.id.input_expired_hours);
        inputWeek1Days = findViewById(R.id.input_week1_days);
        inputWeek2Days = findViewById(R.id.input_week2_days);
        ssidField=findViewById(R.id.ssid);
        passwordField=findViewById(R.id.password);
        save = findViewById(R.id.buttonSave);
        requestBluetoothIfNeeded();
        save.setOnClickListener(view->{
            String ssid = ssidField.getText().toString();
            String password = passwordField.getText().toString();
            if((!ssid.isEmpty())&&(!password.isEmpty())) {
                try {
                    if (btHelper != null) {
                        Log.d("Bluetooth","Supposed to be sending the data");
                        btHelper.transmitCredentials(ssid + "," + password);
                    } else {

                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


            }

        });


    }



    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    interface NumberPickedCallback {
        void onNumberPicked(int value);
    }

    private void showNumberPicker(String title, int min, int max, int current, NumberPickedCallback callback) {
        // Wrap the NumberPicker in a LinearLayout to control layout
        LinearLayout container = new LinearLayout(this);
        container.setPadding(48, 24, 48, 0); // Add spacing around
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        final android.widget.NumberPicker picker = new android.widget.NumberPicker(this);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(current);

        container.addView(picker);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setView(container)
                .setPositiveButton("OK", (dialog, which) -> callback.onNumberPicked(picker.getValue()))
                .setNegativeButton("Cancel", null)
                .show();
    }



    private int parseNumber(String text, int defaultVal) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private void requestBluetoothIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 200);
            }
        }
    }

}