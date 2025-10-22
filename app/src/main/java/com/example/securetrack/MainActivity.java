package com.example.securetrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private Button startTrackingButton;
    private Button stopTrackingButton;
    private Button getCurrentLocationButton;
    private Button mapButton;
    private Button trustedContactsButton;
    private Button settingsButton;
    private TextView statusText;
    
    private LocationService locationService;
    private boolean isServiceBound = false;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize views
        initializeViews();
        
        // Set up click listeners
        setupClickListeners();
        
        // Check if user is authenticated
        checkAuthentication();
        
        // Update status
        updateStatus("Ready to start location tracking");
    }

    private void initializeViews() {
        startTrackingButton = findViewById(R.id.startTrackingButton);
        stopTrackingButton = findViewById(R.id.stopTrackingButton);
        getCurrentLocationButton = findViewById(R.id.getCurrentLocationButton);
        statusText = findViewById(R.id.statusText);
        
        // Add new buttons
        mapButton = findViewById(R.id.mapButton);
        trustedContactsButton = findViewById(R.id.trustedContactsButton);
        settingsButton = findViewById(R.id.settingsButton);
    }

    private void setupClickListeners() {
        startTrackingButton.setOnClickListener(v -> startLocationTracking());
        stopTrackingButton.setOnClickListener(v -> stopLocationTracking());
        getCurrentLocationButton.setOnClickListener(v -> getCurrentLocation());
        
        if (mapButton != null) {
            mapButton.setOnClickListener(v -> openMapActivity());
        }
        
        if (trustedContactsButton != null) {
            trustedContactsButton.setOnClickListener(v -> openTrustedContactsActivity());
        }
        
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> openSettingsActivity());
        }
    }

    private void checkAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User not authenticated, redirect to login
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
        }
    }

    private void startLocationTracking() {
        if (!checkLocationPermissions()) {
            requestLocationPermissions();
            return;
        }

        Intent serviceIntent = new Intent(this, LocationService.class);
        startForegroundService(serviceIntent);
        updateStatus("Location tracking started");
        Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show();
    }

    private void stopLocationTracking() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
        updateStatus("Location tracking stopped");
        Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show();
    }

    private void getCurrentLocation() {
        if (!checkLocationPermissions()) {
            requestLocationPermissions();
            return;
        }

        Intent serviceIntent = new Intent(this, LocationService.class);
        startForegroundService(serviceIntent);
        updateStatus("Getting current location...");
        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();
    }

    private void openMapActivity() {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    private void openTrustedContactsActivity() {
        Intent intent = new Intent(this, TrustedContactsActivity.class);
        startActivity(intent);
    }

    private void openSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private boolean checkLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LocationPermissionHelper.LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationPermissionHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateStatus("Location permissions granted");
                Toast.makeText(this, "Location permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                updateStatus("Location permissions denied");
                Toast.makeText(this, "Location permissions are required for this app to work properly", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateStatus(String message) {
        if (statusText != null) {
            statusText.setText(message);
        }
    }
}