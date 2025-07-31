package com.cryptosecurity.echocrypt.crypto;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class KeyManager {

    private static final String KEY_ALIAS = "EchoCryptKeyAlias";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TAG = "KeyManager";

    private KeyStore keyStore;

    public KeyManager() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            Log.e(TAG, "Failed to initialize KeyStore", e);
        }
    }

    /**
     * Generates a new ECC public/private key pair and stores it securely in the Android Keystore.
     * This should be called once when a user registers.
     */
    public void generateKeyPair() {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE);

                KeyGenParameterSpec parameterSpec = new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY | KeyProperties.PURPOSE_AGREE_KEY)
                        .setAlgorithmParameterSpec(new java.security.spec.ECGenParameterSpec("secp256r1")) // Standard ECC curve
                        .setUserAuthenticationRequired(false) // For simplicity, we don't require biometrics
                        .build();

                keyPairGenerator.initialize(parameterSpec);
                keyPairGenerator.generateKeyPair();
                Log.d(TAG, "Key pair generated successfully.");
            } else {
                Log.d(TAG, "Key pair already exists.");
            }
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | KeyStoreException e) {
            Log.e(TAG, "Failed to generate key pair", e);
        }
    }

    /**
     * Retrieves the user's private key from the Android Keystore.
     * @return The PrivateKey object, or null if an error occurs.
     */
    public PrivateKey getPrivateKey() {
        try {
            return (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            Log.e(TAG, "Failed to get private key", e);
            return null;
        }
    }

    /**
     * Retrieves the user's public key from the Android Keystore.
     * @return The PublicKey object, or null if an error occurs.
     */
    public PublicKey getPublicKey() {
        try {
            return keyStore.getCertificate(KEY_ALIAS).getPublicKey();
        } catch (KeyStoreException e) {
            Log.e(TAG, "Failed to get public key", e);
            return null;
        }
    }

    /**
     * Retrieves the public key and encodes it as a Base64 string.
     * This format is safe to store in Firestore.
     * @return The Base64 encoded public key string, or null on failure.
     */
    public String getPublicKeyAsString() {
        PublicKey publicKey = getPublicKey();
        if (publicKey != null) {
            return Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT);
        }
        return null;
    }
}
