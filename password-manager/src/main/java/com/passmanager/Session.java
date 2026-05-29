package com.passmanager;

import com.passmanager.service.VaultService;

/**
 * Singleton session – holds the currently logged-in user's context.
 */
public class Session {

    private static Session instance;

    private String       username;
    private String       masterPassword;
    private VaultService vaultService;

    private Session() {}

    public static Session get() {
        if (instance == null) instance = new Session();
        return instance;
    }

    public void login(String username, String masterPassword, VaultService vault) {
        this.username       = username;
        this.masterPassword = masterPassword;
        this.vaultService   = vault;
    }

    public void clear() {
        username       = null;
        masterPassword = null;
        vaultService   = null;
    }

    public String       getUsername()       { return username; }
    public String       getMasterPassword() { return masterPassword; }
    public VaultService getVaultService()   { return vaultService; }
    public boolean      isLoggedIn()        { return username != null; }
}
