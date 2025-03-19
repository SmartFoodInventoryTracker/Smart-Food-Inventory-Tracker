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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import androidx.work.WorkManager;
import androidx.work.PeriodicWorkRequest;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class NotificationHelper {

    // ✅ Define public static variables for notification titles
    public static final String FRIDGE_ALERT_TITLE = "Fridge Alert ❄️";
    public static final String EXPIRY_ALERT_TITLE = "Food Expiry 🥛";

    private static final String CHANNEL_ID = "CHANNEL_ID_NOTIFICATION";
    private static final String CHANNEL_NAME = "Smart Food Alerts";
    private static final String CHANNEL_DESCRIPTION = "Notifications for fridge and inventory alerts";
    private final Context context;
    private final DatabaseReference databaseRef;

    public NotificationHelper(Context context, boolean startExpiryCheck) {
        this.context = context;
        this.databaseRef = FirebaseDatabase.getInstance().getReference("notifications");
        createNotificationChannel();

        if (startExpiryCheck) {
            scheduleExpiryNotificationCheck(); // ✅ Only start expiry checks when requested
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
        storeNotificationInFirebase(title, message);

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
