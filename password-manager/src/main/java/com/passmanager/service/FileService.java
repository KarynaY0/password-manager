package com.passmanager.service;

import com.passmanager.model.PasswordEntry;
import com.passmanager.model.User;
import com.passmanager.util.AESUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all file I/O for the password manager.
 *
 * Layout on disk (inside the application data directory):
 *
 *   data/
 *     users.csv          – plain CSV of registered users (username, hash, salt)
 *     <username>.csv.enc – AES-GCM encrypted vault for each user
 *
 * The vault CSV columns are:
 *   title, encryptedPassword, url, notes
 *
 * The vault file is only ever written/read in its encrypted form on disk.
 * The plaintext CSV bytes live in memory only while the app is running.
 */
public class FileService {

    private static final String DATA_DIR    = "data";
    private static final String USERS_FILE  = DATA_DIR + "/users.csv";
    private static final String CSV_HEADER  = "title,encryptedPassword,url,notes";

    // ── Initialisation ─────────────────────────────────────────────────────────

    /** Create the data directory (and users file) on first launch. */
    public static void initDataDirectory() throws IOException {
        Files.createDirectories(Paths.get(DATA_DIR));
        Path usersPath = Paths.get(USERS_FILE);
        if (!Files.exists(usersPath)) {
            Files.writeString(usersPath, "username,passwordHash,salt\n", StandardCharsets.UTF_8);
        }
    }

    // ── User management ────────────────────────────────────────────────────────

    public static List<User> loadUsers() throws IOException {
        List<User> users = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(USERS_FILE), StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) { // skip header
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] parts = splitCsvLine(line);
            if (parts.length >= 3) {
                users.add(new User(parts[0], parts[1], parts[2]));
            }
        }
        return users;
    }

    public static void saveUser(User user) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(USERS_FILE, true))) {
            bw.write(escapeCsv(user.getUsername()) + ","
                    + escapeCsv(user.getPasswordHash()) + ","
                    + escapeCsv(user.getSalt()) + "\n");
        }
    }

    public static boolean userExists(String username) throws IOException {
        return loadUsers().stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
    }

    // ── Vault file (encrypted) ─────────────────────────────────────────────────

    /** Path of the encrypted vault file for a given user. */
    public static String vaultPath(String username) {
        return DATA_DIR + "/" + username + ".csv.enc";
    }

    /**
     * Load and decrypt a user's vault. Returns an empty list if the vault file
     * does not yet exist (first login after registration).
     */
    public static List<PasswordEntry> loadVault(String username, String masterPassword)
            throws Exception {
        Path path = Paths.get(vaultPath(username));
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        byte[] encBytes = Files.readAllBytes(path);
        byte[] csvBytes = AESUtil.decryptBytes(encBytes, masterPassword);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        return parseCsvEntries(csv);
    }

    /**
     * Encrypt and persist a user's vault to disk.
     * This is called on logout and on application close.
     */
    public static void saveVault(String username, String masterPassword,
                                 List<PasswordEntry> entries) throws Exception {
        String csv = buildCsv(entries);
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        byte[] encBytes = AESUtil.encryptBytes(csvBytes, masterPassword);
        Files.write(Paths.get(vaultPath(username)), encBytes,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // ── Disk decrypt / encrypt (for login/logout requirement) ──────────────────

    /**
     * Decrypt the vault and write the plain CSV to disk.
     * Called immediately after a successful login.
     */
    public static void decryptVaultToDisk(String username, String masterPassword) throws Exception {
        Path encPath   = Paths.get(vaultPath(username));
        Path plainPath = Paths.get(DATA_DIR + "/" + username + ".csv");

        if (!Files.exists(encPath)) return; // first login – nothing to decrypt yet

        byte[] encBytes = Files.readAllBytes(encPath);
        byte[] csvBytes = AESUtil.decryptBytes(encBytes, masterPassword);
        Files.write(plainPath, csvBytes,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Read the plain CSV from disk, encrypt it, write the .csv.enc file,
     * then delete the plain file.
     * Called on logout and on application close.
     */
    public static void encryptVaultFromDisk(String username, String masterPassword) throws Exception {
        Path plainPath = Paths.get(DATA_DIR + "/" + username + ".csv");
        Path encPath   = Paths.get(vaultPath(username));

        if (!Files.exists(plainPath)) return; // nothing to encrypt

        byte[] csvBytes = Files.readAllBytes(plainPath);
        byte[] encBytes = AESUtil.encryptBytes(csvBytes, masterPassword);
        Files.write(encPath, encBytes,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        Files.delete(plainPath); // remove plain-text file from disk
    }

    // ── CSV helpers ────────────────────────────────────────────────────────────

    private static List<PasswordEntry> parseCsvEntries(String csv) {
        List<PasswordEntry> entries = new ArrayList<>();
        String[] lines = csv.split("\n");
        for (int i = 1; i < lines.length; i++) { // skip header
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] parts = splitCsvLine(line);
            if (parts.length >= 4) {
                entries.add(new PasswordEntry(parts[0], parts[1], parts[2], parts[3]));
            }
        }
        return entries;
    }

    private static String buildCsv(List<PasswordEntry> entries) {
        StringBuilder sb = new StringBuilder(CSV_HEADER + "\n");
        for (PasswordEntry e : entries) {
            sb.append(escapeCsv(e.getTitle())).append(",")
                    .append(escapeCsv(e.getEncryptedPassword())).append(",")
                    .append(escapeCsv(e.getUrl())).append(",")
                    .append(escapeCsv(e.getNotes())).append("\n");
        }
        return sb.toString();
    }

    /**
     * Minimal RFC-4180-compatible CSV splitter that handles quoted fields.
     */
    private static String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"'); i++; // escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString()); current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    /** Wrap a field in double-quotes and escape internal quotes. */
    static String escapeCsv(String value) {
        if (value == null) value = "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}