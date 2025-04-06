package com.example.smartfoodinventorytracker.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.smartfoodinventorytracker.fridge_conditions.FridgeConditionsActivity;
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
import android.os.Handler;

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

    public static void listenToFridgeConditionChanges(String userId, NotificationHelper helper) {
        DatabaseReference fridgeRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("fridge_condition");

        // Use SharedPreferences to store the last known raw values for each parameter
        Context context = helper.getContext();
        final SharedPreferences lastValuePrefs = context.getSharedPreferences("fridge_last_values", Context.MODE_PRIVATE);

        // Attach a realtime listener to the fridge_condition node
        fridgeRef.orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder notificationMessage = new StringBuilder();
                boolean shouldNotify = false;

                // List of parameter keys to inspect
                String[] parameters = {"temperature", "humidity", "co", "lpg", "smoke"};

                for (String param : parameters) {
                    // Retrieve the current raw value from Firebase
                    // Assuming all parameters can be represented as Double (adjust type as needed)
                    Double currentValue = snapshot.child(param).getValue(Double.class);
                    if (currentValue == null) continue;

                    // Retrieve the last stored value; if not found, use a special default value that forces a notification.
                    float lastValue = lastValuePrefs.getFloat(param, Float.MIN_VALUE);
                    // If this is the first value or if the value has changed, add it to the notification
                    if (lastValue == Float.MIN_VALUE || currentValue.floatValue() != lastValue) {
                        shouldNotify = true;
                        notificationMessage.append("\n• ").append(param)
                                .append(": ").append(currentValue);
                        // Update stored value with the new value
                        lastValuePrefs.edit().putFloat(param, currentValue.floatValue()).apply();
                    }
                }

                if (shouldNotify) {
                    helper.sendNotificationLocalAndFirebase(
                            userId,
                            NotificationHelper.FRIDGE_ALERT_TITLE,
                            notificationMessage.toString(),
                            FridgeConditionsActivity.class
                    );
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FridgeListener", "Listener error", error.toException());
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
    // Inside DatabaseHelper.java

    public static void checkExpiryNotifications(String userId, NotificationHelper notificationHelper) {
        DatabaseReference inventoryRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("inventory_product");

        inventoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                LocalDate today = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");

                // Create lists for each category.
                List<android.util.Pair<String, Long>> expiredList = new ArrayList<>();         // daysLeft < 0
                List<android.util.Pair<String, Long>> expiringTodayList = new ArrayList<>();   // daysLeft == 0
                List<android.util.Pair<String, Long>> withinWeekList = new ArrayList<>();      // daysLeft between 1 and 7
                List<android.util.Pair<String, Long>> within2WeeksList = new ArrayList<>();    // daysLeft between 8 and 14

                Context context = notificationHelper.getContext();
                SharedPreferences prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE);
                SharedPreferences sent = context.getSharedPreferences("notif_times", Context.MODE_PRIVATE);

                boolean enabled = prefs.getBoolean("expiry_alerts", true);
                // Use the same frequency for all groups
                int freqValue = prefs.getInt("expired_interval_value", 4);
                String freqUnit = prefs.getString("expired_interval_unit", "minute(s)");
                long freqDelay = convertIntervalToMillis(freqValue, freqUnit);

                long now = System.currentTimeMillis();

                if (!enabled) return;

                // Process each product from inventory
                for (DataSnapshot child : snapshot.getChildren()) {
                    Product product = child.getValue(Product.class);
                    if (product == null || product.getExpiryDate() == null || product.getExpiryDate().equals("Not set"))
                        continue;
                    try {
                        LocalDate expiry = LocalDate.parse(product.getExpiryDate(), formatter);
                        long daysLeft = ChronoUnit.DAYS.between(today, expiry);
                        if (daysLeft > 14) continue; // ignore products expiring in more than 2 weeks

                        if (daysLeft < 0) {
                            expiredList.add(new android.util.Pair<>(product.getName(), daysLeft));
                        } else if (daysLeft == 0) {
                            expiringTodayList.add(new android.util.Pair<>(product.getName(), daysLeft));
                        } else if (daysLeft <= 7) {
                            withinWeekList.add(new android.util.Pair<>(product.getName(), daysLeft));
                        } else { // daysLeft between 8 and 14
                            within2WeeksList.add(new android.util.Pair<>(product.getName(), daysLeft));
                        }
                    } catch (Exception e) {
                        Log.e("ExpiryCheck", "Error parsing: " + product.getName(), e);
                    }
                }

                // Send notifications for each group (if conditions are met)
                sendGroupNotification("expired", "❌ Expired", expiredList, freqDelay, sent, now, notificationHelper);
                sendGroupNotification("expiring_today", "📅 Expires today", expiringTodayList, freqDelay, sent, now, notificationHelper);
                sendGroupNotification("within_week", "🕒 Expires within a week", withinWeekList, freqDelay, sent, now, notificationHelper);
                sendGroupNotification("within_2weeks", "⏰ Expires within two weeks", within2WeeksList, freqDelay, sent, now, notificationHelper);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("checkExpiryNotifications", "Failed", error.toException());
            }
        });
    }

    /**
     * Helper method to send a grouped notification.
     * If there are more than 5 products, it sends a message with just the item count.
     * Otherwise, it lists each product as a bullet. For groups other than "expired" or "expiring_today",
     * it appends the days left in parentheses.
     */
    private static void sendGroupNotification(String groupKeySuffix, String titlePrefix,
                                              List<android.util.Pair<String, Long>> list, long freqDelay, SharedPreferences sent, long now,
                                              NotificationHelper notificationHelper) {
        if (list.isEmpty()) return;
        String groupKey = "group_" + groupKeySuffix;
        long lastSent = sent.getLong(groupKey, 0);
        if (now - lastSent < freqDelay) return;

        String message;
        if (list.size() > 5) {
            message = titlePrefix + ": " + list.size() + " items";
        } else {
            StringBuilder sb = new StringBuilder();
            for (android.util.Pair<String, Long> pair : list) {
                String productName = pair.first;
                long daysLeft = pair.second;
                // For "within_week" and "within_2weeks", include the number of days left.
                if (groupKeySuffix.equals("expired") || groupKeySuffix.equals("expiring_today")) {
                    sb.append("\n• ").append(productName);
                } else {
                    sb.append("\n• ").append(productName).append(" (").append(daysLeft).append(" days)");
                }
            }
            message = titlePrefix + ":" + sb.toString();
        }

        notificationHelper.sendNotification(
                NotificationHelper.EXPIRY_ALERT_TITLE,
                message,
                com.example.smartfoodinventorytracker.inventory.InventoryActivity.class,
                "" // no extra data
        );

        sent.edit().putLong(groupKey, now).apply();
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

    // Helper method to convert interval values to milliseconds based on the selected unit.
    private static long convertIntervalToMillis(int value, String unit) {
        if (unit.equalsIgnoreCase("hour(s)")) {
            return value * 60 * 60 * 1000L;
        } else if (unit.equalsIgnoreCase("day(s)")) {
            return value * 24 * 60 * 60 * 1000L;
        } else { // default to minute(s)
            return value * 60 * 1000L;
        }
    }
}
