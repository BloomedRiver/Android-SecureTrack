package com.example.securetrack;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            
            String action = remoteMessage.getData().get("action");
            if ("RING_ALARM".equals(action)) {
                handleAlarmNotification(remoteMessage);
            }
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        
        // Send the new token to your server
        sendRegistrationToServer(token);
    }

    private void handleAlarmNotification(RemoteMessage remoteMessage) {
        // Create notification channel for Android O and above
        createNotificationChannel();

        // Create intent for when notification is tapped
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Build notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_person) // Use your app icon
                .setContentTitle("Emergency Alert")
                .setContentText("You have received an emergency alarm!")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        // Show notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

        // You can also trigger additional actions here like:
        // - Play alarm sound
        // - Vibrate device
        // - Show full-screen alert
        // - Send location data
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Alarm Notifications";
            String description = "Channel for emergency alarm notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendRegistrationToServer(String token) {
        Log.d(TAG, "Attempting to send FCM token to server: " + token);

        // Delegate to static helper for reuse from other components
        updateUserFcmToken(token);
    }

    /**
     * Updates the authenticated user's Firestore document with the provided FCM token.
     * Safe to call from anywhere (no context required). Uses merge to avoid overwriting fields.
     */
    public static void updateUserFcmToken(String token) {
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "Cannot update FCM token: token is null or empty");
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "No authenticated user; skipping FCM token update");
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> update = new HashMap<>();
        update.put("fcmToken", token);

        db.collection("users").document(uid)
                .set(update, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated for user: " + uid))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM token for user: " + uid, e));
    }
}
