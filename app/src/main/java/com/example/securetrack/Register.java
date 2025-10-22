package com.example.securetrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;
    private Button buttonCreateAccount;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);
        progressBar = findViewById(R.id.progressBar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        buttonCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });
    }

    private void attemptRegister() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editTextName.setError(getString(R.string.name_required));
            editTextName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError(getString(R.string.email_required));
            editTextEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError(getString(R.string.invalid_email));
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError(getString(R.string.password_required));
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError(getString(R.string.password_too_short));
            editTextPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError(getString(R.string.passwords_do_not_match));
            editTextConfirmPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonCreateAccount.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // User registration successful, now create user document in Firestore
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                createUserDocument(user.getUid(), name, email);
                                // Also fetch and store FCM token on successful signup
                                FirebaseMessaging.getInstance().getToken()
                                        .addOnSuccessListener(token -> MyFirebaseMessagingService.updateUserFcmToken(token))
                                        .addOnFailureListener(e -> {
                                            // Non-fatal: token will be refreshed later
                                        });
                            }
                        } else {
                            progressBar.setVisibility(View.GONE);
                            buttonCreateAccount.setEnabled(true);
                            Toast.makeText(Register.this, getString(R.string.signup_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void createUserDocument(String uid, String name, String email) {
        // Create a new user document in Firestore
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("uid", uid);
        user.put("fcmToken", null);

        db.collection("users").document(uid)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                        buttonCreateAccount.setEnabled(true);
                        
                        if (task.isSuccessful()) {
                            Toast.makeText(Register.this, getString(R.string.signup_successful), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(Register.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(Register.this, "Registration successful but failed to save user data", Toast.LENGTH_LONG).show();
                            // Still navigate to main activity since authentication was successful
                            Intent intent = new Intent(Register.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }
}


