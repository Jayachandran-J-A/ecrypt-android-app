package com.cryptosecurity.echocrypt.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.cryptosecurity.echocrypt.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // You can create a simple layout for this if you want,
        // but for now, we don't even need to set a content view.

        // Use a Handler to delay the screen transition slightly,
        // which is a common practice for splash screens.
        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // User is signed in, go to the main activity
                startActivity(new Intent(SplashActivity.this, ContactListActivity.class));
            } else {
                // No user is signed in, go to the login activity
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            // Close the splash activity so the user can't go back to it
            finish();
        }, 1000); // 1 second delay
    }
}
