package com.cryptosecurity.echocrypt.crypto;

import android.util.Base64;
import android.util.Log;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper {

    private static final String TAG = "EncryptionHelper";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 12 bytes is recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // In bits

    /**
     * Converts a Base64 encoded public key string back into a PublicKey object.
     * @param publicKeyString The Base64 string from Firestore.
     * @return A PublicKey object.
     */
    public static PublicKey getPublicKeyFromString(String publicKeyString) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.decode(publicKeyString, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(spec);
    }

    /**
     * Generates a shared secret key using Elliptic Curve Diffie-Hellman (ECDH).
     * @param myPrivateKey The current user's private key.
     * @param theirPublicKey The other user's public key.
     * @return A SecretKey that can be used for symmetric encryption (AES).
     */
    public static SecretKey generateSharedSecret(PrivateKey myPrivateKey, PublicKey theirPublicKey) {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(myPrivateKey);
            keyAgreement.doPhase(theirPublicKey, true);
            byte[] sharedSecret = keyAgreement.generateSecret();
            // Using the raw shared secret as an AES key is okay here, but for production,
            // you might run it through a KDF (Key Derivation Function) like HKDF.
            return new SecretKeySpec(sharedSecret, 0, 32, "AES"); // Use first 256 bits (32 bytes) for AES-256
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Log.e(TAG, "Failed to generate shared secret", e);
            return null;
        }
    }

    /**
     * Encrypts a plain text message using AES/GCM with the shared secret.
     * @param plainText The message to encrypt.
     * @param secretKey The shared secret key.
     * @return A Base64 encoded string containing the IV and the ciphertext.
     */
    public static String encrypt(String plainText, SecretKey secretKey) {
        try {
            // Generate a random Initialization Vector (IV)
            byte[] iv = new byte[GCM_IV_LENGTH];
            new java.security.SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes());

            // Prepend the IV to the ciphertext for use during decryption
            byte[] ivAndCipherText = new byte[GCM_IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, ivAndCipherText, 0, GCM_IV_LENGTH);
            System.arraycopy(cipherText, 0, ivAndCipherText, GCM_IV_LENGTH, cipherText.length);

            return Base64.encodeToString(ivAndCipherText, Base64.DEFAULT);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            Log.e(TAG, "Encryption failed", e);
            return null;
        }
    }

    /**
     * Decrypts a message using AES/GCM.
     * @param encryptedString The Base64 string containing the IV and ciphertext.
     * @param secretKey The shared secret key.
     * @return The original plain text message.
     */
    public static String decrypt(String encryptedString, SecretKey secretKey) {
        try {
            byte[] ivAndCipherText = Base64.decode(encryptedString, Base64.DEFAULT);

            // Extract the IV from the beginning of the byte array
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(ivAndCipherText, 0, iv, 0, GCM_IV_LENGTH);

            // Extract the ciphertext
            byte[] cipherText = new byte[ivAndCipherText.length - GCM_IV_LENGTH];
            System.arraycopy(ivAndCipherText, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] decryptedText = cipher.doFinal(cipherText);
            return new String(decryptedText);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            Log.e(TAG, "Decryption failed", e);
            // Return a placeholder to indicate decryption failure
            return "[Decryption Error]";
        }
    }
}