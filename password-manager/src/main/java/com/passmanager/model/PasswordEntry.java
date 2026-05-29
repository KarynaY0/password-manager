package com.passmanager.model;

/**
 * Represents a single password entry stored in the vault.
 * The password field holds the AES-encrypted, Base64-encoded ciphertext.
 */
public class PasswordEntry {

    private String title;
    private String encryptedPassword; // AES-encrypted + Base64 encoded
    private String url;
    private String notes;

    public PasswordEntry() {}

    public PasswordEntry(String title, String encryptedPassword, String url, String notes) {
        this.title = title;
        this.encryptedPassword = encryptedPassword;
        this.url = url;
        this.notes = notes;
    }

    // ── Getters & setters ──────────────────────────────────────────────────────

    public String getTitle()                        { return title; }
    public void   setTitle(String title)            { this.title = title; }

    public String getEncryptedPassword()                           { return encryptedPassword; }
    public void   setEncryptedPassword(String encryptedPassword)   { this.encryptedPassword = encryptedPassword; }

    public String getUrl()                    { return url; }
    public void   setUrl(String url)          { this.url = url; }

    public String getNotes()                  { return notes; }
    public void   setNotes(String notes)      { this.notes = notes; }

    @Override
    public String toString() {
        return "PasswordEntry{title='" + title + "', url='" + url + "'}";
    }
}
