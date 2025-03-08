package com.example.smartfoodinventorytracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {

    private static final String CHANNEL_ID = "CHANNEL_ID_NOTIFICATION";
    private static final String CHANNEL_NAME = "Smart Food Alerts";
    private static final String CHANNEL_DESCRIPTION = "Notifications for fridge and inventory alerts";

    private final Context context;
    private final DatabaseReference databaseRef;

    public NotificationHelper(Context context) {
        this.context = context;
        this.databaseRef = FirebaseDatabase.getInstance().getReference("notifications");
        createNotificationChannel(); // Initialize channel once
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            );
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
        // Store the notification in Firebase before sending it
        storeNotificationInFirebase(title, message);

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return; // Permission not granted, exit method
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
        notificationManager.notify(0, builder.build());
    }

    private void storeNotificationInFirebase(String title, String message) {
        String notificationId = databaseRef.push().getKey();

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("timestamp", System.currentTimeMillis() / 1000);  // Unix timestamp
        notificationData.put("template", "FridgeAlert");
        notificationData.put("message", message);

        if (notificationId != null) {
            databaseRef.child(notificationId).setValue(notificationData);
        }
    }
}
