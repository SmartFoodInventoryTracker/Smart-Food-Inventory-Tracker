package com.example.smartfoodinventorytracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;

public class FridgeMonitoringService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, createServiceNotification());

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext(), false, userId);

        // ✅ Ensure fridge notifications are triggered
        notificationHelper.triggerPendingFridgeNotifications();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // ✅ Keeps the service running even if the app is closed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "FridgeMonitorChannel",
                    "Fridge Monitoring Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private android.app.Notification createServiceNotification() {
        return new NotificationCompat.Builder(this, "FridgeMonitorChannel")
                .setContentTitle("Fridge Monitoring Active")
                .setContentText("Monitoring fridge conditions in the background")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW) // ✅ Prevents it from being intrusive
                .build(); // ✅ Correctly returns a Notification object
    }
}
