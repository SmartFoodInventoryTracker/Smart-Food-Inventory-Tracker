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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationCenter extends AppCompatActivity {

    private NotificationHelper notificationHelper;
    private Button test_notifications;
    private ListView notificationListView;
    private ArrayAdapter<String> adapter;
    private List<String> notificationList;
    private DatabaseReference databaseRef;

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

        // Enable Up Navigation
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Firebase reference
        databaseRef = FirebaseDatabase.getInstance().getReference("notifications");

        // Initialize UI elements
        notificationHelper = new NotificationHelper(this);
        test_notifications = findViewById(R.id.test_notifications);
        notificationListView = findViewById(R.id.Notification_ListView);

        // Initialize ListView and Adapter
        notificationList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notificationList);
        notificationListView.setAdapter(adapter);

        // Request Notification Permission if needed (Android 13+)
        requestNotificationPermission();

        // Button for testing notifications
        test_notifications.setOnClickListener(v -> sendFridgeAlertNotification());

        // Fetch notifications initially and listen for updates
        fetchNotificationsFromFirebase();

        // Auto-refresh notifications when database updates
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                fetchNotificationsFromFirebase(); // Refresh ListView when database updates
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("NotificationCenter", "Error fetching notifications", error.toException());
            }
        });
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
                notificationList.clear(); // Clear existing data before adding new
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

                // Notify adapter of data change
                adapter.notifyDataSetChanged();

                // Log notifications for debugging
                Log.d("NotificationCenter", "Fetched Notifications: " + notificationList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("NotificationCenter", "Error fetching notifications", databaseError.toException());
            }
        });
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd @ HH:mm", Locale.getDefault()).format(new Date(timestamp * 1000));
    }

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

    @Override
    public boolean onSupportNavigateUp() {
        finish();  // Close NotificationCenter and return to the previous activity
        return true;
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
}
