package com.example.smartfoodinventorytracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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

                    // ========== TEMPERATURE ==========
                    Long temperature = inventorySnapshot.child("temperature").getValue(Long.class);
                    if (lastTemperature == null || !lastTemperature.equals(temperature)) {
                        notificationHelper.sendConditionNotification("Temperature", temperature);
                    }
                    lastTemperature = temperature;

                    // ========== HUMIDITY ==========
                    Long humidity = inventorySnapshot.child("humidity").getValue(Long.class);
                    if (lastHumidity == null || !lastHumidity.equals(humidity)) {
                        notificationHelper.sendConditionNotification("Humidity", humidity);
                    }
                    lastHumidity = humidity;

                    // ========== CO ==========
                    Long co = inventorySnapshot.child("co").getValue(Long.class);
                    if (lastCO == null || !lastCO.equals(co)) {
                        notificationHelper.sendConditionNotification("CO Level", co);
                    }
                    lastCO = co;

                    // ========== LPG ==========
                    Long lpg = inventorySnapshot.child("lpg").getValue(Long.class);
                    if (lastLPG == null || !lastLPG.equals(lpg)) {
                        notificationHelper.sendConditionNotification("LPG Level", lpg);
                    }
                    lastLPG = lpg;

                    // ========== SMOKE ==========
                    Long smoke = inventorySnapshot.child("smoke").getValue(Long.class);
                    if (lastSmoke == null || !lastSmoke.equals(smoke)) {
                        notificationHelper.sendConditionNotification("Smoke Level", smoke);
                    }
                    lastSmoke = smoke;
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
        DatabaseReference inventoryProdRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("inventory_product");

        inventoryProdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                LocalDate today = LocalDate.now();
                Context context = notificationHelper.getContext();
                SharedPreferences prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE);
                SharedPreferences sentTimes = context.getSharedPreferences("notif_times", Context.MODE_PRIVATE);

                boolean expiryEnabled = prefs.getBoolean("expiry_alerts", true);
                int minutesForExpired = prefs.getInt("expired_every_minutes", 1);
                int daysForWeek1 = prefs.getInt("week1_every_days", 2);
                int daysForWeek2 = prefs.getInt("week2_every_days", 3);

                long now = System.currentTimeMillis();

                if (!expiryEnabled) return;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Product product = snapshot.getValue(Product.class);
                    if (product == null
                            || product.getExpiryDate() == null
                            || product.getExpiryDate().equals("Not set")) {
                        continue;
                    }

                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
                        LocalDate expiry = LocalDate.parse(product.getExpiryDate(), formatter);
                        long daysLeft = ChronoUnit.DAYS.between(today, expiry);
                        String key = product.getBarcode();  // Unique key for each product

                        if (daysLeft <= 0) {
                            long last = sentTimes.getLong(key + "_expired", 0);
                            long secondsSince = TimeUnit.MILLISECONDS.toSeconds(now - last);
                            if (secondsSince >= minutesForExpired * 60) {
                                notificationHelper.sendExpiryNotification(product.getName(), daysLeft);
                                sentTimes.edit().putLong(key + "_expired", now).apply();
                            }
                        } else if (daysLeft <= 7) {
                            long last = sentTimes.getLong(key + "_week1", 0);
                            long daysSince = TimeUnit.MILLISECONDS.toDays(now - last);
                            if (daysSince >= daysForWeek1) {
                                notificationHelper.sendExpiryNotification(product.getName(), daysLeft);
                                sentTimes.edit().putLong(key + "_week1", now).apply();
                            }
                        } else if (daysLeft <= 14) {
                            long last = sentTimes.getLong(key + "_week2", 0);
                            long daysSince = TimeUnit.MILLISECONDS.toDays(now - last);
                            if (daysSince >= daysForWeek2) {
                                notificationHelper.sendExpiryNotification(product.getName(), daysLeft);
                                sentTimes.edit().putLong(key + "_week2", now).apply();
                            }
                        }
                    } catch (Exception e) {
                        Log.e("ExpiryCheck", "Error parsing date for " + product.getName(), e);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("DatabaseHelper", "checkExpiryNotifications failed", databaseError.toException());
            }
        });
    }
}
