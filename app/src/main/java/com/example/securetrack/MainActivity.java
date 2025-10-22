package com.example.securetrack;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private LocationService locationService;
    private boolean isBound = false;
    private TextView statusText;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocationServiceBinder binder = (LocationService.LocationServiceBinder) service;
            locationService = binder.getService();
            isBound = true;
            updateStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            locationService = null;
            updateStatus();
        }
    };

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

        statusText = findViewById(R.id.statusText);

        Button startButton = findViewById(R.id.startTrackingButton);
        Button stopButton = findViewById(R.id.stopTrackingButton);
        Button currentButton = findViewById(R.id.getCurrentLocationButton);

        if (startButton != null) {
            startButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, LocationService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                bindService(new Intent(this, LocationService.class), connection, Context.BIND_AUTO_CREATE);
                Toast.makeText(this, "Starting location tracking", Toast.LENGTH_SHORT).show();
                updateStatus();
            });
        }

        if (stopButton != null) {
            stopButton.setOnClickListener(v -> {
                if (isBound) {
                    unbindService(connection);
                    isBound = false;
                }
                stopService(new Intent(this, LocationService.class));
                Toast.makeText(this, "Stopping location tracking", Toast.LENGTH_SHORT).show();
                updateStatus();
            });
        }

        if (currentButton != null) {
            currentButton.setOnClickListener(v -> {
                if (isBound && locationService != null) {
                    locationService.requestCurrentLocation();
                    Toast.makeText(this, "Requested current location", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Try to bind if service is already running
        bindService(new Intent(this, LocationService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    private void updateStatus() {
        if (statusText == null) return;
        if (!isBound || locationService == null) {
            statusText.setText("Location service not bound");
        } else {
            statusText.setText(locationService.isLocationUpdatesActive()
                    ? "Location updates active"
                    : "Location updates inactive");
        }
    }
}