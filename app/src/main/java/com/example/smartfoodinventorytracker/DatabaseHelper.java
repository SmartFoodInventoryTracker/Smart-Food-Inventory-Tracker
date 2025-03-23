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
    private static final DatabaseReference inventoryRef = FirebaseDatabase.getInstance().getReference("inventory");

    private static Long lastTemperature = null;
    private static Long lastHumidity = null;
    private static Long lastCO = null;
    private static Long lastLPG = null;
    private static Long lastSmoke = null;


    public interface NotificationFetchListener {
        void onNotificationsFetched(List<NotificationItem> notifications);
    }

    // 🔹 Fetch stored notifications from Firebase
    public static void fetchNotifications(String userId, NotificationFetchListener listener) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("notifications");

        notifRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<NotificationItem> notifications = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String message = snapshot.child("message").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    if (message != null && timestamp != null) {
                        String title = message.contains("expires")
                                ? NotificationHelper.EXPIRY_ALERT_TITLE
                                : NotificationHelper.FRIDGE_ALERT_TITLE;
                        notifications.add(new NotificationItem(title, message, timestamp));
                    }
                }

                listener.onNotificationsFetched(notifications);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    public static void clearNotifications(String userId, Runnable onComplete) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("notifications");

        notifRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("DatabaseHelper", "All notifications cleared.");
            } else {
                Log.e("DatabaseHelper", "Failed to clear notifications.", task.getException());
            }
            onComplete.run();
        });
    }

    // 🔹 Listen to temperature & humidity changes
    public static void listenToInventoryChanges(Context context, NotificationHelper notificationHelper) {
        inventoryRef.orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot inventorySnapshot : snapshot.getChildren()) {
                    // ✅ Fetch sensor values
                    Long temperature = inventorySnapshot.child("temperature").getValue(Long.class);
                    Long humidity = inventorySnapshot.child("humidity").getValue(Long.class);
                    Long co = inventorySnapshot.child("co").getValue(Long.class);
                    Long lpg = inventorySnapshot.child("lpg").getValue(Long.class);
                    Long smoke = inventorySnapshot.child("smoke").getValue(Long.class);

                    // ✅ Fetch condition values
                    Long temperatureCondition = inventorySnapshot.child("temperature condition").getValue(Long.class);
                    Long humidityCondition = inventorySnapshot.child("humidity condition").getValue(Long.class);
                    Long coCondition = inventorySnapshot.child("co condition").getValue(Long.class);
                    Long lpgCondition = inventorySnapshot.child("lpg condition").getValue(Long.class);
                    Long smokeCondition = inventorySnapshot.child("smoke condition").getValue(Long.class);

                    // ✅ Send notifications only if condition is greater than 7
                    if (temperatureCondition != null && temperatureCondition > 7) {
                        notificationHelper.sendConditionNotification("Temperature", temperature);
                    }
                    if (humidityCondition != null && humidityCondition > 7) {
                        notificationHelper.sendConditionNotification("Humidity", humidity);
                    }
                    if (coCondition != null && coCondition > 7) {
                        notificationHelper.sendConditionNotification("CO Level", co);
                    }
                    if (lpgCondition != null && lpgCondition > 7) {
                        notificationHelper.sendConditionNotification("LPG Level", lpg);
                    }
                    if (smokeCondition != null && smokeCondition > 7) {
                        notificationHelper.sendConditionNotification("Smoke Level", smoke);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("DatabaseHelper", "Error listening to inventory", error.toException());
            }
        });
    }


    public static void listenForNotificationUpdates(String userId, Runnable callback) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("notifications");

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


    public static class NotificationItem {
        private String title;
        private String message;
        private long timestamp;

        // ✅ Required empty constructor for Firebase
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

    public static void checkExpiryNotifications(String userId, NotificationHelper notificationHelper) {
        DatabaseReference inventoryProdRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("inventory_product");

        inventoryProdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                LocalDate today = LocalDate.now();
                Context context = notificationHelper.getContext();
                SharedPreferences prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE);
                SharedPreferences sentTimes = context.getSharedPreferences("notif_times", Context.MODE_PRIVATE);

                boolean expiryEnabled = prefs.getBoolean("expiry_alerts", true);
                int hoursForExpired = prefs.getInt("expired_every_hours", 4);
                int daysForWeek1 = prefs.getInt("week1_every_days", 2);
                int daysForWeek2 = prefs.getInt("week2_every_days", 3);

                long now = System.currentTimeMillis();

                if (!expiryEnabled) return;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Product product = snapshot.getValue(Product.class);
                    if (product == null || product.getExpiryDate() == null || product.getExpiryDate().equals("Not set")) {
                        continue;
                    }

                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
                        LocalDate expiry = LocalDate.parse(product.getExpiryDate(), formatter);
                        long daysLeft = ChronoUnit.DAYS.between(today, expiry);
                        String key = product.getBarcode();  // Use barcode to uniquely track per item

                        if (daysLeft <= 0) {
                            // Expired or expiring today → hourly
                            long last = sentTimes.getLong(key + "_expired", 0);
                            long hoursSince = TimeUnit.MILLISECONDS.toHours(now - last);
                            if (hoursSince >= hoursForExpired) {
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
