package com.example.smartfoodinventorytracker.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.smartfoodinventorytracker.fridge_conditions.FridgeConditionsActivity;
import com.example.smartfoodinventorytracker.inventory.InventoryActivity;
import com.example.smartfoodinventorytracker.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class NotificationHelper {
    private final String userId;

    // ✅ Define public static variables for notification titles
    public static final String FRIDGE_ALERT_TITLE = "Fridge Alert 🚨";
    public static final String EXPIRY_ALERT_TITLE = "Inventory Alert 🍏";

    private static final String CHANNEL_ID = "CHANNEL_ID_NOTIFICATION";
    private static final String CHANNEL_NAME = "Smart Food Alerts";
    private static final String CHANNEL_DESCRIPTION = "Notifications for fridge and inventory alerts";
    private final Context context;
    private final DatabaseReference databaseRef;
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String LAST_RESET_TIME_KEY = "LastResetTime";
    private static final long FRIDGE_NOTIFICATION_INTERVAL = 30 * 60 * 1000; // ✅ 30 minutes in milliseconds

    public NotificationHelper(Context context, boolean startExpiryCheck, String userId) {
        this.context = context;
        this.userId = userId;
        this.databaseRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("notifications");

        createNotificationChannel();

        if (startExpiryCheck) {
            scheduleExpiryNotificationCheck();
        }
    }

    public void scheduleExpiryNotificationCheck() {
        scheduleOneTimeCheck();
    }

    private void scheduleOneTimeCheck() {
        // Get user setting for expiry notifications delay (in minutes)
        SharedPreferences settingsPrefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE);
        int delayMinutes = settingsPrefs.getInt("expired_every_minutes", 1); // unified key

        // Use a separate preferences file for tracking the first run
        SharedPreferences notificationPrefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        boolean firstRunDone = notificationPrefs.getBoolean("first_run_done", false);

        long delay;
        if (!firstRunDone) {
            // For the very first run, schedule immediately (0 delay)
            delay = 0;
            notificationPrefs.edit().putBoolean("first_run_done", true).apply();
        } else {
            // Otherwise, use the user-defined delay
            delay = delayMinutes;
        }

        WorkRequest workRequest = new OneTimeWorkRequest.Builder(ExpiryWorker.class)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);
    }


    public static class ExpiryWorker extends Worker {
        public ExpiryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                String userId = currentUser.getUid();
                NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext(), false, userId);
                DatabaseHelper.checkExpiryNotifications(userId, notificationHelper);
                notificationHelper.scheduleExpiryNotificationCheck(); // 🔁 Re-schedule
            }

            return Result.success();
        }

    }

    public Context getContext() {
        return context;
    }

    public void startFridgeMonitoringService() {
        Intent serviceIntent = new Intent(context, FridgeMonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent); // ✅ Required for Android 8+
        } else {
            context.startService(serviceIntent);
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void sendNotification(String title, String message, Class<?> targetActivity, String data) {
        Log.d("NotificationHelper", "🛑 Checking duplicate notification: " + message);

        isNotificationAlreadySent(title, message, new NotificationCallback() {
            @Override
            public void onCheckCompleted(boolean allowNotification) {
                if (!allowNotification) {
                    Log.d("NotificationHelper", "🔄 Skipping duplicate notification: " + message);
                    return;
                }

                Log.d("NotificationHelper", "🚀 Sending notification: " + message);
                storeNotificationInFirebase(title, message);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.e("NotificationHelper", "❌ Missing POST_NOTIFICATIONS permission!");
                    return;
                }

                Intent intent = new Intent(context, targetActivity);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("data", data);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                notificationManager.notify(notificationId, builder.build());
                Log.d("NotificationHelper", "✅ Notification Sent - ID: " + notificationId);
            }
        });
    }


    public interface NotificationCallback {
        void onCheckCompleted(boolean allowNotification);
    }

    public void triggerPendingFridgeNotifications() {
        Log.d("NotificationHelper", "🔍 Checking for pending fridge notifications...");

        databaseRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                    String title = notifSnapshot.child("title").getValue(String.class);
                    String message = notifSnapshot.child("message").getValue(String.class);
                    Long timestamp = notifSnapshot.child("timestamp").getValue(Long.class);

                    if (title == null || message == null || timestamp == null) {
                        continue; // Ignore invalid notifications
                    }

                    // ✅ Send the notification manually
                    sendNotification(title, message, FridgeConditionsActivity.class, userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NotificationHelper", "❌ Failed to fetch pending fridge notifications", error.toException());
            }
        });
    }



    private void isNotificationAlreadySent(String title, String message, NotificationCallback callback) {
        long oneMinuteAgo = (System.currentTimeMillis() / 1000) - 60;

        databaseRef.orderByChild("timestamp").startAt(oneMinuteAgo)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean allowNotification = true;

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String storedTitle = child.child("title").getValue(String.class);
                            String storedMessage = child.child("message").getValue(String.class);
                            Long storedTimestamp = child.child("timestamp").getValue(Long.class);

                            if (storedTitle != null && storedMessage != null && storedTimestamp != null) {
                                if (storedTitle.equals(title) && storedMessage.equals(message)) {
                                    Log.d("NotificationHelper", "🔄 Duplicate found in Firebase. Skipping...");
                                    allowNotification = false;
                                    break;
                                }
                            }
                        }

                        callback.onCheckCompleted(allowNotification);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onCheckCompleted(true); // ✅ Default to allow notification if there's an error
                    }
                });
    }



    // ✅ Save notification timestamp for fridge alerts
    private void saveSentNotification(String title, String message) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        long currentTime = System.currentTimeMillis();
        String key = title + "_" + message;

        if (title.equals(FRIDGE_ALERT_TITLE)) {
            editor.putLong(key, currentTime); // ✅ Store last sent time for fridge notifications
        } else if (title.equals(EXPIRY_ALERT_TITLE)) {
            editor.putLong(key, currentTime); // ✅ Reset expiry alerts every 15 minutes
        } else {
            editor.putBoolean(key, true); // ✅ Mark general notifications
        }

        editor.apply();
    }



    private void storeNotificationInFirebase(String title, String message) {
        String notificationId = databaseRef.push().getKey();
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("timestamp", System.currentTimeMillis() / 1000);
        notificationData.put("message", message);
        notificationData.put("title", title); // ✅ Store correct title in Firebase

        if (notificationId != null) {
            databaseRef.child(notificationId).setValue(notificationData);
        }
    }


    public void sendConditionNotification(String userId, String type, Long value, Integer condition) {
        String unit;
        switch (type) {
            case "Temperature": unit = "°C"; break;
            case "Humidity": unit = "%"; break;
            case "CO Level":
            case "LPG Level":
            case "Smoke Level": unit = " ppm"; break;
            default: unit = "";
        }

        if (condition < 5) {
            Log.d("FridgeMonitor", "✅ Condition is safe, skipping notification.");
            return; // ✅ Skip safe notifications
        }

        String severity = (condition >= 9) ? "🔴 CRITICAL" : "🟠 WARNING";
        String message = severity + " - " + type + " changed! Current: " + value + unit;

        Log.d("FridgeMonitor", "🔔 Sending Notification - " + message);

        // ✅ Store notification under "users/{userId}/notifications"
        DatabaseReference userNotificationsRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("notifications");

        String notificationId = userNotificationsRef.push().getKey();
        if (notificationId != null) {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("timestamp", System.currentTimeMillis() / 1000);
            notificationData.put("message", message);
            notificationData.put("title", NotificationHelper.FRIDGE_ALERT_TITLE);
            userNotificationsRef.child(notificationId).setValue(notificationData);
        }

        // ✅ Trigger the notification IMMEDIATELY
        sendNotification(NotificationHelper.FRIDGE_ALERT_TITLE, message, FridgeConditionsActivity.class, userId);
    }



    public void sendNotificationLocalAndFirebase(String userId, String title, String message, Class<?> targetActivity) {
        // ✅ Store the notification in Firebase (same way as inventory product notifications)
        DatabaseReference userNotificationsRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("notifications");

        String notificationId = userNotificationsRef.push().getKey();
        if (notificationId != null) {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("timestamp", System.currentTimeMillis() / 1000);
            notificationData.put("message", message);
            notificationData.put("title", title);
            userNotificationsRef.child(notificationId).setValue(notificationData);
        }

        // ✅ Send system notification immediately like inventory product notifications
        sendNotification(title, message, targetActivity, userId);
    }


    public void sendExpiryNotification(String productName, long daysLeft) {
        String message;

        if (daysLeft < 0) {
            message = productName + " expired! Throw it away.";
        } else if (daysLeft == 0) {
            message = productName + " expires today! Use it before it's too late.";
        } else if (daysLeft == 1) {
            message = productName + " expires tomorrow! Don't forget to use it.";
        } else {
            message = productName + " expires in " + daysLeft + " days! Consume it soon.";
        }

        // ✅ Use EXPIRY_ALERT_TITLE instead of hardcoded string
        sendNotification(EXPIRY_ALERT_TITLE, message, InventoryActivity.class, productName);
    }
}
