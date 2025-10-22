package com.example.securetrack;

import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class AlarmUtils {
    private static final String TAG = "AlarmUtils";
    private static FirebaseFunctions functions = FirebaseFunctions.getInstance();

    /**
     * Sends an alarm notification to a target user
     * @param targetUserId The ID of the user to send the alarm to
     * @return Task that resolves to the message ID if successful
     */
    public static Task<String> sendAlarm(String targetUserId) {
        Map<String, Object> data = new HashMap<>();
        data.put("targetUserId", targetUserId);

        return functions.getHttpsCallable("sendAlarm")
                .call(data)
                // Add the annotation right here, above the 'continueWith' block
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    @SuppressWarnings("unchecked") // <--- ADD THIS ANNOTATION HERE
                    public String then(Task<HttpsCallableResult> task) throws Exception {
                        if (task.isSuccessful()) {
                            Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                            boolean success = (Boolean) result.get("success");

                            if (success) {
                                String messageId = (String) result.get("messageId");
                                Log.d(TAG, "Alarm sent successfully. Message ID: " + messageId);
                                return messageId;
                            } else {
                                String error = (String) result.get("error");
                                Log.e(TAG, "Failed to send alarm: " + error);
                                throw new Exception("Failed to send alarm: " + error);
                            }
                        } else {
                            Log.e(TAG, "Error calling sendAlarm function", task.getException());
                            throw task.getException();
                        }
                    }
                });
    }

    /**
     * Sends an alarm notification to a target user with callback
     * @param targetUserId The ID of the user to send the alarm to
     * @param callback Callback to handle the result
     */
    public static void sendAlarm(String targetUserId, AlarmCallback callback) {
        sendAlarm(targetUserId)
                .addOnSuccessListener(messageId -> {
                    if (callback != null) {
                        callback.onSuccess(messageId);
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Error sending alarm", exception);
                    if (callback != null) {
                        callback.onFailure(exception);
                    }
                });
    }

    /**
     * Callback interface for alarm sending results
     */
    public interface AlarmCallback {
        void onSuccess(String messageId);
        void onFailure(Exception exception);
    }
}
