package com.passmanager.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility.
 *
 * Key derivation  : PBKDF2WithHmacSHA256 (310 000 iterations, 256-bit key)
 * Cipher          : AES/GCM/NoPadding  (128-bit authentication tag)
 * Storage format  : Base64( IV [12 bytes] || salt [16 bytes] || ciphertext )
 *
 * Using GCM provides authenticated encryption – any tampering with the
 * ciphertext will cause decryption to fail with an AEADBadTagException.
 */
public class AESUtil {

    private static final String  ALGORITHM        = "AES/GCM/NoPadding";
    private static final String  KEY_DERIVATION   = "PBKDF2WithHmacSHA256";
    private static final int     KEY_LENGTH_BITS  = 256;
    private static final int     ITERATION_COUNT  = 310_000;
    private static final int     GCM_IV_LENGTH    = 12;   // bytes
    private static final int     GCM_TAG_LENGTH   = 128;  // bits
    private static final int     SALT_LENGTH      = 16;   // bytes

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Encrypt plaintext with the supplied master password.
     * A fresh IV and salt are generated for every call.
     *
     * @return Base64-encoded string: IV || salt || ciphertext
     */
    public static String encrypt(String plaintext, String masterPassword) throws Exception {
        byte[] iv   = new byte[GCM_IV_LENGTH];
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        SECURE_RANDOM.nextBytes(salt);

        SecretKey key = deriveKey(masterPassword, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Pack: IV || salt || ciphertext
        byte[] packed = new byte[iv.length + salt.length + ciphertext.length];
        System.arraycopy(iv,         0, packed, 0,                     iv.length);
        System.arraycopy(salt,       0, packed, iv.length,             salt.length);
        System.arraycopy(ciphertext, 0, packed, iv.length + salt.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(packed);
    }

    /**
     * Decrypt a Base64-encoded blob produced by {@link #encrypt}.
     */
    public static String decrypt(String encryptedBase64, String masterPassword) throws Exception {
        byte[] packed = Base64.getDecoder().decode(encryptedBase64);

        byte[] iv         = new byte[GCM_IV_LENGTH];
        byte[] salt       = new byte[SALT_LENGTH];
        byte[] ciphertext = new byte[packed.length - iv.length - salt.length];

        System.arraycopy(packed, 0,                      iv,         0, iv.length);
        System.arraycopy(packed, iv.length,              salt,       0, salt.length);
        System.arraycopy(packed, iv.length + salt.length, ciphertext, 0, ciphertext.length);

        SecretKey key = deriveKey(masterPassword, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext, "UTF-8");
    }

    /**
     * Encrypt an entire file's raw bytes (used for the vault CSV file).
     * Returns Base64-encoded IV || salt || ciphertext.
     */
    public static byte[] encryptBytes(byte[] data, String masterPassword) throws Exception {
        byte[] iv   = new byte[GCM_IV_LENGTH];
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        SECURE_RANDOM.nextBytes(salt);

        SecretKey key = deriveKey(masterPassword, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] ciphertext = cipher.doFinal(data);

        byte[] packed = new byte[iv.length + salt.length + ciphertext.length];
        System.arraycopy(iv,         0, packed, 0,                       iv.length);
        System.arraycopy(salt,       0, packed, iv.length,               salt.length);
        System.arraycopy(ciphertext, 0, packed, iv.length + salt.length, ciphertext.length);

        return packed;
    }

    /**
     * Decrypt raw bytes produced by {@link #encryptBytes}.
     */
    public static byte[] decryptBytes(byte[] packed, String masterPassword) throws Exception {
        byte[] iv         = new byte[GCM_IV_LENGTH];
        byte[] salt       = new byte[SALT_LENGTH];
        byte[] ciphertext = new byte[packed.length - iv.length - salt.length];

        System.arraycopy(packed, 0,                       iv,         0, iv.length);
        System.arraycopy(packed, iv.length,               salt,       0, salt.length);
        System.arraycopy(packed, iv.length + salt.length, ciphertext, 0, ciphertext.length);

        SecretKey key = deriveKey(masterPassword, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(ciphertext);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION);
        KeySpec spec = new PBEKeySpec(
                password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
