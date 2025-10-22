package com.example.securetrack;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.Timestamp;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "location_service_channel";
    private static final int NOTIFICATION_ID = 1;
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private boolean isLocationUpdatesActive = false;
    
    private final IBinder binder = new LocationServiceBinder();
    
    public class LocationServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService created");
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Create location callback
        createLocationCallback();
        
        // Create notification channel
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationService started");
        
        // Create and start foreground notification
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Start location updates if permissions are granted
        if (checkLocationPermissions()) {
            startLocationUpdates();
        } else {
            Log.w(TAG, "Location permissions not granted");
        }
        
        return START_STICKY; // Service will be restarted if killed
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LocationService destroyed");
        stopLocationUpdates();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Channel for location service");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SecureTrack Location Service")
            .setContentText("Tracking your location for security")
            .setSmallIcon(R.drawable.ic_person) // You may need to add this icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
    
    private boolean checkLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d(TAG, "Location received: " + location.getLatitude() + ", " + location.getLongitude());
                    updateUserLocationInFirestore(location);
                }
            }
        };
    }
    
    private void startLocationUpdates() {
        if (!checkLocationPermissions()) {
            Log.w(TAG, "Cannot start location updates: permissions not granted");
            return;
        }
        
        LocationRequest locationRequest = new LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 seconds interval
            .setMinUpdateIntervalMillis(5000) // Minimum 5 seconds between updates
            .setMaxUpdateDelayMillis(15000) // Maximum 15 seconds delay
            .build();
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            );
            isLocationUpdatesActive = true;
            Log.d(TAG, "Location updates started");
        }
    }
    
    private void stopLocationUpdates() {
        if (isLocationUpdatesActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isLocationUpdatesActive = false;
            Log.d(TAG, "Location updates stopped");
        }
    }
    
    private void updateUserLocationInFirestore(Location location) {
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "No authenticated user found");
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        Timestamp timestamp = Timestamp.now();
        
        // Update the user document in Firestore
        db.collection("users").document(userId)
            .update("lastLocation", geoPoint, "lastSeen", timestamp)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User location updated successfully in Firestore");
                    } else {
                        Log.e(TAG, "Failed to update user location in Firestore", task.getException());
                    }
                }
            });
    }
    
    // Public methods for controlling the service
    public void startLocationTracking() {
        if (checkLocationPermissions()) {
            startLocationUpdates();
        } else {
            Log.w(TAG, "Cannot start location tracking: permissions not granted");
        }
    }
    
    public void stopLocationTracking() {
        stopLocationUpdates();
    }
    
    public boolean isLocationUpdatesActive() {
        return isLocationUpdatesActive;
    }
    
    public void requestCurrentLocation() {
        if (!checkLocationPermissions()) {
            Log.w(TAG, "Cannot get current location: permissions not granted");
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Location location = task.getResult();
                            Log.d(TAG, "Current location retrieved: " + location.getLatitude() + ", " + location.getLongitude());
                            updateUserLocationInFirestore(location);
                        } else {
                            Log.e(TAG, "Failed to get current location", task.getException());
                        }
                    }
                });
        }
    }
}
