# 🔐 SecureVault – Password Manager

A JavaFX desktop password manager with AES-256-GCM encryption.

## Features

### Mandatory Features ✅
| Feature | Implementation |
|---|---|
| **Secure file handling** | Vault created on first launch; AES-256-GCM encrypted on close; decrypted on login |
| **Add password entry** | Title, password (AES-encrypted), URL, and notes |
| **Search passwords** | Case-insensitive substring search by title; password not shown |
| **Update password** | Edit any field; re-encrypts password with AES |
| **Delete entry** | Removes entry and re-saves encrypted vault |

### Optional Features ✅
| Feature | Implementation |
|---|---|
| **User registration & login** | PBKDF2WithHmacSHA256, 310 000 iterations, per-user vault files |
| **File encryption on login/logout** | Vault decrypted on login, encrypted on logout or window close |
| **Password generator** | Cryptographically secure (SecureRandom), configurable length (8–64) |
| **Secure reveal** | Password shown only when "👁 Reveal" is clicked |
| **Copy to clipboard** | One-click clipboard copy from the reveal dialog |

## Security Design

### Encryption: AES-256-GCM
- **Algorithm**: AES/GCM/NoPadding (authenticated encryption)
- **Key derivation**: PBKDF2WithHmacSHA256, 310 000 iterations, 256-bit key
- **IV**: 12 bytes, cryptographically random, unique per encryption
- **Salt**: 16 bytes, cryptographically random, unique per encryption
- **Storage format**: Base64(IV ∥ salt ∥ ciphertext)

### Password Hashing: PBKDF2
- **Algorithm**: PBKDF2WithHmacSHA256
- **Iterations**: 310 000 (meets OWASP 2023 recommendation)
- **Salt**: 16 bytes, per-user, stored in `users.csv`
- **Comparison**: constant-time to prevent timing attacks

### File Layout
```
data/
  users.csv          ← username, PBKDF2 hash, salt
  alice.csv.enc      ← AES-GCM encrypted vault (Alice's passwords)
  bob.csv.enc        ← AES-GCM encrypted vault (Bob's passwords)
```

### Vault CSV columns (plaintext, in-memory only)
```
title, encryptedPassword, url, notes
```
`encryptedPassword` stores the individual AES-GCM ciphertext (Base64).
The entire file is also AES-GCM encrypted at rest.

## Building

### Requirements
- Java 17+ (JDK)
- JavaFX 11+ (installed via package manager or SDK)

### On Ubuntu/Debian
```bash
sudo apt install openjdk-21-jdk openjfx
javac --module-path /usr/share/maven-repo/org/openjfx/javafx-controls/11:... \
      --add-modules javafx.controls,javafx.fxml \
      -d target/classes \
      $(find src/main/java -name "*.java")
```

### With Maven
```bash
mvn javafx:run
```

### Run the pre-built JAR
```bash
# Linux / macOS
./run.sh

# Windows
run.bat
```

## Technology
- **Language**: Java 17
- **UI**: JavaFX (FXML + CSS)
- **Cryptography**: `javax.crypto` (JDK built-in)
- **Architecture**: MVC (Model / Service / Controller)

## Project Structure
```
src/main/java/com/passmanager/
├── MainApp.java                    # Application entry point
├── Session.java                    # Logged-in user state
├── model/
│   ├── PasswordEntry.java          # Vault entry model
│   └── User.java                   # User credentials model
├── service/
│   ├── FileService.java            # CSV read/write + AES file encryption
│   ├── AuthService.java            # Registration & login (PBKDF2)
│   └── VaultService.java           # CRUD on password entries
├── util/
│   ├── AESUtil.java                # AES-256-GCM encrypt/decrypt
│   ├── PasswordHashUtil.java       # PBKDF2 hash & verify
│   └── PasswordGenerator.java      # Secure random password generator
└── controller/
    ├── LoginController.java        # Login & register screen
    └── VaultController.java        # Main vault screen
```
