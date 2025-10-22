package com.example.securetrack;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class SecureTrackApplication extends Application {
    private static final String TAG = "SecureTrackApp";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase", e);
        }
    }
}
