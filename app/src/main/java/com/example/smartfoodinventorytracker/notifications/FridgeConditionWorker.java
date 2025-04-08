package com.example.smartfoodinventorytracker.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.smartfoodinventorytracker.fridge_conditions.FridgeConditionsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FridgeConditionWorker extends Worker {

    public FridgeConditionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) return Result.failure();

        Context context = getApplicationContext();
        NotificationHelper helper = new NotificationHelper(context, false, userId);
        SharedPreferences sent = context.getSharedPreferences("notif_times", Context.MODE_PRIVATE);
        SharedPreferences prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE);
        boolean alertsEnabled = prefs.getBoolean("fridge_alerts", true);
        if (!alertsEnabled) {
            helper.scheduleFridgeConditionCheck();
            return Result.success();
        }

        long now = System.currentTimeMillis();

        final boolean[] success = {false};
        final CountDownLatch latch = new CountDownLatch(1);

        DatabaseReference fridgeRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("fridge_condition");

        fridgeRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        Double temp = snap.child("temperature").getValue(Double.class);
                        Integer tempCond = snap.child("temperature condition").getValue(Integer.class);

                        Double hum = snap.child("humidity").getValue(Double.class);
                        Integer humCond = snap.child("humidity condition").getValue(Integer.class);

                        Double co = snap.child("co").getValue(Double.class);
                        Integer coCond = snap.child("co condition").getValue(Integer.class);

                        Double lpg = snap.child("lpg").getValue(Double.class);
                        Integer lpgCond = snap.child("lpg condition").getValue(Integer.class);

                        Double smoke = snap.child("smoke").getValue(Double.class);
                        Integer smokeCond = snap.child("smoke condition").getValue(Integer.class);

                        Map<String, String> alerts = new HashMap<>();

                        addAlertIfNeeded(alerts, "Temperature", temp, tempCond, "°C");
                        addAlertIfNeeded(alerts, "Humidity", hum, humCond, "%");
                        addAlertIfNeeded(alerts, "CO₂", co, coCond, " ppm");
                        addAlertIfNeeded(alerts, "LPG", lpg, lpgCond, " ppm");
                        addAlertIfNeeded(alerts, "NH₃", smoke, smokeCond, " ppm");

                        long lastSent = sent.getLong("fridge_group", 0);
                        if (now - lastSent < 60_000) {
                            helper.scheduleFridgeConditionCheck();
                            latch.countDown();
                            return;
                        }

                        if (!alerts.isEmpty()) {
                            StringBuilder message = new StringBuilder("Fridge condition update:");
                            for (String msg : alerts.values()) {
                                message.append("\n• ").append(msg);
                            }
                            String finalMessage = message.toString();

                            CountDownLatch duplicateCheckLatch = new CountDownLatch(1);
                            final boolean[] allowNotification = {true};

                            helper.isNotificationAlreadySent(NotificationHelper.FRIDGE_ALERT_TITLE, finalMessage, allow -> {
                                allowNotification[0] = allow;
                                duplicateCheckLatch.countDown();
                            });

                            try {
                                duplicateCheckLatch.await(3, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                Log.e("FridgeWorker", "Interrupted while waiting for duplicate check", e);
                                latch.countDown(); // ensure worker doesn't hang
                                return;
                            }

                            if (allowNotification[0]) {
                                helper.sendNotificationLocalAndFirebase(
                                        userId,
                                        NotificationHelper.FRIDGE_ALERT_TITLE,
                                        finalMessage,
                                        FridgeConditionsActivity.class
                                );
                                sent.edit().putLong("fridge_group", now).apply();
                            } else {
                                Log.d("FridgeWorker", "Duplicate notification blocked.");
                            }

                        }

                        success[0] = true;
                    }
                } finally {
                    helper.scheduleFridgeConditionCheck();
                    latch.countDown();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return Result.failure();
        }

        return success[0] ? Result.success() : Result.retry();
    }

    private void addAlertIfNeeded(Map<String, String> alerts, String name, Double value, Integer cond, String unit) {
        if (value == null || cond == null) return;

        String status = cond >= 7 ? "Poor" : cond >= 4 ? "Moderate" : "Good";
        String emoji = cond >= 7 ? "🔴" : cond >= 4 ? "🟠" : "🟢";

        if (cond >= 4) {
            alerts.put(name, emoji + " " + name + " is " + status + " (" + value + unit + ")");
        }
    }
}
