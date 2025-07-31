package com.cryptosecurity.echocrypt.models;

public class User {
    private String email;
    private String uid;
    private String publicKey;

    // IMPORTANT: A no-argument constructor is required for Firestore deserialization
    public User() {}

    public User(String email, String uid, String publicKey) {
        this.email = email;
        this.uid = uid;
        this.publicKey = publicKey;
    }

    // --- Getters and Setters ---
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}