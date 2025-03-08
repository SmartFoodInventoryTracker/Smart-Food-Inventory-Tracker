package com.example.smartfoodinventorytracker;

import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    private static final DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
    private static final DatabaseReference inventoryRef = FirebaseDatabase.getInstance().getReference("inventory");

    private static Long lastTemperature = null;
    private static Long lastHumidity = null; // ✅ Track last humidity

    public interface NotificationFetchListener {
        void onNotificationsFetched(List<NotificationItem> notifications);
    }

    public interface ConditionChangeListener {
        void onTemperatureChanged(Long newTemperature);
        void onHumidityChanged(Long newHumidity); // ✅ Added humidity callback
    }

    // 🔹 Fetch stored notifications from Firebase
    public static void fetchNotifications(NotificationFetchListener listener) {
        notificationsRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<NotificationItem> notifications = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String message = snapshot.child("message").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    if (message != null && timestamp != null) {
                        notifications.add(new NotificationItem("Fridge Alert ⚠️", message, timestamp));
                    }
                }

                listener.onNotificationsFetched(notifications);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("DatabaseHelper", "Error fetching notifications", error.toException());
            }
        });
    }

    // 🔹 Listen to temperature & humidity changes
    public static void listenToInventoryChanges(ConditionChangeListener listener) {
        inventoryRef.orderByKey().limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot inventorySnapshot : snapshot.getChildren()) {
                    Long newTemperature = inventorySnapshot.child("temperature").getValue(Long.class);
                    Long newHumidity = inventorySnapshot.child("humidity").getValue(Long.class);

                    // ✅ Trigger only if temperature changed
                    if (newTemperature != null && !newTemperature.equals(lastTemperature)) {
                        lastTemperature = newTemperature;
                        listener.onTemperatureChanged(newTemperature);
                    }

                    // ✅ Trigger only if humidity changed
                    if (newHumidity != null && !newHumidity.equals(lastHumidity)) {
                        lastHumidity = newHumidity;
                        listener.onHumidityChanged(newHumidity);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("DatabaseHelper", "Error listening to inventory", error.toException());
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
}
