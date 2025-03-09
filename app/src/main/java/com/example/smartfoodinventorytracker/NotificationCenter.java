package com.example.smartfoodinventorytracker;

import android.os.Bundle;
import android.text.Html;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

        // Fetch Notifications
        loadNotifications();

        // ✅ Auto-refresh when new notifications are added
        DatabaseHelper.listenForNotificationUpdates(this::loadNotifications);
        DatabaseHelper.listenToInventoryChanges(this, notificationHelper);
    }

    private void loadNotifications() {
        DatabaseHelper.fetchNotifications(notifications -> {
            notificationList.clear();

            // ✅ Sort notifications from most recent to least recent
            notifications.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

            for (DatabaseHelper.NotificationItem notification : notifications) {
                String formattedTime = formatTimestamp(notification.getTimestamp());
                String displayText = Html.fromHtml(
                        "<b>" + notification.getTitle() + "</b><br/>" +
                                notification.getMessage() + "<br/>📅 " + formattedTime + "<br/>",
                        Html.FROM_HTML_MODE_LEGACY).toString();
                notificationList.add(displayText);
            }
            adapter.notifyDataSetChanged();
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
