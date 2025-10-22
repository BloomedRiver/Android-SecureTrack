package com.example.securetrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private ProgressBar progressBar;
    private TextView errorMessage;
    private Map<String, Marker> userMarkers = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize views
        progressBar = view.findViewById(R.id.progress_bar);
        errorMessage = view.findViewById(R.id.tv_error_message);

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // ...
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                e.printStackTrace(); // Should not happen
            }
            loadUserData();
        } else {
            requestLocationPermission();
        }
    }


    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    mMap.setMyLocationEnabled(true);
                    loadUserData();
                }
            } else {
                Toast.makeText(requireContext(), "Location permission is required to show your location", 
                        Toast.LENGTH_LONG).show();
                loadUserData(); // Still load other users' data
            }
        }
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) {
            showError("User not authenticated");
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        
        // Load current user's data
        loadCurrentUserData(currentUserId);
        
        // Load trusted contacts
        loadTrustedContacts(currentUserId);
    }

    private void loadCurrentUserData(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                addUserMarker(document, true); // true for current user
                            }
                        }
                        hideProgressBar();
                    }
                });
    }

    private void loadTrustedContacts(String userId) {
        db.collection("users").document(userId)
                .collection("trustedContacts")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                // Get the trusted contact's user ID
                                String trustedUserId = document.getString("userId");
                                if (trustedUserId != null) {
                                    loadTrustedContactData(trustedUserId);
                                }
                            }
                        }
                        hideProgressBar();
                    }
                });
    }

    private void loadTrustedContactData(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                addUserMarker(document, false); // false for trusted contact
                            }
                        }
                    }
                });
    }

    private void addUserMarker(DocumentSnapshot document, boolean isCurrentUser) {
        GeoPoint lastLocation = document.getGeoPoint("lastLocation");
        String userName = document.getString("name");
        Date lastSeen = document.getDate("lastSeen");

        if (lastLocation != null && userName != null) {
            LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            
            // Create marker options
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(userName)
                    .snippet(formatLastSeen(lastSeen));

            // Use different colors for current user vs trusted contacts
            if (isCurrentUser) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            }

            // Add marker to map
            Marker marker = mMap.addMarker(markerOptions);
            if (marker != null) {
                marker.setTag(document.getId());
                userMarkers.put(document.getId(), marker);
            }

            // Move camera to first marker (current user)
            if (isCurrentUser) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
            }
        }
    }

    private String formatLastSeen(Date lastSeen) {
        if (lastSeen == null) {
            return "Last seen: Never";
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return "Last seen: " + sdf.format(lastSeen);
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (errorMessage != null) {
            errorMessage.setText(message);
            errorMessage.setVisibility(View.VISIBLE);
        }
        hideProgressBar();
    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {
        // Handle info window click if needed
        String userId = (String) marker.getTag();
        if (userId != null) {
            Toast.makeText(requireContext(), "Clicked on " + marker.getTitle(), 
                    Toast.LENGTH_SHORT).show();
        }
    }
}
