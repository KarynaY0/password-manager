package com.passmanager.model;

/**
 * Represents a registered user.
 * The passwordHash stores the PBKDF2-hashed master password.
 * The salt is stored as a Base64 string.
 */
public class User {

    private String username;
    private String passwordHash; // PBKDF2WithHmacSHA256
    private String salt;         // Base64-encoded random salt

    public User() {}

    public User(String username, String passwordHash, String salt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public String getUsername()                  { return username; }
    public void   setUsername(String username)   { this.username = username; }

    public String getPasswordHash()                      { return passwordHash; }
    public void   setPasswordHash(String passwordHash)   { this.passwordHash = passwordHash; }

    public String getSalt()               { return salt; }
    public void   setSalt(String salt)    { this.salt = salt; }
}
