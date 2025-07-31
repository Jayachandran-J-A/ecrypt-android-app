package com.cryptosecurity.echocrypt.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cryptosecurity.echocrypt.R;
import com.cryptosecurity.echocrypt.adapters.ChatAdapter;
import com.cryptosecurity.echocrypt.crypto.EncryptionHelper;
import com.cryptosecurity.echocrypt.crypto.KeyManager;
import com.cryptosecurity.echocrypt.models.ChatMessage;
import com.cryptosecurity.echocrypt.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

public class ChatActivity extends AppCompatActivity {
    // ... (variables are the same)
    private RecyclerView recyclerViewChat;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private Toolbar toolbar;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private Map<String, ChatMessage> messageMap; // NEW: Use a Map for efficient updates
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private String receiverId;
    private String chatRoomId;
    private KeyManager keyManager;
    private SecretKey sharedSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // ... (initialization is the same)
        receiverId = getIntent().getStringExtra("USER_ID");
        String receiverEmail = getIntent().getStringExtra("USER_EMAIL");
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        keyManager = new KeyManager();
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(receiverEmail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonSend.setEnabled(false);

        messageList = new ArrayList<>();
        messageMap = new HashMap<>(); // NEW: Initialize the map
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);
        chatRoomId = getChatRoomId(currentUserId, receiverId);
        buttonSend.setOnClickListener(v -> sendMessage());
        initSecureSession();
    }

    private void initSecureSession() {
        // ... (this method is the same)
        db.collection("users").document(receiverId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User receiver = documentSnapshot.toObject(User.class);
                    if (receiver != null && receiver.getPublicKey() != null && !receiver.getPublicKey().isEmpty()) {
                        try {
                            PublicKey theirPublicKey = EncryptionHelper.getPublicKeyFromString(receiver.getPublicKey());
                            PrivateKey myPrivateKey = keyManager.getPrivateKey();
                            if (myPrivateKey == null) {
                                Toast.makeText(this, "Error: Could not retrieve your private key.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            sharedSecret = EncryptionHelper.generateSharedSecret(myPrivateKey, theirPublicKey);
                            if (sharedSecret != null) {
                                buttonSend.setEnabled(true);
                                listenForMessages();
                            } else {
                                Toast.makeText(this, "Error: Could not create secure session.", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e("ChatActivity", "Key exchange failed", e);
                            Toast.makeText(this, "Error: Key exchange failed.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Error: Could not retrieve receiver's public key.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to get user details.", Toast.LENGTH_SHORT).show());
    }

    private void sendMessage() {
        // ... (this method is the same)
        String messageText = editTextMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText) || sharedSecret == null) {
            if (sharedSecret == null) Toast.makeText(this, "Secure session not ready.", Toast.LENGTH_SHORT).show();
            return;
        }
        String encryptedMessage = EncryptionHelper.encrypt(messageText, sharedSecret);
        if (encryptedMessage == null) {
            Toast.makeText(this, "Encryption failed.", Toast.LENGTH_SHORT).show();
            return;
        }
        ChatMessage chatMessage = new ChatMessage(encryptedMessage, currentUserId, receiverId);
        editTextMessage.setText("");
        db.collection("chats").document(chatRoomId)
                .collection("messages")
                .add(chatMessage)
                .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed to send message.", Toast.LENGTH_SHORT).show());
    }

    private void listenForMessages() {
        CollectionReference messagesRef = db.collection("chats").document(chatRoomId).collection("messages");

        messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || sharedSecret == null) return;

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            String docId = dc.getDocument().getId();
                            ChatMessage encryptedMessage = dc.getDocument().toObject(ChatMessage.class);
                            String decryptedText = EncryptionHelper.decrypt(encryptedMessage.getMessage(), sharedSecret);
                            encryptedMessage.setMessage(decryptedText);

                            switch (dc.getType()) {
                                case ADDED:
                                    messageMap.put(docId, encryptedMessage);
                                    break;
                                case MODIFIED:
                                    // NEW: Handle the modification. This happens when the server
                                    // adds the timestamp to our optimistically sent message.
                                    messageMap.put(docId, encryptedMessage);
                                    break;
                                case REMOVED:
                                    messageMap.remove(docId);
                                    break;
                            }
                        }

                        // NEW: Rebuild the list from the map and sort it.
                        messageList.clear();
                        messageList.addAll(messageMap.values());
                        Collections.sort(messageList, (o1, o2) -> {
                            if (o1.getTimestamp() == null) return 1; // Put sending messages at the end
                            if (o2.getTimestamp() == null) return -1;
                            return o1.getTimestamp().compareTo(o2.getTimestamp());
                        });

                        chatAdapter.notifyDataSetChanged();
                        recyclerViewChat.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    // ... (getChatRoomId and onSupportNavigateUp are the same)
    private String getChatRoomId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) > 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
