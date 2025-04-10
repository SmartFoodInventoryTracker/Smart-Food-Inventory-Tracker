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

    public static final String FRIDGE_ALERT_TITLE = "Fridge Alert 🚨";
    public static final String EXPIRY_ALERT_TITLE = "Inventory Alert 🍏";

    private static final String CHANNEL_ID = "CHANNEL_ID_NOTIFICATION";
    private static final String CHANNEL_NAME = "Smart Food Alerts";
    private static final String CHANNEL_DESCRIPTION = "Notifications for fridge and inventory alerts";
    private final Context context;
    private final DatabaseReference databaseRef;
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String LAST_RESET_TIME_KEY = "LastResetTime";
    private static final long FRIDGE_NOTIFICATION_INTERVAL = 30 * 60 * 1000; // 30 minutes

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
        // Cancel any existing expiry jobs
        WorkManager.getInstance(context).cancelAllWorkByTag("expiry_check");

        scheduleOneTimeCheck(); // Schedule a new one
    }


    private void scheduleOneTimeCheck() {
        SharedPreferences settingsPrefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE);
        int value = settingsPrefs.getInt("expired_interval_value", 1);
        String unit = settingsPrefs.getString("expired_interval_unit", "minute(s)");

        SharedPreferences notificationPrefs = context.getSharedPreferences("NotificationPrefs_" + userId, Context.MODE_PRIVATE);
        boolean firstRunDone = notificationPrefs.getBoolean("first_run_done", false);

        long delay;
        TimeUnit timeUnit = TimeUnit.MINUTES;

        if (!firstRunDone) {
            delay = 0;
            notificationPrefs.edit().putBoolean("first_run_done", true).apply();
        } else {
            // Convert all intervals to minutes
            if (unit.equalsIgnoreCase("hour(s)")) {
                delay = value * 60L;
            } else if (unit.equalsIgnoreCase("day(s)")) {
                delay = value * 24L * 60L;
            } else {
                delay = value;
            }
        }

        WorkRequest workRequest = new OneTimeWorkRequest.Builder(ExpiryWorker.class)
                .setInitialDelay(delay, timeUnit)
                .addTag("expiry_check")
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
                NotificationDataHelper.checkExpiryNotifications(userId, notificationHelper);
                notificationHelper.scheduleExpiryNotificationCheck(); // Re-schedule
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
            context.startForegroundService(serviceIntent);
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
        Log.d("NotificationHelper", "Sending notification without duplicate check: " + message);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.e("NotificationHelper", "Missing POST_NOTIFICATIONS permission!");
            return;
        }


        Intent intent = new Intent(context, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
        Log.d("NotificationHelper", "Notification Sent - ID: " + notificationId);
    }

    public void scheduleFridgeConditionCheck() {
        WorkManager.getInstance(context).cancelAllWorkByTag("fridge_condition_check");

        SharedPreferences prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE);
        int value = prefs.getInt("fridge_interval_value", 1);
        String unit = prefs.getString("fridge_interval_unit", "minute(s)");

        long delayMinutes;
        if (unit.equalsIgnoreCase("hour(s)")) {
            delayMinutes = value * 60L;
        } else if (unit.equalsIgnoreCase("day(s)")) {
            delayMinutes = value * 24L * 60L;
        } else {
            delayMinutes = value;
        }

        WorkRequest request = new OneTimeWorkRequest.Builder(FridgeConditionWorker.class)
                .addTag("fridge_condition_check")
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context).enqueue(request);
    }


    public interface NotificationCallback {
        void onCheckCompleted(boolean allowNotification);
    }

    public void triggerPendingFridgeNotifications() {
        Log.d("NotificationHelper", "Checking for pending fridge notifications...");
        databaseRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                    String title = notifSnapshot.child("title").getValue(String.class);
                    String message = notifSnapshot.child("message").getValue(String.class);
                    Long timestamp = notifSnapshot.child("timestamp").getValue(Long.class);

                    if (title == null || message == null || timestamp == null) {
                        continue;
                    }
                    sendNotification(title, message, FridgeConditionsActivity.class, userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("NotificationHelper", "Failed to fetch pending fridge notifications", error.toException());
            }
        });
    }

    public void isNotificationAlreadySent(String title, String message, NotificationCallback callback) {
        // Get the duplicate window from preferences (default to 60 seconds if not set)
        SharedPreferences settingsPrefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE);
        int duplicateWindowInSeconds = settingsPrefs.getInt("duplicate_window_seconds", 30);

        long windowStart = (System.currentTimeMillis() / 1000) - duplicateWindowInSeconds;

        databaseRef.orderByChild("timestamp").startAt(windowStart)
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
                                    Log.d("NotificationHelper", "Duplicate found in Firebase. Skipping...");
                                    allowNotification = false;
                                    break;
                                }
                            }
                        }
                        callback.onCheckCompleted(allowNotification);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onCheckCompleted(true); // Allow notification if there's an error
                    }
                });
    }




    private void storeNotificationInFirebase(String title, String message) {
        String notificationId = databaseRef.push().getKey();
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("timestamp", System.currentTimeMillis() / 1000);
        notificationData.put("message", message);
        notificationData.put("title", title);
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
        if (condition < 4) {
            Log.d("FridgeMonitor", "Condition is safe, skipping notification.");
            return;
        }

        String severity = (condition >= 9) ? "🔴 CRITICAL" : "🟠 WARNING";
        String displayName;
        switch (type) {
            case "CO Level": displayName = "CO₂ Level"; break;
            case "Smoke Level": displayName = "NH₃ Level"; break;
            default: displayName = type;
        }

        String message = severity + " - " + displayName + " changed! Current: " + value + unit;
        Log.d("FridgeMonitor", "Sending Notification - " + message);
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
        sendNotification(NotificationHelper.FRIDGE_ALERT_TITLE, message, FridgeConditionsActivity.class, "");
    }

    public void sendNotificationLocalAndFirebase(String userId, String title, String message, Class<?> targetActivity) {
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
        sendNotification(title, message, targetActivity, "");
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
        sendNotification(EXPIRY_ALERT_TITLE, message, InventoryActivity.class, "");
    }
}
