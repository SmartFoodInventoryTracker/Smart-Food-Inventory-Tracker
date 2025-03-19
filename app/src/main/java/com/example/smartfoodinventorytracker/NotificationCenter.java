package com.example.smartfoodinventorytracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.*;

public class NotificationCenter extends AppCompatActivity {

    private NotificationHelper notificationHelper;

    private ListView notificationListView;
    private ArrayAdapter<String> adapter;
    private List<String> notificationList;

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

        // UI Initialization
        notificationHelper = new NotificationHelper(this);
        notificationListView = findViewById(R.id.Notification_ListView);

        // Initialize ListView
        notificationList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notificationList);
        notificationListView.setAdapter(adapter);

        // ✅ Auto-refresh when new notifications are added
        DatabaseHelper.listenForNotificationUpdates(this::loadNotifications);
        DatabaseHelper.listenToInventoryChanges(this, notificationHelper);

        // Fetch Notifications
        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseHelper.checkExpiryNotifications(notificationHelper); // ✅ Trigger expiry check every time Notification Center is opened
        loadNotifications();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_clear_notifs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.clear_notifs) {
            clearAllNotifications(); // Call method to clear notifications
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearAllNotifications() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear All Notifications")
                .setMessage("Are you sure you want to delete all notifications?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Call the method to clear notifications if confirmed
                    DatabaseHelper.clearNotifications(() -> {
                        Toast.makeText(this, "All notifications cleared!", Toast.LENGTH_SHORT).show();
                        loadNotifications(); // Refresh the ListView
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Do nothing if canceled
                    dialog.dismiss();
                })
                .show();
    }


    private void loadNotifications() {
        DatabaseHelper.fetchNotifications(notifications -> {
            notificationList.clear();

            // ✅ Sort notifications from most recent to least recent
            notifications.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

            // ✅ Use constants from NotificationHelper instead of hardcoded strings
            Map<String, Class<?>> activityMap = new HashMap<>();
            activityMap.put(NotificationHelper.FRIDGE_ALERT_TITLE, FridgeConditions.class);
            activityMap.put(NotificationHelper.EXPIRY_ALERT_TITLE, InventoryActivity.class);

            for (DatabaseHelper.NotificationItem notification : notifications) {
                String formattedTime = formatTimestamp(notification.getTimestamp());
                String displayText = Html.fromHtml(
                        "<b>" + notification.getTitle() + "</b><br/>" +
                                notification.getMessage() + "<br/>📅 " + formattedTime + "<br/>",
                        Html.FROM_HTML_MODE_LEGACY).toString();
                notificationList.add(displayText);
            }

            adapter.notifyDataSetChanged();

            // ✅ Make list items clickable
            notificationListView.setOnItemClickListener((parent, view, position, id) -> {
                DatabaseHelper.NotificationItem selectedNotification = notifications.get(position);
                Class<?> targetActivity = activityMap.getOrDefault(selectedNotification.getTitle(), NotificationCenter.class);

                Intent intent = new Intent(NotificationCenter.this, targetActivity);
                startActivity(intent);
            });
        });
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd @ HH:mm", Locale.getDefault()).format(new Date(timestamp * 1000));
    }

    private void setUpToolbar() {
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
