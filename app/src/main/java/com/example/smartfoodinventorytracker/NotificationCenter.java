package com.example.smartfoodinventorytracker;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NotificationCenter extends AppCompatActivity {

    private NotificationHelper notificationHelper;
    private Button test_notifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Enable Up Navigation
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize NotificationHelper
        notificationHelper = new NotificationHelper(this);

        // Request Notification Permission if needed (Android 13+)
        requestNotificationPermission();

        // Button for testing notifications
        test_notifications = findViewById(R.id.test_notifications);
        test_notifications.setOnClickListener(v -> sendFridgeAlertNotification());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
    }

    private void sendFridgeAlertNotification() {
        notificationHelper.sendNotification(
                "Fridge Alert ⚠️",
                "Some abnormal condition has been detected in your fridge!",
                FridgeConditions.class,
                "Some Value to be passed"
        );
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();  // Close ProfileActivity and return to MainActivity
        return true;
    }
}
