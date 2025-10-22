package com.example.securetrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class TrustedContactsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewContacts;
    private EditText editTextInvitationCode;
    private Button buttonAddContact;
    private TrustedContactsAdapter adapter;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trusted_contacts);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Set click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        editTextInvitationCode = findViewById(R.id.editTextInvitationCode);
        buttonAddContact = findViewById(R.id.buttonAddContact);
    }

    private void setupRecyclerView() {
        if (mAuth.getCurrentUser() != null) {
            adapter = new TrustedContactsAdapter(this, mAuth.getCurrentUser().getUid());
            recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewContacts.setAdapter(adapter);
        }
    }

    private void setupClickListeners() {
        buttonAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTrustedContact();
            }
        });
    }

    private void addTrustedContact() {
        String invitationCode = editTextInvitationCode.getText().toString().trim();
        
        if (invitationCode.isEmpty()) {
            editTextInvitationCode.setError("Please enter an invitation code");
            editTextInvitationCode.requestFocus();
            return;
        }

        // For now, we'll create a simple implementation
        // In a real app, you would validate the invitation code and get the user's details
        String trustedContactUid = "temp_uid_" + System.currentTimeMillis();
        String trustedContactName = "Contact from code: " + invitationCode;

        if (mAuth.getCurrentUser() != null) {
            InvitationUtils.addTrustedContactForCurrentUser(
                trustedContactUid,
                trustedContactName,
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(TrustedContactsActivity.this, 
                            "Trusted contact added successfully!", Toast.LENGTH_SHORT).show();
                        editTextInvitationCode.setText("");
                    }
                },
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(TrustedContactsActivity.this, 
                            "Failed to add trusted contact: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}