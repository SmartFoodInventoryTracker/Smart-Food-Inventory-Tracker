package com.example.smartfoodinventorytracker.settings;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.example.smartfoodinventorytracker.utils.Bluetooth;
import com.example.smartfoodinventorytracker.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchFridge, switchExpiry;
    private Bluetooth btHelper;
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

        // Load saved toggle settings
        switchFridge.setChecked(prefs.getBoolean("fridge_alerts", true));
        switchExpiry.setChecked(prefs.getBoolean("expiry_alerts", true));

        // Load and display expired interval (default 4 Minutes)
        int expiredVal = prefs.getInt("expired_interval_value", 4);
        String expiredUnit = prefs.getString("expired_interval_unit", "minute(s)");
        inputExpiredHours.setText(expiredVal + " " + expiredUnit);

        int week1Val = prefs.getInt("week1_interval_value", 2);
        String week1Unit = prefs.getString("week1_interval_unit", "day(s)");
        inputWeek1Days.setText(week1Val + " " + week1Unit);

        int week2Val = prefs.getInt("week2_interval_value", 3);
        String week2Unit = prefs.getString("week2_interval_unit", "day(s)");
        inputWeek2Days.setText(week2Val + " " + week2Unit);


        // Set up click listeners using the generic picker method
        inputExpiredHours.setOnClickListener(v ->
                showIntervalDialog("Choose frequency",
                        "expired_interval_value", "expired_interval_unit", 1, 60, 4, inputExpiredHours)
        );
        inputWeek1Days.setOnClickListener(v ->
                showIntervalDialog("Choose frequency",
                        "week1_interval_value", "week1_interval_unit", 1, 60, 2, inputWeek1Days)
        );
        inputWeek2Days.setOnClickListener(v ->
                showIntervalDialog("Choose frequency",
                        "week2_interval_value", "week2_interval_unit", 1, 60, 3, inputWeek2Days)
        );

        switchFridge.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("fridge_alerts", isChecked).apply());

        switchExpiry.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("expiry_alerts", isChecked).apply());

        setUpToolbar();
    }

    private void setUpBluetooth(Context cont) {
        btHelper = new Bluetooth(cont);
    }

    private void setUpUi() {
        switchFridge = findViewById(R.id.switch_fridge);
        switchExpiry = findViewById(R.id.switch_expiry);
        inputExpiredHours = findViewById(R.id.input_expired_hours);
        inputWeek1Days = findViewById(R.id.input_week1_days);
        inputWeek2Days = findViewById(R.id.input_week2_days);

        requestBluetoothIfNeeded();
        findViewById(R.id.buttonConfigureWifi).setOnClickListener(v -> showWifiDialog());
    }

    private void showWifiDialog() {
        LinearLayout dialogView = (LinearLayout) getLayoutInflater().inflate(R.layout.dialog_wifi_config, null);
        EditText ssidInput = dialogView.findViewById(R.id.dialog_ssid);
        EditText passInput = dialogView.findViewById(R.id.dialog_password);
        TextView statusText = dialogView.findViewById(R.id.dialog_status); // Add a TextView to your layout with this ID
        Button saveButton = dialogView.findViewById(R.id.dialog_save);

        // Load saved credentials
        String savedSsid = prefs.getString("wifi_ssid", "");
        String savedPass = prefs.getString("wifi_password", "");
        long lastConnected = prefs.getLong("wifi_last_connected", -1);

        ssidInput.setText(savedSsid);
        passInput.setText(savedPass);

        // Show status if available
        if (lastConnected > 0) {
            long elapsedMillis = System.currentTimeMillis() - lastConnected;
            String timeAgo = formatTimeAgo(elapsedMillis);
            statusText.setText("Last connection attempt: " + timeAgo + " ago");
        } else {
            statusText.setText("Not connected yet");
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        saveButton.setOnClickListener(v -> {
            String ssid = ssidInput.getText().toString().trim();
            String password = passInput.getText().toString().trim();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if (!ssid.isEmpty() && !password.isEmpty()) {
                try {
                    if (btHelper != null) {
                        btHelper.transmitCredentials(ssid + "," + password + "," + userId);

                        // ✅ Save attempt info
                        prefs.edit()
                                .putString("wifi_ssid", ssid)
                                .putString("wifi_password", password)
                                .putLong("wifi_last_connected", System.currentTimeMillis())
                                .apply();

                        Toast.makeText(this, "Credentials sent. Please check your sensor screen for status.", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to send WiFi credentials", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please fill in both SSID and password", Toast.LENGTH_SHORT).show();
            }
        });



        dialog.show();
    }

    private String formatTimeAgo(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + " seconds";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minutes";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hours";
        long days = hours / 24;
        return days + " days";
    }


    private void showIntervalDialog(String title, String valueKey, String unitKey, int defaultVal, int defaultMax, int fallbackValue, TextView targetView) {
        // Inflate the custom layout from XML
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.dialog_settings_interval, null);

        // Set up the custom title
        TextView titleView = layout.findViewById(R.id.dialog_title);
        titleView.setText(title);

        final String[] units = {"minute(s)", "hour(s)", "day(s)"};
        final SharedPreferences.Editor editor = prefs.edit();

        // Get the saved value and unit from SharedPreferences
        int savedValue = prefs.getInt(valueKey, fallbackValue);
        String savedUnit = prefs.getString(unitKey, "minute(s)");
        int unitIndex = java.util.Arrays.asList(units).indexOf(savedUnit);

        // Set up the value NumberPicker
        final android.widget.NumberPicker valuePicker = layout.findViewById(R.id.np_value);
        valuePicker.setMinValue(1);
        // Set max value based on the selected unit
        if (savedUnit.equalsIgnoreCase("hour(s)")) {
            valuePicker.setMaxValue(24);
        } else if (savedUnit.equalsIgnoreCase("day(s)")) {
            valuePicker.setMaxValue(7);
        } else {
            valuePicker.setMaxValue(60);
        }

        valuePicker.setValue(savedValue);

        // Set up the unit NumberPicker
        final android.widget.NumberPicker unitPicker = layout.findViewById(R.id.np_unit);
        unitPicker.setDisplayedValues(units);
        unitPicker.setMinValue(0);
        unitPicker.setMaxValue(units.length - 1);
        unitPicker.setValue(unitIndex);

        // Update the valuePicker max value when unit changes
        unitPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            String selectedUnit = units[newVal];
            if (selectedUnit.equalsIgnoreCase("hour(s)")) {
                valuePicker.setMaxValue(24);
            } else if (selectedUnit.equalsIgnoreCase("day(s)")) {
                valuePicker.setMaxValue(7);
            } else {
                valuePicker.setMaxValue(60);
            }
        });


        // Build and show the AlertDialog using the custom layout
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    int value = valuePicker.getValue();
                    String unit = units[unitPicker.getValue()];
                    targetView.setText(value + " " + unit.toLowerCase());
                    editor.putInt(valueKey, value);
                    editor.putString(unitKey, unit);
                    editor.apply();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestBluetoothIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 200);
            }
        }
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
}
