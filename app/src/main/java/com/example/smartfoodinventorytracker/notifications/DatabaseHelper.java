package com.example.smartfoodinventorytracker.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smartfoodinventorytracker.inventory.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DatabaseHelper {

    // --------------------------------------------------
    // 1) This references the GLOBAL "inventory" node
    // --------------------------------------------------
    private static final DatabaseReference inventoryRef =
            FirebaseDatabase.getInstance().getReference("inventory");

    // Optional caching variables (not strictly necessary):
    private static Long lastTemperature = null;
    private static Long lastHumidity = null;
    private static Long lastCO = null;
    private static Long lastLPG = null;
    private static Long lastSmoke = null;

    // ------------------------------------------------------------------------
    // Notification Fetch
    // ------------------------------------------------------------------------
    public interface NotificationFetchListener {
        void onNotificationsFetched(List<NotificationItem> notifications);
    }

    // Fetch stored notifications from "users/{userId}/notifications"
    public static void fetchNotifications(String userId, NotificationFetchListener listener) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("notifications");

        notifRef.orderByChild("timestamp").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<NotificationItem> notifications = new ArrayList<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String message = snapshot.child("message").getValue(String.class);
                            Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                            if (message != null && timestamp != null) {
                                String lowerMsg = message.toLowerCase(); // For case-insensitive matching
                                String title;
                                if (lowerMsg.contains("expires") || lowerMsg.contains("expired")) {
                                    title = NotificationHelper.EXPIRY_ALERT_TITLE; // "Inventory Alert 🍏"
                                } else {
                                    title = NotificationHelper.FRIDGE_ALERT_TITLE; // "Fridge Alert 🚨"
                                }

                                notifications.add(new NotificationItem(title, message, timestamp));
                            }
                        }

                        listener.onNotificationsFetched(notifications);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // handle error
                    }
                });
    }

    // Clear all notifications for the given user
    public static void clearNotifications(String userId, Runnable onComplete) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("notifications");

        notifRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("DatabaseHelper", "All notifications cleared.");
            } else {
                Log.e("DatabaseHelper", "Failed to clear notifications.", task.getException());
            }
            onComplete.run();
        });
    }

    // ------------------------------------------------------------------------
    // 2) Listen to the GLOBAL "inventory" node for sensor data
    // ------------------------------------------------------------------------
    public static void listenToInventoryChanges(Context context, NotificationHelper notificationHelper) {
        inventoryRef.orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot inventorySnapshot : snapshot.getChildren()) {

                    // ✅ Get the connected user
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    // ========== TEMPERATURE ==========
                    Long temperature = inventorySnapshot.child("temperature").getValue(Long.class);
                    Integer temperatureCondition = inventorySnapshot.child("temperature_condition").getValue(Integer.class);
                    if (temperatureCondition != null && temperature != null) {
                        notificationHelper.sendConditionNotification(userId, "Temperature", temperature, temperatureCondition);
                    }

                    // ========== HUMIDITY ==========
                    Long humidity = inventorySnapshot.child("humidity").getValue(Long.class);
                    Integer humidityCondition = inventorySnapshot.child("humidity_condition").getValue(Integer.class);
                    if (humidityCondition != null && humidity != null) {
                        notificationHelper.sendConditionNotification(userId, "Humidity", humidity, humidityCondition);
                    }

                    // ========== CO LEVEL ==========
                    Long co = inventorySnapshot.child("co").getValue(Long.class);
                    Integer coCondition = inventorySnapshot.child("co_condition").getValue(Integer.class);
                    if (coCondition != null && co != null) {
                        notificationHelper.sendConditionNotification(userId, "CO Level", co, coCondition);
                    }

                    // ========== LPG LEVEL ==========
                    Long lpg = inventorySnapshot.child("lpg").getValue(Long.class);
                    Integer lpgCondition = inventorySnapshot.child("lpg_condition").getValue(Integer.class);
                    if (lpgCondition != null && lpg != null) {
                        notificationHelper.sendConditionNotification(userId, "LPG Level", lpg, lpgCondition);
                    }

                    // ========== SMOKE LEVEL ==========
                    Long smoke = inventorySnapshot.child("smoke").getValue(Long.class);
                    Integer smokeCondition = inventorySnapshot.child("smoke_condition").getValue(Integer.class);
                    if (smokeCondition != null && smoke != null) {
                        notificationHelper.sendConditionNotification(userId, "Smoke Level", smoke, smokeCondition);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("DatabaseHelper", "Error listening to inventory", error.toException());
            }
        });
    }



    // Provide a way to listen for notification changes
    public static void listenForNotificationUpdates(String userId, Runnable callback) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("notifications");

        notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.run();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("DatabaseHelper", "Error listening for notifications", error.toException());
            }
        });
    }

    // ------------------------------------------------------------------------
    // NotificationItem Class
    // ------------------------------------------------------------------------
    public static class NotificationItem {
        private String title;
        private String message;
        private long timestamp;

        // Required empty constructor for Firebase
        public NotificationItem() {
        }

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

    // ------------------------------------------------------------------------
    // 3) checkExpiryNotifications for items in "users/{userId}/inventory_product"
    // ------------------------------------------------------------------------
    public static void checkExpiryNotifications(String userId, NotificationHelper notificationHelper) {
        DatabaseReference inventoryRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("inventory_product");

        inventoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                LocalDate today = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");

                Map<Long, List<String>> grouped = new HashMap<>();
                Context context = notificationHelper.getContext();
                SharedPreferences prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE);
                SharedPreferences sent = context.getSharedPreferences("notif_times", Context.MODE_PRIVATE);

                boolean enabled = prefs.getBoolean("expiry_alerts", true);
                int expiredM = prefs.getInt("expired_every_minutes", 4);
                int week1D = prefs.getInt("week1_every_days", 2);
                int week2D = prefs.getInt("week2_every_days", 3);
                long now = System.currentTimeMillis();

                if (!enabled) return;

                for (DataSnapshot child : snapshot.getChildren()) {
                    Product product = child.getValue(Product.class);
                    if (product == null || product.getExpiryDate() == null || product.getExpiryDate().equals("Not set")) continue;

                    try {
                        LocalDate expiry = LocalDate.parse(product.getExpiryDate(), formatter);
                        long daysLeft = ChronoUnit.DAYS.between(today, expiry);
                        if (daysLeft > 14) continue;

                        grouped.computeIfAbsent(daysLeft, k -> new ArrayList<>()).add(product.getName());
                    } catch (Exception e) {
                        Log.e("ExpiryCheck", "Error parsing: " + product.getName(), e);
                    }
                }

                for (Map.Entry<Long, List<String>> entry : grouped.entrySet()) {
                    long daysLeft = entry.getKey();
                    List<String> items = entry.getValue();
                    String groupKey = "group_" + daysLeft;
                    long lastSent = sent.getLong(groupKey, 0);

                    boolean shouldNotify = false;

                    if (daysLeft <= 0 && (now - lastSent >= expiredM * 60 * 1000L)) {
                        shouldNotify = true;
                    } else if (daysLeft <= 7 && (TimeUnit.MILLISECONDS.toDays(now - lastSent) >= week1D)) {
                        shouldNotify = true;
                    } else if (daysLeft <= 14 && (TimeUnit.MILLISECONDS.toDays(now - lastSent) >= week2D)) {
                        shouldNotify = true;
                    }

                    if (shouldNotify) {
                        String message;
                        if (daysLeft < 0) {
                            message = "❌ Expired: " + String.join(", ", items);
                        } else if (daysLeft == 0) {
                            message = "📅 Expires today: " + String.join(", ", items);
                        } else if (daysLeft == 1) {
                            message = "⏰ Expires tomorrow: " + String.join(", ", items);
                        } else {
                            message = "🕒 Expires in " + daysLeft + " days: " + String.join(", ", items);
                        }

                        notificationHelper.sendNotification(
                                NotificationHelper.EXPIRY_ALERT_TITLE,
                                message,
                                com.example.smartfoodinventorytracker.inventory.InventoryActivity.class,
                                "" // no search
                        );

                        sent.edit().putLong(groupKey, now).apply();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("checkExpiryNotifications", "Failed", error.toException());
            }
        });
    }

    public static void deleteNotification(NotificationItem item, String userId, Runnable onComplete) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("notifications");
        notifRef.orderByChild("timestamp").equalTo(item.getTimestamp())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String title = child.child("title").getValue(String.class);
                            String message = child.child("message").getValue(String.class);
                            if (title != null && message != null &&
                                    title.equals(item.getTitle()) && message.equals(item.getMessage())) {
                                child.getRef().removeValue();
                                break;
                            }
                        }
                        onComplete.run();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onComplete.run();
                    }
                });
    }


}
