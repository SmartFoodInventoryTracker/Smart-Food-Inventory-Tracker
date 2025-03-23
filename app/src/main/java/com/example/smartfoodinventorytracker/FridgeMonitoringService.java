package com.example.smartfoodinventorytracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class FridgeMonitoringService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, createServiceNotification());

        // ✅ Start listening to fridge changes
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext(), false, userId);
            DatabaseHelper.listenToInventoryChanges(getApplicationContext(), notificationHelper);
        } else {
            Log.w("FridgeMonitoringService", "No authenticated user. Monitoring skipped.");
        }

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
