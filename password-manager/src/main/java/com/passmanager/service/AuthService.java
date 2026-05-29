package com.passmanager.service;

import com.passmanager.model.User;
import com.passmanager.util.PasswordHashUtil;

import java.util.List;

/**
 * Handles user registration and login.
 */
public class AuthService {

    /**
     * Register a new user.
     *
     * @throws IllegalArgumentException if username already exists
     * @throws Exception                on any I/O or crypto error
     */
    public static User register(String username, String password) throws Exception {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username must not be empty.");
        if (password == null || password.length() < 8)
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        if (FileService.userExists(username))
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");

        String salt = PasswordHashUtil.generateSalt();
        String hash = PasswordHashUtil.hash(password, salt);
        User user = new User(username, hash, salt);
        FileService.saveUser(user);
        return user;
    }

    /**
     * Verify credentials.
     *
     * @return the matching {@link User} or {@code null} if credentials are wrong.
     */
    public static User login(String username, String password) throws Exception {
        List<User> users = FileService.loadUsers();
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                if (PasswordHashUtil.verify(password, u.getSalt(), u.getPasswordHash())) {
                    return u;
                } else {
                    return null; // wrong password
                }
            }
        }
        return null; // user not found
    }
}
