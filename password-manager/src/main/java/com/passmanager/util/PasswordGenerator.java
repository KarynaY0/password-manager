package com.passmanager.util;

import java.security.SecureRandom;

/**
 * Generates cryptographically secure random passwords.
 */
public class PasswordGenerator {

    private static final String UPPER   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER   = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS  = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate a random password of the requested length using all character classes.
     * Guarantees at least one character from each class.
     */
    public static String generate(int length) {
        if (length < 4) throw new IllegalArgumentException("Minimum length is 4");

        String all = UPPER + LOWER + DIGITS + SYMBOLS;
        StringBuilder sb = new StringBuilder(length);

        // Guarantee one of each class
        sb.append(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        sb.append(SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length())));

        for (int i = 4; i < length; i++) {
            sb.append(all.charAt(RANDOM.nextInt(all.length())));
        }

        // Shuffle to avoid predictable first-four characters
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp;
        }
        return new String(chars);
    }
}
