package com.example.smartfoodinventorytracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationCenter extends AppCompatActivity {

    private NotificationAdapter adapter;
    private RecyclerView recyclerView;
    private List<DatabaseHelper.NotificationItem> notificationList = new ArrayList<>();
    private NotificationHelper notificationHelper;
    private String currentFilter = null; // null means "All"
    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

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

        notificationHelper = new NotificationHelper(this, false, userId);

        recyclerView = findViewById(R.id.notificationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        loadNotifications();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_clear_notifs, menu);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);

            // Force black text for top-level items
            if (item.getTitle() != null) {
                SpannableString spanString = new SpannableString(item.getTitle());
                spanString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, spanString.length(), 0);
                item.setTitle(spanString);
            }

            // If this item has a submenu (like your filter)
            if (item.hasSubMenu()) {
                android.view.SubMenu subMenu = item.getSubMenu();
                for (int j = 0; j < subMenu.size(); j++) {
                    MenuItem subItem = subMenu.getItem(j);
                    if (subItem.getTitle() != null) {
                        SpannableString spanString = new SpannableString(subItem.getTitle());
                        spanString.setSpan(new ForegroundColorSpan(Color.BLACK), 0, spanString.length(), 0);
                        subItem.setTitle(spanString);
                    }
                }
            }
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.clear_notifs) {
            clearAllNotifications();
            return true;
        } else if (id == R.id.filter_all) {
            loadNotifications(); // Show all notifications
            return true;
        } else if (id == R.id.filter_fridge) {
            loadFilteredNotifications("Fridge Alert");
            return true;
        } else if (id == R.id.filter_inventory) {
            loadFilteredNotifications("Inventory");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadFilteredNotifications(String filterType) {
        currentFilter = filterType;
        DatabaseHelper.fetchNotifications(userId, notifications -> {
            notificationList.clear();

            for (DatabaseHelper.NotificationItem notification : notifications) {
                if (notification.getTitle().contains(filterType)) {
                    notificationList.add(notification);
                }
            }

            Collections.sort(notificationList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            adapter.notifyDataSetChanged();
        });
    }


    private void clearAllNotifications() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear All Notifications")
                .setMessage("Are you sure you want to delete all notifications?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    DatabaseHelper.clearNotifications(userId, () -> {
                        Toast.makeText(this, "All notifications cleared!", Toast.LENGTH_SHORT).show();
                        loadNotifications();
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadNotifications() {
        currentFilter = null;
        DatabaseHelper.fetchNotifications(userId, notifications -> {
            notificationList.clear();
            Collections.sort(notifications, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            notificationList.addAll(notifications);
            adapter.notifyDataSetChanged();
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

    // ==========================
    // Embedded NotificationAdapter Class
    // ==========================
    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

        private final List<DatabaseHelper.NotificationItem> notificationList;
        private final Map<String, Integer> colorMap;

        public NotificationAdapter(List<DatabaseHelper.NotificationItem> notifications) {
            this.notificationList = notifications;
            this.colorMap = new HashMap<>();
            colorMap.put(NotificationHelper.FRIDGE_ALERT_TITLE, Color.RED);
            colorMap.put(NotificationHelper.EXPIRY_ALERT_TITLE, Color.YELLOW);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DatabaseHelper.NotificationItem notification = notificationList.get(position);
            holder.notificationTitle.setText(notification.getTitle());
            holder.notificationMessage.setText(notification.getMessage());
            holder.notificationTime.setText(DateUtils.getRelativeTimeSpanString(notification.getTimestamp() * 1000));

            // Assign correct icons
            if (notification.getTitle().contains("Fridge Alert")) {
                holder.notificationIcon.setImageResource(R.drawable.ic_fridge);
            } else if (notification.getTitle().contains("Food Expiry") || notification.getTitle().contains("Inventory")) {
                holder.notificationIcon.setImageResource(R.drawable.ic_inventory);
            } else {
                holder.notificationIcon.setImageResource(R.drawable.ic_notification); // Default fallback
            }

            // Set onClickListener to open respective activity
            holder.itemView.setOnClickListener(v -> {
                Intent intent;
                if (notification.getTitle().contains("Fridge Alert")) {
                    intent = new Intent(v.getContext(), FridgeConditions.class);
                } else if (notification.getTitle().contains("Food Expiry") || notification.getTitle().contains("Inventory")) {
                    intent = new Intent(v.getContext(), InventoryActivity.class);
                } else {
                    intent = new Intent(v.getContext(), NotificationCenter.class); // Default fallback
                }
                v.getContext().startActivity(intent);
            });
        }



        @Override
        public int getItemCount() {
            return notificationList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView notificationIcon;
            TextView notificationTitle, notificationMessage, notificationTime;

            public ViewHolder(View itemView) {
                super(itemView);
                notificationIcon = itemView.findViewById(R.id.notificationIcon);
                notificationTitle = itemView.findViewById(R.id.notificationTitle);
                notificationMessage = itemView.findViewById(R.id.notificationMessage);
                notificationTime = itemView.findViewById(R.id.notificationTime);
            }
        }


    }
}
