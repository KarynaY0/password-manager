package com.passmanager.controller;

import com.passmanager.MainApp;
import com.passmanager.Session;
import com.passmanager.model.User;
import com.passmanager.service.AuthService;
import com.passmanager.service.FileService;
import com.passmanager.service.VaultService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

public class LoginController {

    // Login pane
    @FXML private VBox     loginPane;
    @FXML private TextField     loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label         loginError;

    // Register pane
    @FXML private javafx.scene.layout.VBox registerPane;
    @FXML private TextField     regUsername;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirm;
    @FXML private Label         regError;
    @FXML private Label         regSuccess;

    @FXML private Button btnTabLogin;
    @FXML private Button btnTabRegister;

    @FXML
    public void showLogin() {
        loginPane.setVisible(true);    loginPane.setManaged(true);
        registerPane.setVisible(false); registerPane.setManaged(false);
        btnTabLogin.getStyleClass().add("tab-active");
        btnTabRegister.getStyleClass().remove("tab-active");
        clearMessages();
    }

    @FXML
    public void showRegister() {
        loginPane.setVisible(false);    loginPane.setManaged(false);
        registerPane.setVisible(true);  registerPane.setManaged(true);
        btnTabRegister.getStyleClass().add("tab-active");
        btnTabLogin.getStyleClass().remove("tab-active");
        clearMessages();
    }

    @FXML
    public void handleLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            setError(loginError, "Please enter username and password.");
            return;
        }

        try {
            User user = AuthService.login(username, password);
            if (user == null) {
                setError(loginError, "Invalid username or password.");
                return;
            }
            // Decrypt vault
            var entries = FileService.loadVault(username, password);
            VaultService vault = new VaultService(username, password, entries);
            Session.get().login(username, password, vault);

            MainApp.loadScene("vault");
        } catch (Exception ex) {
            setError(loginError, "Login error: " + ex.getMessage());
        }
    }

    @FXML
    public void handleRegister() {
        String username = regUsername.getText().trim();
        String password = regPassword.getText();
        String confirm  = regConfirm.getText();

        clearMessages();

        if (!password.equals(confirm)) {
            setError(regError, "Passwords do not match.");
            return;
        }

        try {
            AuthService.register(username, password);
            regSuccess.setText("Account created! You can now log in.");
            regSuccess.setVisible(true);
            regUsername.clear(); regPassword.clear(); regConfirm.clear();
        } catch (IllegalArgumentException ex) {
            setError(regError, ex.getMessage());
        } catch (Exception ex) {
            setError(regError, "Registration error: " + ex.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
    }

    private void clearMessages() {
        loginError.setVisible(false);
        regError.setVisible(false);
        regSuccess.setVisible(false);
    }
}
