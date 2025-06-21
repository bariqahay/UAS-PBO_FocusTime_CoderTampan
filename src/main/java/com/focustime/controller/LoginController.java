package com.focustime.controller;

import com.focustime.model.UserModel;
import com.focustime.service.Authenticator;
import com.focustime.service.AuthService;
import com.focustime.session.CurrentUser;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController extends BaseController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private Authenticator authenticator;

    public LoginController() {
        this.authenticator = new AuthService(); // default
    }

    // Supaya bisa disuntik mock service untuk testing
    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Login Gagal", "Username dan password tidak boleh kosong");
            return;
        }

        UserModel user = authenticator.login(username, password);

        if (user != null) {
            CurrentUser.login(user);
            switchScene("timer.fxml", "Focus Timer", "timer.css", usernameField);
        } else {
            showError("Login Gagal", "Username atau password salah");
        }
    }

    @FXML
    private void goToSignup() {
        switchScene("signup.fxml", "Daftar Akun", "signup.css", usernameField);
    }
}
