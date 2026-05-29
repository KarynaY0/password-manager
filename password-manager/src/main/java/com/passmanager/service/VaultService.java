package com.passmanager.service;

import com.passmanager.model.PasswordEntry;
import com.passmanager.util.AESUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In-memory vault operations.
 * The vault is loaded on login and flushed (encrypted) on logout / app close.
 */
public class VaultService {

    private final List<PasswordEntry> entries;
    private final String username;
    private final String masterPassword; // kept in memory for re-encryption

    public VaultService(String username, String masterPassword,
                        List<PasswordEntry> entries) {
        this.username       = username;
        this.masterPassword = masterPassword;
        this.entries        = new ArrayList<>(entries);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * Add a new entry. The plaintext password is AES-encrypted before storage.
     *
     * @throws IllegalArgumentException if an entry with the same title already exists.
     */
    public void addEntry(String title, String plaintextPassword,
                         String url, String notes) throws Exception {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Title must not be empty.");
        if (findByTitle(title) != null)
            throw new IllegalArgumentException("An entry with that title already exists.");

        String encrypted = AESUtil.encrypt(plaintextPassword, masterPassword);
        entries.add(new PasswordEntry(title.trim(), encrypted,
                url   != null ? url.trim()   : "",
                notes != null ? notes.trim() : ""));
    }

    /**
     * Search entries by title (case-insensitive, substring match).
     */
    public List<PasswordEntry> search(String query) {
        if (query == null || query.isBlank()) return new ArrayList<>(entries);
        String q = query.toLowerCase();
        return entries.stream()
                .filter(e -> e.getTitle().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    /**
     * Decrypt and return the password for an entry (on-demand reveal).
     */
    public String revealPassword(String title) throws Exception {
        PasswordEntry e = findByTitle(title);
        if (e == null) throw new IllegalArgumentException("Entry not found: " + title);
        return AESUtil.decrypt(e.getEncryptedPassword(), masterPassword);
    }

    /**
     * Update an existing entry. Provide null/empty to leave a field unchanged.
     */
    public void updateEntry(String title, String newPlaintextPassword,
                            String newUrl, String newNotes) throws Exception {
        PasswordEntry e = findByTitle(title);
        if (e == null) throw new IllegalArgumentException("Entry not found: " + title);

        if (newPlaintextPassword != null && !newPlaintextPassword.isBlank()) {
            e.setEncryptedPassword(AESUtil.encrypt(newPlaintextPassword, masterPassword));
        }
        if (newUrl != null)   e.setUrl(newUrl.trim());
        if (newNotes != null) e.setNotes(newNotes.trim());
    }

    /**
     * Delete an entry by title (case-insensitive exact match).
     *
     * @return true if an entry was found and removed.
     */
    public boolean deleteEntry(String title) {
        return entries.removeIf(
                e -> e.getTitle().equalsIgnoreCase(title.trim()));
    }

    /** Return all entries. */
    public List<PasswordEntry> getAllEntries() {
        return new ArrayList<>(entries);
    }

    /**
     * Persist (encrypt) the vault to disk.
     * Called on logout and on application close.
     */
    public void saveVault() throws Exception {
        FileService.saveVault(username, masterPassword, entries);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private PasswordEntry findByTitle(String title) {
        return entries.stream()
                .filter(e -> e.getTitle().equalsIgnoreCase(title.trim()))
                .findFirst()
                .orElse(null);
    }
}
