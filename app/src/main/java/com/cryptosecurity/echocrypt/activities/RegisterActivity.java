package com.cryptosecurity.echocrypt.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cryptosecurity.echocrypt.R;
import com.cryptosecurity.echocrypt.crypto.KeyManager; // NEW: Import KeyManager
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    // ... (variables are the same)
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonRegister;
    private TextView textViewLoginLink;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLoginLink = findViewById(R.id.textViewLoginLink);
        progressBar = findViewById(R.id.progressBar);

        buttonRegister.setOnClickListener(v -> registerUser());
        textViewLoginLink.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // ... (input validation is the same)
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() || password.isEmpty() || password.length() < 6) {
            // Show appropriate errors...
            if (email.isEmpty()) editTextEmail.setError("Email is required");
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) editTextEmail.setError("Please enter a valid email");
            if (password.isEmpty()) editTextPassword.setError("Password is required");
            if (password.length() < 6) editTextPassword.setError("Password should be at least 6 characters");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // NEW: Generate and store the cryptographic keys
                        KeyManager keyManager = new KeyManager();
                        keyManager.generateKeyPair();
                        String publicKeyString = keyManager.getPublicKeyAsString();

                        if (publicKeyString == null) {
                            Toast.makeText(this, "Critical error: Could not generate keys.", Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                            buttonRegister.setEnabled(true);
                            return;
                        }

                        String userId = mAuth.getCurrentUser().getUid();

                        Map<String, Object> user = new HashMap<>();
                        user.put("email", email);
                        user.put("uid", userId);
                        user.put("publicKey", publicKeyString); // NEW: Save the public key

                        db.collection("users").document(userId)
                                .set(user)
                                .addOnCompleteListener(saveTask -> {
                                    progressBar.setVisibility(View.GONE);
                                    buttonRegister.setEnabled(true);
                                    if(saveTask.isSuccessful()){
                                        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(RegisterActivity.this, ContactListActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Failed to save user data.", Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                        buttonRegister.setEnabled(true);
                    }
                });
    }
}