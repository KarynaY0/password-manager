package com.passmanager.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Hashes user master-passwords with PBKDF2WithHmacSHA256.
 *
 * Parameters
 * ----------
 * Algorithm  : PBKDF2WithHmacSHA256
 * Iterations : 310 000  (OWASP 2023 minimum recommendation)
 * Key length : 256 bits
 * Salt       : 16 bytes, cryptographically random, unique per user
 */
public class PasswordHashUtil {

    private static final String ALGORITHM       = "PBKDF2WithHmacSHA256";
    private static final int    ITERATION_COUNT = 310_000;
    private static final int    KEY_LENGTH_BITS = 256;
    private static final int    SALT_BYTES      = 16;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Generate a new random salt (Base64-encoded). */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hash a plaintext password with the supplied Base64-encoded salt.
     *
     * @return Base64-encoded hash
     */
    public static String hash(String password, String saltBase64) throws Exception {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        KeySpec spec = new PBEKeySpec(
                password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS);
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Constant-time comparison of two Base64 hashes to prevent timing attacks.
     */
    public static boolean verify(String inputPassword, String saltBase64, String storedHash)
            throws Exception {
        String inputHash = hash(inputPassword, saltBase64);
        // constant-time compare
        byte[] a = Base64.getDecoder().decode(inputHash);
        byte[] b = Base64.getDecoder().decode(storedHash);
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= (a[i] ^ b[i]);
        return diff == 0;
    }
}
