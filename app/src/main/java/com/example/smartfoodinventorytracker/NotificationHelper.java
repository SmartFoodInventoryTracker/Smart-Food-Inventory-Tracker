package com.example.smartfoodinventorytracker;

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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import androidx.work.WorkManager;
import androidx.work.PeriodicWorkRequest;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

public class NotificationHelper {

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

    public NotificationHelper(Context context, boolean startExpiryCheck) {
        this.context = context;
        this.databaseRef = FirebaseDatabase.getInstance().getReference("notifications");
        createNotificationChannel();

        if (startExpiryCheck) {
            scheduleExpiryNotificationCheck(); // ✅ Only starts expiry checks when needed
        }
    }

    public void scheduleExpiryNotificationCheck() {
        WorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ExpiryWorker.class, // ✅ Create this worker inside NotificationHelper (Step 3)
                15, TimeUnit.MINUTES) // ✅ Android enforces a minimum of 15 minutes for periodic tasks
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "ExpiryNotificationWorker",
                ExistingPeriodicWorkPolicy.UPDATE,
                (PeriodicWorkRequest) workRequest
        );
    }
    public static class ExpiryWorker extends Worker {
        public ExpiryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext(), true); // ✅ Start expiry check
            DatabaseHelper.checkExpiryNotifications(notificationHelper); // ✅ Run expiry check
            return Result.success();
        }
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
        isNotificationAlreadySent(title, message, new NotificationCallback() {
            @Override
            public void onCheckCompleted(boolean allowNotification) {
                if (!allowNotification) {
                    return; // ✅ Skip sending duplicate notifications
                }

                storeNotificationInFirebase(title, message); // ✅ Store in Firebase before sending

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            }
        });
    }

    public interface NotificationCallback {
        void onCheckCompleted(boolean allowNotification);
    }


    private void isNotificationAlreadySent(String title, String message, NotificationCallback callback) {
        long oneHourAgo = (System.currentTimeMillis() / 1000) - 3600; // ✅ Get timestamp from 1 hour ago

        databaseRef.orderByChild("timestamp").startAt(oneHourAgo) // ✅ Fetch only recent notifications
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean allowNotification = true;

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String storedTitle = child.child("title").getValue(String.class);
                            String storedMessage = child.child("message").getValue(String.class);
                            Long storedTimestamp = child.child("timestamp").getValue(Long.class);

                            if (storedTitle != null && storedMessage != null && storedTimestamp != null) {
                                // ✅ Check for exact duplicate message within the last hour
                                if (storedTitle.equals(title) && storedMessage.equals(message)) {
                                    allowNotification = false;
                                    break; // ✅ Stop checking once we find a match
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

    public void sendConditionNotification(String type, Long value) {
        String unit;

        switch (type) {
            case "Temperature":
                unit = "°C";
                break;
            case "Humidity":
                unit = "%";
                break;
            case "CO Level":
            case "LPG Level":
            case "Smoke Level":
                unit = " ppm";
                break;
            default:
                unit = "";
        }

        // ✅ Use FRIDGE_ALERT_TITLE instead of hardcoded string
        sendNotification(
                FRIDGE_ALERT_TITLE,
                type + " changed! Current: " + value + unit,
                FridgeConditions.class,
                ""
        );
    }

    public void sendExpiryNotification(String productName, long daysLeft) {
        String message;

        if (daysLeft == 0) {
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
