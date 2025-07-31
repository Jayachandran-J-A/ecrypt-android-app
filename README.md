# ECrypt: An End-to-End Encrypted Android Messenger

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Language](https://img.shields.io/badge/Language-Java-orange.svg)
![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow.svg)
![Cryptography](https://img.shields.io/badge/Crypto-ECC%20%26%20AES-blue.svg)

ECrypt is a fully functional, real-time messaging application for Android that implements robust **End-to-End Encryption (E2EE)** using Elliptic Curve Cryptography (ECC). This project was built to demonstrate the practical application of modern cryptographic principles in a mobile environment. The backend, used for user authentication and message transport, has zero visibility into the content of the messages, ensuring true user privacy.

---

## Core Concept: End-to-End Encryption Flow

The primary goal of this project is to showcase a secure E2EE implementation. Unlike standard messaging apps where the server can often read messages, ECrypt ensures that only the sender and the intended recipient can decrypt and read the conversation.

The security model is built on the following flow:

1.  **Permanent Identity Keys:**
    * Upon registration, each user device generates a permanent **ECC (Elliptic Curve Cryptography) key pair** using the `secp256r1` curve.
    * The **private key**, which is the user's ultimate secret, is stored securely in the **Android Keystore System**. This makes it extremely difficult to extract the key, even with physical access to the device.
    * The corresponding **public key** is published to the user's profile in the Firestore database for other users to retrieve.

2.  **Secure Session Initiation (Key Exchange):**
    * When User A wants to message User B, User A's app fetches User B's public key from Firestore.
    * A secure, temporary shared secret for the session is then established using the **Elliptic Curve Diffie-Hellman (ECDH)** key exchange protocol.
    * This process uses User A's private key and User B's public key to calculate the shared secret. User B performs the same calculation with their private key and User A's public key. Both users arrive at the **exact same secret key** without ever transmitting it over the network.

3.  **Message Encryption & Decryption:**
    * With the shared secret established, all messages are encrypted and decrypted on the device using **AES-256 in GCM mode**.
    * AES-GCM is a modern, authenticated encryption standard that ensures both the **confidentiality** (no one else can read it) and **integrity** (the message hasn't been tampered with) of every message.
    * The encrypted ciphertext is what gets sent to the Firebase backend. The server only sees and stores this encrypted data, never the plain text.

---

## Features

* **Secure User Authentication:** User registration and login handled by Firebase Authentication.
* **End-to-End Encrypted Real-time Chat:** Live messaging between two users with strong E2EE.
* **Contact List & User Search:** View a list of all registered users and search for specific users by email.
* **Message Delivery Status:** Messages display a clock icon when sending and a single tick icon once successfully delivered to the server.
* **Secure Key Storage:** Private keys are protected by the hardware-backed Android Keystore.

---

## Tech Stack

* **Language:** Java
* **Platform:** Android SDK (Min API 26)
* **Backend:** Firebase
    * **Firebase Authentication:** For managing user accounts.
    * **Cloud Firestore:** As a real-time database for user data and encrypted message transport.
* **Cryptography:**
    * Java Cryptography Architecture (JCA)
    * **ECC** for asymmetric key pairs.
    * **ECDH** for the key exchange protocol.
    * **AES-256 GCM** for symmetric message encryption.
* **Architecture:** Model-View-Adapter pattern with `RecyclerView`.

---

## Screenshots

*(Here you can add screenshots of your app. For example:)*

| Login Screen                        | Contact List & Search                   | Chat Screen                    |
| :---------------------------------- | :-------------------------------------- | :----------------------------- |
| *(Insert your login_screen.png here)* | *(Insert your contact_list.png here)* | *(Insert your chat_screen.png here)* |

---

## Setup & Installation

To clone and run this project yourself, you will need to set up your own Firebase project.

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/your-username/ecrypt-android-app.git](https://github.com/your-username/ecrypt-android-app.git)
    ```

2.  **Open in Android Studio:** Open the cloned folder in Android Studio.

3.  **Create a Firebase Project:**
    * Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
    * Add an Android app to the project with the package name `com.cryptosecurity.echocrypt`.
    * Download the `google-services.json` file.

4.  **Add Configuration File:**
    * Place the downloaded `google-services.json` file into the `app/` directory of the project. **This file should not be committed to Git.**

5.  **Enable Firebase Services:**
    * In the Firebase Console, enable **Authentication** (with the "Email/Password" provider).
    * Enable the **Cloud Firestore** database.

6.  **Build and Run:** Build and run the application on an emulator or a physical device. You will need to register at least two different users to test the chat functionality.

---

## Future Improvements

* **Double Tick "Seen" Status:** Implement logic to track when a message has been seen by the recipient.
* **Group Chat:** Extend the E2EE protocol to support secure group conversations.
* **Profile Pictures:** Allow users to upload and set profile pictures.

---

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
