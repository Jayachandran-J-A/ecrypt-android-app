package com.cryptosecurity.echocrypt.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText; // NEW: Import EditText
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cryptosecurity.echocrypt.R;
import com.cryptosecurity.echocrypt.adapters.ContactListAdapter;
import com.cryptosecurity.echocrypt.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ContactListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewContacts;
    private ProgressBar progressBar;
    private ContactListAdapter contactListAdapter;
    private List<User> userList;
    private Toolbar toolbar;
    private EditText editTextSearch; // NEW: Search bar variable

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        progressBar = findViewById(R.id.progressBar);
        editTextSearch = findViewById(R.id.editTextSearch); // NEW: Link search bar

        userList = new ArrayList<>();
        contactListAdapter = new ContactListAdapter(this, userList);
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewContacts.setAdapter(contactListAdapter);

        // NEW: Add a listener to the search bar
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });


        fetchUsers();
    }

    // NEW: Method to filter the user list based on search query
    private void filter(String text) {
        ArrayList<User> filteredList = new ArrayList<>();
        for (User item : userList) {
            if (item.getEmail().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        // Update the adapter with the filtered list
        contactListAdapter = new ContactListAdapter(this, filteredList);
        recyclerViewContacts.setAdapter(contactListAdapter);
    }

    // ... (onCreateOptionsMenu and onOptionsItemSelected are the same)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            Intent intent = new Intent(ContactListActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchUsers() {
        // ... (fetchUsers method remains the same)
        progressBar.setVisibility(View.VISIBLE);
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            if (!user.getUid().equals(currentUserId)) {
                                userList.add(user);
                            }
                        }
                        // Initially, show the full list
                        contactListAdapter = new ContactListAdapter(this, userList);
                        recyclerViewContacts.setAdapter(contactListAdapter);

                    } else {
                        Toast.makeText(ContactListActivity.this, "Error getting users: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}