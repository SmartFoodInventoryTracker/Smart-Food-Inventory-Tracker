package com.example.smartfoodinventorytracker;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.*;

public class NotificationCenter extends AppCompatActivity {

    private NotificationHelper notificationHelper;
    private Button test_notifications;
    private ListView notificationListView;
    private ArrayAdapter<String> adapter;
    private List<String> notificationList;
    private DatabaseReference databaseRef;
    private static DatabaseReference inventoryRef; // Make static so it persists across openings
    private static Long lastTemperature = null;
    private static boolean isListenerAttached = false; // Ensure listener is only attached once

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

        setUpToolbar();

        // Firebase References
        databaseRef = FirebaseDatabase.getInstance().getReference("notifications");
        inventoryRef = FirebaseDatabase.getInstance().getReference("inventory");

        // UI Initialization
        notificationHelper = new NotificationHelper(this);
        test_notifications = findViewById(R.id.test_notifications);
        notificationListView = findViewById(R.id.Notification_ListView);

        // Initialize ListView
        notificationList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notificationList);
        notificationListView.setAdapter(adapter);

        // Request Notification Permission
        requestNotificationPermission();

        // Fetch Notifications Initially
        fetchNotificationsFromFirebase();

        // Auto-refresh when new notifications are added
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                fetchNotificationsFromFirebase();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("NotificationCenter", "Error fetching notifications", error.toException());
            }
        });

        // ✅ Attach Temperature Listener Only Once
        if (!isListenerAttached) {
            listenToTemperatureChanges();
            isListenerAttached = true;
        }

        // Button to Send Test Notification
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

    private void fetchNotificationsFromFirebase() {
        databaseRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                notificationList.clear();
                List<NotificationItem> tempNotificationList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String message = snapshot.child("message").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    if (message != null && timestamp != null) {
                        tempNotificationList.add(new NotificationItem("Fridge Alert ⚠️", message, timestamp));
                    }
                }

                // Sort notifications from most recent to least recent
                Collections.sort(tempNotificationList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                // Convert to formatted list for display with spacing
                for (NotificationItem notification : tempNotificationList) {
                    String formattedTime = formatTimestamp(notification.getTimestamp());
                    String displayText = Html.fromHtml(
                            "<b>" + notification.getTitle() + "</b><br/>" +
                                    notification.getMessage() + "<br/>📅 " + formattedTime + "<br/>",
                            Html.FROM_HTML_MODE_LEGACY).toString();
                    notificationList.add(displayText);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("NotificationCenter", "Error fetching notifications", databaseError.toException());
            }
        });
    }

    private void listenToTemperatureChanges() {
        inventoryRef.orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot inventorySnapshot : snapshot.getChildren()) {
                    checkTemperatureAndNotify(inventorySnapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("NotificationCenter", "Error listening to inventory updates", error.toException());
            }
        });
    }

    private void checkTemperatureAndNotify(DataSnapshot snapshot) {
        Long newTemperature = snapshot.child("temperature").getValue(Long.class);

        if (newTemperature != null) {
            Log.d("TemperatureMonitor", "New Temperature: " + newTemperature);

            if (lastTemperature == null || !newTemperature.equals(lastTemperature)) {
                // ✅ Temperature changed → Send notification
                String message = "Temperature changed! Current: " + newTemperature + "°C";

                notificationHelper.sendNotification("Temperature Alert ⚠️", message, FridgeConditions.class, "");

                // ✅ Update last known temperature
                lastTemperature = newTemperature;
            }
        }
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd @ HH:mm", Locale.getDefault()).format(new Date(timestamp * 1000));
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

    // ✅ NotificationItem class to store notifications
    private static class NotificationItem {
        private final String title;
        private final String message;
        private final long timestamp;

        public NotificationItem(String title, String message, long timestamp) {
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
